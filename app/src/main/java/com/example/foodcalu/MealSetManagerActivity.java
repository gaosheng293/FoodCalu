package com.example.foodcalu;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MealSetManagerActivity extends AppCompatActivity {

    private AppDatabase db;
    private AppDao dao;
    private ListView lvSetList;
    private SetAdapter adapter;

    // 👇 升级后的包装类：包含总营养素数据
    static class MealSetWrapper {
        MealSet set;
        String content;
        double totalCal;
        double totalCarbs;
        double totalProtein;
        double totalFat;

        public MealSetWrapper(MealSet set, String content, double cal, double c, double p, double f) {
            this.set = set;
            this.content = content;
            this.totalCal = cal;
            this.totalCarbs = c;
            this.totalProtein = p;
            this.totalFat = f;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_set_manager);

        db = AppDatabase.getDatabase(this);
        dao = db.appDao();

        lvSetList = findViewById(R.id.lvSetList);
        // 去掉默认分割线，因为卡片自带边距
        lvSetList.setDivider(null);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            List<MealSet> sets = dao.getAllMealSets();
            List<MealSetWrapper> wrappers = new ArrayList<>();

            for (MealSet set : sets) {
                List<MealSetItem> items = dao.getMealSetItems(set.id);
                StringBuilder sb = new StringBuilder();

                // 临时累加器
                double tCal = 0, tCarbs = 0, tPro = 0, tFat = 0;

                for (MealSetItem item : items) {
                    Food f = dao.getFoodById(item.foodId);
                    if (f != null) {
                        if (sb.length() > 0) sb.append("、");
                        sb.append(f.name).append((int)item.weight).append("g");

                        // 计算营养
                        double ratio = item.weight / 100.0;
                        tCal += f.calories * ratio;
                        tCarbs += f.carbs * ratio;
                        tPro += f.protein * ratio;
                        tFat += f.fat * ratio;
                    }
                }

                if (sb.length() == 0) sb.append("(空套餐)");

                wrappers.add(new MealSetWrapper(set, sb.toString(), tCal, tCarbs, tPro, tFat));
            }

            runOnUiThread(() -> {
                adapter = new SetAdapter(wrappers);
                lvSetList.setAdapter(adapter);
            });
        }).start();
    }

    private void showRenameDialog(MealSet set) {
        final EditText etName = new EditText(this);
        etName.setText(set.name);

        new AlertDialog.Builder(this)
                .setTitle("重命名套餐")
                .setView(etName)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        new Thread(() -> {
                            set.name = newName;
                            dao.updateMealSet(set);
                            runOnUiThread(() -> {
                                loadData();
                                Toast.makeText(this, "已重命名", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDeleteDialog(MealSet set) {
        new AlertDialog.Builder(this)
                .setTitle("删除套餐")
                .setMessage("确定删除 “" + set.name + "” 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    new Thread(() -> {
                        dao.deleteMealSetItemsBySetId(set.id);
                        dao.deleteMealSet(set);
                        runOnUiThread(() -> {
                            loadData();
                            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 👇 使用新的 item_meal_set 布局
    class SetAdapter extends ArrayAdapter<MealSetWrapper> {
        public SetAdapter(List<MealSetWrapper> list) {
            super(MealSetManagerActivity.this, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                // 加载新的精美卡片布局
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_meal_set, parent, false);
            }

            MealSetWrapper wrapper = getItem(position);
            MealSet set = wrapper.set;

            // 绑定控件
            TextView tvName = convertView.findViewById(R.id.tvSetName);
            TextView tvCal = convertView.findViewById(R.id.tvSetCal);
            TextView tvCarbs = convertView.findViewById(R.id.tvSetCarbs);
            TextView tvPro = convertView.findViewById(R.id.tvSetProtein);
            TextView tvFat = convertView.findViewById(R.id.tvSetFat);
            TextView tvContent = convertView.findViewById(R.id.tvSetContent);
            ImageView ivEdit = convertView.findViewById(R.id.ivEdit);

            // 填充数据
            tvName.setText(set.name);
            tvCal.setText(String.format("%.0f", wrapper.totalCal));

            tvCarbs.setText(String.format("%.0fg", wrapper.totalCarbs));
            tvPro.setText(String.format("%.0fg", wrapper.totalProtein));
            tvFat.setText(String.format("%.0fg", wrapper.totalFat));

            tvContent.setText(wrapper.content);

            // 点击编辑图标 -> 改名
            ivEdit.setOnClickListener(v -> showRenameDialog(set));

            // 整个卡片长按 -> 删除
            convertView.setOnLongClickListener(v -> {
                showDeleteDialog(set);
                return true;
            });

            return convertView;
        }
    }
}