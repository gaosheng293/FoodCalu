package com.example.foodcalu;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private AppDao dao;

    // UI 控件
    private TextView tvDate;
    private TextView tvTotalCalories, tvTotalCarbs, tvTotalProtein, tvTotalFat;

    // 四个餐别的容器
    private LinearLayout llBreakfastItems, llLunchItems, llDinnerItems, llSnackItems;

    private String currentSelectedDate;
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDatabase(this);
        dao = db.appDao();
//        checkAndInitFoodData();

        initViews();

        currentSelectedDate = getTodayDate();
        updateDateDisplay();

        loadDataForDate(currentSelectedDate);

        // 点击日期
        tvDate.setOnClickListener(v -> showDatePicker());

        // --- 绑定 4 个添加按钮的点击事件 ---
        // 0=早, 1=中, 2=晚, 3=加
// 绑定 4 个标题头的点击事件
        findViewById(R.id.headerBreakfast).setOnClickListener(v -> openAddPage(0));
        findViewById(R.id.headerLunch).setOnClickListener(v -> openAddPage(1));
        findViewById(R.id.headerDinner).setOnClickListener(v -> openAddPage(2));
        findViewById(R.id.headerSnack).setOnClickListener(v -> openAddPage(3));

        findViewById(R.id.fabCreateFood).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FoodListActivity.class);
            startActivity(intent);
        });
    }

    private void openAddPage(int mealType) {
        Intent intent = new Intent(MainActivity.this, AddRecordActivity.class);
        intent.putExtra("DATE_KEY", currentSelectedDate);
        intent.putExtra("MEAL_TYPE", mealType); // 告诉下一个页面是哪顿饭
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentSelectedDate != null) {
            loadDataForDate(currentSelectedDate);
        }
    }

    // --- 核心逻辑：加载数据并填充到4个板块 ---
    private void loadDataForDate(String date) {
        List<Record> records = dao.getRecordsByDate(date);

        // 1. 清空旧数据（防止重复添加）
        llBreakfastItems.removeAllViews();
        llLunchItems.removeAllViews();
        llDinnerItems.removeAllViews();
        llSnackItems.removeAllViews();

        double totalCal = 0, totalCarb = 0, totalPro = 0, totalFat = 0;

        // 2. 遍历记录，生成 View 并塞入对应的容器
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Record record : records) {
            Food food = dao.getFoodById(record.foodId);
            if (food == null) continue;

            // 计算数值
            double ratio = record.weight / 100.0;
            double cal = food.calories * ratio;
            totalCal += cal;
            totalCarb += food.carbs * ratio;
            totalPro += food.protein * ratio;
            totalFat += food.fat * ratio;

            // 生成卡片 View
            View itemView = inflater.inflate(R.layout.item_food_record, null);

            // 填入数据
            TextView tvName = itemView.findViewById(R.id.tvFoodName);
            TextView tvWeight = itemView.findViewById(R.id.tvFoodWeight);
            TextView tvCal = itemView.findViewById(R.id.tvItemCalories);
            TextView tvTag = itemView.findViewById(R.id.tvMealType);


            tvName.setText(food.name);
            tvWeight.setText(String.format("%.0f 克", record.weight));
            tvCal.setText(String.format("%.0f", cal));
            tvTag.setVisibility(View.GONE); // 在分类板块里，不需要再显示“早餐”标签了，隐藏掉更清爽

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // 弹出确认对话框
                    new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("删除记录")
                            .setMessage("确定要删除这条 " + food.name + " 吗？")
                            .setPositiveButton("删除", (dialog, which) -> {
                                // 1. 数据库删除
                                dao.deleteRecord(record);

                                // 2. 界面刷新 (重新加载当前日期数据)
                                loadDataForDate(currentSelectedDate);

                                Toast.makeText(MainActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("取消", null) // 点取消啥也不干
                            .show();

                    return true; // 返回 true 表示“我处理了这个事件”，防止触发普通的点击事件
                }
            });

            // 决定塞进哪个容器
            switch (record.mealType) {
                case 0: llBreakfastItems.addView(itemView); break;
                case 1: llLunchItems.addView(itemView); break;
                case 2: llDinnerItems.addView(itemView); break;
                case 3: llSnackItems.addView(itemView); break;
            }
        }

        // 3. 更新顶部总数
        tvTotalCalories.setText(String.format(Locale.getDefault(), "%.0f", totalCal));
        tvTotalCarbs.setText(String.format(Locale.getDefault(), "碳水 %.1fg", totalCarb));
        tvTotalProtein.setText(String.format(Locale.getDefault(), "蛋白 %.1fg", totalPro));
        tvTotalFat.setText(String.format(Locale.getDefault(), "脂肪 %.1fg", totalFat));
    }

    private void initViews() {
        tvDate = findViewById(R.id.tvDate);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvTotalCarbs = findViewById(R.id.tvTotalCarbs);
        tvTotalProtein = findViewById(R.id.tvTotalProtein);
        tvTotalFat = findViewById(R.id.tvTotalFat);

        // 绑定新的4个容器
        llBreakfastItems = findViewById(R.id.llBreakfastItems);
        llLunchItems = findViewById(R.id.llLunchItems);
        llDinnerItems = findViewById(R.id.llDinnerItems);
        llSnackItems = findViewById(R.id.llSnackItems);
    }

    // ... 日期选择、初始化数据等辅助方法保持不变 (复制原来的即可) ...
    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            currentSelectedDate = sdf.format(calendar.getTime());
            updateDateDisplay();
            loadDataForDate(currentSelectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateDisplay() {
        if (currentSelectedDate.equals(getTodayDate())) {
            tvDate.setText(currentSelectedDate + " (今天) ▼");
        } else {
            tvDate.setText(currentSelectedDate + " ▼");
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void checkAndInitFoodData() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("isFirstRun", true);
        if (isFirstRun) {
            // === 主食类 (碳水大户) ===
            dao.insertFood(new Food("米饭(熟)", 116, 2.6, 0.3, 25.9));  // 煮熟的米饭，含水量高
            dao.insertFood(new Food("大米(生)", 346, 7.9, 0.9, 77.2));  // 生米，热量高
            dao.insertFood(new Food("红薯", 86, 1.6, 0.2, 20.1));   // 优质碳水
            dao.insertFood(new Food("紫薯", 106, 1.5, 0.2, 25.0));
            dao.insertFood(new Food("土豆", 81, 2.6, 0.2, 17.8));
            dao.insertFood(new Food("燕麦片", 377, 15.0, 6.7, 66.0));
            dao.insertFood(new Food("全麦面包", 246, 8.5, 3.5, 46.0));
            dao.insertFood(new Food("馒头", 223, 7.0, 1.1, 47.0));
            dao.insertFood(new Food("玉米", 112, 4.0, 1.2, 22.8));

// === 肉蛋奶 (蛋白质大户) ===
            dao.insertFood(new Food("鸡胸肉", 118, 24.6, 1.9, 0.6)); // 健身神肉
            dao.insertFood(new Food("瘦牛肉", 106, 20.2, 2.3, 0.0)); // 这里的热量取决于肥瘦，取比较瘦的均值
            dao.insertFood(new Food("基围虾", 93, 18.2, 1.1, 0.0));
            dao.insertFood(new Food("鸡蛋", 143, 12.6, 9.5, 0.7)); // 100g大约是2个鸡蛋
            dao.insertFood(new Food("鸡蛋白", 60, 11.6, 0.1, 0.8)); // 纯蛋白
            dao.insertFood(new Food("全脂牛奶", 54, 3.0, 3.2, 3.4));     // 100ml
            dao.insertFood(new Food("脱脂牛奶", 33, 3.2, 0.1, 4.6));
            dao.insertFood(new Food("三文鱼", 139, 17.2, 7.8, 0.0));
            dao.insertFood(new Food("猪瘦肉", 143, 20.3, 6.2, 1.5));

// === 蔬菜 (低卡，补充微量元素) ===
            dao.insertFood(new Food("大白菜", 17, 1.5, 0.1, 3.2));
            dao.insertFood(new Food("圆白菜/卷心菜", 24, 1.5, 0.2, 3.6));
            dao.insertFood(new Food("青椒", 22, 1.0, 0.2, 5.0));
            dao.insertFood(new Food("西蓝花", 34, 4.1, 0.6, 4.3));   // 健身常备
            dao.insertFood(new Food("黄瓜", 16, 0.8, 0.2, 2.9));
            dao.insertFood(new Food("西红柿", 18, 0.9, 0.2, 3.5));
            dao.insertFood(new Food("生菜", 15, 1.4, 0.4, 2.1));
            dao.insertFood(new Food("胡萝卜", 39, 1.0, 0.2, 8.8));

// === 水果 ===
            dao.insertFood(new Food("香蕉", 93, 1.4, 0.2, 22.0));   // 热量较高
            dao.insertFood(new Food("苹果", 52, 0.2, 0.2, 13.5));
            dao.insertFood(new Food("橙子", 47, 0.8, 0.2, 10.5));
            dao.insertFood(new Food("西瓜", 31, 0.6, 0.1, 6.8));    // 主要是水

// === 油脂与调味 (热量炸弹) ===
            dao.insertFood(new Food("菜籽油", 899, 0.0, 99.9, 0.0)); // 纯脂肪
            dao.insertFood(new Food("橄榄油", 899, 0.0, 99.9, 0.0));
            dao.insertFood(new Food("花生酱", 598, 24.0, 50.0, 21.0));
            dao.insertFood(new Food("混合坚果", 617, 16.0, 54.0, 19.0));
            prefs.edit().putBoolean("isFirstRun", false).apply();
        }
    }
}