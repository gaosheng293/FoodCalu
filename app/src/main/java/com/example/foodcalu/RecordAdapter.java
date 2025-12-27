package com.example.foodcalu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    private List<Record> recordList = new ArrayList<>();
    private AppDao dao; // 我们需要DAO来查食物的名字

    public RecordAdapter(AppDao dao) {
        this.dao = dao;
    }

    public void setRecordList(List<Record> list) {
        this.recordList = list;
        notifyDataSetChanged(); // 刷新列表
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Record record = recordList.get(position);

        // 1. 根据 record 里的 foodId，去查具体的食物信息（名字、热量）
        Food food = dao.getFoodById(record.foodId);

        // 2. 只有查到了才显示
        if (food != null) {
            holder.tvFoodName.setText(food.name);

            // 3. 计算这一顿的具体热量： (摄入重量 / 100) * 每100g热量
            double thisMealCal = (record.weight / 100.0) * food.calories;
            holder.tvItemCalories.setText(String.format("%.0f", thisMealCal)); // %.0f 表示不保留小数
        }

        holder.tvFoodWeight.setText(String.format("%.0f 克", record.weight));

        // 4. 设置餐别文字
        String[] mealNames = {"早餐", "午餐", "晚餐", "加餐"};
        if (record.mealType >= 0 && record.mealType < mealNames.length) {
            holder.tvMealType.setText(mealNames[record.mealType]);
        }
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMealType, tvFoodName, tvFoodWeight, tvItemCalories;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMealType = itemView.findViewById(R.id.tvMealType);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodWeight = itemView.findViewById(R.id.tvFoodWeight);
            tvItemCalories = itemView.findViewById(R.id.tvItemCalories);
        }
    }
}