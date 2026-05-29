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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.sazon.proyectointegrador.util.SessionManager;
import com.sazon.proyectointegrador.util.SimpleTextWatcher;

public class RegisterActivity extends AppCompatActivity {

    // UI
    private View root;
    private TextInputLayout tilName, tilEmail, tilPass, tilPass2;
    private TextInputEditText etName, etEmail, etPass, etPass2;
    private MaterialButton btnRegister;
    private TextView tvGoLogin;

    // Firebase
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();

        bindViews();
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
        if (btnRegister != null) btnRegister.setOnClickListener(v -> attemptRegister());
        if (tvGoLogin != null) tvGoLogin.setOnClickListener(v -> goToLogin());

        // UX: al escribir, limpiamos errores
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

        // ======= FIREBASE AUTH (Email/Password) =======
        firebaseAuth.createUserWithEmailAndPassword(email, pass1)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        showMessage(parseAuthError(task.getException()));
                        return;
                    }

                    // Usuario creado OK -> actualizamos displayName
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        setLoading(false);
                        showMessage("Cuenta creada, pero no se pudo obtener el usuario.");
                        goToLogin();
                        return;
                    }

                    UserProfileChangeRequest profileUpdates =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                    // Envío del correo de verificación (silencioso, no bloquea el alta)
                    user.sendEmailVerification();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(this, task2 -> {
                                // Cacheamos en SharedPreferences para que el saludo del feed lo coja al vuelo
                                new SessionManager(RegisterActivity.this).login(user.getUid(), name);

                                // Creamos el doc de Firestore. Si falla, igual entramos al main:
                                // la cuenta ya existe en Auth y se puede recuperar luego.
                                SessionManager.createUserDoc(
                                        user.getUid(), name, email,
                                        unused -> {
                                            setLoading(false);
                                            goToMain();
                                        },
                                        e -> {
                                            setLoading(false);
                                            showMessage("Cuenta creada (perfil pendiente de guardar)");
                                            goToMain();
                                        });
                            });
                });
    }

    private boolean validate(String name, String email, String pass1, String pass2) {
        boolean ok = true;

        if (tilName != null && name.isEmpty()) {
            tilName.setError("Introduce tu nombre");
            ok = false;
        }

        if (tilEmail != null && (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            tilEmail.setError("Email no válido");
            ok = false;
        }

        if (tilPass != null && pass1.length() < 6) {
            tilPass.setError("La contraseña debe tener al menos 6 caracteres");
            ok = false;
        }

        if (tilPass2 != null && !pass1.equals(pass2)) {
            tilPass2.setError("Las contraseñas no coinciden");
            ok = false;
        }

        return ok;
    }

    private void clearAllErrors() {
        if (tilName != null) tilName.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPass != null) tilPass.setError(null);
        if (tilPass2 != null) tilPass2.setError(null);
    }

    private void setLoading(boolean loading) {
        if (btnRegister != null) {
            btnRegister.setEnabled(!loading);
            btnRegister.setText(loading ? "Creando..." : "Registrarse");
        }

        if (etName != null) etName.setEnabled(!loading);
        if (etEmail != null) etEmail.setEnabled(!loading);
        if (etPass != null) etPass.setEnabled(!loading);
        if (etPass2 != null) etPass2.setEnabled(!loading);
        if (tvGoLogin != null) tvGoLogin.setEnabled(!loading);
    }

    private void goToLogin() {
        // Si Register se abre desde Login, con finish() vale
        try {
            finish();
        } catch (Exception ignored) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void goToMain() {
        Intent i = new Intent(RegisterActivity.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString() : "";
    }

    private void showMessage(String msg) {
        if (root != null) Snackbar.make(root, msg, Snackbar.LENGTH_SHORT).show();
    }

    private String parseAuthError(Exception e) {
        if (e == null) return "Error al registrar";

        // Primero los tipos conocidos de Firebase Auth (más fiable que parsear texto)
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Ese email ya está registrado.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Contraseña demasiado débil (mínimo 6 caracteres).";
        }

        // Como fallback, miramos el texto del mensaje
        String msg = e.getMessage();
        if (msg == null) return "Error al registrar";
        String lower = msg.toLowerCase();

        if (lower.contains("badly formatted")) return "Email mal formado.";
        if (lower.contains("network")) return "Sin conexión. Inténtalo de nuevo.";

        return "No se pudo completar el registro";
    }
}