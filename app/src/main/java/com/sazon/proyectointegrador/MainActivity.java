package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.ui.chats.ChatsController;
import com.sazon.proyectointegrador.ui.feed.FeedController;
import com.sazon.proyectointegrador.ui.profile.ProfileController;

public class MainActivity extends AppCompatActivity {

    private LinearLayout pantallaInicio, pantallaMensajes, pantallaPerfil;

    private FeedController feedController;
    private ChatsController chatsController;
    private ProfileController profileController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (getIntent() != null && getIntent().getBooleanExtra("BLOCK_MAIN", false)) {
            finish();
            return;
        }

        pantallaInicio = findViewById(R.id.pantallaInicio);
        pantallaMensajes = findViewById(R.id.pantallaMensajes);
        pantallaPerfil = findViewById(R.id.pantallaPerfil);

        feedController = new FeedController(this);
        chatsController = new ChatsController(this);
        profileController = new ProfileController(this);

        feedController.init();
        chatsController.init();
        profileController.init();

        setupBottomNav();
        setupFab();
        avisoEmailNoVerificado();
    }

    /**
     * Si el usuario tiene cuenta pero el email no está verificado, se le ofrece
     * reenviar el correo de verificación. Una vez por sesión.
     */
    private boolean emailWarningShown = false;
    private void avisoEmailNoVerificado() {
        if (emailWarningShown) return;
        com.google.firebase.auth.FirebaseUser user =
                com.sazon.proyectointegrador.util.SessionManager.currentUser();
        if (user == null || user.isEmailVerified()) return;
        emailWarningShown = true;
        com.google.android.material.snackbar.Snackbar
                .make(findViewById(android.R.id.content),
                        "Verifica tu correo para asegurar tu cuenta",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setAction("Reenviar", v ->
                        user.sendEmailVerification()
                                .addOnSuccessListener(x -> android.widget.Toast.makeText(this,
                                        "Correo de verificación enviado",
                                        android.widget.Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> android.widget.Toast.makeText(this,
                                        "No se pudo enviar ahora",
                                        android.widget.Toast.LENGTH_SHORT).show()))
                .show();
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabCreateRecipe);
        if (fab != null) {
            fab.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateRecipeActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescar perfil y feed cuando volvemos de Crear receta o Perfil ajeno
        if (feedController != null) feedController.refresh();
        if (profileController != null) profileController.refresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (feedController != null) feedController.onDestroy();
        if (chatsController != null) chatsController.onDestroy();
        if (profileController != null) profileController.onDestroy();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_feed);
        showScreen(R.id.nav_feed);

        bottomNav.setOnItemSelectedListener(item -> {
            showScreen(item.getItemId());
            return true;
        });
    }

    private void showScreen(int id) {
        pantallaInicio.setVisibility(View.GONE);
        pantallaMensajes.setVisibility(View.GONE);
        pantallaPerfil.setVisibility(View.GONE);

        if (id == R.id.nav_feed) pantallaInicio.setVisibility(View.VISIBLE);
        else if (id == R.id.nav_chats) pantallaMensajes.setVisibility(View.VISIBLE);
        else if (id == R.id.nav_profile) pantallaPerfil.setVisibility(View.VISIBLE);
    }
}
