package com.starttwo.tvfilemanager;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MediaUtils {
    private static final Set<String> VIDEO_EXTS = new HashSet<>(Arrays.asList(
            "mp4", "wmv", "avi", "mkv", "mov", "flv", "ts", "m4v", "3gp", "webm", "rmvb", "mpg", "mpeg"
    ));

    private static final Set<String> IMAGE_EXTS = new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "bmp", "webp", "gif"
    ));

    public static String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            return name.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    public static boolean isVideoFile(File file) {
        return !file.isDirectory() && VIDEO_EXTS.contains(getExtension(file));
    }

    public static boolean isImageFile(File file) {
        return !file.isDirectory() && IMAGE_EXTS.contains(getExtension(file));
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.ROOT, "%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format(Locale.ROOT, "%.2f GB", gb);
    }

    public static String formatTimeMs(int ms) {
        if (ms < 0) ms = 0;
        int totalSec = ms / 1000;
        int sec = totalSec % 60;
        int min = (totalSec / 60) % 60;
        int hr = totalSec / 3600;
        if (hr > 0) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hr, min, sec);
        }
        return String.format(Locale.ROOT, "%02d:%02d", min, sec);
    }
}
