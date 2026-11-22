package com.starttwo.tvfilemanager;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class ImageViewerActivity extends Activity {
    public static final String EXTRA_IMAGE_LIST = "image_list";
    public static final String EXTRA_IMAGE_INDEX = "image_index";
    private static final long DOUBLE_BACK_INTERVAL_MS = 2000L;

    private ImageView ivPhoto;
    private TextView tvImageName;
    private TextView tvImageIndex;

    private ArrayList<String> imagePaths;
    private int currentIndex = 0;
    private Bitmap currentBitmap;
    private long lastBackPressTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ivPhoto = findViewById(R.id.ivPhoto);
        tvImageName = findViewById(R.id.tvImageName);
        tvImageIndex = findViewById(R.id.tvImageIndex);

        imagePaths = getIntent().getStringArrayListExtra(EXTRA_IMAGE_LIST);
        currentIndex = getIntent().getIntExtra(EXTRA_IMAGE_INDEX, 0);

        if (imagePaths == null || imagePaths.isEmpty()) {
            finish();
            return;
        }
        showImage(currentIndex);
    }

    private void showImage(int index) {
        if (index < 0 || index >= imagePaths.size()) return;
        currentIndex = index;
        String path = imagePaths.get(currentIndex);
        File file = new File(path);

        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);

        int reqW = 1920;
        int reqH = 1080;
        int inSampleSize = 1;
        while ((opts.outWidth / inSampleSize) > reqW * 2 || (opts.outHeight / inSampleSize) > reqH * 2) {
            inSampleSize *= 2;
        }
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = inSampleSize;

        currentBitmap = BitmapFactory.decodeFile(path, opts);
        if (currentBitmap != null) {
            ivPhoto.setImageBitmap(currentBitmap);
        } else {
            Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show();
        }

        tvImageName.setText(file.getName());
        tvImageIndex.setText((currentIndex + 1) + " / " + imagePaths.size());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (currentIndex < imagePaths.size() - 1) {
                showImage(currentIndex + 1);
            } else {
                Toast.makeText(this, "已经是最后一张", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (currentIndex > 0) {
                showImage(currentIndex - 1);
            } else {
                Toast.makeText(this, "已经是第一张", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            long now = System.currentTimeMillis();
            if (now - lastBackPressTime <= DOUBLE_BACK_INTERVAL_MS) {
                finish();
            } else {
                lastBackPressTime = now;
                Toast.makeText(this, "再按一次返回上一级目录", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
        super.onDestroy();
    }
}
