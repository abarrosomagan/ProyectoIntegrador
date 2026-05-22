package com.sazon.proyectointegrador.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.RecipeComment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RecipeCommentAdapter extends RecyclerView.Adapter<RecipeCommentAdapter.VH> {

    private final ArrayList<RecipeComment> data;

    public RecipeCommentAdapter(ArrayList<RecipeComment> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_comment, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RecipeComment c = data.get(position);
        String author = c.getAuthorName() == null || c.getAuthorName().trim().isEmpty()
                ? "Chef"
                : c.getAuthorName().trim();
        h.tvAvatar.setText(String.valueOf(Character.toUpperCase(author.charAt(0))));
        h.tvAuthor.setText(author);
        h.tvText.setText(c.getText() == null ? "" : c.getText());
        h.tvDate.setText(formatTime(c.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateData(ArrayList<RecipeComment> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvAuthor, tvText, tvDate;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvCommentAvatar);
            tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvText = itemView.findViewById(R.id.tvCommentText);
            tvDate = itemView.findViewById(R.id.tvCommentDate);
        }
    }

    private static String formatTime(long createdAt) {
        if (createdAt <= 0) return "";
        long diff = System.currentTimeMillis() - createdAt;
        if (diff < 0) diff = 0;

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) return "ahora";
        if (minutes < 60) return "hace " + minutes + " min";
        if (hours < 24) return "hace " + hours + " h";
        if (days < 7) return "hace " + days + " días";

        return new SimpleDateFormat("d MMM", new Locale("es", "ES"))
                .format(new Date(createdAt));
    }
}
