package com.example.foodcalu;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class CreateFoodActivity extends AppCompatActivity {

    private AppDao dao;
    private TextInputEditText etName, etCarbs, etProtein, etFat;
    private TextView tvTitle;
    private MaterialButton btnCreate;

    private int editFoodId = -1; // 如果是编辑模式，这里会存ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_food);

        dao = AppDatabase.getDatabase(this).appDao();

        tvTitle = findViewById(R.id.tvTitle);
        etName = findViewById(R.id.etName);
        etCarbs = findViewById(R.id.etCarbs);
        etProtein = findViewById(R.id.etProtein);
        etFat = findViewById(R.id.etFat);
        btnCreate = findViewById(R.id.btnCreate);

        // 1. 检查是否有 ID 传进来 (判断是 编辑 还是 新建)
        editFoodId = getIntent().getIntExtra("FOOD_ID", -1);

        if (editFoodId != -1) {
            // === 编辑模式 ===
            initEditMode();
        } else {
            // === 新建模式 ===
            tvTitle.setText("创建新食物");
            btnCreate.setText("保存并添加");
        }

        btnCreate.setOnClickListener(v -> saveFood());
    }

    private void initEditMode() {
        tvTitle.setText("修改食物信息");
        btnCreate.setText("保存修改");

        // 从数据库查出旧数据填进去
        Food food = dao.getFoodById(editFoodId);
        if (food != null) {
            etName.setText(food.name);
            etCarbs.setText(String.valueOf(food.carbs));
            etProtein.setText(String.valueOf(food.protein));
            etFat.setText(String.valueOf(food.fat));
        }
    }

    private void saveFood() {
        String name = etName.getText().toString().trim();
        String carbStr = etCarbs.getText().toString().trim();
        String proStr = etProtein.getText().toString().trim();
        String fatStr = etFat.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(carbStr) ||
                TextUtils.isEmpty(proStr) || TextUtils.isEmpty(fatStr)) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }

        double carbs = Double.parseDouble(carbStr);
        double protein = Double.parseDouble(proStr);
        double fat = Double.parseDouble(fatStr);
        double calories = (carbs * 4) + (protein * 4) + (fat * 9); // 自动算热量

        if (editFoodId != -1) {
            // === 执行更新 ===
            Food food = dao.getFoodById(editFoodId);
            food.name = name;
            food.carbs = carbs;
            food.protein = protein;
            food.fat = fat;
            food.calories = calories;

            dao.updateFood(food);
            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
        } else {
            // === 执行插入 ===
            Food newFood = new Food(name, calories, protein, fat, carbs);
            dao.insertFood(newFood);
            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}