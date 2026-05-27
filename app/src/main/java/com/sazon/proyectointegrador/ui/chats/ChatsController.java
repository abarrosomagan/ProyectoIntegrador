package com.sazon.proyectointegrador.ui.chats;

import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
    private ListenerRegistration chatSettingsListener;
    private ListenerRegistration blockedListener;

    /** uids que tengo bloqueados — sus chats no aparecen en la lista. */
    private final java.util.HashSet<String> bloqueados = new java.util.HashSet<>();

    /** Caches por chatId del estado silenciado / fijado / oculto / marcado no leído del usuario actual. */
    private final HashMap<String, Boolean> mutedByChatId = new HashMap<>();
    private final HashMap<String, Boolean> pinnedByChatId = new HashMap<>();
    private final HashMap<String, Boolean> hiddenByChatId = new HashMap<>();
    private final HashMap<String, Boolean> markedUnreadByChatId = new HashMap<>();

    /** Guardamos el último snapshot ordenado para repintar tras cambios de chatSettings o de filtro. */
    private final ArrayList<Object[]> filasUltimoSnapshot = new ArrayList<>();

    private EditText etBuscarChats;
    private String filtroActual = "";

    public ChatsController(AppCompatActivity activity) {
        this.a = activity;
    }

    public void init() {
        bind();
        setupRecycler();
        setupListeners();
        suscribirBloqueados();
        suscribirChatSettings();
        suscribirChats();
        actualizarEstadoVacio();
    }

    public void onDestroy() {
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }
        if (chatSettingsListener != null) {
            chatSettingsListener.remove();
            chatSettingsListener = null;
        }
        if (blockedListener != null) {
            blockedListener.remove();
            blockedListener = null;
        }
    }

    /** Listener sobre users/{uid}/blocked: lista en tiempo real. */
    private void suscribirBloqueados() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;
        blockedListener = SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection("blocked")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    bloqueados.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        bloqueados.add(doc.getId());
                    }
                    repintarLista();
                });
    }

    private void bind() {
        rvChats = a.findViewById(R.id.rvChats);
        tvChatsVacio = a.findViewById(R.id.tvChatsVacio);
        btnNuevaConversacion = a.findViewById(R.id.btnNuevaConversacion);
        etBuscarChats = a.findViewById(R.id.etBuscarChats);
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

        chatsAdapter.setOnChatLongClick(this::mostrarDialogoOpcionesChat);

        rvChats.setAdapter(chatsAdapter);
    }

    private void setupListeners() {
        if (btnNuevaConversacion != null) {
            btnNuevaConversacion.setOnClickListener(v -> mostrarDialogoNuevoChat());
        }
        if (etBuscarChats != null) {
            etBuscarChats.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    filtroActual = s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());
                    repintarLista();
                }
            });
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

                        boolean muted = Boolean.TRUE.equals(mutedByChatId.get(chatId));
                        boolean pinned = Boolean.TRUE.equals(pinnedByChatId.get(chatId));
                        boolean markedUnread = Boolean.TRUE.equals(markedUnreadByChatId.get(chatId));

                        int unreadValue = (hasUnread(uid, lastSenderId, lastAt, lastReadAt) || markedUnread) ? 1 : 0;

                        ChatThread thread = new ChatThread(
                                chatId,
                                otherName,
                                preview,
                                formatChatTime(lastAt),
                                unreadValue,
                                Boolean.TRUE.equals(otherActive)
                                        || Boolean.TRUE.equals(otherTyping),
                                lastIsMine,
                                lastReadByOther,
                                muted,
                                pinned
                        );
                        filas.add(new Object[]{ thread, lastAt, otherUid });
                    }

                    filasUltimoSnapshot.clear();
                    filasUltimoSnapshot.addAll(filas);
                    repintarLista();
                });
    }

    private void repintarLista() {
        // Ordenamos: primero fijados, dentro de cada grupo por timestamp descendente
        Collections.sort(filasUltimoSnapshot, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] a, Object[] b) {
                ChatThread ca = (ChatThread) a[0];
                ChatThread cb = (ChatThread) b[0];
                if (ca.isPinned() != cb.isPinned()) {
                    return ca.isPinned() ? -1 : 1;
                }
                Timestamp ta = (Timestamp) a[1];
                Timestamp tb = (Timestamp) b[1];
                if (ta == null && tb == null) return 0;
                if (ta == null) return 1;
                if (tb == null) return -1;
                return tb.compareTo(ta);
            }
        });

        chatsData.clear();
        if (DemoData.ENABLED) {
            for (ChatThread demo : DemoData.chats()) {
                if (coincideFiltro(demo)) chatsData.add(demo);
            }
        }
        boolean buscando = !filtroActual.isEmpty();
        for (Object[] fila : filasUltimoSnapshot) {
            ChatThread t = (ChatThread) fila[0];
            String otherUid = fila.length > 2 ? (String) fila[2] : null;
            // Chats con usuarios bloqueados no aparecen nunca
            if (otherUid != null && bloqueados.contains(otherUid)) continue;
            // Ocultos no aparecen salvo que estés buscando explícitamente por nombre
            if (!buscando && Boolean.TRUE.equals(hiddenByChatId.get(t.getId()))) continue;
            if (!coincideFiltro(t)) continue;
            chatsData.add(t);
        }

        if (chatsAdapter != null) chatsAdapter.notifyDataSetChanged();
        actualizarEstadoVacio();
    }

    private boolean coincideFiltro(ChatThread t) {
        if (filtroActual.isEmpty()) return true;
        String name = t.getName() == null ? "" : t.getName().toLowerCase(Locale.getDefault());
        String last = t.getLastMessage() == null ? "" : t.getLastMessage().toLowerCase(Locale.getDefault());
        return name.contains(filtroActual) || last.contains(filtroActual);
    }

    /** Listener sobre users/{uid}/chatSettings: cambios de mute/pin del usuario actual. */
    private void suscribirChatSettings() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;

        chatSettingsListener = SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection("chatSettings")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    mutedByChatId.clear();
                    pinnedByChatId.clear();
                    hiddenByChatId.clear();
                    markedUnreadByChatId.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String chatId = doc.getId();
                        if (Boolean.TRUE.equals(doc.getBoolean("muted"))) mutedByChatId.put(chatId, true);
                        if (Boolean.TRUE.equals(doc.getBoolean("pinned"))) pinnedByChatId.put(chatId, true);
                        if (Boolean.TRUE.equals(doc.getBoolean("hidden"))) hiddenByChatId.put(chatId, true);
                        if (Boolean.TRUE.equals(doc.getBoolean("markedUnread"))) markedUnreadByChatId.put(chatId, true);
                    }

                    // Reaplicamos los flags al snapshot vigente y repintamos
                    for (Object[] fila : filasUltimoSnapshot) {
                        ChatThread t = (ChatThread) fila[0];
                        boolean muted = Boolean.TRUE.equals(mutedByChatId.get(t.getId()));
                        boolean pinned = Boolean.TRUE.equals(pinnedByChatId.get(t.getId()));
                        boolean markedUnread = Boolean.TRUE.equals(markedUnreadByChatId.get(t.getId()));
                        int newUnread = Math.max(t.getUnread(), markedUnread ? 1 : 0);

                        if (muted != t.isMuted() || pinned != t.isPinned() || newUnread != t.getUnread()) {
                            fila[0] = new ChatThread(
                                    t.getId(),
                                    t.getName(),
                                    t.getLastMessage(),
                                    t.getTime(),
                                    newUnread,
                                    t.isPresenceActive(),
                                    t.isLastIsMine(),
                                    t.isLastReadByOther(),
                                    muted,
                                    pinned
                            );
                        }
                    }
                    repintarLista();
                });
    }

    // ===== Long-press: silenciar / fijar / marcar no leída / ocultar =====

    private void mostrarDialogoOpcionesChat(ChatThread chat) {
        String uid = SessionManager.currentUid();
        if (uid == null) return;

        boolean muted = Boolean.TRUE.equals(mutedByChatId.get(chat.getId()));
        boolean pinned = Boolean.TRUE.equals(pinnedByChatId.get(chat.getId()));
        boolean markedUnread = Boolean.TRUE.equals(markedUnreadByChatId.get(chat.getId()));

        String optPin = pinned ? "Desfijar conversación" : "Fijar conversación";
        String optMute = muted ? "Activar notificaciones" : "Silenciar notificaciones";
        String optUnread = markedUnread ? "Marcar como leída" : "Marcar como no leída";
        String optDelete = "Borrar conversación";

        String[] opciones = new String[] { optPin, optMute, optUnread, optDelete };

        new AlertDialog.Builder(a)
                .setTitle(chat.getName())
                .setItems(opciones, (d, which) -> {
                    if (which == 0)      togglePinned(uid, chat.getId(), !pinned);
                    else if (which == 1) toggleMuted(uid, chat.getId(), !muted);
                    else if (which == 2) toggleMarkedUnread(uid, chat.getId(), !markedUnread);
                    else if (which == 3) confirmarBorrarConversacion(uid, chat);
                })
                .show();
    }

    private void toggleMarkedUnread(String uid, String chatId, boolean newValue) {
        Map<String, Object> data = new HashMap<>();
        data.put("markedUnread", newValue);
        data.put("updatedAt", FieldValue.serverTimestamp());
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection("chatSettings")
                .document(chatId)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(a, "No se pudo actualizar el chat", Toast.LENGTH_SHORT).show());
    }

    private void confirmarBorrarConversacion(String uid, ChatThread chat) {
        new AlertDialog.Builder(a)
                .setTitle("Borrar conversación")
                .setMessage("Se ocultará para ti. " + chat.getName()
                        + " seguirá teniéndola en su lista.")
                .setPositiveButton("Borrar", (d, w) -> ocultarConversacion(uid, chat.getId()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ocultarConversacion(String uid, String chatId) {
        Map<String, Object> data = new HashMap<>();
        data.put("hidden", true);
        data.put("pinned", false);
        data.put("updatedAt", FieldValue.serverTimestamp());
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection("chatSettings")
                .document(chatId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Toast.makeText(a, "Conversación borrada de tu lista", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(a, "No se pudo borrar", Toast.LENGTH_SHORT).show());
    }

    private void togglePinned(String uid, String chatId, boolean newValue) {
        Map<String, Object> data = new HashMap<>();
        data.put("pinned", newValue);
        data.put("updatedAt", FieldValue.serverTimestamp());
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection("chatSettings")
                .document(chatId)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(a, "No se pudo actualizar el chat", Toast.LENGTH_SHORT).show());
    }

    private void toggleMuted(String uid, String chatId, boolean newValue) {
        Map<String, Object> data = new HashMap<>();
        data.put("muted", newValue);
        data.put("updatedAt", FieldValue.serverTimestamp());
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection("chatSettings")
                .document(chatId)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(a, "No se pudo actualizar el chat", Toast.LENGTH_SHORT).show());
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
        input.setHint("Nombre o correo del otro usuario");
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        int pad = (int) (16 * a.getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(a);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad * 2, pad, pad * 2, 0);
        container.addView(input);

        new AlertDialog.Builder(a)
                .setTitle("Nueva conversación")
                .setView(container)
                .setPositiveButton("Buscar", (d, w) -> {
                    String q = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!q.isEmpty()) buscarUsuarioYAbrirChat(q);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void buscarUsuarioYAbrirChat(String query) {
        if (query.contains("@")) {
            // Búsqueda exacta por email
            SessionManager.db()
                    .collection(SessionManager.COLLECTION_USERS)
                    .whereEqualTo("email", query)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            Toast.makeText(a, "No hemos encontrado a nadie con ese correo",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        DocumentSnapshot doc = snap.getDocuments().get(0);
                        String otherUid = doc.getId();
                        String otherName = doc.getString("name");
                        if (otherName == null || otherName.isEmpty()) otherName = query;
                        crearOAbrirChatCon(otherUid, otherName);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(a, "Error buscando usuario", Toast.LENGTH_SHORT).show());
            return;
        }
        // Búsqueda por nombre con prefijo
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .orderBy("name")
                .startAt(query)
                .endAt(query + "")
                .limit(10)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(a, "No hemos encontrado a nadie con ese nombre",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String myUid = SessionManager.currentUid();
                    java.util.List<DocumentSnapshot> candidatos = new java.util.ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (myUid == null || !doc.getId().equals(myUid)) candidatos.add(doc);
                    }
                    if (candidatos.isEmpty()) {
                        Toast.makeText(a, "Eres tú mismo", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (candidatos.size() == 1) {
                        DocumentSnapshot doc = candidatos.get(0);
                        String nombre = doc.getString("name");
                        crearOAbrirChatCon(doc.getId(),
                                nombre != null ? nombre : query);
                        return;
                    }
                    // Varios candidatos → diálogo de selección
                    String[] nombres = new String[candidatos.size()];
                    for (int i = 0; i < candidatos.size(); i++) {
                        String n = candidatos.get(i).getString("name");
                        String em = candidatos.get(i).getString("email");
                        nombres[i] = (n != null ? n : "Sin nombre")
                                + (em != null ? "  ·  " + em : "");
                    }
                    new AlertDialog.Builder(a)
                            .setTitle("Elige a quién escribir")
                            .setItems(nombres, (d, idx) -> {
                                DocumentSnapshot doc = candidatos.get(idx);
                                String nombre = doc.getString("name");
                                crearOAbrirChatCon(doc.getId(),
                                        nombre != null ? nombre : query);
                            })
                            .show();
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
        if (bloqueados.contains(otherUid)) {
            Toast.makeText(a, "Has bloqueado a " + otherName
                    + ". Desbloquéalo desde su perfil.", Toast.LENGTH_LONG).show();
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
