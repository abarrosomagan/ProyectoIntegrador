package com.sazon.proyectointegrador.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
 *   - TYPE_MESSAGE → burbuja asimétrica con texto, hora y check de leído
 *
 * Las burbujas de "yo" llevan tail abajo a la derecha y color naranja;
 * las del otro, tail abajo a la izquierda y fondo crema con borde.
 * Mensajes consecutivos del mismo emisor se agrupan visualmente (menos padding).
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnDeleteMessage {
        void onDelete(@NonNull String docId);
    }

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_HEADER  = 1;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm", new Locale("es", "ES"));

    private final List<ChatItem> data;
    private OnDeleteMessage deleteListener;

    public ChatMessageAdapter(List<ChatItem> data) {
        this.data = data;
    }

    public void setOnDeleteMessage(OnDeleteMessage listener) {
        this.deleteListener = listener;
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
            // ¿el mensaje anterior es del mismo emisor? → consecutivo
            boolean consecutivo = false;
            if (position > 0) {
                ChatItem prev = data.get(position - 1);
                if (prev instanceof ChatMessage) {
                    consecutivo = ((ChatMessage) prev).isMine() == ((ChatMessage) item).isMine();
                }
            }
            ((MessageVH) holder).bind((ChatMessage) item, consecutivo, deleteListener);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

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
        final ImageView ivCheck;

        MessageVH(@NonNull View itemView) {
            super(itemView);
            root = (ConstraintLayout) itemView;
            bubble = itemView.findViewById(R.id.bubble);
            tvMsg = itemView.findViewById(R.id.tvMsg);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivCheck = itemView.findViewById(R.id.ivCheck);
        }

        void bind(ChatMessage m, boolean consecutivo, OnDeleteMessage deleteListener) {
            Context ctx = root.getContext();
            tvMsg.setText(m.getText());
            tvTime.setText(TIME_FMT.format(new Date(m.getCreatedAt())));

            // Padding vertical reducido si el mensaje anterior es del mismo emisor
            int topPx = (int) ((consecutivo ? 1 : 3) * ctx.getResources().getDisplayMetrics().density);
            root.setPadding(root.getPaddingLeft(), topPx,
                    root.getPaddingRight(), root.getPaddingBottom());

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

                // Check de leído solo en mis mensajes
                ivCheck.setVisibility(View.VISIBLE);
                ivCheck.setImageResource(m.isReadByOther()
                        ? R.drawable.ic_check_double : R.drawable.ic_check);
                ivCheck.setAlpha(m.isReadByOther() ? 1f : 0.75f);
            } else {
                set.connect(R.id.bubble, ConstraintSet.START,
                        ConstraintSet.PARENT_ID, ConstraintSet.START);
                bubble.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_bubble_other));
                tvMsg.setTextColor(ContextCompat.getColor(ctx, R.color.texto_principal));
                tvTime.setTextColor(ContextCompat.getColor(ctx, R.color.texto_secundario));
                tvTime.setAlpha(1f);
                ivCheck.setVisibility(View.GONE);
            }

            set.applyTo(root);

            // Long-press → menú Copiar / Eliminar (Eliminar solo en los míos)
            bubble.setOnLongClickListener(v -> {
                mostrarMenuMensaje(ctx, m, deleteListener);
                return true;
            });
        }

        private void mostrarMenuMensaje(Context ctx, ChatMessage m, OnDeleteMessage del) {
            boolean puedeBorrar = m.isMine() && m.getDocId() != null && del != null;
            String[] opciones = puedeBorrar
                    ? new String[]{ "Copiar", "Eliminar" }
                    : new String[]{ "Copiar" };

            new AlertDialog.Builder(ctx)
                    .setItems(opciones, (d, which) -> {
                        if (which == 0) {
                            ClipboardManager cm = (ClipboardManager)
                                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                            if (cm != null) {
                                cm.setPrimaryClip(ClipData.newPlainText("mensaje", m.getText()));
                                Toast.makeText(ctx, "Copiado", Toast.LENGTH_SHORT).show();
                            }
                        } else if (which == 1 && puedeBorrar) {
                            del.onDelete(m.getDocId());
                        }
                    })
                    .show();
        }
    }
}
