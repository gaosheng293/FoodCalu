package com.example.foodcalu;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "record_table")
public class Record {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int foodId;      // 对应 Food 表的 id
    public String date;     // 记录日期，格式如 "2023-10-27"
    public int mealType;    // 0=早, 1=中, 2=晚
    public double weight;   // 摄入重量(克)

    public Record(int foodId, String date, int mealType, double weight) {
        this.foodId = foodId;
        this.date = date;
        this.mealType = mealType;
        this.weight = weight;
    }
}