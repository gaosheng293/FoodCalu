package com.example.foodcalu;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ExportActivity extends AppCompatActivity {

    private AppDatabase db;
    private AppDao dao;

    private TextView tvExportDate, tvTotalCal;
    private TextView tvTotalCarbs, tvTotalProtein, tvTotalFat;
    private LinearLayout llExportList;
    private LinearLayout layoutScreenshot;

    private String startDate;
    private String endDate;
    private boolean isSingleDay;

    private String[] mealNames = {"早餐", "午餐", "晚餐", "加餐"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        // 获取参数
        startDate = getIntent().getStringExtra("START_DATE");
        endDate = getIntent().getStringExtra("END_DATE");

        // 如果起止日期一样，就是单日模式
        isSingleDay = startDate != null && startDate.equals(endDate);

        db = AppDatabase.getDatabase(this);
        dao = db.appDao();

        initViews();
        loadData();
    }

    private void initViews() {
        tvExportDate = findViewById(R.id.tvExportDate);
        tvTotalCal = findViewById(R.id.tvTotalCal);
        tvTotalCarbs = findViewById(R.id.tvTotalCarbs);
        tvTotalProtein = findViewById(R.id.tvTotalProtein);
        tvTotalFat = findViewById(R.id.tvTotalFat);

        llExportList = findViewById(R.id.llExportList);
        layoutScreenshot = findViewById(R.id.layoutScreenshot);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // 保存图片按钮
        findViewById(R.id.btnSaveImage).setOnClickListener(v -> saveImageToGallery());

        // 修改标题
        // 👇👇👇 修复点：直接用 ID 查找，简单直接，不会报错 👇👇👇
        TextView titleTv = findViewById(R.id.tvToolbarTitle);
        titleTv.setText(isSingleDay ? "生成日报" : "周报/长图");
    }

    private void loadData() {
        // 显示日期范围
        if (isSingleDay) {
            tvExportDate.setText(startDate);
        } else {
            tvExportDate.setText(startDate.substring(5) + " 至 " + endDate.substring(5));
        }

        // 查询数据
        List<Record> records = dao.getRecordsByRange(startDate, endDate);

        double totalCal = 0, totalCarbs = 0, totalPro = 0, totalFat = 0;

        // 按日期分组
        Map<String, List<Record>> dailyMap = new TreeMap<>();
        // 简单手动分组，因为 Record 里没有直接存 list
        // 这里我们可以偷懒，因为 List<Record> 已经是按日期排序的了
        // 我们直接遍历，遇到新日期就加一个日期头

        llExportList.removeAllViews();
        String lastDate = "";

        // 临时变量用于计算每一天的总数 (用于做分割线或者小结，这里为了紧凑省略小结)

        for (Record r : records) {
            Food f = dao.getFoodById(r.foodId);
            if (f != null) {
                double ratio = r.weight / 100.0;
                double cal = f.calories * ratio;
                totalCal += cal;
                totalCarbs += f.carbs * ratio;
                totalPro += f.protein * ratio;
                totalFat += f.fat * ratio;

                // 如果换了一天，加一个日期大标题
                if (!r.date.equals(lastDate)) {
                    addDateHeader(r.date);
                    lastDate = r.date;
                }

                // 添加一行食物（使用超级紧凑模式）
                addCompactFoodRow(f.name, mealNames[r.mealType], r.weight, cal, f.carbs*ratio, f.protein*ratio, f.fat*ratio);
            }
        }

        if (records.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("该时间段无记录");
            empty.setPadding(0, 50, 0, 0);
            empty.setGravity(Gravity.CENTER);
            llExportList.addView(empty);
        }

        // 更新头部汇总
        tvTotalCal.setText(String.format("%.0f", totalCal));
        tvTotalCarbs.setText(String.format("碳:%.0f", totalCarbs));
        tvTotalProtein.setText(String.format("蛋:%.0f", totalPro));
        tvTotalFat.setText(String.format("脂:%.0f", totalFat));
    }

    // 添加日期标题 (例如: "10-27")
    private void addDateHeader(String date) {
        TextView tv = new TextView(this);
        tv.setText(date);
        tv.setTextSize(18);
        tv.setTextColor(Color.parseColor("#2EC195")); // 绿色高亮
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 32, 0, 8); // 上面留空大一点，区分不同天
        tv.setBackgroundColor(Color.TRANSPARENT);

        // 加一条分割线
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
        line.setBackgroundColor(Color.parseColor("#E0E0E0"));

        llExportList.addView(line);
        llExportList.addView(tv);
    }

    // 添加超级紧凑的食物行 (一行显示所有信息)
    // 样式: [早餐] 鸡蛋 100g  150kcal (C:1 P:10 F:8)
    private void addCompactFoodRow(String name, String mealName, double weight, double cal, double c, double p, double f) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        // 餐别+食物名
        TextView tvMain = new TextView(this);
        tvMain.setText("[" + mealName + "] " + name);
        tvMain.setTextColor(Color.parseColor("#333333"));
        tvMain.setTextSize(14);
        tvMain.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)); // 占比大一点

        // 重量+热量
        TextView tvInfo = new TextView(this);
        tvInfo.setText(String.format("%.0fg  %.0f大卡", weight, cal));
        tvInfo.setTextColor(Color.parseColor("#666666"));
        tvInfo.setTextSize(13);
        tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvInfo.setGravity(Gravity.END);

        // 营养素 (小字)
        TextView tvMacros = new TextView(this);
        tvMacros.setText(String.format("C:%.0f P:%.0f F:%.0f", c, p, f));
        tvMacros.setTextColor(Color.parseColor("#999999"));
        tvMacros.setTextSize(11);
        tvMacros.setPadding(16, 0, 0, 0);
        tvMacros.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(tvMain);
        row.addView(tvInfo);
        // 如果屏幕太窄，可以考虑把 macros 换行，或者这里就不加了，上面两个已经够详细了
        // 这里为了详细，还是加上
        row.addView(tvMacros);

        llExportList.addView(row);
    }

    // === 功能：保存图片 ===
    private void saveImageToGallery() {
        Bitmap bitmap = getBitmapFromView(layoutScreenshot);
        if (bitmap != null) {
            try {
                saveBitmap(bitmap);
                Toast.makeText(this, "长图已保存到相册！", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return bitmap;
    }

    private void saveBitmap(Bitmap bitmap) throws IOException {
        String fileName = "FoodCalu_Weekly_" + System.currentTimeMillis() + ".jpg";
        OutputStream fos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/FoodCalu");
            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            fos = getContentResolver().openOutputStream(imageUri);
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
            java.io.File image = new java.io.File(imagesDir, fileName);
            fos = new java.io.FileOutputStream(image);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        if (fos != null) fos.close();
    }
}