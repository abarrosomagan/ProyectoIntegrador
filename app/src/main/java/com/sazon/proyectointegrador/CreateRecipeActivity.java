package com.sazon.proyectointegrador;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;

public class CreateRecipeActivity extends AppCompatActivity {

    private TextInputLayout tilTitle, tilDesc;
    private TextInputEditText etTitle, etDesc;
    private MaterialButton btnPublish;
    private ImageButton btnCancel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_recipe);

        tilTitle = findViewById(R.id.tilRecipeTitle);
        tilDesc  = findViewById(R.id.tilRecipeDesc);
        etTitle  = findViewById(R.id.etRecipeTitle);
        etDesc   = findViewById(R.id.etRecipeDesc);
        btnPublish = findViewById(R.id.btnPublishRecipe);
        btnCancel  = findViewById(R.id.btnCancelRecipe);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());
        if (btnPublish != null) btnPublish.setOnClickListener(v -> attemptPublish());
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

        RecipeRepository.create(uid, authorName, title, desc,
                ref -> {
                    Toast.makeText(this, "Receta publicada 🍽️", Toast.LENGTH_SHORT).show();
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
    }
}
