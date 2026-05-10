package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.sazon.proyectointegrador.util.SessionManager;
import com.sazon.proyectointegrador.util.SimpleTextWatcher;

public class LoginActivity extends AppCompatActivity {

    private View root;

    private TextInputLayout tilEmail, tilPass;
    private TextInputEditText etEmail, etPass;

    private MaterialButton btnLogin, btnGoogle;
    private TextView tvForgot, tvRegister;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        bindViews();
        setupListeners();
    }

    private void bindViews() {
        root = findViewById(R.id.activity_login);
        if (root == null) root = getWindow().getDecorView();

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

        if (tvForgot != null) tvForgot.setOnClickListener(v -> showForgotPasswordDialog());

        if (btnGoogle != null) btnGoogle.setOnClickListener(v ->
                showMessage("Login con Google (pendiente)")
        );

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

        firebaseAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (!task.isSuccessful()) {
                        handleLoginError(task.getException());
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        showMessage("Login OK, pero user es null.");
                        return;
                    }

                    String name = (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty())
                            ? user.getDisplayName().trim()
                            : (user.getEmail() != null && user.getEmail().contains("@")
                            ? user.getEmail().substring(0, user.getEmail().indexOf("@"))
                            : "Usuario");

                    new SessionManager(this).login(user.getUid(), name);

                    goToMain();
                });
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

    private void handleLoginError(Exception e) {
        // Mapeo legible en castellano. Tipos > parseo de texto.
        if (e instanceof FirebaseAuthInvalidUserException) {
            if (tilEmail != null) tilEmail.setError("No existe una cuenta con ese correo");
            return;
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            if (tilPass != null) tilPass.setError("Contraseña incorrecta");
            return;
        }
        String msg = e != null && e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("network")) {
            showMessage("Sin conexión. Inténtalo de nuevo.");
            return;
        }
        showMessage("No se pudo entrar. Inténtalo de nuevo.");
    }

    // ===== Recuperar contraseña =====

    private void showForgotPasswordDialog() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Tu correo electrónico");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Prerellenamos con lo que ya esté escrito en el login
        String prefill = getText(etEmail).trim();
        if (!prefill.isEmpty()) input.setText(prefill);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad * 2, pad, pad * 2, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña")
                .setMessage("Te enviaremos un enlace para crear una nueva contraseña.")
                .setView(container)
                .setPositiveButton("Enviar", (d, w) -> sendResetEmail(
                        input.getText() != null ? input.getText().toString().trim() : ""))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void sendResetEmail(String email) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Email no válido");
            return;
        }
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        showMessage("Te hemos enviado un correo a " + email))
                .addOnFailureListener(e ->
                        showMessage("No se pudo enviar el correo de recuperación"));
    }

    private String getText(TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString() : "";
    }

    private void showMessage(String msg) {
        if (root != null) Snackbar.make(root, msg, Snackbar.LENGTH_SHORT).show();
    }
}