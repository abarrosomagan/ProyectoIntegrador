package com.sazon.proyectointegrador;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (feedController != null) feedController.onDestroy();
        if (chatsController != null) chatsController.onDestroy();
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