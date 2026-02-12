package com.example.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // Duración del splash (2 segundos)
    private static final long DURACION_SPLASH_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // IMPORTANTÍSIMO: aplicar tema normal antes de inflar la vista
        setTheme(R.style.Base_Theme_ProyectoIntegrador);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, 2000);
    }
}