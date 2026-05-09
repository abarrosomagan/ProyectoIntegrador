package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.sazon.proyectointegrador.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long DURACION_SPLASH_MS = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, DURACION_SPLASH_MS);
    }

    private void routeNext() {
        // Si Firebase tiene sesión persistida, vamos directos al main.
        // Si no, al login. La sesión la persiste el propio SDK entre arranques.
        Class<?> target = SessionManager.isAuthenticated()
                ? MainActivity.class
                : LoginActivity.class;

        Intent i = new Intent(SplashActivity.this, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
