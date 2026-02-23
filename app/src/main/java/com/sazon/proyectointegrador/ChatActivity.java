package com.sazon.proyectointegrador;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sazon.proyectointegrador.adapters.ChatMessageAdapter;
import com.sazon.proyectointegrador.model.ChatMessage;

import java.util.ArrayList;

// ===== Firebase imports (DEJAR COMENTADOS) =====
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.auth.FirebaseUser;
// import com.google.firebase.firestore.DocumentSnapshot;
// import com.google.firebase.firestore.FieldValue;
// import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.firebase.firestore.ListenerRegistration;
// import com.google.firebase.firestore.Query;
// import com.google.firebase.firestore.SetOptions;
// import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chat_id";
    public static final String EXTRA_CHAT_NAME = "chat_name";

    private RecyclerView rvMessages;
    private ChatMessageAdapter adapter;
    private final ArrayList<ChatMessage> messages = new ArrayList<>();

    private TextInputEditText etMessage;
    private MaterialButton btnSend;

    private String chatId;

    // ===== Firebase (DEJAR COMENTADO) =====
    // private FirebaseAuth firebaseAuth;
    // private FirebaseFirestore firestore;
    // private ListenerRegistration messagesListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        String chatName = getIntent().getStringExtra(EXTRA_CHAT_NAME);
        if (chatName == null) chatName = "Chat";

        TextView tvTitle = findViewById(R.id.tvChatTitle);
        tvTitle.setText(chatName);

        ImageButton btnBack = findViewById(R.id.btnBackProfile);
        btnBack.setOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        // ===== Modo mock (FUNCIONAL) =====
        messages.clear();
        messages.addAll(createMockMessages());

        adapter = new ChatMessageAdapter(messages);
        rvMessages.setAdapter(adapter);
        scrollToBottom();

        btnSend.setOnClickListener(v -> sendMessage());

        // ===== Firebase listo (comentado) =====
        /*
        if (AppConfig.USE_FIREBASE) {
            initFirebase();
            listenMessagesFirestore();
        }
        */
    }

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        etMessage.setText("");

        // ===== Mock: añade y simula respuesta =====
        adapter.addMessage(new ChatMessage(text, true));
        scrollToBottom();

        rvMessages.postDelayed(() -> {
            adapter.addMessage(new ChatMessage("👌", false));
            scrollToBottom();
        }, 600);

        // ===== Firebase (comentado) =====
        /*
        if (AppConfig.USE_FIREBASE) {
            sendMessageFirestore(text);
        }
        */
    }

    private void scrollToBottom() {
        if (adapter != null && adapter.getItemCount() > 0) {
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private ArrayList<ChatMessage> createMockMessages() {
        ArrayList<ChatMessage> mock = new ArrayList<>();
        mock.add(new ChatMessage("¡Buenas! ¿Tienes la receta?", false));
        mock.add(new ChatMessage("Sí, ahora te la paso 😄", true));
        mock.add(new ChatMessage("Perfecto, gracias!", false));
        return mock;
    }

    // ===============================
    // ========== FIREBASE (LISTO) ====
    // ===============================

    /*
    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void listenMessagesFirestore() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;
        if (chatId == null || chatId.trim().isEmpty()) return;

        messagesListener = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    ArrayList<ChatMessage> data = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String text = doc.getString("text");
                        String senderId = doc.getString("senderId");
                        boolean mine = senderId != null && senderId.equals(user.getUid());
                        data.add(new ChatMessage(text != null ? text : "", mine));
                    }

                    messages.clear();
                    messages.addAll(data);
                    adapter.notifyDataSetChanged();
                    scrollToBottom();
                });
    }

    private void sendMessageFirestore(String text) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;
        if (chatId == null || chatId.trim().isEmpty()) return;

        HashMap<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("senderId", user.getUid());
        msg.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(msg);

        HashMap<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", text);
        chatUpdate.put("lastSenderId", user.getUid());
        chatUpdate.put("lastMessageAt", FieldValue.serverTimestamp());

        firestore.collection("chats")
                .document(chatId)
                .set(chatUpdate, SetOptions.merge());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }
    */
}