package com.example.foodcalu;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

public class MealDetailActivity extends AppCompatActivity {

    private AppDatabase db;
    private AppDao dao;

    private TextView tvPageTitle;

    // 仪表盘控件
    private TextView tvCircleCalVal;
    private TextView tvCarbsGram, tvProteinGram, tvFatGram;
    private CircularProgressIndicator circleCalorie;

    private LinearLayout llFoodList;
    private MaterialButton btnAddFood;

    private String targetDate;
    private int targetMealType;
    private String[] mealNames = {"早餐", "午餐", "晚餐", "加餐"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_detail);

        targetDate = getIntent().getStringExtra("DATE_KEY");
        targetMealType = getIntent().getIntExtra("MEAL_TYPE", 0);
        if (targetDate == null) targetDate = "2023-01-01";

        db = AppDatabase.getDatabase(this);
        dao = db.appDao();

        initViews();

        tvPageTitle.setText(targetDate + " " + mealNames[targetMealType]);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        btnAddFood.setOnClickListener(v -> {
            Intent intent = new Intent(MealDetailActivity.this, AddRecordActivity.class);
            intent.putExtra("MEAL_TYPE", targetMealType);
            intent.putExtra("DATE_KEY", targetDate);
            startActivity(intent);
        });

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews() {
        tvPageTitle = findViewById(R.id.tvPageTitle);

        tvCircleCalVal = findViewById(R.id.tvCircleCalVal);
        tvCarbsGram = findViewById(R.id.tvCarbsGram);
        tvProteinGram = findViewById(R.id.tvProteinGram);
        tvFatGram = findViewById(R.id.tvFatGram);
        circleCalorie = findViewById(R.id.circleCalorie);

        llFoodList = findViewById(R.id.llFoodList);
        btnAddFood = findViewById(R.id.btnAddFood);
    }

    private void loadData() {
        List<Record> records = dao.getRecordsByDateAndMealType(targetDate, targetMealType);

        double totalCal = 0, totalCarbs = 0, totalProtein = 0, totalFat = 0;

        llFoodList.removeAllViews();

        for (Record r : records) {
            Food food = dao.getFoodById(r.foodId);
            if (food != null) {
                double ratio = r.weight / 100.0;
                double itemCal = food.calories * ratio;
                totalCal += itemCal;
                totalCarbs += food.carbs * ratio;
                totalProtein += food.protein * ratio;
                totalFat += food.fat * ratio;

                addListItem(r, food, itemCal);
            }
        }

        tvCircleCalVal.setText(String.format("%.0f", totalCal));
        tvCarbsGram.setText(String.format("%.1f克", totalCarbs));
        tvProteinGram.setText(String.format("%.1f克", totalProtein));
        tvFatGram.setText(String.format("%.1f克", totalFat));

        int progress = (int) ((totalCal / 800.0) * 100);
        if (progress > 100) progress = 100;
        circleCalorie.setProgress(progress);
    }

    private void addListItem(Record r, Food food, double itemCal) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_record, null);

        TextView tvName = itemView.findViewById(R.id.tvFoodName);
        TextView tvWeight = itemView.findViewById(R.id.tvFoodWeight);
        TextView tvCal = itemView.findViewById(R.id.tvItemCalories);

//        TextView tvType = itemView.findViewById(R.id.tvMealType);
//        if (tvType != null) tvType.setVisibility(View.GONE);

        tvName.setText(food.name);
        tvWeight.setText((int)r.weight + "克");
        tvCal.setText(String.format("%.0f 千卡", itemCal));

        // 点击 -> 修改
        itemView.setOnClickListener(v -> showBeautifulEditDialog(r));

        // 👇👇👇 新增：长按 -> 删除 👇👇👇
        itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除记录")
                    .setMessage("确定要删除 “" + food.name + "” 吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        dao.deleteRecord(r);
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        loadData(); // 刷新本页
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        llFoodList.addView(itemView);
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
                // 👇👇👇 新增：判0逻辑 👇👇👇
                if (newWeight <= 0) {
                    Toast.makeText(this, "重量必须大于 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                record.weight = newWeight;
                dao.updateRecord(record);
                Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
                loadData();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "请输入重量", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}