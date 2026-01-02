package com.example.foodcalu;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private AppDao dao;

    // UI 控件
    private TextView tvDate;
    private ImageView ivSettings;
    private ImageView ivShare;
    private ImageView ivFoodLibrary;

    // 仪表盘控件
    private CircularProgressIndicator progressCalorie;
    private LinearProgressIndicator progressCarbs, progressProtein, progressFat;
    private TextView tvCalorieLeft, tvTotalEaten, tvBudget;
    private TextView tvCarbsVal, tvProteinVal, tvFatVal;

    private TextView tvBreakfastCal, tvLunchCal, tvDinnerCal, tvSnackCal;
    private LinearLayout llBreakfastItems, llLunchItems, llDinnerItems, llSnackItems;
    private Button btnNavBreakfast, btnNavLunch, btnNavDinner, btnNavSnack;

    private String currentSelectedDate;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // 动态目标变量
    private int dailyTargetCalories = 0;
    private int dailyTargetCarbs = 0;
    private int dailyTargetProtein = 0;
    private int dailyTargetFat = 0;

    // 循环规则定义
    private final double[] CARB_MULTIPLIERS = {1.0, 1.0, 2.0, 1.0, 1.0, 3.0};
    private final double[] FAT_MULTIPLIERS  = {1.0, 1.0, 0.5, 1.0, 1.0, 0.2};
    private final double PROTEIN_MULTIPLIER = 1.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDatabase(this);
        dao = db.appDao();

        initViews();
        setupListeners();

        currentSelectedDate = sdf.format(new Date());
        tvDate.setText(currentSelectedDate);

        loadDataForDate(currentSelectedDate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentSelectedDate != null) {
            loadDataForDate(currentSelectedDate);
        }
    }

    private void initViews() {
        tvDate = findViewById(R.id.tvDate);
        ivSettings = findViewById(R.id.ivSettings);
        ivShare = findViewById(R.id.ivShare);
        ivFoodLibrary = findViewById(R.id.ivFoodLibrary);

        progressCalorie = findViewById(R.id.progressCalorie);
        tvCalorieLeft = findViewById(R.id.tvCalorieLeft);
        tvTotalEaten = findViewById(R.id.tvTotalEaten);
        tvBudget = findViewById(R.id.tvBudget);

        progressCarbs = findViewById(R.id.progressCarbs);
        tvCarbsVal = findViewById(R.id.tvCarbsVal);
        progressProtein = findViewById(R.id.progressProtein);
        tvProteinVal = findViewById(R.id.tvProteinVal);
        progressFat = findViewById(R.id.progressFat);
        tvFatVal = findViewById(R.id.tvFatVal);

        llBreakfastItems = findViewById(R.id.llBreakfastItems);
        llLunchItems = findViewById(R.id.llLunchItems);
        llDinnerItems = findViewById(R.id.llDinnerItems);
        llSnackItems = findViewById(R.id.llSnackItems);

        tvBreakfastCal = findViewById(R.id.tvBreakfastCal);
        tvLunchCal = findViewById(R.id.tvLunchCal);
        tvDinnerCal = findViewById(R.id.tvDinnerCal);
        tvSnackCal = findViewById(R.id.tvSnackCal);

        btnNavBreakfast = findViewById(R.id.btnNavBreakfast);
        btnNavLunch = findViewById(R.id.btnNavLunch);
        btnNavDinner = findViewById(R.id.btnNavDinner);
        btnNavSnack = findViewById(R.id.btnNavSnack);
    }

    private void setupListeners() {
        tvDate.setOnClickListener(v -> showDatePicker());
        ivSettings.setOnClickListener(v -> showUserInfoDialog());
        ImageView ivShare = findViewById(R.id.ivShare); // 别忘了绑定控件
        if (ivShare != null) {
            ivShare.setOnClickListener(v -> showExportDialog());
        }

        ivFoodLibrary.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FoodListActivity.class);
            startActivity(intent);
        });

        View.OnClickListener navListener = v -> {
            int mealType = 0;
            if (v.getId() == R.id.btnNavBreakfast) mealType = 0;
            else if (v.getId() == R.id.btnNavLunch) mealType = 1;
            else if (v.getId() == R.id.btnNavDinner) mealType = 2;
            else if (v.getId() == R.id.btnNavSnack) mealType = 3;
            navigateToMealDetail(mealType);
        };
        btnNavBreakfast.setOnClickListener(navListener);
        btnNavLunch.setOnClickListener(navListener);
        btnNavDinner.setOnClickListener(navListener);
        btnNavSnack.setOnClickListener(navListener);

        tvBreakfastCal.setOnClickListener(v -> navigateToMealDetail(0));
        tvLunchCal.setOnClickListener(v -> navigateToMealDetail(1));
        tvDinnerCal.setOnClickListener(v -> navigateToMealDetail(2));
        tvSnackCal.setOnClickListener(v -> navigateToMealDetail(3));
    }

    // 新增：导出选择弹窗
    private void showExportDialog() {
        // 选项列表
        String[] options = {
                "导出今日明细 (小票图)",
                "导出本周明细 (7天长图)",
                "复制本月全部记录 (纯文本)"
        };

        new AlertDialog.Builder(this)
                .setTitle("选择导出方式")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 1. 今日明细
                        Intent intent = new Intent(MainActivity.this, ExportActivity.class);
                        intent.putExtra("START_DATE", currentSelectedDate);
                        intent.putExtra("END_DATE", currentSelectedDate);
                        startActivity(intent);

                    } else if (which == 1) {
                        // 2. 本周明细 (算出本周一和周日)
                        String[] weekRange = getWeekDateRange(currentSelectedDate);
                        Intent intent = new Intent(MainActivity.this, ExportActivity.class);
                        intent.putExtra("START_DATE", weekRange[0]);
                        intent.putExtra("END_DATE", weekRange[1]);
                        startActivity(intent);

                    } else {
                        // 3. 复制本月文本 (Copy to Clipboard)
                        copyMonthTextToClipboard(currentSelectedDate);
                    }
                })
                .show();
    }

    // 辅助：获取本周一和周日的日期字符串
    private String[] getWeekDateRange(String dateStr) {
        try {
            Date date = sdf.parse(dateStr);
            Calendar c = Calendar.getInstance();
            c.setTime(date);

            // 将第一天设为周一 (中国习惯)
            c.setFirstDayOfWeek(Calendar.MONDAY);

            // 设为本周一
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            String start = sdf.format(c.getTime());

            // 设为本周日
            c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            String end = sdf.format(c.getTime());

            return new String[]{start, end};
        } catch (Exception e) {
            return new String[]{dateStr, dateStr};
        }
    }

    // 辅助：生成文本并复制
    private void copyMonthTextToClipboard(String dateStr) {
        String monthStr = dateStr.substring(0, 7); // "2023-10"
        new Thread(() -> {
            List<Record> records = dao.getRecordsByMonth(monthStr + "%");
            StringBuilder sb = new StringBuilder();
            sb.append("【").append(monthStr).append(" 饮食记录】\n");

            String lastDate = "";
            String[] mealNames = {"早餐", "午餐", "晚餐", "加餐"};

            for (Record r : records) {
                if (!r.date.equals(lastDate)) {
                    sb.append("\n📅 ").append(r.date).append("\n");
                    lastDate = r.date;
                }
                Food f = dao.getFoodById(r.foodId);
                if (f != null) {
                    sb.append("- [").append(mealNames[r.mealType]).append("] ")
                            .append(f.name).append(" ")
                            .append((int)r.weight).append("g  ")
                            .append((int)(f.calories * r.weight / 100)).append("kcal\n");
                }
            }

            runOnUiThread(() -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Food Records", sb.toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "本月记录已复制到剪贴板！可直接粘贴分享", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void navigateToMealDetail(int mealType) {
        Intent intent = new Intent(MainActivity.this, MealDetailActivity.class);
        intent.putExtra("MEAL_TYPE", mealType);
        intent.putExtra("DATE_KEY", currentSelectedDate);
        startActivity(intent);
    }

    private int getCycleIndexForDate(String dateStr) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String startDateStr = prefs.getString("CYCLE_START_DATE", sdf.format(new Date()));

        long daysDiff = 0;
        try {
            Date current = sdf.parse(dateStr);
            Date start = sdf.parse(startDateStr);
            if (current != null && start != null) {
                long diffInMillis = current.getTime() - start.getTime();
                daysDiff = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (daysDiff < 0) {
            daysDiff = (daysDiff % 6 + 6) % 6;
        }
        return (int) (daysDiff % 6);
    }

    private void calculateTargetsForDate(String dateStr) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        float weight = prefs.getFloat("USER_WEIGHT", 60f);

        int cycleIndex = getCycleIndexForDate(dateStr);

        double carbs = weight * CARB_MULTIPLIERS[cycleIndex];
        double fat = weight * FAT_MULTIPLIERS[cycleIndex];
        double protein = weight * PROTEIN_MULTIPLIER;
        double calories = (carbs * 4) + (protein * 4) + (fat * 9);

        dailyTargetCalories = (int) calories;
        dailyTargetCarbs = (int) carbs;
        dailyTargetProtein = (int) protein;
        dailyTargetFat = (int) fat;

        tvBudget.setText("推荐预算 " + dailyTargetCalories + " (Day " + (cycleIndex + 1) + ")");
    }

    private void updateProgressMaxValues() {
        progressCalorie.setMax(dailyTargetCalories);
        progressCarbs.setMax(dailyTargetCarbs);
        progressProtein.setMax(dailyTargetProtein);
        progressFat.setMax(dailyTargetFat);
    }

    private void showUserInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_user_info, null);
        EditText etWeight = view.findViewById(R.id.etWeight);
        EditText etHeight = view.findViewById(R.id.etHeight);
        EditText etAge = view.findViewById(R.id.etAge);
        RadioButton rbMale = view.findViewById(R.id.rbMale);

        RadioGroup rgCycleDay = view.findViewById(R.id.rgCycleDay);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        etWeight.setText(String.valueOf(prefs.getFloat("USER_WEIGHT", 60)));
        etHeight.setText(String.valueOf(prefs.getInt("USER_HEIGHT", 170)));
        etAge.setText(String.valueOf(prefs.getInt("USER_AGE", 25)));
        boolean isMale = prefs.getBoolean("USER_IS_MALE", true);
        if (isMale) rbMale.setChecked(true); else ((RadioButton)view.findViewById(R.id.rbFemale)).setChecked(true);

        String today = sdf.format(new Date());
        int currentDayIndex = getCycleIndexForDate(today);

        switch (currentDayIndex) {
            case 0: rgCycleDay.check(R.id.rbDay1); break;
            case 1: rgCycleDay.check(R.id.rbDay2); break;
            case 2: rgCycleDay.check(R.id.rbDay3); break;
            case 3: rgCycleDay.check(R.id.rbDay4); break;
            case 4: rgCycleDay.check(R.id.rbDay5); break;
            case 5: rgCycleDay.check(R.id.rbDay6); break;
        }

        builder.setView(view)
                .setTitle("设置与循环")
                .setPositiveButton("保存并校准", (dialog, which) -> {
                    String wStr = etWeight.getText().toString();
                    if (!TextUtils.isEmpty(wStr)) {
                        int selectedIndex = 0;
                        int checkedId = rgCycleDay.getCheckedRadioButtonId();
                        if (checkedId == R.id.rbDay1) selectedIndex = 0;
                        else if (checkedId == R.id.rbDay2) selectedIndex = 1;
                        else if (checkedId == R.id.rbDay3) selectedIndex = 2;
                        else if (checkedId == R.id.rbDay4) selectedIndex = 3;
                        else if (checkedId == R.id.rbDay5) selectedIndex = 4;
                        else if (checkedId == R.id.rbDay6) selectedIndex = 5;

                        saveUserData(Float.parseFloat(wStr), rbMale.isChecked(), selectedIndex);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveUserData(float weight, boolean isMale, int selectedDayIndex) {
        SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit();
        editor.putFloat("USER_WEIGHT", weight);
        editor.putBoolean("USER_IS_MALE", isMale);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -selectedDayIndex);
        String newStartDate = sdf.format(c.getTime());

        editor.putString("CYCLE_START_DATE", newStartDate);
        editor.apply();

        Toast.makeText(this, "已校准，今天是 Day " + (selectedDayIndex + 1), Toast.LENGTH_SHORT).show();
        loadDataForDate(currentSelectedDate);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            currentSelectedDate = sdf.format(calendar.getTime());
            tvDate.setText(currentSelectedDate);
            loadDataForDate(currentSelectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void loadDataForDate(String date) {
        calculateTargetsForDate(date);
        updateProgressMaxValues();

        List<Record> records = dao.getRecordsByDate(date);
        updateUI(records);
    }

    private void updateUI(List<Record> records) {
        double totalCal = 0, totalCarbs = 0, totalProtein = 0, totalFat = 0;
        double breakCal = 0, lunchCal = 0, dinnerCal = 0, snackCal = 0;

        llBreakfastItems.removeAllViews();
        llLunchItems.removeAllViews();
        llDinnerItems.removeAllViews();
        llSnackItems.removeAllViews();

        for (Record r : records) {
            Food food = dao.getFoodById(r.foodId);
            if (food != null) {
                double ratio = r.weight / 100.0;
                double itemCal = food.calories * ratio;
                totalCal += itemCal;
                totalCarbs += (food.carbs * ratio);
                totalProtein += (food.protein * ratio);
                totalFat += (food.fat * ratio);

                switch (r.mealType) {
                    case 0: breakCal += itemCal; break;
                    case 1: lunchCal += itemCal; break;
                    case 2: dinnerCal += itemCal; break;
                    case 3: snackCal += itemCal; break;
                }
                addRecordView(r, food, itemCal);
            }
        }

        int eaten = (int) totalCal;
        int remaining = dailyTargetCalories - eaten;
        if (remaining < 0) remaining = 0;

        tvTotalEaten.setText(String.valueOf(eaten));
        progressCalorie.setProgress(remaining);
        tvCalorieLeft.setText(String.valueOf(remaining));

        progressCarbs.setProgress((int) totalCarbs);
        tvCarbsVal.setText(String.format("%.0f/%d克", totalCarbs, dailyTargetCarbs));
        progressProtein.setProgress((int) totalProtein);
        tvProteinVal.setText(String.format("%.0f/%d克", totalProtein, dailyTargetProtein));
        progressFat.setProgress((int) totalFat);
        tvFatVal.setText(String.format("%.0f/%d克", totalFat, dailyTargetFat));

        tvBreakfastCal.setText((int)breakCal + " 千卡 >");
        tvLunchCal.setText((int)lunchCal + " 千卡 >");
        tvDinnerCal.setText((int)dinnerCal + " 千卡 >");
        tvSnackCal.setText((int)snackCal + " 千卡 >");
    }

    private void addRecordView(Record r, Food food, double itemCal) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_record, null);
        TextView tvName = itemView.findViewById(R.id.tvFoodName);
        TextView tvWeight = itemView.findViewById(R.id.tvFoodWeight);
        TextView tvCal = itemView.findViewById(R.id.tvItemCalories);
        TextView tvMacros = itemView.findViewById(R.id.tvMacros); // 新增绑定

        TextView tvType = itemView.findViewById(R.id.tvMealType);
        if (tvType != null) tvType.setVisibility(View.GONE);

        tvName.setText(food.name);
        tvWeight.setText((int)r.weight + "克");
        tvCal.setText(String.format("%.0f 千卡", itemCal));

        // 👇 计算并显示具体营养素 👇
        double ratio = r.weight / 100.0;
        double c = food.carbs * ratio;
        double p = food.protein * ratio;
        double f = food.fat * ratio;
        tvMacros.setText(String.format("碳%.1f 蛋%.1f 脂%.1f", c, p, f));

        itemView.setOnClickListener(v -> showBeautifulEditDialog(r));
        itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除记录")
                    .setMessage("确定要删除 “" + food.name + "” 吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        dao.deleteRecord(r);
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        loadDataForDate(currentSelectedDate);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        switch (r.mealType) {
            case 0: llBreakfastItems.addView(itemView); break;
            case 1: llLunchItems.addView(itemView); break;
            case 2: llDinnerItems.addView(itemView); break;
            case 3: llSnackItems.addView(itemView); break;
            default: llSnackItems.addView(itemView); break;
        }
    }

    private void showBeautifulEditDialog(Record record) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_weight, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etWeight = view.findViewById(R.id.etDialogWeight);
        Button btnCancel = view.findViewById(R.id.btnDialogCancel);
        Button btnSave = view.findViewById(R.id.btnDialogSave);

        etWeight.setText(String.valueOf(record.weight));
        etWeight.setSelection(etWeight.getText().length());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newWeightStr = etWeight.getText().toString();
            if (!TextUtils.isEmpty(newWeightStr)) {
                double newWeight = Double.parseDouble(newWeightStr);
                // 👇👇👇 新增：禁止输入0或负数 👇👇👇
                if (newWeight <= 0) {
                    Toast.makeText(this, "重量必须大于 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                record.weight = newWeight;
                dao.updateRecord(record);
                Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
                loadDataForDate(currentSelectedDate);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "请输入重量", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}