package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.sazon.proyectointegrador.util.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvUid, tvBio, tvRecipes, tvFollowers, tvFollowing;
    private MaterialButton btnRefresh, btnLogout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        bind();
        setupListeners();
        loadAccount();
    }

    private void bind() {
        tvName = findViewById(R.id.tvSettingsName);
        tvEmail = findViewById(R.id.tvSettingsEmail);
        tvUid = findViewById(R.id.tvSettingsUid);
        tvBio = findViewById(R.id.tvSettingsBio);
        tvRecipes = findViewById(R.id.tvSettingsRecipes);
        tvFollowers = findViewById(R.id.tvSettingsFollowers);
        tvFollowing = findViewById(R.id.tvSettingsFollowing);
        btnRefresh = findViewById(R.id.btnSettingsRefresh);
        btnLogout = findViewById(R.id.btnSettingsLogout);

        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        if (btnRefresh != null) btnRefresh.setOnClickListener(v -> loadAccount());
        if (btnLogout != null) btnLogout.setOnClickListener(v -> confirmLogout());

        MaterialButton btnChangePwd = findViewById(R.id.btnSettingsChangePassword);
        if (btnChangePwd != null) btnChangePwd.setOnClickListener(v -> cambiarContrasena());

        MaterialButton btnClearDrafts = findViewById(R.id.btnSettingsClearDrafts);
        if (btnClearDrafts != null) btnClearDrafts.setOnClickListener(v -> borrarBorradoresChat());

        MaterialButton btnDelete = findViewById(R.id.btnSettingsDeleteAccount);
        if (btnDelete != null) btnDelete.setOnClickListener(v -> confirmarEliminarCuenta());
    }

    private void cambiarContrasena() {
        FirebaseUser user = SessionManager.currentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "No hay sesión activa", Toast.LENGTH_SHORT).show();
            return;
        }
        String email = user.getEmail();
        new AlertDialog.Builder(this)
                .setTitle("Cambiar contraseña")
                .setMessage("Te enviaremos un correo a " + email
                        + " con un enlace para cambiar tu contraseña.")
                .setPositiveButton("Enviar correo", (d, w) ->
                        com.google.firebase.auth.FirebaseAuth.getInstance()
                                .sendPasswordResetEmail(email)
                                .addOnSuccessListener(v -> Toast.makeText(this,
                                        "Correo de cambio de contraseña enviado",
                                        Toast.LENGTH_LONG).show())
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "No se pudo enviar el correo",
                                        Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void borrarBorradoresChat() {
        getSharedPreferences("chat_drafts", MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, "Borradores de chat eliminados",
                Toast.LENGTH_SHORT).show();
    }

    private void confirmarEliminarCuenta() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("Esta acción cerrará tu sesión y eliminará tu cuenta "
                        + "de Firebase. Tus recetas y mensajes se mantendrán hasta "
                        + "que decidas borrarlos manualmente desde otra cuenta.\n\n"
                        + "Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar definitivamente", (d, w) -> eliminarCuenta())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCuenta() {
        FirebaseUser user = SessionManager.currentUser();
        if (user == null) {
            goLogin();
            return;
        }
        user.delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_SHORT).show();
                    SessionManager.signOutCompat(this);
                    goLogin();
                })
                .addOnFailureListener(e -> {
                    // Firebase requiere reautenticación reciente para eliminar
                    Toast.makeText(this,
                            "Por seguridad, vuelve a iniciar sesión e intenta borrar de nuevo",
                            Toast.LENGTH_LONG).show();
                    SessionManager.signOutCompat(this);
                    goLogin();
                });
    }

    private void loadAccount() {
        FirebaseUser user = SessionManager.currentUser();
        if (user == null) {
            goLogin();
            return;
        }

        String fallbackName = user.getDisplayName();
        if (fallbackName == null || fallbackName.trim().isEmpty()) {
            String email = user.getEmail();
            fallbackName = email != null && email.contains("@")
                    ? email.substring(0, email.indexOf("@"))
                    : "Chef";
        }

        if (tvName != null) tvName.setText(fallbackName);
        if (tvEmail != null) tvEmail.setText(user.getEmail() == null ? "" : user.getEmail());
        if (tvUid != null) tvUid.setText(user.getUid());

        SessionManager.loadUserDoc(user.getUid(),
                doc -> {
                    if (!doc.exists()) return;
                    String name = doc.getString("name");
                    String bio = doc.getString("bio");
                    Long recipes = doc.getLong("recipes");
                    Long followers = doc.getLong("followers");
                    Long following = doc.getLong("following");

                    if (name != null && !name.trim().isEmpty() && tvName != null) {
                        tvName.setText(name.trim());
                    }
                    if (tvBio != null) {
                        tvBio.setText(bio == null || bio.trim().isEmpty()
                                ? "Sin biografia"
                                : bio.trim());
                    }
                    if (tvRecipes != null) tvRecipes.setText(String.valueOf(recipes == null ? 0 : recipes));
                    if (tvFollowers != null) tvFollowers.setText(String.valueOf(followers == null ? 0 : followers));
                    if (tvFollowing != null) tvFollowing.setText(String.valueOf(following == null ? 0 : following));
                },
                e -> Toast.makeText(this, "No se pudo cargar la cuenta", Toast.LENGTH_SHORT).show());
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesion")
                .setMessage("Se cerrara tu sesion en este dispositivo.")
                .setPositiveButton("Cerrar sesion", (d, w) -> {
                    SessionManager.signOutCompat(this);
                    goLogin();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void goLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
