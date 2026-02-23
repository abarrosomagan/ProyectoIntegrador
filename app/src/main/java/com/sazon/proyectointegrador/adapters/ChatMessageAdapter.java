package com.sazon.proyectointegrador.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.ChatMessage;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.VH> {

    private final List<ChatMessage> data;

    public ChatMessageAdapter(List<ChatMessage> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatMessage m = data.get(position);
        h.tvMsg.setText(m.getText());

        Context ctx = h.root.getContext();

        ConstraintSet set = new ConstraintSet();
        set.clone(h.root);

        set.clear(R.id.bubble, ConstraintSet.START);
        set.clear(R.id.bubble, ConstraintSet.END);

        if (m.isMine()) {
            set.connect(R.id.bubble, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            h.bubble.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.color_principal_variante));
            h.tvMsg.setTextColor(ContextCompat.getColor(ctx, R.color.texto_sobre_principal));
        } else {
            set.connect(R.id.bubble, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            h.bubble.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.fondo_superficie));
            h.tvMsg.setTextColor(ContextCompat.getColor(ctx, R.color.texto_principal));
        }

        set.applyTo(h.root);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void addMessage(ChatMessage msg) {
        data.add(msg);
        notifyItemInserted(data.size() - 1);
    }

    static class VH extends RecyclerView.ViewHolder {
        ConstraintLayout root;
        MaterialCardView bubble;
        TextView tvMsg;

        VH(@NonNull View itemView) {
            super(itemView);
            root = (ConstraintLayout) itemView;
            bubble = itemView.findViewById(R.id.bubble);
            tvMsg = itemView.findViewById(R.id.tvMsg);
        }
    }
}