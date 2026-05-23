package com.sazon.proyectointegrador.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.UserListItem;
import com.sazon.proyectointegrador.util.RecipeImageHelper;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.VH> {

    public interface OnUserClick {
        void onUserClick(UserListItem user);
    }

    public interface OnFollowClick {
        void onFollowClick(UserListItem user);
    }

    private final ArrayList<UserListItem> data;
    private final OnUserClick listener;
    private final OnFollowClick followListener;

    public UserListAdapter(ArrayList<UserListItem> data, OnUserClick listener) {
        this(data, listener, null);
    }

    public UserListAdapter(ArrayList<UserListItem> data,
                           OnUserClick listener,
                           OnFollowClick followListener) {
        this.data = data;
        this.listener = listener;
        this.followListener = followListener;
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

        bindFollowButton(h, user);

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

    private void bindFollowButton(@NonNull VH h, UserListItem user) {
        if (h.btnFollow == null) return;
        String currentUid = SessionManager.currentUid();
        boolean canFollow = followListener != null
                && currentUid != null
                && user.getUid() != null
                && !currentUid.equals(user.getUid());
        h.btnFollow.setVisibility(canFollow ? View.VISIBLE : View.GONE);
        if (!canFollow) return;

        boolean following = user.isFollowing();
        h.btnFollow.setText(following ? "Siguiendo" : "Seguir");
        h.btnFollow.setTextColor(h.itemView.getResources().getColor(following
                ? R.color.texto_principal
                : R.color.texto_sobre_principal));
        h.btnFollow.setBackgroundTintList(h.itemView.getContext().getColorStateList(following
                ? R.color.fondo_superficie
                : R.color.color_principal_variante));
        h.btnFollow.setOnClickListener(v -> followListener.onFollowClick(user));
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
        MaterialButton btnFollow;

        VH(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivUserListAvatar);
            tvAvatar = itemView.findViewById(R.id.tvUserListAvatar);
            tvName = itemView.findViewById(R.id.tvUserListName);
            tvHandle = itemView.findViewById(R.id.tvUserListHandle);
            tvBio = itemView.findViewById(R.id.tvUserListBio);
            btnFollow = itemView.findViewById(R.id.btnUserListFollow);
        }
    }
}
