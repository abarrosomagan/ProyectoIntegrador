package com.sazon.proyectointegrador.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.ChatDateHeader;
import com.sazon.proyectointegrador.model.ChatItem;
import com.sazon.proyectointegrador.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter del chat con dos tipos de vista:
 *   - TYPE_HEADER  → chip flotante con la fecha ("Hoy", "Ayer", "12 may")
 *   - TYPE_MESSAGE → burbuja asimétrica con texto y hora dentro
 *
 * Las burbujas de "yo" llevan tail abajo a la derecha y color naranja;
 * las del otro, tail abajo a la izquierda y fondo crema con borde.
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_HEADER  = 1;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm", new Locale("es", "ES"));

    private final List<ChatItem> data;

    public ChatMessageAdapter(List<ChatItem> data) {
        this.data = data;
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position) instanceof ChatDateHeader ? TYPE_HEADER : TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_message_header, parent, false);
            return new HeaderVH(v);
        }
        View v = inflater.inflate(R.layout.item_message, parent, false);
        return new MessageVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatItem item = data.get(position);
        if (holder instanceof HeaderVH && item instanceof ChatDateHeader) {
            ((HeaderVH) holder).bind((ChatDateHeader) item);
        } else if (holder instanceof MessageVH && item instanceof ChatMessage) {
            ((MessageVH) holder).bind((ChatMessage) item);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /** API legacy: añade un mensaje al final y notifica. */
    public void addMessage(ChatMessage msg) {
        data.add(msg);
        notifyItemInserted(data.size() - 1);
    }

    // ===== ViewHolders =====

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView tvDate;

        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateHeader);
        }

        void bind(ChatDateHeader h) {
            tvDate.setText(h.getLabel());
        }
    }

    static class MessageVH extends RecyclerView.ViewHolder {
        final ConstraintLayout root;
        final LinearLayout bubble;
        final TextView tvMsg;
        final TextView tvTime;

        MessageVH(@NonNull View itemView) {
            super(itemView);
            root = (ConstraintLayout) itemView;
            bubble = itemView.findViewById(R.id.bubble);
            tvMsg = itemView.findViewById(R.id.tvMsg);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(ChatMessage m) {
            Context ctx = root.getContext();
            tvMsg.setText(m.getText());
            tvTime.setText(TIME_FMT.format(new Date(m.getCreatedAt())));

            // Alineación + fondo + colores según remitente
            ConstraintSet set = new ConstraintSet();
            set.clone(root);
            set.clear(R.id.bubble, ConstraintSet.START);
            set.clear(R.id.bubble, ConstraintSet.END);

            if (m.isMine()) {
                set.connect(R.id.bubble, ConstraintSet.END,
                        ConstraintSet.PARENT_ID, ConstraintSet.END);
                bubble.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_bubble_mine));
                tvMsg.setTextColor(ContextCompat.getColor(ctx, R.color.texto_sobre_principal));
                tvTime.setTextColor(ContextCompat.getColor(ctx, R.color.texto_sobre_principal));
                tvTime.setAlpha(0.75f);
            } else {
                set.connect(R.id.bubble, ConstraintSet.START,
                        ConstraintSet.PARENT_ID, ConstraintSet.START);
                bubble.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_bubble_other));
                tvMsg.setTextColor(ContextCompat.getColor(ctx, R.color.texto_principal));
                tvTime.setTextColor(ContextCompat.getColor(ctx, R.color.texto_secundario));
                tvTime.setAlpha(1f);
            }

            set.applyTo(root);
        }
    }
}
