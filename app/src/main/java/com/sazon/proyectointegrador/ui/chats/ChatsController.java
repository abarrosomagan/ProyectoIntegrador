package com.sazon.proyectointegrador.ui.chats;

import android.content.Intent;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.sazon.proyectointegrador.ChatActivity;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.adapters.ChatThreadAdapter;
import com.sazon.proyectointegrador.model.ChatThread;
import com.sazon.proyectointegrador.util.DemoData;
import com.sazon.proyectointegrador.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatsController {

    private final AppCompatActivity a;

    private RecyclerView rvChats;
    private TextView tvChatsVacio;
    private ImageButton btnNuevaConversacion;

    private ChatThreadAdapter chatsAdapter;
    private final ArrayList<ChatThread> chatsData = new ArrayList<>();

    private ListenerRegistration chatsListener;

    public ChatsController(AppCompatActivity activity) {
        this.a = activity;
    }

    public void init() {
        bind();
        setupRecycler();
        setupListeners();
        suscribirChats();
        actualizarEstadoVacio();
    }

    public void onDestroy() {
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }
    }

    private void bind() {
        rvChats = a.findViewById(R.id.rvChats);
        tvChatsVacio = a.findViewById(R.id.tvChatsVacio);
        btnNuevaConversacion = a.findViewById(R.id.btnNuevaConversacion);
    }

    private void setupRecycler() {
        if (rvChats == null) return;
        rvChats.setLayoutManager(new LinearLayoutManager(a));

        chatsAdapter = new ChatThreadAdapter(chatsData, chat -> {
            Intent i = new Intent(a, ChatActivity.class);
            i.putExtra(ChatActivity.EXTRA_CHAT_ID, chat.getId());
            i.putExtra(ChatActivity.EXTRA_CHAT_NAME, chat.getName());
            a.startActivity(i);
        });

        rvChats.setAdapter(chatsAdapter);
    }

    private void setupListeners() {
        if (btnNuevaConversacion != null) {
            btnNuevaConversacion.setOnClickListener(v -> mostrarDialogoNuevoChat());
        }
    }

    // ===== Listener real de Firestore =====

    private void suscribirChats() {
        String uid = SessionManager.currentUid();
        if (uid == null) {
            if (DemoData.ENABLED) {
                chatsData.clear();
                chatsData.addAll(DemoData.chats());
                if (chatsAdapter != null) chatsAdapter.notifyDataSetChanged();
                actualizarEstadoVacio();
            }
            return;
        }

        // No ordeno en la query (no quiero forzar a Fernando a crear un índice compuesto en Firestore).
        // Lo ordenamos en cliente con un map paralelo de timestamps.
        chatsListener = SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .whereArrayContains("participants", uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    // Pares (ChatThread, lastMessageAt) para poder ordenar después
                    ArrayList<Object[]> filas = new ArrayList<>();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String chatId = doc.getId();
                        String lastMessage = doc.getString("lastMessage");
                        String lastSenderId = doc.getString("lastSenderId");
                        Timestamp lastAt = doc.getTimestamp("lastMessageAt");
                        Timestamp lastReadAt = doc.getTimestamp("lastReadAt." + uid);
                        String otherUid = null;
                        List<String> participants = (List<String>) doc.get("participants");
                        if (participants != null) {
                            for (String participant : participants) {
                                if (participant != null && !participant.equals(uid)) {
                                    otherUid = participant;
                                    break;
                                }
                            }
                        }

                        // Nombre del otro participante (el que NO soy yo)
                        String otherName = "Chat";
                        Object mapObj = doc.get("participantsNames");
                        if (mapObj instanceof Map) {
                            Map<?, ?> names = (Map<?, ?>) mapObj;
                            for (Map.Entry<?, ?> entry : names.entrySet()) {
                                if (entry.getKey() != null
                                        && !entry.getKey().toString().equals(uid)) {
                                    otherName = String.valueOf(entry.getValue());
                                    break;
                                }
                            }
                        }

                        Boolean otherTyping = otherUid != null
                                ? doc.getBoolean("presence." + otherUid + ".typing")
                                : false;
                        Boolean otherActive = otherUid != null
                                ? doc.getBoolean("presence." + otherUid + ".active")
                                : false;
                        String preview = Boolean.TRUE.equals(otherTyping)
                                ? "escribiendo..."
                                : (lastMessage != null ? lastMessage : "");

                        boolean lastIsMine = lastSenderId != null && lastSenderId.equals(uid);
                        // El otro ha leído mi último mensaje si su lastReadAt es >= lastAt
                        boolean lastReadByOther = false;
                        if (lastIsMine && lastAt != null && otherUid != null) {
                            Timestamp otherReadAt = doc.getTimestamp("lastReadAt." + otherUid);
                            lastReadByOther = otherReadAt != null
                                    && otherReadAt.compareTo(lastAt) >= 0;
                        }

                        ChatThread thread = new ChatThread(
                                chatId,
                                otherName,
                                preview,
                                formatChatTime(lastAt),
                                hasUnread(uid, lastSenderId, lastAt, lastReadAt) ? 1 : 0,
                                Boolean.TRUE.equals(otherActive)
                                        || Boolean.TRUE.equals(otherTyping),
                                lastIsMine,
                                lastReadByOther
                        );
                        filas.add(new Object[]{ thread, lastAt });
                    }

                    // Más reciente arriba
                    Collections.sort(filas, new Comparator<Object[]>() {
                        @Override
                        public int compare(Object[] a, Object[] b) {
                            Timestamp ta = (Timestamp) a[1];
                            Timestamp tb = (Timestamp) b[1];
                            if (ta == null && tb == null) return 0;
                            if (ta == null) return 1;
                            if (tb == null) return -1;
                            return tb.compareTo(ta);
                        }
                    });

                    chatsData.clear();
                    // Chats demo arriba para que la lista no se vea vacía
                    if (DemoData.ENABLED) chatsData.addAll(DemoData.chats());
                    for (Object[] fila : filas) chatsData.add((ChatThread) fila[0]);

                    if (chatsAdapter != null) chatsAdapter.notifyDataSetChanged();
                    actualizarEstadoVacio();
                });
    }

    /**
     * Hora del último mensaje:
     *   - hoy   → "HH:mm"
     *   - ayer  → "ayer"
     *   - <7d   → "lun", "mar", "mié"…
     *   - resto → "dd/MM"
     */
    private static String formatChatTime(Timestamp ts) {
        if (ts == null) return "";
        Date date = ts.toDate();

        Calendar then = Calendar.getInstance();
        then.setTime(date);
        Calendar now = Calendar.getInstance();

        boolean mismoDia = then.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        if (mismoDia) {
            return new SimpleDateFormat("HH:mm", new Locale("es", "ES")).format(date);
        }

        Calendar ayer = (Calendar) now.clone();
        ayer.add(Calendar.DAY_OF_YEAR, -1);
        boolean fueAyer = then.get(Calendar.YEAR) == ayer.get(Calendar.YEAR)
                && then.get(Calendar.DAY_OF_YEAR) == ayer.get(Calendar.DAY_OF_YEAR);
        if (fueAyer) return "ayer";

        long diffMs = now.getTimeInMillis() - then.getTimeInMillis();
        if (diffMs < 7L * 24 * 60 * 60 * 1000) {
            return new SimpleDateFormat("EEE", new Locale("es", "ES")).format(date);
        }

        return new SimpleDateFormat("dd/MM", new Locale("es", "ES")).format(date);
    }

    private static boolean hasUnread(String uid,
                                     String lastSenderId,
                                     Timestamp lastAt,
                                     Timestamp lastReadAt) {
        if (uid == null || lastSenderId == null || lastAt == null) return false;
        if (uid.equals(lastSenderId)) return false;
        return lastReadAt == null || lastAt.compareTo(lastReadAt) > 0;
    }

    private void actualizarEstadoVacio() {
        if (tvChatsVacio == null || rvChats == null) return;
        boolean vacio = chatsData.isEmpty();
        tvChatsVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvChats.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    // ===== Nueva conversación =====

    private void mostrarDialogoNuevoChat() {
        if (!SessionManager.isAuthenticated()) return;

        EditText input = new EditText(a);
        input.setHint("Correo del otro usuario");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        int pad = (int) (16 * a.getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(a);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad * 2, pad, pad * 2, 0);
        container.addView(input);

        new AlertDialog.Builder(a)
                .setTitle("Nueva conversación")
                .setView(container)
                .setPositiveButton("Buscar", (d, w) -> {
                    String email = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!email.isEmpty()) buscarUsuarioYAbrirChat(email);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void buscarUsuarioYAbrirChat(String email) {
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(a, "No hemos encontrado a nadie con ese correo", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String otherUid = doc.getId();
                    String otherName = doc.getString("name");
                    if (otherName == null || otherName.isEmpty()) otherName = email;
                    crearOAbrirChatCon(otherUid, otherName);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(a, "Error buscando usuario", Toast.LENGTH_SHORT).show());
    }

    private void crearOAbrirChatCon(String otherUid, String otherName) {
        String myUid = SessionManager.currentUid();
        if (myUid == null || myUid.equals(otherUid)) {
            Toast.makeText(a, "No puedes hablar contigo mismo", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId = SessionManager.buildChatId(myUid, otherUid);

        // Nombre cacheado en SharedPreferences si está; si no, email
        String myName = new SessionManager(a).getUserName();
        if (myName == null || myName.isEmpty()) {
            myName = SessionManager.currentEmail() != null
                    ? SessionManager.currentEmail() : "Yo";
        }

        Map<String, Object> participantsNames = new HashMap<>();
        participantsNames.put(myUid, myName);
        participantsNames.put(otherUid, otherName);

        Map<String, Object> data = new HashMap<>();
        data.put("participants", Arrays.asList(myUid, otherUid));
        data.put("participantsNames", participantsNames);
        data.put("lastMessage", "");
        data.put("lastMessageAt", FieldValue.serverTimestamp());

        final String finalOtherName = otherName;
        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Intent i = new Intent(a, ChatActivity.class);
                    i.putExtra(ChatActivity.EXTRA_CHAT_ID, chatId);
                    i.putExtra(ChatActivity.EXTRA_CHAT_NAME, finalOtherName);
                    a.startActivity(i);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(a, "No se pudo crear la conversación", Toast.LENGTH_SHORT).show());
    }
}
