package com.sazon.proyectointegrador;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sazon.proyectointegrador.util.RecipeImageHelper;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;

public class CreateRecipeActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_RECIPE_ID = "EXTRA_EDIT_RECIPE_ID";

    private TextInputLayout tilTitle, tilDesc, tilPrepMinutes, tilServings;
    private TextInputEditText etTitle, etDesc, etPrepMinutes, etServings, etDifficulty, etTags;
    private TextInputEditText etIngredientes, etPasos;
    private TextView tvScreenTitle, tvScreenSubtitle;
    private MaterialButton btnPublish, btnRemoveImage;
    private ImageButton btnCancel;
    private MaterialCardView cardImagePicker;
    private ImageView ivRecipePreview;
    private View photoPlaceholder;
    private Uri selectedImageUri;
    private String editingRecipeId;
    private String currentImageValue = "";
    private boolean removeImage = false;
    private ActivityResultLauncher<PickVisualMediaRequest> imagePicker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_recipe);

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri == null) return;
                    selectedImageUri = uri;
                    removeImage = false;
                    if (ivRecipePreview != null) {
                        ivRecipePreview.setImageURI(uri);
                        ivRecipePreview.setVisibility(View.VISIBLE);
                    }
                    if (photoPlaceholder != null) photoPlaceholder.setVisibility(View.GONE);
                    if (btnRemoveImage != null) btnRemoveImage.setVisibility(View.VISIBLE);
                });

        editingRecipeId = getIntent().getStringExtra(EXTRA_EDIT_RECIPE_ID);
        tilTitle = findViewById(R.id.tilRecipeTitle);
        tilDesc  = findViewById(R.id.tilRecipeDesc);
        tilPrepMinutes = findViewById(R.id.tilPrepMinutes);
        tilServings = findViewById(R.id.tilServings);
        etTitle  = findViewById(R.id.etRecipeTitle);
        etDesc   = findViewById(R.id.etRecipeDesc);
        etPrepMinutes = findViewById(R.id.etPrepMinutes);
        etServings = findViewById(R.id.etServings);
        etDifficulty = findViewById(R.id.etDifficulty);
        etTags = findViewById(R.id.etRecipeTags);
        etIngredientes = findViewById(R.id.etIngredientes);
        etPasos = findViewById(R.id.etPasos);
        tvScreenTitle = findViewById(R.id.tvCreateRecipeTitle);
        tvScreenSubtitle = findViewById(R.id.tvCreateRecipeSubtitle);
        btnPublish = findViewById(R.id.btnPublishRecipe);
        btnRemoveImage = findViewById(R.id.btnRemoveRecipeImage);
        btnCancel  = findViewById(R.id.btnCancelRecipe);
        cardImagePicker = findViewById(R.id.cardRecipeImagePicker);
        ivRecipePreview = findViewById(R.id.ivRecipePreview);
        photoPlaceholder = findViewById(R.id.photoPlaceholder);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());
        if (btnPublish != null) btnPublish.setOnClickListener(v -> attemptPublish());
        if (btnRemoveImage != null) btnRemoveImage.setOnClickListener(v -> clearImage());
        if (cardImagePicker != null) {
            cardImagePicker.setOnClickListener(v -> imagePicker.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build()));
        }

        if (isEditMode()) setupEditMode();
    }

    private void attemptPublish() {
        if (!SessionManager.isAuthenticated()) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String desc  = etDesc.getText()  != null ? etDesc.getText().toString().trim()  : "";
        int prepMinutes = parsePositiveInt(etPrepMinutes, 0);
        int servings = parsePositiveInt(etServings, 0);
        String difficulty = textOf(etDifficulty);
        String tags = normalizeTags(textOf(etTags));

        if (title.isEmpty()) {
            tilTitle.setError("Ponle un título");
            return;
        }
        if (desc.isEmpty()) {
            tilDesc.setError("Cuenta cómo se hace");
            return;
        }

        if (prepMinutes < 0) {
            if (tilPrepMinutes != null) tilPrepMinutes.setError("Usa minutos validos");
            return;
        }
        if (servings < 0) {
            if (tilServings != null) tilServings.setError("Usa raciones validas");
            return;
        }

        tilTitle.setError(null);
        tilDesc.setError(null);
        if (tilPrepMinutes != null) tilPrepMinutes.setError(null);
        if (tilServings != null) tilServings.setError(null);
        setLoading(true);

        String uid = SessionManager.currentUid();
        String authorName = new SessionManager(this).getUserName();
        if (authorName == null || authorName.isEmpty()) {
            String email = SessionManager.currentEmail();
            authorName = email != null && email.contains("@")
                    ? email.substring(0, email.indexOf("@"))
                    : "Chef";
        }

        String imageValue = removeImage ? "" : currentImageValue;
        if (selectedImageUri != null) {
            imageValue = RecipeImageHelper.toFirestoreImage(getContentResolver(), selectedImageUri);
            if (imageValue == null) {
                setLoading(false);
                Toast.makeText(this, "La foto es demasiado grande para guardarla gratis",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        java.util.List<String> ingredientes = splitLines(etIngredientes);
        java.util.List<String> pasos = splitLines(etPasos);

        if (isEditMode()) {
            updateRecipe(title, desc, imageValue, difficulty, tags, prepMinutes, servings,
                    ingredientes, pasos);
        } else {
            createRecipe(uid, authorName, title, desc, imageValue,
                    difficulty, tags, prepMinutes, servings,
                    ingredientes, pasos);
        }
    }

    private void createRecipe(@NonNull String uid,
                              @NonNull String authorName,
                              @NonNull String title,
                              @NonNull String desc,
                              @NonNull String imageUrl,
                              @NonNull String difficulty,
                              @NonNull String tags,
                              int prepMinutes,
                              int servings,
                              @NonNull java.util.List<String> ingredientes,
                              @NonNull java.util.List<String> pasos) {
        RecipeRepository.create(uid, authorName, title, desc, imageUrl,
                difficulty, tags, prepMinutes, servings, ingredientes, pasos,
                ref -> {
                    Toast.makeText(this, "Receta publicada", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> {
                    setLoading(false);
                    Toast.makeText(this, "No se pudo publicar la receta",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRecipe(@NonNull String title,
                              @NonNull String desc,
                              @NonNull String imageUrl,
                              @NonNull String difficulty,
                              @NonNull String tags,
                              int prepMinutes,
                              int servings,
                              @NonNull java.util.List<String> ingredientes,
                              @NonNull java.util.List<String> pasos) {
        RecipeRepository.updateRecipe(editingRecipeId, title, desc, imageUrl,
                difficulty, tags, prepMinutes, servings, ingredientes, pasos,
                unused -> {
                    Toast.makeText(this, "Receta actualizada", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> {
                    setLoading(false);
                    Toast.makeText(this, "No se pudo actualizar la receta",
                            Toast.LENGTH_SHORT).show();
                });
    }

    /** Parte un EditText multilinea en lista de strings, descartando líneas vacías. */
    private static java.util.List<String> splitLines(TextInputEditText input) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (input == null || input.getText() == null) return out;
        String raw = input.getText().toString();
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private void setupEditMode() {
        if (tvScreenTitle != null) tvScreenTitle.setText("Edita tu receta");
        if (tvScreenSubtitle != null) {
            tvScreenSubtitle.setText("Actualiza el texto o cambia la foto.");
        }
        if (btnPublish != null) btnPublish.setText("Guardar cambios");
        setLoading(true);

        SessionManager.db()
                .collection(RecipeRepository.COLLECTION_RECIPES)
                .document(editingRecipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this, "Esta receta ya no existe", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    String currentUid = SessionManager.currentUid();
                    String authorId = doc.getString("authorId");
                    if (currentUid == null || !currentUid.equals(authorId)) {
                        Toast.makeText(this, "Sólo el autor puede editar esta receta",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    String title = doc.getString("titulo");
                    String desc = doc.getString("descripcion");
                    String difficulty = doc.getString("difficulty");
                    String tags = doc.getString("tags");
                    Long prepMinutes = doc.getLong("prepMinutes");
                    Long servings = doc.getLong("servings");
                    currentImageValue = doc.getString("imageUrl");
                    if (currentImageValue == null) currentImageValue = "";

                    if (etTitle != null) etTitle.setText(title == null ? "" : title);
                    if (etDesc != null) etDesc.setText(desc == null ? "" : desc);
                    if (etDifficulty != null) etDifficulty.setText(difficulty == null ? "" : difficulty);
                    if (etTags != null) etTags.setText(tags == null ? "" : tags);
                    if (etPrepMinutes != null && prepMinutes != null && prepMinutes > 0) {
                        etPrepMinutes.setText(String.valueOf(prepMinutes));
                    }
                    if (etServings != null && servings != null && servings > 0) {
                        etServings.setText(String.valueOf(servings));
                    }
                    // Repoblar ingredientes y pasos uniéndolos por salto de línea
                    Object ingObj = doc.get("ingredientes");
                    if (etIngredientes != null && ingObj instanceof java.util.List) {
                        StringBuilder sb = new StringBuilder();
                        for (Object o : (java.util.List<?>) ingObj) {
                            if (o == null) continue;
                            if (sb.length() > 0) sb.append('\n');
                            sb.append(o.toString());
                        }
                        etIngredientes.setText(sb.toString());
                    }
                    Object pasosObj = doc.get("pasos");
                    if (etPasos != null && pasosObj instanceof java.util.List) {
                        StringBuilder sb = new StringBuilder();
                        for (Object o : (java.util.List<?>) pasosObj) {
                            if (o == null) continue;
                            if (sb.length() > 0) sb.append('\n');
                            sb.append(o.toString());
                        }
                        etPasos.setText(sb.toString());
                    }
                    showCurrentImage();
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No se pudo cargar la receta",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void showCurrentImage() {
        if (currentImageValue == null || currentImageValue.trim().isEmpty()) {
            if (ivRecipePreview != null) {
                ivRecipePreview.setImageDrawable(null);
                ivRecipePreview.setVisibility(View.GONE);
            }
            if (photoPlaceholder != null) photoPlaceholder.setVisibility(View.VISIBLE);
            if (btnRemoveImage != null) btnRemoveImage.setVisibility(View.GONE);
            return;
        }
        if (ivRecipePreview != null) {
            ivRecipePreview.setVisibility(View.VISIBLE);
            RecipeImageHelper.loadInto(ivRecipePreview, currentImageValue);
        }
        if (photoPlaceholder != null) photoPlaceholder.setVisibility(View.GONE);
        if (btnRemoveImage != null) btnRemoveImage.setVisibility(View.VISIBLE);
    }

    private void clearImage() {
        selectedImageUri = null;
        currentImageValue = "";
        removeImage = true;
        showCurrentImage();
    }

    private boolean isEditMode() {
        return editingRecipeId != null && !editingRecipeId.trim().isEmpty();
    }

    private void setLoading(boolean loading) {
        btnPublish.setEnabled(!loading);
        if (isEditMode()) {
            btnPublish.setText(loading ? "Guardando..." : "Guardar cambios");
        } else {
            btnPublish.setText(loading ? "Publicando..." : "Publicar receta");
        }
        etTitle.setEnabled(!loading);
        etDesc.setEnabled(!loading);
        if (etPrepMinutes != null) etPrepMinutes.setEnabled(!loading);
        if (etServings != null) etServings.setEnabled(!loading);
        if (etDifficulty != null) etDifficulty.setEnabled(!loading);
        if (etTags != null) etTags.setEnabled(!loading);
        if (cardImagePicker != null) cardImagePicker.setEnabled(!loading);
        if (btnRemoveImage != null) btnRemoveImage.setEnabled(!loading);
    }

    private static String textOf(TextInputEditText input) {
        return input != null && input.getText() != null ? input.getText().toString().trim() : "";
    }

    private static int parsePositiveInt(TextInputEditText input, int fallback) {
        String value = textOf(input);
        if (value.isEmpty()) return fallback;
        try {
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? -1 : parsed;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String normalizeTags(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String[] parts = raw.split(",");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            String tag = part.trim();
            if (tag.isEmpty()) continue;
            if (out.length() > 0) out.append(", ");
            out.append(tag);
        }
        return out.toString();
    }
}
