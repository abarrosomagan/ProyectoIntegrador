package com.sazon.proyectointegrador.util;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.model.RecipeComment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Acceso a recetas en Firestore:
 *   /recipes/{id}                                  → doc receta
 *   /recipes/{id}/likes/{uid}                      → quien le ha dado like
 *   /users/{uid}/saved/{recipeId}                  → recetas guardadas por el usuario
 *
 * Los contadores (likes en /recipes/{id}.likes) se mantienen denormalizados
 * con FieldValue.increment para no tener que contar subcolecciones al pintar.
 */
public final class RecipeRepository {

    public static final String COLLECTION_RECIPES = "recipes";
    public static final String SUBCOLLECTION_LIKES = "likes";
    public static final String SUBCOLLECTION_SAVED = "saved";
    public static final String SUBCOLLECTION_COMMENTS = "comments";

    private RecipeRepository() { /* util estática */ }

    // ===== Crear =====

    /** Crea una receta. Rellena authorId, autor y createdAt en server. */
    public static void create(@NonNull String authorId,
                              @NonNull String authorName,
                              @NonNull String titulo,
                              @NonNull String descripcion,
                              @NonNull String imageUrl,
                              @NonNull OnSuccessListener<DocumentReference> onOk,
                              @NonNull OnFailureListener onErr) {

        Map<String, Object> data = new HashMap<>();
        data.put("authorId", authorId);
        data.put("autor", authorName);
        data.put("titulo", titulo);
        data.put("descripcion", descripcion);
        data.put("imageUrl", imageUrl);
        data.put("likes", 0);
        data.put("createdAt", System.currentTimeMillis());

        SessionManager.db()
                .collection(COLLECTION_RECIPES)
                .add(data)
                .addOnSuccessListener(ref -> {
                    // Sumamos 1 al contador del usuario para el cuadro de stats
                    SessionManager.db()
                            .collection(SessionManager.COLLECTION_USERS)
                            .document(authorId)
                            .update("recipes", FieldValue.increment(1));
                    onOk.onSuccess(ref);
                })
                .addOnFailureListener(onErr);
    }

    // ===== Listados =====

    /** Recetas del usuario indicado, ordenadas por fecha de creación DESC. */
    public static void byAuthor(@NonNull String authorId,
                                @NonNull OnSuccessListener<List<Publicacion>> onOk,
                                @NonNull OnFailureListener onErr) {
        SessionManager.db()
                .collection(COLLECTION_RECIPES)
                .whereEqualTo("authorId", authorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> onOk.onSuccess(parse(snap)))
                .addOnFailureListener(onErr);
    }

    /** Feed global (las N más recientes). */
    public static void feed(int limit,
                            @NonNull OnSuccessListener<List<Publicacion>> onOk,
                            @NonNull OnFailureListener onErr) {
        SessionManager.db()
                .collection(COLLECTION_RECIPES)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(snap -> onOk.onSuccess(parse(snap)))
                .addOnFailureListener(onErr);
    }

    /** Recetas guardadas por el usuario indicado. */
    public static void savedBy(@NonNull String uid,
                               @NonNull OnSuccessListener<List<Publicacion>> onOk,
                               @NonNull OnFailureListener onErr) {
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_SAVED)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(saveSnap -> {
                    if (saveSnap.isEmpty()) {
                        onOk.onSuccess(new ArrayList<>());
                        return;
                    }
                    // Cargamos los docs reales de recipes/{id} en batch
                    List<DocumentSnapshot> docs = saveSnap.getDocuments();
                    final List<Publicacion> result = new ArrayList<>();
                    final int[] pending = { docs.size() };

                    for (DocumentSnapshot s : docs) {
                        String recipeId = s.getId();
                        SessionManager.db()
                                .collection(COLLECTION_RECIPES)
                                .document(recipeId)
                                .get()
                                .addOnCompleteListener(t -> {
                                    if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) {
                                        Publicacion p = t.getResult().toObject(Publicacion.class);
                                        if (p != null) {
                                            p.setId(t.getResult().getId());
                                            p.setGuardada(true);
                                            result.add(p);
                                        }
                                    }
                                    pending[0]--;
                                    if (pending[0] == 0) {
                                        Collections.sort(result, (a, b) ->
                                                Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                                        onOk.onSuccess(result);
                                    }
                                });
                    }
                })
                .addOnFailureListener(onErr);
    }

    // ===== Likes =====

    /** Da like (true) o lo quita (false), actualizando contador denormalizado. */
    public static void toggleLike(@NonNull String recipeId,
                                  @NonNull String uid,
                                  boolean like,
                                  OnSuccessListener<Void> onOk,
                                  OnFailureListener onErr) {

        DocumentReference recipeRef = SessionManager.db()
                .collection(COLLECTION_RECIPES).document(recipeId);
        DocumentReference likeRef = recipeRef
                .collection(SUBCOLLECTION_LIKES).document(uid);

        WriteBatch batch = SessionManager.db().batch();

        if (like) {
            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("createdAt", System.currentTimeMillis());
            batch.set(likeRef, data);
            batch.update(recipeRef, "likes", FieldValue.increment(1));
        } else {
            batch.delete(likeRef);
            batch.update(recipeRef, "likes", FieldValue.increment(-1));
        }

        batch.commit()
                .addOnSuccessListener(v -> { if (onOk != null) onOk.onSuccess(v); })
                .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
    }

    /** Devuelve el set de recipeIds a los que el usuario ya dio like. */
    public static void likedIds(@NonNull String uid,
                                @NonNull OnSuccessListener<Set<String>> onOk,
                                @NonNull OnFailureListener onErr) {
        SessionManager.db()
                .collectionGroup(SUBCOLLECTION_LIKES)
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    Set<String> ids = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        DocumentReference recipeRef = doc.getReference().getParent().getParent();
                        if (recipeRef != null) ids.add(recipeRef.getId());
                    }
                    onOk.onSuccess(ids);
                })
                .addOnFailureListener(onErr);
    }

    public static void updateRecipe(@NonNull String recipeId,
                                    @NonNull String titulo,
                                    @NonNull String descripcion,
                                    @NonNull String imageUrl,
                                    @NonNull OnSuccessListener<Void> onOk,
                                    @NonNull OnFailureListener onErr) {
        Map<String, Object> data = new HashMap<>();
        data.put("titulo", titulo);
        data.put("descripcion", descripcion);
        data.put("imageUrl", imageUrl);
        data.put("updatedAt", System.currentTimeMillis());

        SessionManager.db()
                .collection(COLLECTION_RECIPES)
                .document(recipeId)
                .update(data)
                .addOnSuccessListener(onOk)
                .addOnFailureListener(onErr);
    }

    /** Devuelve si el usuario ya dio like a una receta concreta. */
    public static void isLiked(@NonNull String recipeId,
                               @NonNull String uid,
                               @NonNull OnSuccessListener<Boolean> onOk,
                               @NonNull OnFailureListener onErr) {
        SessionManager.db()
                .collection(COLLECTION_RECIPES)
                .document(recipeId)
                .collection(SUBCOLLECTION_LIKES)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> onOk.onSuccess(doc != null && doc.exists()))
                .addOnFailureListener(onErr);
    }

    // ===== Guardados =====

    /** Guarda o quita guardado, sin tocar contadores en la receta. */
    public static void toggleSaved(@NonNull String recipeId,
                                   @NonNull String uid,
                                   boolean save,
                                   OnSuccessListener<Void> onOk,
                                   OnFailureListener onErr) {

        DocumentReference savedRef = SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_SAVED)
                .document(recipeId);

        if (save) {
            Map<String, Object> data = new HashMap<>();
            data.put("recipeId", recipeId);
            data.put("createdAt", System.currentTimeMillis());
            savedRef.set(data)
                    .addOnSuccessListener(v -> { if (onOk != null) onOk.onSuccess(v); })
                    .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
        } else {
            savedRef.delete()
                    .addOnSuccessListener(v -> { if (onOk != null) onOk.onSuccess(v); })
                    .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
        }
    }

    /** Devuelve los recipeIds guardados por el usuario. */
    public static void savedIds(@NonNull String uid,
                                @NonNull OnSuccessListener<Set<String>> onOk,
                                @NonNull OnFailureListener onErr) {
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_SAVED)
                .get()
                .addOnSuccessListener(snap -> {
                    Set<String> ids = new HashSet<>();
                    for (DocumentSnapshot d : snap.getDocuments()) ids.add(d.getId());
                    onOk.onSuccess(ids);
                })
                .addOnFailureListener(onErr);
    }

    // ===== Comentarios =====

    public static Query commentsQuery(@NonNull String recipeId) {
        return SessionManager.db()
                .collection(COLLECTION_RECIPES)
                .document(recipeId)
                .collection(SUBCOLLECTION_COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING);
    }

    public static void addComment(@NonNull String recipeId,
                                  @NonNull String uid,
                                  @NonNull String authorName,
                                  @NonNull String text,
                                  OnSuccessListener<DocumentReference> onOk,
                                  OnFailureListener onErr) {
        Map<String, Object> data = new HashMap<>();
        data.put("recipeId", recipeId);
        data.put("authorId", uid);
        data.put("authorName", authorName);
        data.put("text", text);
        data.put("createdAt", System.currentTimeMillis());

        SessionManager.db()
                .collection(COLLECTION_RECIPES)
                .document(recipeId)
                .collection(SUBCOLLECTION_COMMENTS)
                .add(data)
                .addOnSuccessListener(ref -> { if (onOk != null) onOk.onSuccess(ref); })
                .addOnFailureListener(e -> { if (onErr != null) onErr.onFailure(e); });
    }

    public static List<RecipeComment> parseComments(QuerySnapshot snap) {
        List<RecipeComment> list = new ArrayList<>();
        if (snap == null) return list;
        for (DocumentSnapshot doc : snap.getDocuments()) {
            RecipeComment c = doc.toObject(RecipeComment.class);
            if (c != null) {
                c.setId(doc.getId());
                list.add(c);
            }
        }
        return list;
    }

    // ===== Helpers =====

    private static List<Publicacion> parse(QuerySnapshot snap) {
        List<Publicacion> list = new ArrayList<>();
        if (snap == null) return list;
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Publicacion p = doc.toObject(Publicacion.class);
            if (p != null) {
                p.setId(doc.getId());
                list.add(p);
            }
        }
        return list;
    }
}
