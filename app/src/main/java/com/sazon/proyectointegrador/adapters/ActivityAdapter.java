package com.sazon.proyectointegrador.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.ActivityItem;
import com.sazon.proyectointegrador.util.ActivityRepository;

import java.util.ArrayList;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.VH> {

    private final ArrayList<ActivityItem> data;

    public ActivityAdapter(ArrayList<ActivityItem> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ActivityItem item = data.get(position);
        h.tvIcon.setText(iconFor(item.getType()));
        h.tvMessage.setText(item.getMessage() == null ? "" : item.getMessage());
        h.tvMeta.setText(metaFor(item));
        h.unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateData(ArrayList<ActivityItem> items) {
        data.clear();
        data.addAll(items);
        notifyDataSetChanged();
    }

    private static String iconFor(String type) {
        if (ActivityRepository.TYPE_LIKE.equals(type)) return "♥";
        if (ActivityRepository.TYPE_SAVE.equals(type)) return "★";
        if (ActivityRepository.TYPE_COMMENT.equals(type)) return "✎";
        if (ActivityRepository.TYPE_FOLLOW.equals(type)) return "+";
        return "•";
    }

    private static String metaFor(ActivityItem item) {
        String recipeTitle = item.getRecipeTitle();
        String time = formatTime(item.getCreatedAt());
        if (recipeTitle != null && !recipeTitle.trim().isEmpty()) {
            return recipeTitle.trim() + " · " + time;
        }
        return time;
    }

    private static String formatTime(long createdAt) {
        if (createdAt <= 0) return "";
        long diff = System.currentTimeMillis() - createdAt;
        if (diff < 0) diff = 0;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);
        if (minutes < 60) return "hace " + minutes + " min";
        if (hours < 24) return "hace " + hours + " h";
        if (days == 1) return "ayer";
        return "hace " + days + " dias";
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvIcon, tvMessage, tvMeta;
        final View unreadDot;

        VH(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvActivityIcon);
            tvMessage = itemView.findViewById(R.id.tvActivityMessage);
            tvMeta = itemView.findViewById(R.id.tvActivityMeta);
            unreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}
