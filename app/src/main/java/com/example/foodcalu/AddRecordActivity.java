package com.example.foodcalu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AddRecordActivity extends AppCompatActivity {

    private AppDatabase db;
    private AppDao dao;

    private TextView tvTitle;
    private ImageView ivBack;
    private AutoCompleteTextView actvFood;
    private EditText etWeight;
    private MaterialButton btnSave;

    private String targetDate;
    private int targetMealType;
    private List<Food> foodList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_record);

        targetDate = getIntent().getStringExtra("DATE_KEY");
        targetMealType = getIntent().getIntExtra("MEAL_TYPE", 0);
        if (targetDate == null) targetDate = "2023-01-01";

        db = AppDatabase.getDatabase(this);
        dao = db.appDao();

        initViews();

        String[] titles = {"记录早餐", "记录午餐", "记录晚餐", "记录加餐"};
        tvTitle.setText(titles[targetMealType]);

        initFoodSearch();

        ivBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveRecord());

        actvFood.requestFocus();
        findViewById(R.id.btnImportSet).setOnClickListener(v -> showImportSetDialog());


    }

    // 显示套餐列表弹窗
    private void showImportSetDialog() {
        List<MealSet> sets = dao.getAllMealSets();

        // 准备选项：先把套餐名放进去
        String[] names = new String[sets.size()];
        for (int i = 0; i < sets.size(); i++) {
            names[i] = sets.get(i).name;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择套餐导入");

        // 如果没有套餐，就显示“去管理”
        if (sets.isEmpty()) {
            builder.setMessage("暂无套餐");
        } else {
            builder.setItems(names, (dialog, which) -> {
                importSetToRecord(sets.get(which));
            });
        }

        // 👇👇👇 新增：中性按钮“管理套餐” 👇👇👇
        builder.setNeutralButton("管理套餐", (dialog, which) -> {
            startActivity(new Intent(this, MealSetManagerActivity.class));
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 核心逻辑：导入套餐
    private void importSetToRecord(MealSet set) {
        new Thread(() -> {
            // 1. 查出这个套餐里包含哪些食物
            List<MealSetItem> items = dao.getMealSetItems(set.id);

            // 2. 循环插入到今天的记录表 Record 中
            for (MealSetItem item : items) {
                dao.insertRecord(new Record(item.foodId, targetDate, targetMealType, item.weight));
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "已导入: " + set.name, Toast.LENGTH_SHORT).show();
                finish(); // 导入完直接关闭页面，回上一页看结果
            });
        }).start();
    }
    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        ivBack = findViewById(R.id.ivBack);
        actvFood = findViewById(R.id.actvFood);
        etWeight = findViewById(R.id.etWeight);
        btnSave = findViewById(R.id.btnSave);
    }

    private void initFoodSearch() {
        foodList = dao.getAllFoods();
        List<String> foodNames = new ArrayList<>();
        for (Food food : foodList) {
            foodNames.add(food.name);
        }
        FoodSearchAdapter adapter = new FoodSearchAdapter(this, foodNames);
        actvFood.setAdapter(adapter);

        actvFood.setOnItemClickListener((parent, view, position, id) -> {
            etWeight.requestFocus();
        });
    }

    private void saveRecord() {
        String inputName = actvFood.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (TextUtils.isEmpty(inputName)) {
            Toast.makeText(this, "请输入食物名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(weightStr)) {
            Toast.makeText(this, "请输入重量", Toast.LENGTH_SHORT).show();
            return;
        }

        Food selectedFood = null;
        for (Food f : foodList) {
            if (f.name.equals(inputName)) {
                selectedFood = f;
                break;
            }
        }

        if (selectedFood == null) {
            Toast.makeText(this, "食物库中找不到该食物，请先去创建", Toast.LENGTH_LONG).show();
            return;
        }

        double weight = Double.parseDouble(weightStr);
        if (weight <= 0) {
            Toast.makeText(this, "重量必须大于 0", Toast.LENGTH_SHORT).show();
            return;
        }

        dao.insertRecord(new Record(selectedFood.id, targetDate, targetMealType, weight));

        Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show();
        finish();
    }

    // 👇👇👇 智能排序适配器 (和食物库逻辑保持一致) 👇👇👇
    public class FoodSearchAdapter extends ArrayAdapter<String> {
        private List<String> originalData;
        private List<String> filteredData;
        private Filter mFilter;

        public FoodSearchAdapter(android.content.Context context, List<String> data) {
            super(context, android.R.layout.simple_dropdown_item_1line, data);
            this.originalData = new ArrayList<>(data);
            this.filteredData = new ArrayList<>(data);
        }

        @Override
        public int getCount() { return filteredData.size(); }
        @Override
        public String getItem(int position) { return filteredData.get(position); }
        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        List<String> list = new ArrayList<>();

                        if (constraint == null || constraint.length() == 0) {
                            list.addAll(originalData);
                        } else {
                            String query = constraint.toString().toLowerCase().trim();
                            for (String item : originalData) {
                                if (item.toLowerCase().contains(query)) {
                                    list.add(item);
                                }
                            }

                            // 核心：排序逻辑
                            Collections.sort(list, (s1, s2) -> {
                                String lower1 = s1.toLowerCase();
                                String lower2 = s2.toLowerCase();

                                // 1. 完全匹配优先 ("鸡蛋" > "鸡蛋面")
                                boolean exact1 = lower1.equals(query);
                                boolean exact2 = lower2.equals(query);
                                if (exact1 && !exact2) return -1;
                                if (!exact1 && exact2) return 1;

                                // 2. 开头匹配优先 ("鸡蛋汤" > "西红柿鸡蛋")
                                boolean start1 = lower1.startsWith(query);
                                boolean start2 = lower2.startsWith(query);
                                if (start1 && !start2) return -1;
                                if (!start1 && start2) return 1;

                                // 3. 长度优先 ("鸡蛋饼" > "韭菜鸡蛋饼")
                                return Integer.compare(s1.length(), s2.length());
                            });
                        }

                        results.values = list;
                        results.count = list.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        if (results.values != null) {
                            filteredData = (List<String>) results.values;
                        } else {
                            filteredData = new ArrayList<>();
                        }

                        if (results.count > 0) {
                            notifyDataSetChanged();
                        } else {
                            notifyDataSetInvalidated();
                        }
                    }
                };
            }
            return mFilter;
        }
    }
}