package com.example.foodcalu;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// 声明包含哪些表，版本号为1
@Database(entities = {Food.class, Record.class, MealSet.class, MealSetItem.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract AppDao appDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "my_food_app.db") // 手机里实际保存的文件名
                            // 👇👇👇 核心：告诉 Room 第一次创建时从 assets 复制数据
                            .createFromAsset("food_database.db")
                            .allowMainThreadQueries() // 允许主线程查询(简单项目可用，大项目建议用异步)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}