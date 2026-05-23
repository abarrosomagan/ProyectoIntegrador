package com.sazon.proyectointegrador.util;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.Query;
import com.sazon.proyectointegrador.model.ActivityItem;

import java.util.HashMap;
import java.util.Map;

public final class ActivityRepository {

    public static final String SUB_ACTIVITY = "activity";
    public static final String TYPE_LIKE = "like";
    public static final String TYPE_SAVE = "save";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_FOLLOW = "follow";

    private ActivityRepository() { }

    public static Query query(@NonNull String uid) {
        return SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection(SUB_ACTIVITY)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(80);
    }

    public static void notifyRecipe(@NonNull String targetUid,
                                    @NonNull String type,
                                    @NonNull String actorId,
                                    @NonNull String actorName,
                                    @NonNull String recipeId,
                                    @NonNull String recipeTitle,
                                    OnSuccessListener<Void> onOk,
                                    OnFailureListener onErr) {
        if (targetUid.equals(actorId)) {
            if (onOk != null) onOk.onSuccess(null);
            return;
        }
        String message;
        if (TYPE_LIKE.equals(type)) {
            message = actorName + " dio me gusta a tu receta";
        } else if (TYPE_SAVE.equals(type)) {
            message = actorName + " guardo tu receta";
        } else {
            message = actorName + " comento tu receta";
        }
        create(targetUid, type, actorId, actorName, recipeId, recipeTitle, message, onOk, onErr);
    }

    public static void notifyFollow(@NonNull String targetUid,
                                    @NonNull String actorId,
                                    @NonNull String actorName,
                                    OnSuccessListener<Void> onOk,
                                    OnFailureListener onErr) {
        if (targetUid.equals(actorId)) {
            if (onOk != null) onOk.onSuccess(null);
            return;
        }
        create(targetUid, TYPE_FOLLOW, actorId, actorName, "", "",
                actorName + " empezo a seguirte", onOk, onErr);
    }

    public static void markAllRead(@NonNull String uid,
                                   OnSuccessListener<Void> onOk,
                                   OnFailureListener onErr) {
        query(uid).get()
                .addOnSuccessListener(snap -> {
                    com.google.firebase.firestore.WriteBatch batch = SessionManager.db().batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        ActivityItem item = doc.toObject(ActivityItem.class);
                        if (item == null || !item.isRead()) {
                            batch.update(doc.getReference(), "read", true);
                        }
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> { if (onOk != null) onOk.onSuccess(v); })
                            .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
                })
                .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
    }

    private static void create(@NonNull String targetUid,
                               @NonNull String type,
                               @NonNull String actorId,
                               @NonNull String actorName,
                               @NonNull String recipeId,
                               @NonNull String recipeTitle,
                               @NonNull String message,
                               OnSuccessListener<Void> onOk,
                               OnFailureListener onErr) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("actorId", actorId);
        data.put("actorName", actorName);
        data.put("recipeId", recipeId);
        data.put("recipeTitle", recipeTitle);
        data.put("message", message);
        data.put("createdAt", System.currentTimeMillis());
        data.put("read", false);

        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(targetUid)
                .collection(SUB_ACTIVITY)
                .add(data)
                .addOnSuccessListener(ref -> { if (onOk != null) onOk.onSuccess(null); })
                .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
    }
}
