package com.example.foodcalu;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class MealSet {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name; // 套餐名字，比如 "减脂早餐"

    public MealSet(String name) {
        this.name = name;
    }
}