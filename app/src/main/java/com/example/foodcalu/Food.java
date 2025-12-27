package com.example.foodcalu; // 确保包名和你的一致

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "food")
public class Food {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public double calories; // 每100g热量
    public double protein;  // 每100g蛋白质
    public double fat;      // 每100g脂肪
    public double carbs;    // 每100g碳水

    // 构造函数
    public Food(String name, double calories, double protein, double fat, double carbs) {
        this.name = name;
        this.calories = calories;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
    }
}