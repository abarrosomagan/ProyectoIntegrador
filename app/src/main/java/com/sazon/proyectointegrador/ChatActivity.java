package com.sazon.proyectointegrador;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.sazon.proyectointegrador.adapters.ChatMessageAdapter;
import com.sazon.proyectointegrador.model.ChatDateHeader;
import com.sazon.proyectointegrador.model.ChatItem;
import com.sazon.proyectointegrador.model.ChatMessage;
import com.sazon.proyectointegrador.util.DemoData;
import com.sazon.proyectointegrador.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

    private String chatId;
    private String chatName;

    private ListenerRegistration messagesListener;

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
        if (tvChatSubtitle != null) {
            // Estado por defecto. Cuando metamos presencia (Realtime DB) lo cambiamos en vivo.
            tvChatSubtitle.setText("Activo recientemente");
        }

        ImageButton btnBack = findViewById(R.id.btnBackProfile);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(items);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DemoData.isDemoChatId(chatId)) {
            cargarMensajesDemo();
        } else {
            listenMessages();
        }
    }

    private void cargarMensajesDemo() {
        items.clear();
        items.addAll(DemoData.messages(chatId));
        adapter.notifyDataSetChanged();
        scrollToBottom();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    // ===== Carga en tiempo real =====

    private void listenMessages() {
        FirebaseUser user = SessionManager.currentUser();
        if (user == null) return;

        messagesListener = SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    items.clear();

                    Calendar prevDay = null;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String text = doc.getString("text");
                        String senderId = doc.getString("senderId");
                        Timestamp ts = doc.getTimestamp("createdAt");

                        long createdAt = ts != null ? ts.toDate().getTime()
                                : System.currentTimeMillis();
                        boolean mine = senderId != null && senderId.equals(user.getUid());

                        // Cabecera de fecha si cambiamos de día respecto al mensaje anterior
                        Calendar thisDay = Calendar.getInstance();
                        thisDay.setTimeInMillis(createdAt);
                        if (prevDay == null || !sameDay(prevDay, thisDay)) {
                            items.add(new ChatDateHeader(formatDayLabel(thisDay)));
                            prevDay = thisDay;
                        }

                        items.add(new ChatMessage(text != null ? text : "", mine, createdAt));
                    }
                    adapter.notifyDataSetChanged();
                    scrollToBottom();
                });
    }

    // ===== Envío =====

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        // En los chats demo no tocamos Firestore: añadimos el mensaje al vuelo
        if (DemoData.isDemoChatId(chatId)) {
            etMessage.setText("");
            items.add(new ChatMessage(text, true, System.currentTimeMillis()));
            adapter.notifyDataSetChanged();
            scrollToBottom();
            return;
        }

        FirebaseUser user = SessionManager.currentUser();
        if (user == null) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText("");

        Map<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("senderId", user.getUid());
        msg.put("createdAt", FieldValue.serverTimestamp());

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
    }

    private void scrollToBottom() {
        if (adapter != null && adapter.getItemCount() > 0) {
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
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
