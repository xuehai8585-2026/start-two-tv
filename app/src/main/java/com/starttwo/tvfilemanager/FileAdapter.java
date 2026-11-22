package com.starttwo.tvfilemanager;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class FileAdapter extends BaseAdapter {
    private final Context context;
    private final List<File> items;
    private final LayoutInflater inflater;
    private int selectedPos = -1;

    public FileAdapter(Context context, List<File> items) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    public void setSelectedPosition(int pos) {
        this.selectedPos = pos;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public File getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_file_row, parent, false);
            holder = new ViewHolder();
            holder.container = convertView.findViewById(R.id.rowContainer);
            holder.tvTypeTag = convertView.findViewById(R.id.tvTypeTag);
            holder.tvFileName = convertView.findViewById(R.id.tvFileName);
            holder.tvFileSize = convertView.findViewById(R.id.tvFileSize);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        File file = items.get(position);
        boolean isSelected = (position == selectedPos);

        if (file.isDirectory()) {
            holder.container.setBackgroundResource(R.drawable.bg_folder_selector);
            holder.container.setSelected(isSelected);
            holder.tvTypeTag.setText("[文件夹]");
            holder.tvTypeTag.setTextColor(Color.parseColor("#1A1A1A"));
            holder.tvFileName.setTextColor(Color.parseColor("#1A1A1A"));
            holder.tvFileSize.setTextColor(Color.parseColor("#333333"));
            holder.tvFileSize.setText("");
        } else {
            holder.container.setBackgroundResource(R.drawable.bg_file_selector);
            holder.container.setSelected(isSelected);
            if (MediaUtils.isVideoFile(file)) {
                holder.tvTypeTag.setText("[视频]");
                holder.tvTypeTag.setTextColor(Color.parseColor("#4FC3F7"));
            } else if (MediaUtils.isImageFile(file)) {
                holder.tvTypeTag.setText("[图片]");
                holder.tvTypeTag.setTextColor(Color.parseColor("#81C784"));
            } else {
                holder.tvTypeTag.setText("[文件]");
                holder.tvTypeTag.setTextColor(Color.parseColor("#B0BEC5"));
            }
            holder.tvFileName.setTextColor(Color.parseColor("#F5F5F5"));
            holder.tvFileSize.setTextColor(Color.parseColor("#B0BEC5"));
            holder.tvFileSize.setText(MediaUtils.formatSize(file.length()));
        }

        holder.tvFileName.setText(file.getName());
        return convertView;
    }

    static class ViewHolder {
        LinearLayout container;
        TextView tvTypeTag;
        TextView tvFileName;
        TextView tvFileSize;
    }
}
