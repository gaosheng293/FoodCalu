package com.example.foodcalu;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView; // 👈 变了
import android.widget.EditText;
import android.widget.Filter;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AddRecordActivity extends AppCompatActivity {

    private AppDatabase db;
    private AppDao dao;

    private RadioGroup rgMealType;
    private AutoCompleteTextView actvFood; // 👈 变了
    private EditText etWeight;

    private String targetDate;
    private List<Food> foodList; // 缓存所有食物数据，用于比对

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_record);

        targetDate = getIntent().getStringExtra("DATE_KEY");
        int targetMealType = getIntent().getIntExtra("MEAL_TYPE", 0);
        if (targetDate == null) targetDate = "2023-01-01";

        db = AppDatabase.getDatabase(this);
        dao = db.appDao();

        rgMealType = findViewById(R.id.rgMealType);
        actvFood = findViewById(R.id.actvFood); // 👈 绑定新控件
        etWeight = findViewById(R.id.etWeight);

        // 1. 初始化食物搜索框
        initFoodSearch();

        // 2. 界面设置 (选中餐别、隐藏、标题)
        switch (targetMealType) {
            case 0: rgMealType.check(R.id.rbBreakfast); break;
            case 1: rgMealType.check(R.id.rbLunch); break;
            case 2: rgMealType.check(R.id.rbDinner); break;
            case 3: rgMealType.check(R.id.rbSnack); break;
        }
        findViewById(R.id.lblMeal).setVisibility(View.GONE);
        rgMealType.setVisibility(View.GONE);

        String[] titles = {"记录早餐", "记录午餐", "记录晚餐", "记录加餐"};
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(titles[targetMealType]);

        // 3. 监听键盘“完成”键 (保持之前的改动)
        etWeight.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                saveRecord();
                return true;
            }
            return false;
        });

        // 可选：一进来光标先在食物框，方便直接搜
        // actvFood.requestFocus();
    }

    private void initFoodSearch() {
        foodList = dao.getAllFoods(); // 拿所有食物

        // 提取名字列表
        List<String> foodNames = new ArrayList<>();
        for (Food food : foodList) {
            foodNames.add(food.name);
        }

        // ❌ 删掉原来这句: ArrayAdapter<String> adapter = new ArrayAdapter<>(...);

        // ✅ 换成我们自定义的“模糊搜索适配器”
        FoodSearchAdapter adapter = new FoodSearchAdapter(this, foodNames);

        actvFood.setAdapter(adapter);

        // 设置点击后光标跳动，保持体验顺滑
        actvFood.setOnItemClickListener((parent, view, position, id) -> {
            etWeight.requestFocus();
        });
    }

    private void saveRecord() {
        // A. 校验重量
        String weightStr = etWeight.getText().toString();
        if (TextUtils.isEmpty(weightStr)) {
            Toast.makeText(this, "请输入重量", Toast.LENGTH_SHORT).show();
            return;
        }
        double weight = Double.parseDouble(weightStr);

        // B. 校验食物
        // 获取用户输入的文字
        String inputName = actvFood.getText().toString().trim();

        if (TextUtils.isEmpty(inputName)) {
            Toast.makeText(this, "请输入食物名称", Toast.LENGTH_SHORT).show();
            return;
        }

        // C. 根据名字反查 ID (因为不再是 Spinner 了，需要自己遍历找)
        int selectedFoodId = -1;
        for (Food food : foodList) {
            if (food.name.equals(inputName)) {
                selectedFoodId = food.id;
                break;
            }
        }

        // 如果找不到 ID，说明用户输入的食物不在数据库里
        if (selectedFoodId == -1) {
            Toast.makeText(this, "未找到该食物，请检查名称或去添加新食物", Toast.LENGTH_LONG).show();
            return;
        }

        // D. 获取餐别
        int mealType = 0;
        int checkedId = rgMealType.getCheckedRadioButtonId();
        if (checkedId == R.id.rbLunch) mealType = 1;
        else if (checkedId == R.id.rbDinner) mealType = 2;
        else if (checkedId == R.id.rbSnack) mealType = 3;

        // E. 保存并关闭
        dao.insertRecord(new Record(selectedFoodId, targetDate, mealType, weight));
        Toast.makeText(this, "已记录: " + inputName, Toast.LENGTH_SHORT).show();
        finish();
    }

    // 自定义的搜索适配器，实现“包含”逻辑 (Contains)
    public class FoodSearchAdapter extends ArrayAdapter<String> {
        private List<String> originalData; // 保存原始的所有数据
        private List<String> filteredData; // 保存过滤后的数据
        private Filter mFilter;

        public FoodSearchAdapter(android.content.Context context, List<String> data) {
            super(context, android.R.layout.simple_dropdown_item_1line, data);
            this.originalData = new ArrayList<>(data); // 备份一份原始数据
            this.filteredData = new ArrayList<>(data);
        }

        @Override
        public int getCount() {
            return filteredData.size();
        }

        @Override
        public String getItem(int position) {
            return filteredData.get(position);
        }

        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        List<String> list = new ArrayList<>();

                        if (constraint == null || constraint.length() == 0) {
                            // 如果没输入，显示所有
                            list.addAll(originalData);
                        } else {
                            // 👇👇👇 核心逻辑在这里：改成 contains (包含) 👇👇👇
                            String filterPattern = constraint.toString().toLowerCase().trim();
                            for (String item : originalData) {
                                // 只要名字包含输入的字，就加进去
                                if (item.toLowerCase().contains(filterPattern)) {
                                    list.add(item);
                                }
                            }
                        }

                        results.values = list;
                        results.count = list.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        // 更新列表数据
                        filteredData = (List<String>) results.values;
                        notifyDataSetChanged();
                    }
                };
            }
            return mFilter;
        }
    }
}