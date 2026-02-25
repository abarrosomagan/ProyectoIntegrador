package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sazon.proyectointegrador.util.SimpleTextWatcher;

public class RegisterActivity extends AppCompatActivity {

    // UI
    private View root;
    private TextInputLayout tilName, tilEmail, tilPass, tilPass2;
    private TextInputEditText etName, etEmail, etPass, etPass2;
    private MaterialButton btnRegister;
    private TextView tvGoLogin;

    // (Preparado para Firebase - aún no lo usamos)
    // private FirebaseAuth firebaseAuth;
    // private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        bindViews();
        // initFirebase(); // <- lo activaremos cuando metas dependencias y google-services

        setupListeners();
    }

    private void bindViews() {
        root = findViewById(R.id.register_root);

        tilName = findViewById(R.id.til_name);
        tilEmail = findViewById(R.id.til_email);
        tilPass = findViewById(R.id.til_pass);
        tilPass2 = findViewById(R.id.til_pass2);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPass = findViewById(R.id.et_pass);
        etPass2 = findViewById(R.id.et_pass2);

        btnRegister = findViewById(R.id.btn_register);
        tvGoLogin = findViewById(R.id.tv_go_login);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());

        tvGoLogin.setOnClickListener(v -> goToLogin());

        // UX: al escribir, limpiamos errores (buena práctica)
        addClearErrorOnType(etName, tilName);
        addClearErrorOnType(etEmail, tilEmail);
        addClearErrorOnType(etPass, tilPass);
        addClearErrorOnType(etPass2, tilPass2);
    }

    private void addClearErrorOnType(TextInputEditText editText, TextInputLayout layout) {
        if (editText == null || layout == null) return;
        editText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
        });
    }

    private void attemptRegister() {
        clearAllErrors();

        String name = getText(etName).trim();
        String email = getText(etEmail).trim();
        String pass1 = getText(etPass);
        String pass2 = getText(etPass2);

        if (!validate(name, email, pass1, pass2)) return;

        setLoading(true);

        // ======= MODO ACTUAL (sin Firebase): simulación =======
        // Aquí luego llamaremos a Firebase. De momento simulamos OK.
        root.postDelayed(() -> {
            setLoading(false);
            showMessage("Cuenta creada (simulación).");
            // Decide tu flujo:
            // 1) Volver a Login:
            goToLogin();

            // 2) O entrar directo a Main:
            // startActivity(new Intent(this, MainActivity.class));
            // finish();
        }, 700);

        // ======= FUTURO: Firebase (lo dejamos preparado) =======
        /*
        firebaseAuth.createUserWithEmailAndPassword(email, pass1)
            .addOnSuccessListener(authResult -> {
                String uid = authResult.getUser().getUid();

                // Crear documento usuario en Firestore
                Map<String, Object> user = new HashMap<>();
                user.put("uid", uid);
                user.put("name", name);
                user.put("email", email);
                user.put("createdAt", FieldValue.serverTimestamp());

                firestore.collection("users").document(uid).set(user)
                    .addOnSuccessListener(unused -> {
                        setLoading(false);
                        showMessage("Cuenta creada correctamente");
                        goToLogin();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        showMessage("Error guardando perfil: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                handleFirebaseRegisterError(e);
            });
        */
    }

    private boolean validate(String name, String email, String pass1, String pass2) {
        boolean ok = true;

        if (name.isEmpty()) {
            tilName.setError("Introduce tu nombre");
            ok = false;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email no válido");
            ok = false;
        }

        // Reglas típicas (ajústalas si el profe pide otra cosa)
        if (pass1.length() < 6) {
            tilPass.setError("La contraseña debe tener al menos 6 caracteres");
            ok = false;
        }

        if (!pass1.equals(pass2)) {
            tilPass2.setError("Las contraseñas no coinciden");
            ok = false;
        }

        return ok;
    }

    private void clearAllErrors() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPass.setError(null);
        tilPass2.setError(null);
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Creando..." : "Registrarse");

        // Opcional: desactivar inputs para evitar doble envío
        etName.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPass.setEnabled(!loading);
        etPass2.setEnabled(!loading);
        tvGoLogin.setEnabled(!loading);
    }

    private void goToLogin() {
        // Si Register se abre desde Login, normalmente con finish() vale.
        // Si no, usa Intent explícito:
        try {
            finish();
        } catch (Exception ignored) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString() : "";
    }

    private void showMessage(String msg) {
        Snackbar.make(root, msg, Snackbar.LENGTH_SHORT).show();
    }

    // (Preparado para Firebase)
    /*
    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void handleFirebaseRegisterError(Exception e) {
        // Aquí podrías mapear errores típicos:
        // - email en uso
        // - contraseña débil
        // - formato email
        showMessage("Error: " + e.getMessage());
    }
    */
}