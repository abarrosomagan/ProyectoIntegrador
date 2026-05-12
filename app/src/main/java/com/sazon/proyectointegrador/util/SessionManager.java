package com.sazon.proyectointegrador.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Doble papel:
 *  - INSTANCIA (con Context): cachea uid/nombre del usuario en SharedPreferences,
 *    así el feed puede saludar sin pegarse contra Firebase en cada arranque.
 *  - ESTÁTICO: helpers para FirebaseAuth y Firestore (users/{uid}, chats/...).
 *    El instance-side y el static-side son independientes a propósito.
 */
public class SessionManager {

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_CHATS = "chats";

    private static final String PREF = "session_prefs";
    private static final String KEY_LOGGED = "logged_in";
    private static final String KEY_UID = "user_id";
    private static final String KEY_NAME = "user_name";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // ===== Estado local (SharedPreferences) =====

    public boolean isLoggedIn() {
        return sp.getBoolean(KEY_LOGGED, false);
    }

    public void login(String userId, String userName) {
        sp.edit()
                .putBoolean(KEY_LOGGED, true)
                .putString(KEY_UID, userId)
                .putString(KEY_NAME, userName)
                .apply();
    }

    public void logout() {
        sp.edit()
                .putBoolean(KEY_LOGGED, false)
                .remove(KEY_UID)
                .remove(KEY_NAME)
                .apply();
    }

    public String getUserId() {
        return sp.getString(KEY_UID, "");
    }

    public String getUserName() {
        return sp.getString(KEY_NAME, "");
    }

    // ===== Helpers estáticos de Firebase =====

    public static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    @Nullable
    public static FirebaseUser currentUser() {
        return auth().getCurrentUser();
    }

    @Nullable
    public static String currentUid() {
        FirebaseUser u = currentUser();
        return u != null ? u.getUid() : null;
    }

    @Nullable
    public static String currentEmail() {
        FirebaseUser u = currentUser();
        return u != null ? u.getEmail() : null;
    }

    public static boolean isAuthenticated() {
        return currentUser() != null;
    }

    /** Cierra sesión en Firebase y limpia el caché local de SharedPreferences. */
    public static void signOutCompat(@NonNull Context ctx) {
        auth().signOut();
        new SessionManager(ctx).logout();
    }

    // ===== Documento de usuario en Firestore =====

    /** Crea (o sobrescribe) el doc /users/{uid} con los datos básicos. */
    public static void createUserDoc(@NonNull String uid,
                                     @NonNull String name,
                                     @NonNull String email,
                                     @NonNull OnSuccessListener<Void> onOk,
                                     @NonNull OnFailureListener onErr) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("name", name);
        data.put("email", email);
        data.put("bio", "");
        data.put("avatarUrl", "");
        data.put("createdAt", FieldValue.serverTimestamp());

        db().collection(COLLECTION_USERS)
                .document(uid)
                .set(data)
                .addOnSuccessListener(onOk)
                .addOnFailureListener(onErr);
    }

    /** Lee el doc /users/{uid}. */
    public static void loadUserDoc(@NonNull String uid,
                                   @NonNull OnSuccessListener<DocumentSnapshot> onOk,
                                   @NonNull OnFailureListener onErr) {
        db().collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(onOk)
                .addOnFailureListener(onErr);
    }

    /**
     * Mergea cambios sobre /users/{uid}. Funciona aunque el doc no exista todavía
     * (clases viejas de Auth creadas antes de tener Firestore montado).
     */
    public static void updateUserDoc(@NonNull String uid,
                                     @NonNull Map<String, Object> updates,
                                     @NonNull OnSuccessListener<Void> onOk,
                                     @NonNull OnFailureListener onErr) {
        db().collection(COLLECTION_USERS)
                .document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(onOk)
                .addOnFailureListener(onErr);
    }

    // ===== Chats =====

    /**
     * Construye un id determinista para el chat 1-a-1 entre dos uids.
     * Siempre el menor primero, para que ambos extremos calculen lo mismo.
     */
    @NonNull
    public static String buildChatId(@NonNull String uidA, @NonNull String uidB) {
        return uidA.compareTo(uidB) <= 0 ? uidA + "_" + uidB : uidB + "_" + uidA;
    }
}
