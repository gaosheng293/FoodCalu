package com.example.foodcalu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.widget.SearchView; // 注意引入这个
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FoodListActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView rvFoodList;
    private FoodManagerAdapter adapter;
    private SearchView svFood; // 搜索框

    // 线程池：专门用来在后台查数据库，防止主线程卡顿
    private ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    // 防抖用的 Handler
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable; // 待执行的任务

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        db = AppDatabase.getDatabase(this);
        rvFoodList = findViewById(R.id.rvFoodList);
        svFood = findViewById(R.id.svFood);

        rvFoodList.setLayoutManager(new LinearLayoutManager(this));

        // 1. 初始化空列表的 Adapter
        adapter = new FoodManagerAdapter(new ArrayList<>(), new FoodManagerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Food food) {
                Intent intent = new Intent(FoodListActivity.this, CreateFoodActivity.class);
                intent.putExtra("FOOD_ID", food.id);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Food food) {
                // 长按删除逻辑（保持之前的代码即可）
                // ... 略 ...
            }
        });
        rvFoodList.setAdapter(adapter);

        // 2. 设置搜索监听器
        setupSearchListener();

        // 3. 初始加载数据 (显示全部/前200条)
        performSearch("");

        // 绑定添加按钮
        findViewById(R.id.fabAddFood).setOnClickListener(v -> {
            Intent intent = new Intent(FoodListActivity.this, CreateFoodActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回来刷新一下当前的搜索词
        if (svFood != null) {
            String query = svFood.getQuery().toString();
            performSearch(query);
        }
    }

    // --- 核心逻辑：设置搜索监听 + 防抖 ---
    private void setupSearchListener() {
        svFood.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 点击键盘回车时，立即搜索
                searchHandler.removeCallbacks(searchRunnable); // 移除之前的延迟任务
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 每次输入变化时，不要立刻搜！
                // 1. 先移除之前的任务（如果用户打字很快，之前的任务就会被取消）
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // 2. 定义一个新的任务
                searchRunnable = () -> performSearch(newText);

                // 3. 延迟 300 毫秒后执行
                // 意思是：等用户停手 300ms 后，我才去查数据库
                searchHandler.postDelayed(searchRunnable, 300);

                return true;
            }
        });
    }

    // --- 执行搜索 (在后台线程) ---
    private void performSearch(String keyword) {
        // 放到后台线程去跑，绝对不卡 UI
        dbExecutor.execute(() -> {
            List<Food> result;

            if (keyword == null || keyword.trim().isEmpty()) {
                // 如果没关键字，查默认列表 (带 Limit)
                result = db.appDao().getAllFoodsLimit();
            } else {
                // 有关键字，进行模糊搜索
                result = db.appDao().searchFoods(keyword);
            }

            // 拿到数据后，必须切回主线程更新 UI
            runOnUiThread(() -> {
                adapter.updateData(result);
                // 滚回到顶部，体验更好
                rvFoodList.scrollToPosition(0);
            });
        });
    }
}