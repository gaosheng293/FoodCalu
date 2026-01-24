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

import java.util.ArrayList;
import java.util.List;

public class MealDetailActivity extends AppCompatActivity {

    private AppDatabase db;
    private AppDao dao;

    private TextView tvPageTitle;
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
        findViewById(R.id.btnSaveAsSet).setOnClickListener(v -> showSaveSetDialog());
        loadData();
    }

    // 1. 修改保存弹窗逻辑
    private void showSaveSetDialog() {
        List<Record> currentRecords = dao.getRecordsByDateAndMealType(targetDate, targetMealType);
        if (currentRecords.isEmpty()) {
            Toast.makeText(this, "当前没有食物，无法保存", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText etName = new EditText(this);
        etName.setHint("输入套餐名称");

        new AlertDialog.Builder(this)
                .setTitle("存为套餐")
                .setView(etName)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!TextUtils.isEmpty(name)) {
                        checkAndSaveSet(name, currentRecords); // 👈 改为调用检查方法
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 2. 新增：检查是否重名
    private void checkAndSaveSet(String name, List<Record> records) {
        new Thread(() -> {
            MealSet existingSet = dao.getMealSetByName(name);
            runOnUiThread(() -> {
                if (existingSet != null) {
                    // 发现重名，弹出覆盖确认框
                    new AlertDialog.Builder(this)
                            .setTitle("套餐已存在")
                            .setMessage("“" + name + "” 已存在，是否覆盖旧的设置？")
                            .setPositiveButton("覆盖", (d, w) -> overwriteMealSet(existingSet, records))
                            .setNegativeButton("取消", null)
                            .show();
                } else {
                    // 没重名，直接存
                    saveNewMealSet(name, records);
                }
            });
        }).start();
    }

    // 3. 覆盖旧套餐 (核心修改逻辑)
    private void overwriteMealSet(MealSet set, List<Record> records) {
        new Thread(() -> {
            // 先删掉旧的详情
            dao.deleteMealSetItemsBySetId(set.id);

            // 再插入新的详情
            List<MealSetItem> items = new ArrayList<>();
            for (Record r : records) {
                items.add(new MealSetItem(set.id, r.foodId, r.weight));
            }
            dao.insertMealSetItems(items);

            runOnUiThread(() -> Toast.makeText(this, "套餐已更新！", Toast.LENGTH_SHORT).show());
        }).start();
    }

    // 4. 原来的保存逻辑 (稍微改名)
    private void saveNewMealSet(String name, List<Record> records) {
        new Thread(() -> {
            long setId = dao.insertMealSet(new MealSet(name));
            List<MealSetItem> items = new ArrayList<>();
            for (Record r : records) {
                items.add(new MealSetItem((int)setId, r.foodId, r.weight));
            }
            dao.insertMealSetItems(items);
            runOnUiThread(() -> Toast.makeText(this, "新套餐保存成功！", Toast.LENGTH_SHORT).show());
        }).start();
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

                addListItem(r, food, itemCal, ratio); // 传入 ratio 方便计算
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

    // 👇👇👇 修改了这里：显示每个食物的具体营养素 👇👇👇
    private void addListItem(Record r, Food food, double itemCal, double ratio) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_record, null);

        TextView tvName = itemView.findViewById(R.id.tvFoodName);
        TextView tvWeight = itemView.findViewById(R.id.tvFoodWeight);
        TextView tvCal = itemView.findViewById(R.id.tvItemCalories);
        TextView tvMacros = itemView.findViewById(R.id.tvMacros); // 绑定新控件

        TextView tvType = itemView.findViewById(R.id.tvMealType);
        if (tvType != null) tvType.setVisibility(View.GONE);

        tvName.setText(food.name);
        tvWeight.setText((int)r.weight + "克");
        tvCal.setText(String.format("%.0f 千卡", itemCal));

        // 计算当前重量下的具体营养素
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
                        loadData();
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
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

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
                if (newWeight <= 0) {
                    Toast.makeText(this, "重量必须大于 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                record.weight = newWeight;
                dao.updateRecord(record);
                Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
                loadData();
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}