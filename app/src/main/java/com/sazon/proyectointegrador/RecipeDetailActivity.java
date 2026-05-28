package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.sazon.proyectointegrador.adapters.RecipeCommentAdapter;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.model.RecipeComment;
import com.sazon.proyectointegrador.util.ActivityRepository;
import com.sazon.proyectointegrador.util.RecipeImageHelper;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.RecipeStateBus;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "EXTRA_RECIPE_ID";

    private String recipeId;
    private Publicacion receta;
    private boolean guardada = false;
    private boolean liked = false;

    private TextView tvTitle, tvAuthor, tvAuthorAvatar, tvDate, tvDescription, tvRecipeMeta, tvRecipeTags;
    private TextView tvIngredientesHeader, tvIngredientes, tvPasosHeader, tvPasos;
    private ImageView recipeHero;
    private TextView tvCommentsTitle, tvCommentsEmpty;
    private TextInputEditText etComment;
    private MaterialButton btnLike, btnSave, btnDelete, btnEdit, btnViewProfile;
    private MaterialButton btnSendComment;
    private RecyclerView rvComments;
    private RecipeCommentAdapter commentsAdapter;
    private ListenerRegistration commentsRegistration;
    private final RecipeStateBus.Listener recipeStateListener = this::onRecipeStateChanged;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_detail);

        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (recipeId == null || recipeId.isEmpty()) {
            Toast.makeText(this, "Receta no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bind();
        RecipeStateBus.register(recipeStateListener);
        cargarReceta();
        cargarEstadoGuardado();
        cargarEstadoLike();
        escucharComentarios();
    }

    private void bind() {
        tvTitle = findViewById(R.id.tvRecipeTitle);
        tvAuthor = findViewById(R.id.tvRecipeAuthor);
        tvAuthorAvatar = findViewById(R.id.tvAuthorAvatar);
        tvDate = findViewById(R.id.tvRecipeDate);
        tvDescription = findViewById(R.id.tvRecipeDescription);
        tvIngredientesHeader = findViewById(R.id.tvIngredientesHeader);
        tvIngredientes = findViewById(R.id.tvIngredientes);
        tvPasosHeader = findViewById(R.id.tvPasosHeader);
        tvPasos = findViewById(R.id.tvPasos);
        tvRecipeMeta = findViewById(R.id.tvRecipeMeta);
        tvRecipeTags = findViewById(R.id.tvRecipeTags);
        recipeHero = findViewById(R.id.recipeHero);
        btnLike = findViewById(R.id.btnLikeRecipe);
        btnSave = findViewById(R.id.btnSaveRecipe);
        btnDelete = findViewById(R.id.btnDeleteRecipe);
        btnEdit = findViewById(R.id.btnEditRecipe);
        btnViewProfile = findViewById(R.id.btnFollowFromRecipe);
        tvCommentsTitle = findViewById(R.id.tvCommentsTitle);
        tvCommentsEmpty = findViewById(R.id.tvCommentsEmpty);
        etComment = findViewById(R.id.etRecipeComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        rvComments = findViewById(R.id.rvRecipeComments);

        if (rvComments != null) {
            rvComments.setLayoutManager(new LinearLayoutManager(this));
            rvComments.setNestedScrollingEnabled(false);
            commentsAdapter = new RecipeCommentAdapter(new ArrayList<>());
            rvComments.setAdapter(commentsAdapter);
        }

        ImageButton btnBack = findViewById(R.id.btnBackRecipe);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageButton btnShare = findViewById(R.id.btnShareRecipe);
        if (btnShare != null) btnShare.setOnClickListener(v -> compartirReceta());

        if (btnLike != null) {
            btnLike.setOnClickListener(v -> darLike());
            btnLike.setOnLongClickListener(v -> { mostrarLikers(); return true; });
        }
        if (btnSave != null) btnSave.setOnClickListener(v -> alternarGuardado());
        if (btnEdit != null) btnEdit.setOnClickListener(v -> editarReceta());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> confirmarEliminar());
        if (btnViewProfile != null) btnViewProfile.setOnClickListener(v -> abrirPerfilAutor());
        if (btnSendComment != null) btnSendComment.setOnClickListener(v -> enviarComentario());

        MaterialButton btnCook = findViewById(R.id.btnCookMode);
        if (btnCook != null) btnCook.setOnClickListener(v -> abrirModoCocinar());
        MaterialButton btnAddShopping = findViewById(R.id.btnAddToShoppingList);
        if (btnAddShopping != null) btnAddShopping.setOnClickListener(v -> anadirALaCompra());
    }

    private void cargarReceta() {
        SessionManager.db()
                .collection(RecipeRepository.COLLECTION_RECIPES)
                .document(recipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this, "Esta receta ya no existe",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    receta = doc.toObject(Publicacion.class);
                    if (receta == null) {
                        finish();
                        return;
                    }
                    receta.setId(doc.getId());
                    // Lectura defensiva del campo views por si no estaba en el constructor
                    Long viewsLong = doc.getLong("views");
                    if (viewsLong != null) receta.setViews(viewsLong.intValue());
                    pintarReceta();
                    registrarVisualizacion();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No se pudo cargar la receta",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void pintarReceta() {
        if (receta == null) return;

        String titulo = receta.getTitulo() != null ? receta.getTitulo() : "Receta";
        String autor = receta.getAutor() != null ? receta.getAutor() : "Chef";

        tvTitle.setText(titulo);
        tvAuthor.setText(autor);
        if (!autor.isEmpty()) {
            tvAuthorAvatar.setText(
                    String.valueOf(Character.toUpperCase(autor.charAt(0))));
        }
        tvDate.setText(formatRelativeTime(receta.getCreatedAt()));
        String desc = receta.getDescripcion();
        tvDescription.setText(desc == null || desc.isEmpty()
                ? "(Sin descripción)" : desc);

        pintarIngredientesYPasos();
        pintarMetaReceta();

        String imageUrl = receta.getImageUrl();
        if (recipeHero != null) {
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                RecipeImageHelper.loadInto(recipeHero, imageUrl);
            } else {
                recipeHero.setImageDrawable(null);
            }
        }

        pintarBotonLike();

        // El autor es quien la creó: muestra el botón Eliminar
        String meUid = SessionManager.currentUid();
        boolean soyAutor = meUid != null && meUid.equals(receta.getAuthorId());
        btnEdit.setVisibility(soyAutor ? android.view.View.VISIBLE
                : android.view.View.GONE);
        btnDelete.setVisibility(soyAutor ? android.view.View.VISIBLE
                : android.view.View.GONE);
        btnViewProfile.setVisibility(soyAutor ? android.view.View.GONE
                : android.view.View.VISIBLE);
    }

    private void registrarVisualizacion() {
        String uid = SessionManager.currentUid();
        if (uid == null || receta == null || receta.getId() == null) return;
        // No contar la visualización del propio autor (no inflamos nuestras métricas)
        if (uid.equals(receta.getAuthorId())) return;

        RecipeRepository.registerView(receta.getId(), uid,
                unused -> {
                    // Incrementamos el contador local y refrescamos meta
                    receta.setViews(receta.getViews() + 1);
                    pintarMetaReceta();
                },
                e -> { /* ya estaba contado o falló: nada */ });
    }

    private void pintarIngredientesYPasos() {
        if (receta == null) return;
        java.util.List<String> ings = receta.getIngredientes();
        if (ings != null && !ings.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String i : ings) {
                if (i == null || i.isEmpty()) continue;
                if (sb.length() > 0) sb.append('\n');
                sb.append("•  ").append(i);
            }
            if (tvIngredientes != null) tvIngredientes.setText(sb.toString());
            if (tvIngredientesHeader != null) tvIngredientesHeader.setVisibility(android.view.View.VISIBLE);
            if (tvIngredientes != null) tvIngredientes.setVisibility(android.view.View.VISIBLE);
        } else {
            if (tvIngredientesHeader != null) tvIngredientesHeader.setVisibility(android.view.View.GONE);
            if (tvIngredientes != null) tvIngredientes.setVisibility(android.view.View.GONE);
        }
        java.util.List<String> pasos = receta.getPasos();
        if (pasos != null && !pasos.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int n = 1;
            for (String p : pasos) {
                if (p == null || p.isEmpty()) continue;
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(n++).append(". ").append(p);
            }
            if (tvPasos != null) tvPasos.setText(sb.toString());
            if (tvPasosHeader != null) tvPasosHeader.setVisibility(android.view.View.VISIBLE);
            if (tvPasos != null) tvPasos.setVisibility(android.view.View.VISIBLE);
        } else {
            if (tvPasosHeader != null) tvPasosHeader.setVisibility(android.view.View.GONE);
            if (tvPasos != null) tvPasos.setVisibility(android.view.View.GONE);
        }
    }

    private void pintarMetaReceta() {
        if (receta == null) return;
        StringBuilder meta = new StringBuilder();
        if (receta.getPrepMinutes() > 0) appendMeta(meta, receta.getPrepMinutes() + " min");
        if (receta.getServings() > 0) appendMeta(meta, receta.getServings() + " raciones");
        if (receta.getDifficulty() != null && !receta.getDifficulty().trim().isEmpty()) {
            appendMeta(meta, receta.getDifficulty().trim());
        }
        if (receta.getViews() > 0) {
            appendMeta(meta, formatViewsCount(receta.getViews()));
        }
        if (tvRecipeMeta != null) {
            tvRecipeMeta.setText(meta.toString());
            tvRecipeMeta.setVisibility(meta.length() == 0
                    ? android.view.View.GONE
                    : android.view.View.VISIBLE);
        }
        String tags = receta.getTags();
        if (tvRecipeTags != null) {
            if (tags == null || tags.trim().isEmpty()) {
                tvRecipeTags.setVisibility(android.view.View.GONE);
            } else {
                tvRecipeTags.setText(formatTags(tags));
                tvRecipeTags.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    private static void appendMeta(StringBuilder out, String value) {
        if (out.length() > 0) out.append("  ·  ");
        out.append(value);
    }

    private static String formatTags(String tags) {
        StringBuilder out = new StringBuilder();
        for (String part : tags.split(",")) {
            String tag = part.trim();
            if (tag.isEmpty()) continue;
            if (out.length() > 0) out.append("  ");
            out.append("#").append(tag.replace(" ", ""));
        }
        return out.toString();
    }

    private void cargarEstadoGuardado() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;
        RecipeRepository.savedIds(uid,
                ids -> {
                    guardada = ids.contains(recipeId);
                    pintarBotonGuardar();
                },
                e -> { /* nada */ });
    }

    private void pintarBotonGuardar() {
        if (btnSave == null) return;
        btnSave.setText(guardada ? "Guardada" : "Guardar");
        btnSave.setIconResource(guardada
                ? R.drawable.ic_bookmark_filled
                : R.drawable.ic_bookmark);
        btnSave.setIconTintResource(guardada
                ? R.color.color_guardado
                : R.color.color_principal_variante);
    }

    private void cargarEstadoLike() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;
        RecipeRepository.isLiked(recipeId, uid,
                exists -> {
                    liked = Boolean.TRUE.equals(exists);
                    if (receta != null) receta.setLiked(liked);
                    pintarBotonLike();
                },
                e -> { /* nada */ });
    }

    private void pintarBotonLike() {
        if (btnLike == null || receta == null) return;
        btnLike.setText(String.valueOf(receta.getLikes()));
        btnLike.setIconResource(liked
                ? R.drawable.ic_heart_filled
                : R.drawable.ic_heart_outline);
        btnLike.setIconTintResource(liked
                ? R.color.color_like
                : R.color.color_principal_variante);
    }

    private void escucharComentarios() {
        commentsRegistration = RecipeRepository.commentsQuery(recipeId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    ArrayList<RecipeComment> comments =
                            new ArrayList<>(RecipeRepository.parseComments(snap));
                    if (commentsAdapter != null) commentsAdapter.updateData(comments);
                    pintarEstadoComentarios(comments.size());
                });
    }

    private void pintarEstadoComentarios(int count) {
        if (tvCommentsTitle != null) {
            tvCommentsTitle.setText(count == 1 ? "1 comentario" : count + " comentarios");
        }
        if (tvCommentsEmpty != null) {
            tvCommentsEmpty.setVisibility(count == 0
                    ? android.view.View.VISIBLE
                    : android.view.View.GONE);
        }
    }

    // ===== Acciones =====

    private void darLike() {
        String uid = SessionManager.currentUid();
        if (uid == null || receta == null) return;
        boolean nuevoLiked = !liked;
        int likesAnteriores = receta.getLikes();
        int nuevosLikes = Math.max(0, likesAnteriores + (nuevoLiked ? 1 : -1));
        liked = nuevoLiked;
        receta.setLiked(nuevoLiked);
        receta.setLikes(nuevosLikes);
        pintarBotonLike();
        RecipeStateBus.publish(receta, nuevoLiked, nuevosLikes, null);
        RecipeRepository.toggleLike(recipeId, uid, nuevoLiked, v -> {
            if (nuevoLiked && receta != null) {
                ActivityRepository.notifyRecipe(receta.getAuthorId(),
                        ActivityRepository.TYPE_LIKE, uid, currentAuthorName(),
                        recipeId, receta.getTitulo() == null ? "" : receta.getTitulo(),
                        null, null);
            }
        }, e -> {
            liked = !nuevoLiked;
            receta.setLiked(liked);
            receta.setLikes(likesAnteriores);
            pintarBotonLike();
            RecipeStateBus.publish(receta, liked, likesAnteriores, null);
        });
    }

    private void alternarGuardado() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;
        boolean nuevo = !guardada;
        guardada = nuevo;
        if (receta != null) receta.setGuardada(nuevo);
        pintarBotonGuardar();
        if (receta != null) RecipeStateBus.publish(receta, null, null, nuevo);
        RecipeRepository.toggleSaved(recipeId, uid, nuevo, v -> {
            if (nuevo && receta != null) {
                ActivityRepository.notifyRecipe(receta.getAuthorId(),
                        ActivityRepository.TYPE_SAVE, uid, currentAuthorName(),
                        recipeId, receta.getTitulo() == null ? "" : receta.getTitulo(),
                        null, null);
            }
        }, e -> {
            guardada = !nuevo;
            if (receta != null) receta.setGuardada(guardada);
            pintarBotonGuardar();
            if (receta != null) RecipeStateBus.publish(receta, null, null, guardada);
            Toast.makeText(this, "No se pudo actualizar el guardado",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void enviarComentario() {
        String uid = SessionManager.currentUid();
        if (uid == null) {
            Toast.makeText(this, "Inicia sesión para comentar", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etComment == null || etComment.getText() == null) return;

        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) return;
        if (text.length() > 500) {
            Toast.makeText(this, "El comentario es demasiado largo", Toast.LENGTH_SHORT).show();
            return;
        }

        String authorName = currentAuthorName();
        btnSendComment.setEnabled(false);
        RecipeRepository.addComment(recipeId, uid, authorName, text,
                ref -> {
                    etComment.setText("");
                    btnSendComment.setEnabled(true);
                    if (receta != null) {
                        ActivityRepository.notifyRecipe(receta.getAuthorId(),
                                ActivityRepository.TYPE_COMMENT, uid, authorName,
                                recipeId, receta.getTitulo() == null ? "" : receta.getTitulo(),
                                null, null);
                    }
                },
                e -> {
                    btnSendComment.setEnabled(true);
                    Toast.makeText(this, "No se pudo publicar el comentario",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar receta")
                .setMessage("Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> eliminarReceta())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarReceta() {
        SessionManager.db()
                .collection(RecipeRepository.COLLECTION_RECIPES)
                .document(recipeId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Receta eliminada",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudo eliminar",
                                Toast.LENGTH_SHORT).show());
    }

    private void compartirReceta() {
        if (receta == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Compartir receta")
                .setItems(new String[] { "Enviar a un chat de Sazón", "Compartir fuera de Sazón" },
                        (d, which) -> {
                            if (which == 0) elegirChatParaCompartir();
                            else compartirFuera();
                        })
                .show();
    }

    private void compartirFuera() {
        StringBuilder sb = new StringBuilder();
        sb.append("📖 ").append(receta.getTitulo() != null ? receta.getTitulo() : "")
          .append("\n").append("Por ").append(receta.getAutor() != null ? receta.getAutor() : "")
          .append("\n");
        if (receta.getDescripcion() != null && !receta.getDescripcion().isEmpty()) {
            sb.append("\n").append(receta.getDescripcion()).append("\n");
        }
        java.util.List<String> ings = receta.getIngredientes();
        if (ings != null && !ings.isEmpty()) {
            sb.append("\nIngredientes:\n");
            for (String i : ings) sb.append("• ").append(i).append("\n");
        }
        java.util.List<String> pasos = receta.getPasos();
        if (pasos != null && !pasos.isEmpty()) {
            sb.append("\nPasos:\n");
            int n = 1;
            for (String p : pasos) sb.append(n++).append(". ").append(p).append("\n");
        }
        sb.append("\n— Sazón");

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT,
                receta.getTitulo() != null ? receta.getTitulo() : "Receta");
        share.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(share, "Compartir receta"));
    }

    private void elegirChatParaCompartir() {
        String uid = SessionManager.currentUid();
        if (uid == null) {
            Toast.makeText(this, "Inicia sesión para enviar la receta", Toast.LENGTH_SHORT).show();
            return;
        }
        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .whereArrayContains("participants", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    java.util.List<String[]> opciones = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        String chatId = doc.getId();
                        String otherName = "Chat";
                        Object mapObj = doc.get("participantsNames");
                        if (mapObj instanceof java.util.Map) {
                            java.util.Map<?, ?> names = (java.util.Map<?, ?>) mapObj;
                            for (java.util.Map.Entry<?, ?> entry : names.entrySet()) {
                                if (entry.getKey() != null
                                        && !entry.getKey().toString().equals(uid)) {
                                    otherName = String.valueOf(entry.getValue());
                                    break;
                                }
                            }
                        }
                        opciones.add(new String[] { chatId, otherName });
                    }
                    if (opciones.isEmpty()) {
                        Toast.makeText(this, "No tienes chats todavía", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] nombres = new String[opciones.size()];
                    for (int i = 0; i < opciones.size(); i++) nombres[i] = opciones.get(i)[1];
                    new AlertDialog.Builder(this)
                            .setTitle("Enviar a…")
                            .setItems(nombres, (d, idx) ->
                                    enviarRecetaAChat(opciones.get(idx)[0], opciones.get(idx)[1]))
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudieron cargar los chats",
                                Toast.LENGTH_SHORT).show());
    }

    private void enviarRecetaAChat(String chatId, String nombreChat) {
        String uid = SessionManager.currentUid();
        if (uid == null || chatId == null || receta == null) return;
        String texto = "📖 " + receta.getTitulo() + "\nde " + receta.getAutor()
                + "\n\nMira esta receta en Sazón.";

        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("text", texto);
        msg.put("senderId", uid);
        msg.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        msg.put("readBy", java.util.Collections.singletonList(uid));
        msg.put("recipeId", recipeId);

        SessionManager.db()
                .collection(SessionManager.COLLECTION_CHATS)
                .document(chatId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(ref -> {
                    // Actualizamos cabecera del chat para que la receta sea el último mensaje
                    java.util.Map<String, Object> update = new java.util.HashMap<>();
                    update.put("lastMessage", "📖 " + receta.getTitulo());
                    update.put("lastSenderId", uid);
                    update.put("lastMessageAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    SessionManager.db()
                            .collection(SessionManager.COLLECTION_CHATS)
                            .document(chatId)
                            .set(update, com.google.firebase.firestore.SetOptions.merge());
                    Toast.makeText(this, "Receta enviada a " + nombreChat,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudo enviar",
                                Toast.LENGTH_SHORT).show());
    }

    private void abrirPerfilAutor() {
        if (receta == null || receta.getAuthorId() == null) return;
        Intent i = new Intent(this, ProfileActivity.class);
        i.putExtra(ProfileActivity.EXTRA_USER_ID, receta.getAuthorId());
        i.putExtra(ProfileActivity.EXTRA_USERNAME, receta.getAutor());
        i.putExtra(ProfileActivity.EXTRA_IS_OWN_PROFILE, false);
        startActivity(i);
    }

    private void mostrarLikers() {
        if (recipeId == null) return;
        SessionManager.db()
                .collection(RecipeRepository.COLLECTION_RECIPES)
                .document(recipeId)
                .collection("likes")
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this, "Aún nadie ha dado like 💔",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    java.util.List<String> uids = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        uids.add(doc.getId());
                    }
                    resolverNombresYAbrirDialogo(uids);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudo cargar la lista",
                                Toast.LENGTH_SHORT).show());
    }

    private void resolverNombresYAbrirDialogo(java.util.List<String> uids) {
        java.util.List<String> nombres = new java.util.ArrayList<>();
        int[] pendientes = { uids.size() };
        Runnable showDialog = () -> {
            new AlertDialog.Builder(this)
                    .setTitle("A " + nombres.size() + " chefs les gusta")
                    .setItems(nombres.toArray(new String[0]), null)
                    .setPositiveButton("Cerrar", null)
                    .show();
        };
        for (String uid : uids) {
            SessionManager.db()
                    .collection(SessionManager.COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        String n = null;
                        if (task.isSuccessful() && task.getResult() != null) {
                            n = task.getResult().getString("name");
                        }
                        if (n == null || n.isEmpty()) n = "Chef anónimo";
                        nombres.add(n);
                        pendientes[0]--;
                        if (pendientes[0] == 0) {
                            java.util.Collections.sort(nombres, String.CASE_INSENSITIVE_ORDER);
                            showDialog.run();
                        }
                    });
        }
    }

    private void abrirModoCocinar() {
        if (receta == null || recipeId == null) return;
        if (receta.getPasos() == null || receta.getPasos().isEmpty()) {
            Toast.makeText(this,
                    "Esta receta aún no tiene pasos paso a paso. Edítala para añadirlos.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(CookModeActivity.intentFor(this, recipeId));
    }

    private void anadirALaCompra() {
        if (receta == null) return;
        java.util.List<String> ings = receta.getIngredientes();
        if (ings == null || ings.isEmpty()) {
            Toast.makeText(this,
                    "Esta receta no tiene ingredientes para añadir a la lista.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        com.sazon.proyectointegrador.util.ShoppingList.addAll(this, ings,
                receta.getTitulo() == null ? "" : receta.getTitulo());
        Toast.makeText(this, "Ingredientes añadidos a tu lista de la compra",
                Toast.LENGTH_SHORT).show();
    }

    private void editarReceta() {
        if (receta == null || receta.getId() == null) return;
        Intent i = new Intent(this, CreateRecipeActivity.class);
        i.putExtra(CreateRecipeActivity.EXTRA_EDIT_RECIPE_ID, receta.getId());
        startActivity(i);
    }

    private String currentAuthorName() {
        FirebaseUser user = SessionManager.currentUser();
        if (user != null) {
            String display = user.getDisplayName();
            if (display != null && !display.trim().isEmpty()) return display.trim();
            String email = user.getEmail();
            if (email != null && email.contains("@")) {
                return email.substring(0, email.indexOf("@"));
            }
        }
        return "Chef";
    }

    @Override
    protected void onDestroy() {
        RecipeStateBus.unregister(recipeStateListener);
        if (commentsRegistration != null) {
            commentsRegistration.remove();
            commentsRegistration = null;
        }
        super.onDestroy();
    }

    private void onRecipeStateChanged(RecipeStateBus.RecipeState state) {
        if (recipeId == null || receta == null || !recipeId.equals(state.recipeId)) return;
        RecipeStateBus.apply(receta, state);
        if (state.liked != null) liked = state.liked;
        if (state.saved != null) guardada = state.saved;
        pintarBotonLike();
        pintarBotonGuardar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (receta != null && recipeId != null) cargarReceta();
    }

    /** "1.2k visualizaciones" / "47 visualizaciones". */
    private static String formatViewsCount(int views) {
        if (views >= 1000) {
            double k = views / 1000.0;
            return String.format(new java.util.Locale("es", "ES"),
                    "%.1fk visualizaciones", k);
        }
        return views + " visualizaciones";
    }

    private static String formatRelativeTime(long createdAt) {
        if (createdAt <= 0) return "";
        long diff = System.currentTimeMillis() - createdAt;
        if (diff < 0) diff = 0;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);
        if (minutes < 60) return "hace " + minutes + " min";
        if (hours < 24) return "hace " + hours + " h";
        if (days == 1) return "ayer";
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(createdAt);
        Calendar now = Calendar.getInstance();
        if (then.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            return new java.text.SimpleDateFormat("d 'de' MMMM",
                    new java.util.Locale("es","ES")).format(then.getTime());
        }
        return new java.text.SimpleDateFormat("d MMM yyyy",
                new java.util.Locale("es","ES")).format(then.getTime());
    }
}
