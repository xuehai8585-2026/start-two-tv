package com.starttwo.tvfilemanager;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_STORAGE_PERMISSION = 1001;
    private static final long DOUBLE_BACK_INTERVAL_MS = 2000L;

    private ListView lvFiles;
    private TextView tvCurrentPath;
    private TextView tvItemCount;
    private TextView tvEmpty;

    private File rootDir;
    private File currentDir;
    private final List<File> fileList = new ArrayList<>();
    private FileAdapter adapter;
    private long lastBackPressTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvFiles = findViewById(R.id.lvFiles);
        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        tvItemCount = findViewById(R.id.tvItemCount);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new FileAdapter(this, fileList);
        lvFiles.setAdapter(adapter);

        lvFiles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.setSelectedPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                adapter.setSelectedPosition(-1);
            }
        });

        lvFiles.setOnItemClickListener((parent, view, position, id) -> openItem(fileList.get(position)));

        rootDir = Environment.getExternalStorageDirectory();
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE_PERMISSION);
        } else {
            loadDirectory(rootDir);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadDirectory(rootDir);
            } else {
                Toast.makeText(this, "需要存储权限才能浏览本地文件", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        currentDir = dir;
        tvCurrentPath.setText(dir.getAbsolutePath());

        fileList.clear();
        File[] children = dir.listFiles();
        if (children != null) {
            List<File> dirs = new ArrayList<>();
            List<File> files = new ArrayList<>();
            for (File f : children) {
                if (f.isHidden()) continue;
                if (f.isDirectory()) {
                    dirs.add(f);
                } else if (MediaUtils.isVideoFile(f) || MediaUtils.isImageFile(f)) {
                    files.add(f);
                }
            }
            Comparator<File> byName = (a, b) -> a.getName().compareToIgnoreCase(b.getName());
            Collections.sort(dirs, byName);
            Collections.sort(files, byName);
            fileList.addAll(dirs);
            fileList.addAll(files);
        }

        adapter.notifyDataSetChanged();
        tvItemCount.setText(fileList.size() + " 项");

        if (fileList.isEmpty()) {
            lvFiles.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            lvFiles.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            lvFiles.setSelection(0);
            adapter.setSelectedPosition(0);
            lvFiles.requestFocus();
        }
    }

    private void openItem(File item) {
        if (item.isDirectory()) {
            loadDirectory(item);
        } else if (MediaUtils.isVideoFile(item)) {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_PATH, item.getAbsolutePath());
            startActivity(intent);
        } else if (MediaUtils.isImageFile(item)) {
            ArrayList<String> imagePaths = new ArrayList<>();
            int targetIndex = 0;
            for (File f : fileList) {
                if (MediaUtils.isImageFile(f)) {
                    if (f.getAbsolutePath().equals(item.getAbsolutePath())) {
                        targetIndex = imagePaths.size();
                    }
                    imagePaths.add(f.getAbsolutePath());
                }
            }
            Intent intent = new Intent(this, ImageViewerActivity.class);
            intent.putStringArrayListExtra(ImageViewerActivity.EXTRA_IMAGE_LIST, imagePaths);
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_INDEX, targetIndex);
            startActivity(intent);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (currentDir != null && !currentDir.equals(rootDir) && currentDir.getParentFile() != null) {
                loadDirectory(currentDir.getParentFile());
                return true;
            }
            long now = System.currentTimeMillis();
            if (now - lastBackPressTime <= DOUBLE_BACK_INTERVAL_MS) {
                finish();
            } else {
                lastBackPressTime = now;
                Toast.makeText(this, "再按一次返回键退出 Start two", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
