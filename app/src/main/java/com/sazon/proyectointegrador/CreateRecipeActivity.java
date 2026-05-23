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

    private TextInputLayout tilTitle, tilDesc;
    private TextInputEditText etTitle, etDesc;
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
        etTitle  = findViewById(R.id.etRecipeTitle);
        etDesc   = findViewById(R.id.etRecipeDesc);
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

        if (title.isEmpty()) {
            tilTitle.setError("Ponle un título");
            return;
        }
        if (desc.isEmpty()) {
            tilDesc.setError("Cuenta cómo se hace");
            return;
        }

        tilTitle.setError(null);
        tilDesc.setError(null);
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

        if (isEditMode()) {
            updateRecipe(title, desc, imageValue);
        } else {
            createRecipe(uid, authorName, title, desc, imageValue);
        }
    }

    private void createRecipe(@NonNull String uid,
                              @NonNull String authorName,
                              @NonNull String title,
                              @NonNull String desc,
                              @NonNull String imageUrl) {
        RecipeRepository.create(uid, authorName, title, desc, imageUrl,
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
                              @NonNull String imageUrl) {
        RecipeRepository.updateRecipe(editingRecipeId, title, desc, imageUrl,
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
                    currentImageValue = doc.getString("imageUrl");
                    if (currentImageValue == null) currentImageValue = "";

                    if (etTitle != null) etTitle.setText(title == null ? "" : title);
                    if (etDesc != null) etDesc.setText(desc == null ? "" : desc);
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
        if (cardImagePicker != null) cardImagePicker.setEnabled(!loading);
        if (btnRemoveImage != null) btnRemoveImage.setEnabled(!loading);
    }
}
