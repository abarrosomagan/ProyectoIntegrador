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
import com.sazon.proyectointegrador.util.SessionManager;
import com.sazon.proyectointegrador.util.SimpleTextWatcher;

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
        if (btnLogin != null) btnLogin.setOnClickListener(v -> attemptLogin());

        if (tvRegister != null) tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        if (tvForgot != null) tvForgot.setOnClickListener(v ->
                showMessage("Recuperación de contraseña (pendiente)")
        );

        if (btnGoogle != null) btnGoogle.setOnClickListener(v ->
                showMessage("Login con Google (pendiente)")
        );

        // UX: limpiar error al escribir
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

        // ======= MODO ACTUAL (MOCK, sin Firebase) =======
        root.postDelayed(() -> {
            setLoading(false);

            // Mock de sesión: SOLO aquí (no en onCreate)
            SessionManager session = new SessionManager(this);
            session.login("u_001", "Fernando");

            goToMain();
        }, 250);
    }

    private boolean validate(String email, String pass) {
        boolean ok = true;

        if (tilEmail != null && (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            tilEmail.setError("Email no válido");
            ok = false;
        }

        if (tilPass != null && pass.isEmpty()) {
            tilPass.setError("Introduce la contraseña");
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPass != null) tilPass.setError(null);
    }

    private void setLoading(boolean loading) {
        if (btnLogin != null) {
            btnLogin.setEnabled(!loading);
            btnLogin.setText(loading ? "Entrando..." : "Entrar");
        }

        if (etEmail != null) etEmail.setEnabled(!loading);
        if (etPass != null) etPass.setEnabled(!loading);

        if (btnGoogle != null) btnGoogle.setEnabled(!loading);
        if (tvForgot != null) tvForgot.setEnabled(!loading);
        if (tvRegister != null) tvRegister.setEnabled(!loading);
    }

    private void goToMain() {
        Intent i = new Intent(LoginActivity.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private String getText(TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString() : "";
    }

    private void showMessage(String msg) {
        if (root != null) Snackbar.make(root, msg, Snackbar.LENGTH_SHORT).show();
    }
}