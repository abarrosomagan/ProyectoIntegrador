package com.sazon.proyectointegrador.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.Gravity;
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
import java.util.Map;

/**
 * Adapter del chat con dos tipos de vista:
 *   - TYPE_HEADER  → chip flotante con la fecha
 *   - TYPE_MESSAGE → burbuja asimétrica con texto, hora, check de leído y reacciones
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnDeleteMessage {
        void onDelete(@NonNull String docId);
    }

    public interface OnReactionToggle {
        void onReact(@NonNull String docId, @NonNull String emoji, boolean nowReacting);
    }

    public interface OnEditMessage {
        void onEdit(@NonNull String docId, @NonNull String currentText);
    }

    /** Emojis que se ofrecen en el menú de reacciones. */
    static final String[] EMOJI_OPTIONS = { "❤", "😂", "😮", "🔥", "👏" };

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_HEADER  = 1;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm", new Locale("es", "ES"));

    private final List<ChatItem> data;
    private OnDeleteMessage deleteListener;
    private OnReactionToggle reactionListener;
    private OnEditMessage editListener;
    private String myUid;

    public ChatMessageAdapter(List<ChatItem> data) {
        this.data = data;
    }

    public void setOnDeleteMessage(OnDeleteMessage listener) {
        this.deleteListener = listener;
    }

    public void setOnReactionToggle(OnReactionToggle listener) {
        this.reactionListener = listener;
    }

    public void setOnEditMessage(OnEditMessage listener) {
        this.editListener = listener;
    }

    public void setMyUid(String uid) {
        this.myUid = uid;
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
            boolean consecutivo = false;
            if (position > 0) {
                ChatItem prev = data.get(position - 1);
                if (prev instanceof ChatMessage) {
                    consecutivo = ((ChatMessage) prev).isMine() == ((ChatMessage) item).isMine();
                }
            }
            ((MessageVH) holder).bind((ChatMessage) item, consecutivo,
                    deleteListener, reactionListener, editListener, myUid);
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
        final LinearLayout reactionsRow;

        MessageVH(@NonNull View itemView) {
            super(itemView);
            root = (ConstraintLayout) itemView;
            bubble = itemView.findViewById(R.id.bubble);
            tvMsg = itemView.findViewById(R.id.tvMsg);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivCheck = itemView.findViewById(R.id.ivCheck);
            reactionsRow = itemView.findViewById(R.id.reactionsRow);
        }

        void bind(ChatMessage m, boolean consecutivo,
                  OnDeleteMessage deleteListener,
                  OnReactionToggle reactionListener,
                  OnEditMessage editListener,
                  String myUid) {

            Context ctx = root.getContext();
            tvMsg.setText(m.getText());
            tvTime.setText(TIME_FMT.format(new Date(m.getCreatedAt())));

            int topPx = (int) ((consecutivo ? 1 : 3) * ctx.getResources().getDisplayMetrics().density);
            root.setPadding(root.getPaddingLeft(), topPx,
                    root.getPaddingRight(), root.getPaddingBottom());

            // Alineación
            ConstraintSet set = new ConstraintSet();
            set.clone(root);
            set.clear(R.id.bubble, ConstraintSet.START);
            set.clear(R.id.bubble, ConstraintSet.END);
            set.clear(R.id.reactionsRow, ConstraintSet.START);
            set.clear(R.id.reactionsRow, ConstraintSet.END);

            if (m.isMine()) {
                set.connect(R.id.bubble, ConstraintSet.END,
                        ConstraintSet.PARENT_ID, ConstraintSet.END);
                set.connect(R.id.reactionsRow, ConstraintSet.END,
                        ConstraintSet.PARENT_ID, ConstraintSet.END);
                bubble.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_bubble_mine));
                tvMsg.setTextColor(ContextCompat.getColor(ctx, R.color.texto_sobre_principal));
                tvTime.setTextColor(ContextCompat.getColor(ctx, R.color.texto_sobre_principal));
                tvTime.setAlpha(0.75f);
                ivCheck.setVisibility(View.VISIBLE);
                ivCheck.setImageResource(m.isReadByOther()
                        ? R.drawable.ic_check_double : R.drawable.ic_check);
                ivCheck.setAlpha(m.isReadByOther() ? 1f : 0.75f);
            } else {
                set.connect(R.id.bubble, ConstraintSet.START,
                        ConstraintSet.PARENT_ID, ConstraintSet.START);
                set.connect(R.id.reactionsRow, ConstraintSet.START,
                        ConstraintSet.PARENT_ID, ConstraintSet.START);
                bubble.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_bubble_other));
                tvMsg.setTextColor(ContextCompat.getColor(ctx, R.color.texto_principal));
                tvTime.setTextColor(ContextCompat.getColor(ctx, R.color.texto_secundario));
                tvTime.setAlpha(1f);
                ivCheck.setVisibility(View.GONE);
            }

            set.applyTo(root);

            // Reacciones
            pintarReacciones(ctx, m, myUid, reactionListener);

            // Long-press → menú reacciones + copiar + editar + eliminar
            bubble.setOnLongClickListener(v -> {
                mostrarMenuMensaje(ctx, m, deleteListener, reactionListener, editListener, myUid);
                return true;
            });

            // Si el mensaje es una receta compartida, hacemos la burbuja tappable
            if (m.getRecipeId() != null && !m.getRecipeId().isEmpty()) {
                final String rid = m.getRecipeId();
                bubble.setOnClickListener(v -> {
                    android.content.Intent i = new android.content.Intent(ctx,
                            com.sazon.proyectointegrador.RecipeDetailActivity.class);
                    i.putExtra(com.sazon.proyectointegrador.RecipeDetailActivity.EXTRA_RECIPE_ID, rid);
                    ctx.startActivity(i);
                });
            } else {
                bubble.setOnClickListener(null);
                bubble.setClickable(false);
            }

            // Indicador "Editado"
            if (m.isEdited()) {
                tvTime.setText(tvTime.getText() + " · editado");
            }
        }

        private void pintarReacciones(Context ctx, ChatMessage m, String myUid,
                                      OnReactionToggle listener) {
            if (reactionsRow == null) return;
            reactionsRow.removeAllViews();

            Map<String, List<String>> reactions = m.getReactions();
            boolean tieneAlguna = false;
            if (reactions != null) {
                for (Map.Entry<String, List<String>> entry : reactions.entrySet()) {
                    List<String> uids = entry.getValue();
                    if (uids == null || uids.isEmpty()) continue;
                    tieneAlguna = true;

                    boolean meReaccion = myUid != null && uids.contains(myUid);
                    TextView chip = new TextView(ctx);
                    chip.setText(entry.getKey() + " " + uids.size());
                    chip.setTextSize(11f);
                    chip.setTextColor(ContextCompat.getColor(ctx,
                            meReaccion ? R.color.color_principal_variante : R.color.texto_principal));
                    int hPad = (int) (8 * ctx.getResources().getDisplayMetrics().density);
                    int vPad = (int) (3 * ctx.getResources().getDisplayMetrics().density);
                    chip.setPadding(hPad, vPad, hPad, vPad);
                    chip.setBackgroundResource(meReaccion
                            ? R.drawable.bg_reaction_chip_mine
                            : R.drawable.bg_reaction_chip);
                    chip.setGravity(Gravity.CENTER_VERTICAL);

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    int margin = (int) (4 * ctx.getResources().getDisplayMetrics().density);
                    lp.setMargins(0, 0, margin, 0);
                    chip.setLayoutParams(lp);

                    final String emoji = entry.getKey();
                    chip.setOnClickListener(v -> {
                        if (listener == null || m.getDocId() == null) return;
                        listener.onReact(m.getDocId(), emoji, !meReaccion);
                    });

                    reactionsRow.addView(chip);
                }
            }
            reactionsRow.setVisibility(tieneAlguna ? View.VISIBLE : View.GONE);
        }

        private void mostrarMenuMensaje(Context ctx, ChatMessage m,
                                        OnDeleteMessage delete,
                                        OnReactionToggle reactionListener,
                                        OnEditMessage editListener,
                                        String myUid) {
            boolean puedeReaccionar = m.getDocId() != null && reactionListener != null;
            // Solo se puede editar texto plano del usuario, no recetas compartidas
            boolean puedeEditar = m.isMine() && m.getDocId() != null
                    && editListener != null
                    && (m.getRecipeId() == null || m.getRecipeId().isEmpty());
            boolean puedeBorrar = m.isMine() && m.getDocId() != null && delete != null;

            java.util.List<String> ops = new java.util.ArrayList<>();
            if (puedeReaccionar) ops.add("Reaccionar…");
            ops.add("Copiar");
            if (puedeEditar)   ops.add("Editar");
            if (puedeBorrar)   ops.add("Eliminar");

            String[] opciones = ops.toArray(new String[0]);

            new AlertDialog.Builder(ctx)
                    .setItems(opciones, (d, which) -> {
                        String accion = opciones[which];
                        if ("Reaccionar…".equals(accion)) {
                            elegirReaccion(ctx, m, reactionListener, myUid);
                        } else if ("Copiar".equals(accion)) {
                            copiarAlPortapapeles(ctx, m.getText());
                        } else if ("Editar".equals(accion)) {
                            editListener.onEdit(m.getDocId(), m.getText() == null ? "" : m.getText());
                        } else if ("Eliminar".equals(accion)) {
                            confirmarEliminar(ctx, m, delete);
                        }
                    })
                    .show();
        }

        private void confirmarEliminar(Context ctx, ChatMessage m, OnDeleteMessage delete) {
            new AlertDialog.Builder(ctx)
                    .setTitle("Eliminar mensaje")
                    .setMessage("Se borrará para todos.")
                    .setPositiveButton("Eliminar", (d, w) -> delete.onDelete(m.getDocId()))
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private void elegirReaccion(Context ctx, ChatMessage m,
                                    OnReactionToggle listener, String myUid) {
            new AlertDialog.Builder(ctx)
                    .setTitle("Tu reacción")
                    .setItems(EMOJI_OPTIONS, (d, which) -> {
                        if (listener == null || m.getDocId() == null) return;
                        String emoji = EMOJI_OPTIONS[which];
                        boolean meReaccion = m.reactedByMe(emoji, myUid);
                        listener.onReact(m.getDocId(), emoji, !meReaccion);
                    })
                    .show();
        }

        private static void copiarAlPortapapeles(Context ctx, String text) {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("mensaje", text));
                Toast.makeText(ctx, "Copiado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
