package com.example.foodcalu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FoodManagerAdapter extends RecyclerView.Adapter<FoodManagerAdapter.ViewHolder> {

    private List<Food> foodList;
    private OnItemClickListener listener;

    // 修改接口，增加长按方法
    public interface OnItemClickListener {
        void onItemClick(Food food);      // 点击修改
        void onItemLongClick(Food food);  // 长按删除 👈 新增
    }

    public FoodManagerAdapter(List<Food> foodList, OnItemClickListener listener) {
        this.foodList = foodList;
        this.listener = listener;
    }

    public void updateData(List<Food> newData) {
        this.foodList = newData;
        notifyDataSetChanged();
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
        Food food = foodList.get(position);

        holder.tvFoodName.setText(food.name);
        holder.tvItemCalories.setText(String.format("%.0f", food.calories));
        holder.tvFoodWeight.setText("每100克含量");
        holder.tvMealType.setVisibility(View.GONE);

        // 点击 -> 修改
        holder.itemView.setOnClickListener(v -> listener.onItemClick(food));

        // 👇👇👇 长按 -> 删除 👇👇👇
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(food);
            return true; // 返回true表示事件已处理，不会再触发普通点击
        });
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvFoodWeight, tvItemCalories, tvMealType;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodWeight = itemView.findViewById(R.id.tvFoodWeight);
            tvItemCalories = itemView.findViewById(R.id.tvItemCalories);
            tvMealType = itemView.findViewById(R.id.tvMealType);
        }
    }
}