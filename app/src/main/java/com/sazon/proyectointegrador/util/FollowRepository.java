package com.sazon.proyectointegrador.util;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sistema de seguidores / siguiendo en Firestore.
 *
 *   /users/{uid}/followers/{followerUid}   → quien me sigue
 *   /users/{uid}/following/{followingUid}  → a quien sigo
 *   /users/{uid}.followers (number)         → contador denormalizado
 *   /users/{uid}.following (number)         → contador denormalizado
 */
public final class FollowRepository {

    public static final String SUB_FOLLOWERS = "followers";
    public static final String SUB_FOLLOWING = "following";

    private FollowRepository() { /* util estática */ }

    /**
     * El usuario {meUid} sigue (true) o deja de seguir (false) a {otherUid}.
     * Actualiza ambas relaciones y los contadores en un único batch.
     */
    public static void toggleFollow(@NonNull String meUid,
                                    @NonNull String otherUid,
                                    boolean follow,
                                    OnSuccessListener<Void> onOk,
                                    OnFailureListener onErr) {

        if (meUid.equals(otherUid)) {
            if (onErr != null) onErr.onFailure(
                    new IllegalArgumentException("No te puedes seguir a ti mismo"));
            return;
        }

        DocumentReference meDoc = SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS).document(meUid);
        DocumentReference otherDoc = SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS).document(otherUid);

        // {otherUid} aparece en /me/following/{otherUid}
        DocumentReference iFollowOther = meDoc.collection(SUB_FOLLOWING).document(otherUid);
        // {meUid} aparece en /other/followers/{meUid}
        DocumentReference otherIsFollowedByMe = otherDoc.collection(SUB_FOLLOWERS).document(meUid);

        WriteBatch batch = SessionManager.db().batch();

        if (follow) {
            Map<String, Object> data = new HashMap<>();
            data.put("createdAt", System.currentTimeMillis());

            batch.set(iFollowOther, data);
            batch.set(otherIsFollowedByMe, data);
            batch.update(meDoc, "following", FieldValue.increment(1));
            batch.update(otherDoc, "followers", FieldValue.increment(1));
        } else {
            batch.delete(iFollowOther);
            batch.delete(otherIsFollowedByMe);
            batch.update(meDoc, "following", FieldValue.increment(-1));
            batch.update(otherDoc, "followers", FieldValue.increment(-1));
        }

        batch.commit()
                .addOnSuccessListener(v -> { if (onOk != null) onOk.onSuccess(v); })
                .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
    }

    /** ¿{meUid} sigue a {otherUid}? */
    public static void isFollowing(@NonNull String meUid,
                                   @NonNull String otherUid,
                                   @NonNull OnSuccessListener<Boolean> onOk,
                                   OnFailureListener onErr) {

        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(meUid)
                .collection(SUB_FOLLOWING)
                .document(otherUid)
                .get()
                .addOnSuccessListener(snap -> onOk.onSuccess(snap != null && snap.exists()))
                .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
    }

    /** IDs de usuarios a los que sigue {uid}. */
    public static void followingIds(@NonNull String uid,
                                    @NonNull OnSuccessListener<Set<String>> onOk,
                                    OnFailureListener onErr) {
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection(SUB_FOLLOWING)
                .get()
                .addOnSuccessListener(snap -> {
                    Set<String> ids = new HashSet<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            ids.add(doc.getId());
                        }
                    }
                    onOk.onSuccess(ids);
                })
                .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
    }
}
