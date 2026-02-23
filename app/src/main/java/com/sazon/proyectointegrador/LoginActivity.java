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

public class LoginActivity extends AppCompatActivity {

    // Root (para Snackbar)
    private View root;

    // Inputs
    private TextInputLayout tilEmail, tilPass;
    private TextInputEditText etEmail, etPass;

    // Actions
    private MaterialButton btnLogin, btnGoogle;
    private TextView tvForgot, tvRegister;

    // (Preparado para Firebase - aún no lo usamos)
    // private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        bindViews();
        // initFirebase(); // <- activar cuando metas dependencias + google-services.json

        setupListeners();
    }

    private void bindViews() {
        root = findViewById(R.id.activity_login);

        tilEmail = findViewById(R.id.uso_input_correo);
        tilPass  = findViewById(R.id.uso_input_contrasena);

        etEmail = findViewById(R.id.uso_campo_correo);
        etPass  = findViewById(R.id.uso_campo_contrasena);

        btnLogin  = findViewById(R.id.uso_boton_iniciar_sesion);
        btnGoogle = findViewById(R.id.uso_boton_google);

        tvForgot   = findViewById(R.id.uso_olvidaste_contrasena);
        tvRegister = findViewById(R.id.uso_texto_registro);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvRegister.setOnClickListener(v -> {
            // Mantén tu navegación actual al register (si tu clase se llama distinto, cámbialo aquí)
            startActivity(new Intent(this, RegisterActivity.class));
        });

        tvForgot.setOnClickListener(v -> {
            // Funcionalidad pendiente (más adelante Firebase reset)
            showMessage("Recuperación de contraseña (pendiente)");
        });

        btnGoogle.setOnClickListener(v -> {
            // Funcionalidad pendiente (más adelante Google Sign-In + Firebase)
            showMessage("Login con Google (pendiente)");
        });

        // Buena práctica: limpiar error al escribir
        addClearErrorOnType(etEmail, tilEmail);
        addClearErrorOnType(etPass, tilPass);
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

    private void attemptLogin() {
        clearErrors();

        String email = getText(etEmail).trim();
        String pass  = getText(etPass);

        if (!validate(email, pass)) return;

        setLoading(true);

        // ======= MODO ACTUAL (sin Firebase): NO cambia funcionalidad =======
        // Si hoy tu login “entra” al pasar validación, lo mantenemos igual.
        root.postDelayed(() -> {
            setLoading(false);
            goToMain();
        }, 250);

        // ======= FUTURO: Firebase Auth =======
        /*
        firebaseAuth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener(authResult -> {
                setLoading(false);
                goToMain();
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                handleFirebaseLoginError(e);
            });
        */
    }

    private boolean validate(String email, String pass) {
        boolean ok = true;

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email no válido");
            ok = false;
        }

        if (pass.isEmpty()) {
            tilPass.setError("Introduce la contraseña");
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPass.setError(null);
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Entrando..." : "Entrar");

        etEmail.setEnabled(!loading);
        etPass.setEnabled(!loading);

        btnGoogle.setEnabled(!loading);
        tvForgot.setEnabled(!loading);
        tvRegister.setEnabled(!loading);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
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
    }

    private void handleFirebaseLoginError(Exception e) {
        // Aquí mapearemos errores típicos: credenciales inválidas, usuario no existe, etc.
        showMessage("Error: " + e.getMessage());
    }
    */
}