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

import com.google.android.material.button.MaterialButton;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.Calendar;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "EXTRA_RECIPE_ID";

    private String recipeId;
    private Publicacion receta;
    private boolean guardada = false;

    private TextView tvTitle, tvAuthor, tvAuthorAvatar, tvDate, tvDescription;
    private MaterialButton btnLike, btnSave, btnDelete, btnViewProfile;

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

        ImageButton btnBack = findViewById(R.id.btnBackRecipe);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageButton btnShare = findViewById(R.id.btnShareRecipe);
        if (btnShare != null) btnShare.setOnClickListener(v -> compartirReceta());

        if (btnLike != null) btnLike.setOnClickListener(v -> darLike());
        if (btnSave != null) btnSave.setOnClickListener(v -> alternarGuardado());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> confirmarEliminar());
        if (btnViewProfile != null) btnViewProfile.setOnClickListener(v -> abrirPerfilAutor());
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

        btnLike.setText("❤ " + receta.getLikes());

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

    // ===== Acciones =====

    private void darLike() {
        String uid = SessionManager.currentUid();
        if (uid == null || receta == null) return;
        int n = receta.getLikes() + 1;
        receta.setLikes(n);
        btnLike.setText("❤ " + n);
        RecipeRepository.toggleLike(recipeId, uid, true, null, e -> {
            receta.setLikes(n - 1);
            btnLike.setText("❤ " + (n - 1));
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
