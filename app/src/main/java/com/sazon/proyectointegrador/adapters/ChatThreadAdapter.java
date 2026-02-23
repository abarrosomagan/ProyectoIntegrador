package com.sazon.proyectointegrador.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.ChatThread;

import java.util.List;

public class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.VH> {

    public interface OnChatClick {
        void onClick(ChatThread chat);
    }

    private final List<ChatThread> data;
    private final OnChatClick listener;

    public ChatThreadAdapter(List<ChatThread> data, OnChatClick listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatThread c = data.get(position);

        h.tvName.setText(c.getName());
        h.tvLast.setText(c.getLastMessage());
        h.tvTime.setText(c.getTime());

        String avatar = c.getName() == null || c.getName().isEmpty()
                ? "?"
                : ("" + Character.toUpperCase(c.getName().charAt(0)));
        h.tvAvatar.setText(avatar);

        if (c.getUnread() > 0) {
            h.badge.setVisibility(View.VISIBLE);
            h.tvUnread.setText(String.valueOf(c.getUnread()));
        } else {
            h.badge.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(c));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvLast, tvTime, tvUnread;
        MaterialCardView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLast = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            badge = itemView.findViewById(R.id.badge);
            tvUnread = itemView.findViewById(R.id.tvUnread);
        }
    }
}