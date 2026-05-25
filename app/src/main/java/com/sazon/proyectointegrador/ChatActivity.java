package com.sazon.proyectointegrador;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.sazon.proyectointegrador.adapters.ChatMessageAdapter;
import com.sazon.proyectointegrador.model.ChatDateHeader;
import com.sazon.proyectointegrador.model.ChatItem;
import com.sazon.proyectointegrador.model.ChatMessage;
import com.sazon.proyectointegrador.util.DemoData;
import com.sazon.proyectointegrador.util.SessionManager;
import com.sazon.proyectointegrador.util.SimpleTextWatcher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chat_id";
    public static final String EXTRA_CHAT_NAME = "chat_name";

    private RecyclerView rvMessages;
    private ChatMessageAdapter adapter;
    private final ArrayList<ChatItem> items = new ArrayList<>();

    private TextInputEditText etMessage;
    private MaterialButton btnSend;
    private TextView tvChatAvatar;
    private TextView tvChatSubtitle;
    private TextView tvChatEmpty;
    private View presenceDotHeader;

    private String chatId;
    private String chatName;
    private String otherUid;

    private ListenerRegistration messagesListener;
    private ListenerRegistration chatStateListener;
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private boolean typingSent = false;
    private final Runnable stopTypingRunnable = () -> updateTyping(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        chatName = getIntent().getStringExtra(EXTRA_CHAT_NAME);
        if (chatName == null) chatName = "Chat";

        if (TextUtils.isEmpty(chatId)) {
            Toast.makeText(this, "Chat no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tvChatTitle);
        tvTitle.setText(chatName);

        tvChatAvatar = findViewById(R.id.tvChatAvatar);
        if (tvChatAvatar != null && !chatName.isEmpty()) {
            tvChatAvatar.setText(String.valueOf(Character.toUpperCase(chatName.charAt(0))));
        }

        tvChatSubtitle = findViewById(R.id.tvChatSubtitle);
        if (tvChatSubtitle != null) tvChatSubtitle.setText("Activo recientemente");

        presenceDotHeader = findViewById(R.id.presenceDotHeader);

        tvChatEmpty = findViewById(R.id.tvChatEmpty);
        if (tvChatEmpty != null && chatName != null) {
            tvChatEmpty.setText("👋 Aún no hay mensajes.\nSé el primero en saludar a " + chatName + ".");
        }

        ImageButton btnBack = findViewById(R.id.btnBackProfile);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(items);
        adapter.setOnDeleteMessage(this::eliminarMensaje);
        adapter.setOnReactionToggle(this::reaccionarMensaje);
        adapter.setMyUid(SessionManager.currentUid());
        rvMessages.setAdapter(adapter);

        // El otro participante lo derivamos del chatId determinista ("uidA_uidB")
        deduceOtherUid();

        btnSend.setOnClickListener(v -> sendMessage());
        setupTypingWatcher();

        // Restaurar borrador (si el usuario empezó a escribir y salió sin enviar)
        restaurarBorrador();
    }

    private void deduceOtherUid() {
        if (DemoData.isDemoChatId(chatId)) {
            otherUid = null;
            return;
        }
        String meUid = SessionManager.currentUid();
        if (meUid == null) return;
        String[] parts = chatId.split("_");
        if (parts.length == 2) {
            otherUid = parts[0].equals(meUid) ? parts[1] : parts[0];
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DemoData.isDemoChatId(chatId)) {
            cargarMensajesDemo();
        } else {
            updatePresence(true);
            listenChatState();
            listenMessages();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (chatStateListener != null) {
            chatStateListener.remove();
            chatStateListener = null;
        }
        typingHandler.removeCallbacks(stopTypingRunnable);
        if (!DemoData.isDemoChatId(chatId)) {
            updateTyping(false);
            updatePresence(false);
        }

        // Guardar borrador al salir del chat
        guardarBorrador();
    }

    // ===== Borradores =====

    private static final String PREFS_DRAFTS = "chat_drafts";

    private void restaurarBorrador() {
        if (etMessage == null || chatId == null) return;
        String draft = getSharedPreferences(PREFS_DRAFTS, MODE_PRIVATE)
                .getString(chatId, null);
        if (draft != null && !draft.isEmpty()) {
            etMessage.setText(draft);
            etMessage.setSelection(draft.length());
        }
    }

    private void guardarBorrador() {
        if (etMessage == null || chatId == null) return;
        String texto = etMessage.getText() != null
                ? etMessage.getText().toString()
                : "";
        if (texto.trim().isEmpty()) {
            getSharedPreferences(PREFS_DRAFTS, MODE_PRIVATE)
                    .edit().remove(chatId).apply();
        } else {
            getSharedPreferences(PREFS_DRAFTS, MODE_PRIVATE)
                    .edit().putString(chatId, texto).apply();
        }
    }

    private void limpiarBorrador() {
        if (chatId == null) return;
        getSharedPreferences(PREFS_DRAFTS, MODE_PRIVATE)
                .edit().remove(chatId).apply();
    }

    private void cargarMensajesDemo() {
        items.clear();
        items.addAll(DemoData.messages(chatId));
        adapter.notifyDataSetChanged();
        actualizarEstadoVacio();
        scrollToBottom();
    }

    // ===== Carga en tiempo real =====

    private void listenMessages() {
        FirebaseUser user = SessionManager.currentUser();
        if (user == null) return;
        final String meUid = user.getUid();

        messagesListener = SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    items.clear();
                    List<DocumentSnapshot> docsParaMarcar = new ArrayList<>();
                    Calendar prevDay = null;

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String text = doc.getString("text");
                        String senderId = doc.getString("senderId");
                        Timestamp ts = doc.getTimestamp("createdAt");
                        List<String> readBy = (List<String>) doc.get("readBy");

                        java.util.Map<String, java.util.List<String>> reactionsParsed
                                = new java.util.HashMap<>();
                        Object reactionsObj = doc.get("reactions");
                        if (reactionsObj instanceof java.util.Map) {
                            for (java.util.Map.Entry<?, ?> entry :
                                    ((java.util.Map<?, ?>) reactionsObj).entrySet()) {
                                if (entry.getKey() != null
                                        && entry.getValue() instanceof java.util.List) {
                                    java.util.List<String> uids = new java.util.ArrayList<>();
                                    for (Object o : (java.util.List<?>) entry.getValue()) {
                                        if (o != null) uids.add(o.toString());
                                    }
                                    if (!uids.isEmpty()) {
                                        reactionsParsed.put(entry.getKey().toString(), uids);
                                    }
                                }
                            }
                        }

                        long createdAt = ts != null ? ts.toDate().getTime()
                                : System.currentTimeMillis();
                        boolean mine = senderId != null && senderId.equals(meUid);

                        Calendar thisDay = Calendar.getInstance();
                        thisDay.setTimeInMillis(createdAt);
                        if (prevDay == null || !sameDay(prevDay, thisDay)) {
                            items.add(new ChatDateHeader(formatDayLabel(thisDay)));
                            prevDay = thisDay;
                        }

                        boolean readByOther = mine && otherUid != null
                                && readBy != null && readBy.contains(otherUid);

                        items.add(new ChatMessage(
                                text != null ? text : "",
                                mine,
                                createdAt,
                                doc.getId(),
                                readByOther,
                                reactionsParsed));

                        // Si es del otro y aún no lo he leído, lo marco
                        if (!mine && (readBy == null || !readBy.contains(meUid))) {
                            docsParaMarcar.add(doc);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    actualizarEstadoVacio();
                    scrollToBottom();

                    if (!docsParaMarcar.isEmpty()) {
                        marcarComoLeidos(docsParaMarcar, meUid);
                    }
                });
    }

    /** Añade meUid al array readBy de cada mensaje en un único batch. */
    private void marcarComoLeidos(List<DocumentSnapshot> docs, String meUid) {
        WriteBatch batch = SessionManager.db().batch();
        for (DocumentSnapshot d : docs) {
            batch.update(d.getReference(), "readBy", FieldValue.arrayUnion(meUid));
        }
        batch.commit(); // los errores aquí no rompen nada visible

        Map<String, Object> readUpdate = new HashMap<>();
        readUpdate.put("lastReadAt." + meUid, FieldValue.serverTimestamp());
        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .set(readUpdate, SetOptions.merge());
    }

    // ===== Envío =====

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        if (DemoData.isDemoChatId(chatId)) {
            etMessage.setText("");
            limpiarBorrador();
            items.add(new ChatMessage(text, true, System.currentTimeMillis()));
            adapter.notifyDataSetChanged();
            actualizarEstadoVacio();
            scrollToBottom();
            return;
        }

        FirebaseUser user = SessionManager.currentUser();
        if (user == null) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText("");
        limpiarBorrador();

        Map<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("senderId", user.getUid());
        msg.put("createdAt", FieldValue.serverTimestamp());
        msg.put("readBy", java.util.Collections.singletonList(user.getUid()));

        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .collection("messages")
                .add(msg);

        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", text);
        chatUpdate.put("lastSenderId", user.getUid());
        chatUpdate.put("lastMessageAt", FieldValue.serverTimestamp());

        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .set(chatUpdate, SetOptions.merge());

        updateTyping(false);
    }

    // ===== Eliminar =====

    private void reaccionarMensaje(String docId, String emoji, boolean nowReacting) {
        if (DemoData.isDemoChatId(chatId) || docId == null || emoji == null) return;
        String uid = SessionManager.currentUid();
        if (uid == null) return;

        Object change = nowReacting
                ? FieldValue.arrayUnion(uid)
                : FieldValue.arrayRemove(uid);
        Map<String, Object> update = new HashMap<>();
        update.put("reactions." + emoji, change);

        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .collection("messages")
                .document(docId)
                .set(update, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudo reaccionar",
                                Toast.LENGTH_SHORT).show());
    }

    private void eliminarMensaje(String docId) {
        if (DemoData.isDemoChatId(chatId) || docId == null) return;
        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .collection("messages")
                .document(docId)
                .delete()
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_SHORT).show());
    }

    private void scrollToBottom() {
        if (adapter != null && adapter.getItemCount() > 0) {
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void actualizarEstadoVacio() {
        if (tvChatEmpty == null || rvMessages == null) return;
        boolean conMensajes = false;
        for (ChatItem it : items) {
            if (it instanceof ChatMessage) { conMensajes = true; break; }
        }
        tvChatEmpty.setVisibility(conMensajes ? View.GONE : View.VISIBLE);
    }

    // ===== Presencia y escribiendo =====

    private void setupTypingWatcher() {
        if (etMessage == null) return;
        etMessage.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (DemoData.isDemoChatId(chatId)) return;
                boolean hasText = s != null && s.toString().trim().length() > 0;
                if (hasText) {
                    updateTyping(true);
                    typingHandler.removeCallbacks(stopTypingRunnable);
                    typingHandler.postDelayed(stopTypingRunnable, 1800);
                } else {
                    updateTyping(false);
                }
            }
        });
    }

    private void listenChatState() {
        if (otherUid == null) return;
        chatStateListener = SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    Boolean typing = doc.getBoolean("presence." + otherUid + ".typing");
                    Boolean active = doc.getBoolean("presence." + otherUid + ".active");
                    Timestamp lastSeen = doc.getTimestamp("presence." + otherUid + ".lastSeen");

                    if (Boolean.TRUE.equals(typing)) {
                        tvChatSubtitle.setText("escribiendo...");
                    } else if (Boolean.TRUE.equals(active)) {
                        tvChatSubtitle.setText("en línea");
                    } else {
                        tvChatSubtitle.setText(formatLastSeen(lastSeen));
                    }
                    if (presenceDotHeader != null) {
                        boolean enLinea = Boolean.TRUE.equals(active)
                                || Boolean.TRUE.equals(typing);
                        presenceDotHeader.setVisibility(enLinea ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void updatePresence(boolean active) {
        String uid = SessionManager.currentUid();
        if (uid == null || DemoData.isDemoChatId(chatId)) return;

        Map<String, Object> data = new HashMap<>();
        data.put("presence." + uid + ".active", active);
        data.put("presence." + uid + ".typing", false);
        data.put("presence." + uid + ".lastSeen", FieldValue.serverTimestamp());

        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .set(data, SetOptions.merge());
        typingSent = false;
    }

    private void updateTyping(boolean typing) {
        String uid = SessionManager.currentUid();
        if (uid == null || DemoData.isDemoChatId(chatId)) return;
        if (typingSent == typing) return;
        typingSent = typing;

        Map<String, Object> data = new HashMap<>();
        data.put("presence." + uid + ".typing", typing);
        data.put("presence." + uid + ".active", true);
        data.put("presence." + uid + ".lastSeen", FieldValue.serverTimestamp());

        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .set(data, SetOptions.merge());
    }

    private static String formatLastSeen(Timestamp ts) {
        if (ts == null) return "Activo recientemente";
        long diff = System.currentTimeMillis() - ts.toDate().getTime();
        if (diff < 0) diff = 0;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);
        if (minutes < 1) return "Activo ahora";
        if (minutes < 60) return "Activo hace " + minutes + " min";
        if (hours < 24) return "Activo hace " + hours + " h";
        if (days == 1) return "Activo ayer";
        return "Activo hace " + days + " días";
    }

    // ===== Helpers fecha =====

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static String formatDayLabel(Calendar day) {
        Calendar now = Calendar.getInstance();
        if (sameDay(day, now)) return "Hoy";

        Calendar yesterday = (Calendar) now.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(day, yesterday)) return "Ayer";

        Locale es = new Locale("es", "ES");
        if (day.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            return new SimpleDateFormat("d 'de' MMMM", es).format(day.getTime());
        }
        return new SimpleDateFormat("d MMM yyyy", es).format(day.getTime());
    }
}
