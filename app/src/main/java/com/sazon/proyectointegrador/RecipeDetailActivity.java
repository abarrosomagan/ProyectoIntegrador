package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
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
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "EXTRA_RECIPE_ID";

    private String recipeId;
    private Publicacion receta;
    private boolean guardada = false;
    private boolean liked = false;

    private TextView tvTitle, tvAuthor, tvAuthorAvatar, tvDate, tvDescription;
    private TextView tvCommentsTitle, tvCommentsEmpty;
    private TextInputEditText etComment;
    private MaterialButton btnLike, btnSave, btnDelete, btnViewProfile;
    private MaterialButton btnSendComment;
    private RecyclerView rvComments;
    private RecipeCommentAdapter commentsAdapter;
    private ListenerRegistration commentsRegistration;

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
        btnLike = findViewById(R.id.btnLikeRecipe);
        btnSave = findViewById(R.id.btnSaveRecipe);
        btnDelete = findViewById(R.id.btnDeleteRecipe);
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

        if (btnLike != null) btnLike.setOnClickListener(v -> darLike());
        if (btnSave != null) btnSave.setOnClickListener(v -> alternarGuardado());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> confirmarEliminar());
        if (btnViewProfile != null) btnViewProfile.setOnClickListener(v -> abrirPerfilAutor());
        if (btnSendComment != null) btnSendComment.setOnClickListener(v -> enviarComentario());
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
                    pintarReceta();
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

        pintarBotonLike();

        // El autor es quien la creó: muestra el botón Eliminar
        String meUid = SessionManager.currentUid();
        boolean soyAutor = meUid != null && meUid.equals(receta.getAuthorId());
        btnDelete.setVisibility(soyAutor ? android.view.View.VISIBLE
                : android.view.View.GONE);
        btnViewProfile.setVisibility(soyAutor ? android.view.View.GONE
                : android.view.View.VISIBLE);
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
        btnSave.setText(guardada ? "Guardada ⭐" : "Guardar");
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
        btnLike.setText((liked ? "♥ " : "♡ ") + receta.getLikes());
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
        RecipeRepository.toggleLike(recipeId, uid, nuevoLiked, null, e -> {
            liked = !nuevoLiked;
            receta.setLiked(liked);
            receta.setLikes(likesAnteriores);
            pintarBotonLike();
        });
    }

    private void alternarGuardado() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;
        boolean nuevo = !guardada;
        guardada = nuevo;
        pintarBotonGuardar();
        RecipeRepository.toggleSaved(recipeId, uid, nuevo, null, e -> {
            guardada = !nuevo;
            pintarBotonGuardar();
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
        String texto = "📖 \"" + receta.getTitulo() + "\" de "
                + receta.getAutor() + " — en Sazón";
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, texto);
        startActivity(Intent.createChooser(share, "Compartir receta"));
    }

    private void abrirPerfilAutor() {
        if (receta == null || receta.getAuthorId() == null) return;
        Intent i = new Intent(this, ProfileActivity.class);
        i.putExtra(ProfileActivity.EXTRA_USER_ID, receta.getAuthorId());
        i.putExtra(ProfileActivity.EXTRA_USERNAME, receta.getAutor());
        i.putExtra(ProfileActivity.EXTRA_IS_OWN_PROFILE, false);
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
        if (commentsRegistration != null) {
            commentsRegistration.remove();
            commentsRegistration = null;
        }
        super.onDestroy();
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
