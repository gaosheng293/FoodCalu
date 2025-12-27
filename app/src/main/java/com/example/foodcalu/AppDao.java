package com.example.foodcalu;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppDao {
    // --- 食物相关操作 ---
    @Insert
    void insertFood(Food food);

    @Query("SELECT * FROM food")
    List<Food> getAllFoods();

    @Query("SELECT * FROM food WHERE id = :id")
    Food getFoodById(int id);

    // --- 记录相关操作 ---
    @Insert
    void insertRecord(Record record);

    // 获取某天的所有记录
    @Query("SELECT * FROM record_table WHERE date = :date")
    List<Record> getRecordsByDate(String date);


    // LIMIT 200 是为了保护性能，防止一次性搜出太多把内存撑爆
    @Query("SELECT * FROM food WHERE name LIKE '%' || :keyword || '%' LIMIT 200")
    List<Food> searchFoods(String keyword);

    // 如果想显示所有数据，也建议加个 LIMIT，防止 1万条全部一次性读入
    @Query("SELECT * FROM food LIMIT 200")
    List<Food> getAllFoodsLimit();

    @Delete
    void deleteRecord(Record record);

    @Update
    void updateFood(Food food);

    @Delete
    void deleteFood(Food food);
}