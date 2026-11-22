package com.starttwo.tvfilemanager;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class VideoPlayerActivity extends Activity implements SurfaceHolder.Callback {
    public static final String EXTRA_VIDEO_PATH = "video_path";

    private static final long LONG_PRESS_DELAY_MS = 1500L;
    private static final long FAST_SEEK_TICK_MS = 200L;
    private static final int FAST_SEEK_MULTIPLIER = 5;
    private static final int SHORT_SEEK_MS = 10000;

    private SurfaceView surfaceView;
    private LinearLayout osdOverlay;
    private TextView tvVideoTitle;
    private TextView tvPlayState;
    private ProgressBar progressBar;
    private TextView tvTimeInfo;

    private MediaPlayer mediaPlayer;
    private String videoPath;
    private boolean isPrepared = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isLongPressSeeking = false;
    private int fastSeekDirection = 0;
    private int pendingKeyCode = 0;

    private final Runnable hideOsdRunnable = () -> osdOverlay.setVisibility(View.GONE);

    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            updateProgressUi();
            handler.postDelayed(this, 500);
        }
    };

    private final Runnable startLongPressSeekRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPrepared || mediaPlayer == null) return;
            isLongPressSeeking = true;
            fastSeekDirection = (pendingKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT) ? 1 : -1;
            showOsd(false);
            handler.post(fastSeekTickRunnable);
        }
    };

    private final Runnable fastSeekTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isLongPressSeeking || !isPrepared || mediaPlayer == null) return;
            int duration = mediaPlayer.getDuration();
            int current = mediaPlayer.getCurrentPosition();
            int step = (int) (FAST_SEEK_TICK_MS * FAST_SEEK_MULTIPLIER) * fastSeekDirection;
            int target = Math.max(0, Math.min(duration, current + step));
            mediaPlayer.seekTo(target);
            tvPlayState.setText(fastSeekDirection > 0 ? ">> 5x 快进" : "<< 5x 快退");
            updateProgressUi();
            handler.postDelayed(this, FAST_SEEK_TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        surfaceView = findViewById(R.id.surfaceView);
        osdOverlay = findViewById(R.id.osdOverlay);
        tvVideoTitle = findViewById(R.id.tvVideoTitle);
        tvPlayState = findViewById(R.id.tvPlayState);
        progressBar = findViewById(R.id.progressBar);
        tvTimeInfo = findViewById(R.id.tvTimeInfo);

        videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        if (videoPath == null) {
            finish();
            return;
        }
        tvVideoTitle.setText(new File(videoPath).getName());
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDisplay(holder);
            mediaPlayer.setDataSource(videoPath);
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                adjustAspectRatio(mp.getVideoWidth(), mp.getVideoHeight());
                progressBar.setMax(mp.getDuration());
                mp.start();
                updatePlayStateLabel();
                showOsd(true);
                handler.post(progressUpdater);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                updatePlayStateLabel();
                showOsd(false);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "无法播放该视频格式或文件已损坏", Toast.LENGTH_LONG).show();
                finish();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "打开视频失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        if (videoWidth <= 0 || videoHeight <= 0) return;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        float videoRatio = (float) videoWidth / videoHeight;
        float screenRatio = (float) screenWidth / screenHeight;

        android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        if (videoRatio > screenRatio) {
            lp.width = screenWidth;
            lp.height = (int) (screenWidth / videoRatio);
        } else {
            lp.height = screenHeight;
            lp.width = (int) (screenHeight * videoRatio);
        }
        surfaceView.setLayoutParams(lp);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releasePlayer();
    }

    private void updateProgressUi() {
        if (!isPrepared || mediaPlayer == null) return;
        int current = mediaPlayer.getCurrentPosition();
        int total = mediaPlayer.getDuration();
        progressBar.setMax(total);
        progressBar.setProgress(current);
        tvTimeInfo.setText(MediaUtils.formatTimeMs(current) + " / " + MediaUtils.formatTimeMs(total));
    }

    private void updatePlayStateLabel() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            tvPlayState.setText("▶ 播放中");
        } else {
            tvPlayState.setText("❚❚ 已暂停");
        }
    }

    private void showOsd(boolean autoHide) {
        osdOverlay.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideOsdRunnable);
        if (autoHide) {
            handler.postDelayed(hideOsdRunnable, 4000);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            if (isPrepared && mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
                updatePlayStateLabel();
                showOsd(true);
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (event.getRepeatCount() == 0) {
                pendingKeyCode = keyCode;
                isLongPressSeeking = false;
                handler.postDelayed(startLongPressSeekRunnable, LONG_PRESS_DELAY_MS);
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            handler.removeCallbacks(startLongPressSeekRunnable);
            if (isLongPressSeeking) {
                isLongPressSeeking = false;
                handler.removeCallbacks(fastSeekTickRunnable);
                updatePlayStateLabel();
                showOsd(true);
            } else if (isPrepared && mediaPlayer != null) {
                int delta = (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) ? SHORT_SEEK_MS : -SHORT_SEEK_MS;
                int target = Math.max(0, Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + delta));
                mediaPlayer.seekTo(target);
                updateProgressUi();
                showOsd(true);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void releasePlayer() {
        handler.removeCallbacksAndMessages(null);
        isPrepared = false;
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
