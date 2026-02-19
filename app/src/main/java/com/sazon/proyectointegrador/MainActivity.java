package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ImageButton btnMenuFeed = findViewById(R.id.btnMenuFeed);

        btnMenuFeed.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_feed, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.action_settings) {
                    Toast.makeText(MainActivity.this, "Ajustes (pendiente)", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (id == R.id.action_logout) {
                    // Si existe LoginActivity, navega y limpia el backstack
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return true;
                }

                return false;
            });

            popupMenu.show();
        });

        // Nota: quitamos el listener de insets para no meter padding raro al contenedor
        // (si luego quieres edge-to-edge perfecto, lo ajustamos con calma)
    }
}