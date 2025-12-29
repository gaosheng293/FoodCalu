package com.example.foodcalu;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FoodListActivity extends AppCompatActivity {

    private AppDatabase db;
    private AppDao dao;
    private ListView lvFoodList;
    private EditText etSearchFood;

    private List<Food> allFoods;
    private List<Food> displayFoods;
    private FoodAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        db = AppDatabase.getDatabase(this);
        dao = db.appDao();

        initViews();
        loadFoodList();
    }

    private void initViews() {
        lvFoodList = findViewById(R.id.lvFoodList);
        etSearchFood = findViewById(R.id.etSearchFood);
        ImageView ivBack = findViewById(R.id.ivBack);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        ivBack.setOnClickListener(v -> finish());
        fabAdd.setOnClickListener(v -> showAddFoodDialog());

        lvFoodList.setOnItemLongClickListener((parent, view, position, id) -> {
            Food foodToDelete = displayFoods.get(position);
            showDeleteConfirmDialog(foodToDelete);
            return true;
        });

        etSearchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadFoodList() {
        allFoods = dao.getAllFoods();
        displayFoods = new ArrayList<>(allFoods);
        adapter = new FoodAdapter(displayFoods);
        lvFoodList.setAdapter(adapter);
    }

    // 👇👇👇 修改后的智能排序搜索 👇👇👇
    private void filterList(String query) {
        displayFoods.clear();
        if (TextUtils.isEmpty(query)) {
            displayFoods.addAll(allFoods);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Food f : allFoods) {
                if (f.name.toLowerCase().contains(lowerQuery)) {
                    displayFoods.add(f);
                }
            }

            // --- 核心：对结果进行排序 ---
            Collections.sort(displayFoods, (f1, f2) -> {
                String s1 = f1.name.toLowerCase();
                String s2 = f2.name.toLowerCase();

                // 1. 完全匹配最优先 ("鸡蛋" > "鸡蛋面")
                boolean exact1 = s1.equals(lowerQuery);
                boolean exact2 = s2.equals(lowerQuery);
                if (exact1 && !exact2) return -1;
                if (!exact1 && exact2) return 1;

                // 2. 开头匹配次优先 ("鸡蛋汤" > "西红柿鸡蛋")
                boolean start1 = s1.startsWith(lowerQuery);
                boolean start2 = s2.startsWith(lowerQuery);
                if (start1 && !start2) return -1;
                if (!start1 && start2) return 1;

                // 3. 字数越少越优先 ("鸡蛋饼" > "韭菜鸡蛋饼")
                return Integer.compare(s1.length(), s2.length());
            });
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showDeleteConfirmDialog(Food food) {
        new AlertDialog.Builder(this)
                .setTitle("删除食物")
                .setMessage("确定要删除 “" + food.name + "” 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    dao.deleteFood(food);
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    loadFoodList();
                    String currentSearch = etSearchFood.getText().toString();
                    if(!TextUtils.isEmpty(currentSearch)){
                        filterList(currentSearch);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAddFoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加新食物 (自动计算热量)");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        final EditText etName = new EditText(this);
        etName.setHint("食物名称 (如: 牛油果)");
        layout.addView(etName);

        final EditText etCarbs = new EditText(this);
        etCarbs.setHint("碳水 (克/100g)");
        etCarbs.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etCarbs);

        final EditText etProtein = new EditText(this);
        etProtein.setHint("蛋白质 (克/100g)");
        etProtein.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etProtein);

        final EditText etFat = new EditText(this);
        etFat.setHint("脂肪 (克/100g)");
        etFat.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etFat);

        builder.setView(layout);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                double carbs = parseDoubleSafe(etCarbs.getText().toString());
                double pro = parseDoubleSafe(etProtein.getText().toString());
                double fat = parseDoubleSafe(etFat.getText().toString());
                double cal = (carbs * 4) + (pro * 4) + (fat * 9);

                Food newFood = new Food(name, cal, carbs, pro, fat);
                dao.insertFood(newFood);

                Toast.makeText(this, "已添加，热量: " + (int)cal, Toast.LENGTH_LONG).show();
                loadFoodList();
            } else {
                Toast.makeText(this, "请输入食物名称", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private double parseDoubleSafe(String str) {
        if (TextUtils.isEmpty(str)) return 0;
        try { return Double.parseDouble(str); } catch (NumberFormatException e) { return 0; }
    }

    class FoodAdapter extends ArrayAdapter<Food> {
        public FoodAdapter(List<Food> foods) {
            super(FoodListActivity.this, 0, foods);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_record, parent, false);
            }
            Food food = getItem(position);
            TextView tvName = convertView.findViewById(R.id.tvFoodName);
            TextView tvWeight = convertView.findViewById(R.id.tvFoodWeight);
            TextView tvCal = convertView.findViewById(R.id.tvItemCalories);
            TextView tvType = convertView.findViewById(R.id.tvMealType);
            if (tvType != null) tvType.setVisibility(View.GONE);

            if (food != null) {
                tvName.setText(food.name);
                String desc = String.format("碳%.1f 蛋%.1f 脂%.1f", food.carbs, food.protein, food.fat);
                tvWeight.setText(desc);
                tvCal.setText((int)food.calories + " 千卡/100g");
            }
            return convertView;
        }
    }
}