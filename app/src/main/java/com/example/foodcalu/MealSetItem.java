package com.example.foodcalu;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class MealSetItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int setId;   // 属于哪个套餐
    public int foodId;  // 是什么食物
    public double weight; // 多少克

    public MealSetItem(int setId, int foodId, double weight) {
        this.setId = setId;
        this.foodId = foodId;
        this.weight = weight;
    }
}