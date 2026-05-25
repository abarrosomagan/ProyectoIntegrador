package com.sazon.proyectointegrador.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.ChatThread;

import java.util.List;

public class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.VH> {

    public interface OnChatClick {
        void onClick(ChatThread chat);
    }

    public interface OnChatLongClick {
        void onLongClick(ChatThread chat);
    }

    private final List<ChatThread> data;
    private final OnChatClick listener;
    private OnChatLongClick longListener;

    public ChatThreadAdapter(List<ChatThread> data, OnChatClick listener) {
        this.data = data;
        this.listener = listener;
    }

    public void setOnChatLongClick(OnChatLongClick l) {
        this.longListener = l;
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
        h.tvTime.setText(c.getTime());

        // Inicial del avatar
        String avatar = c.getName() == null || c.getName().isEmpty()
                ? "?"
                : ("" + Character.toUpperCase(c.getName().charAt(0)));
        h.tvAvatar.setText(avatar);

        // Último mensaje: si soy yo el que lo envió, prefijo "Tú: "
        String preview = c.getLastMessage() == null ? "" : c.getLastMessage();
        if (c.isLastIsMine() && !preview.isEmpty() && !preview.equals("escribiendo...")) {
            preview = "Tú: " + preview;
        }
        // "Escribiendo..." en cursiva mediante color de acento
        if ("escribiendo...".equals(preview)) {
            h.tvLastMessage.setTextColor(
                    ContextCompat.getColor(h.itemView.getContext(),
                            R.color.color_principal_variante));
        } else {
            h.tvLastMessage.setTextColor(
                    ContextCompat.getColor(h.itemView.getContext(),
                            R.color.texto_secundario));
        }
        h.tvLastMessage.setText(preview);

        // Check de entrega/lectura solo para mensajes propios
        if (c.isLastIsMine() && !"escribiendo...".equals(preview) && !preview.isEmpty()) {
            h.ivLastStatus.setVisibility(View.VISIBLE);
            h.ivLastStatus.setImageResource(c.isLastReadByOther()
                    ? R.drawable.ic_check_double
                    : R.drawable.ic_check);
            int tintColor = c.isLastReadByOther()
                    ? R.color.color_principal_variante
                    : R.color.texto_secundario;
            h.ivLastStatus.setColorFilter(
                    ContextCompat.getColor(h.itemView.getContext(), tintColor));
        } else {
            h.ivLastStatus.setVisibility(View.GONE);
        }

        // Puntito de presencia (verde) si el otro está activo o escribiendo
        h.presenceDot.setVisibility(c.isPresenceActive() ? View.VISIBLE : View.GONE);

        // Iconos pin / mute
        h.ivPinned.setVisibility(c.isPinned() ? View.VISIBLE : View.GONE);
        h.ivMuted.setVisibility(c.isMuted() ? View.VISIBLE : View.GONE);

        // Badge de no leídos: si el chat está silenciado lo bajamos al gris para no llamar la atención
        if (c.getUnread() > 0) {
            h.badge.setVisibility(View.VISIBLE);
            h.tvUnread.setText(String.valueOf(c.getUnread()));
            int badgeColor = c.isMuted()
                    ? R.color.texto_secundario
                    : R.color.color_principal;
            h.badge.setCardBackgroundColor(
                    ContextCompat.getColor(h.itemView.getContext(), badgeColor));
            // Cuando hay no leídos, el nombre y el último mensaje se ven más oscuros
            h.tvName.setTextColor(
                    ContextCompat.getColor(h.itemView.getContext(), R.color.texto_principal));
            if (!"escribiendo...".equals(preview)) {
                h.tvLastMessage.setTextColor(
                        ContextCompat.getColor(h.itemView.getContext(), R.color.texto_principal));
            }
            h.tvLastMessage.setTypeface(h.tvLastMessage.getTypeface(),
                    android.graphics.Typeface.BOLD);
        } else {
            h.badge.setVisibility(View.GONE);
            h.tvLastMessage.setTypeface(android.graphics.Typeface.DEFAULT);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(c));
        h.itemView.setOnLongClickListener(v -> {
            if (longListener != null) {
                longListener.onLongClick(c);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvLast, tvTime, tvUnread;
        TextView tvLastMessage;
        ImageView ivLastStatus, ivPinned, ivMuted;
        View presenceDot;
        MaterialCardView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLast = tvLastMessage; // alias para compat
            tvTime = itemView.findViewById(R.id.tvTime);
            badge = itemView.findViewById(R.id.badge);
            tvUnread = itemView.findViewById(R.id.tvUnread);
            ivLastStatus = itemView.findViewById(R.id.ivLastStatus);
            ivPinned = itemView.findViewById(R.id.ivPinned);
            ivMuted = itemView.findViewById(R.id.ivMuted);
            presenceDot = itemView.findViewById(R.id.presenceDot);
        }
    }
}
