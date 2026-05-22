package com.sazon.proyectointegrador;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;

public class CreateRecipeActivity extends AppCompatActivity {

    private TextInputLayout tilTitle, tilDesc;
    private TextInputEditText etTitle, etDesc;
    private MaterialButton btnPublish;
    private ImageButton btnCancel;
    private MaterialCardView cardImagePicker;
    private ImageView ivRecipePreview;
    private View photoPlaceholder;
    private Uri selectedImageUri;
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
                    if (ivRecipePreview != null) {
                        ivRecipePreview.setImageURI(uri);
                        ivRecipePreview.setVisibility(View.VISIBLE);
                    }
                    if (photoPlaceholder != null) photoPlaceholder.setVisibility(View.GONE);
                });

        tilTitle = findViewById(R.id.tilRecipeTitle);
        tilDesc  = findViewById(R.id.tilRecipeDesc);
        etTitle  = findViewById(R.id.etRecipeTitle);
        etDesc   = findViewById(R.id.etRecipeDesc);
        btnPublish = findViewById(R.id.btnPublishRecipe);
        btnCancel  = findViewById(R.id.btnCancelRecipe);
        cardImagePicker = findViewById(R.id.cardRecipeImagePicker);
        ivRecipePreview = findViewById(R.id.ivRecipePreview);
        photoPlaceholder = findViewById(R.id.photoPlaceholder);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());
        if (btnPublish != null) btnPublish.setOnClickListener(v -> attemptPublish());
        if (cardImagePicker != null) {
            cardImagePicker.setOnClickListener(v -> imagePicker.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build()));
        }
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

        publishWithImage(uid, authorName, title, desc);
    }

    private void publishWithImage(@NonNull String uid,
                                  @NonNull String authorName,
                                  @NonNull String title,
                                  @NonNull String desc) {
        if (selectedImageUri == null) {
            createRecipe(uid, authorName, title, desc, "");
            return;
        }

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("recipes/" + uid + "/" + System.currentTimeMillis() + ".jpg");

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri ->
                                createRecipe(uid, authorName, title, desc, uri.toString()))
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(this, "No se pudo obtener la URL de la foto",
                                    Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "No se pudo subir la foto",
                            Toast.LENGTH_SHORT).show();
                });
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

    private void setLoading(boolean loading) {
        btnPublish.setEnabled(!loading);
        btnPublish.setText(loading ? "Publicando..." : "Publicar receta");
        etTitle.setEnabled(!loading);
        etDesc.setEnabled(!loading);
        if (cardImagePicker != null) cardImagePicker.setEnabled(!loading);
    }
}
