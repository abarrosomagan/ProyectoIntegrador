package com.sazon.proyectointegrador.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.UserListItem;
import com.sazon.proyectointegrador.util.RecipeImageHelper;

import java.util.ArrayList;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.VH> {

    public interface OnUserClick {
        void onUserClick(UserListItem user);
    }

    private final ArrayList<UserListItem> data;
    private final OnUserClick listener;

    public UserListAdapter(ArrayList<UserListItem> data, OnUserClick listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_list, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UserListItem user = data.get(position);
        String name = user.displayName();
        h.tvName.setText(name);
        h.tvHandle.setText(user.handle());
        String bio = user.getBio();
        if (bio != null && !bio.trim().isEmpty()) {
            h.tvBio.setVisibility(View.VISIBLE);
            h.tvBio.setText(bio.trim());
        } else {
            h.tvBio.setVisibility(View.GONE);
        }

        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            h.ivAvatar.setVisibility(View.VISIBLE);
            h.tvAvatar.setVisibility(View.GONE);
            RecipeImageHelper.loadInto(h.ivAvatar, avatarUrl);
        } else {
            h.ivAvatar.setVisibility(View.GONE);
            h.tvAvatar.setVisibility(View.VISIBLE);
            h.tvAvatar.setText(String.valueOf(Character.toUpperCase(name.charAt(0))));
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateData(ArrayList<UserListItem> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvAvatar, tvName, tvHandle, tvBio;

        VH(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivUserListAvatar);
            tvAvatar = itemView.findViewById(R.id.tvUserListAvatar);
            tvName = itemView.findViewById(R.id.tvUserListName);
            tvHandle = itemView.findViewById(R.id.tvUserListHandle);
            tvBio = itemView.findViewById(R.id.tvUserListBio);
        }
    }
}
