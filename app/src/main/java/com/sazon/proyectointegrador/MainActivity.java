package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sazon.proyectointegrador.adapters.PublicacionAdapter;
import com.sazon.proyectointegrador.model.Publicacion;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private LinearLayout pantallaInicio;
    private LinearLayout pantallaMensajes;
    private LinearLayout pantallaPerfil;

    private RecyclerView rvFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Pantallas
        pantallaInicio = findViewById(R.id.pantallaInicio);
        pantallaMensajes = findViewById(R.id.pantallaMensajes);
        pantallaPerfil = findViewById(R.id.pantallaPerfil);

        // BottomNav
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_feed);
        mostrarPantalla(R.id.nav_feed);

        bottomNav.setOnItemSelectedListener(item -> {
            mostrarPantalla(item.getItemId());
            return true;
        });

        // Menú cabecera feed
        ImageButton btnMenuFeed = findViewById(R.id.btnMenuFeed);
        btnMenuFeed.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_feed, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();

                if (id == R.id.action_settings) {
                    Toast.makeText(MainActivity.this, "Ajustes (pendiente)", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (id == R.id.action_logout) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return true;
                }

                return false;
            });

            popupMenu.show();
        });

        // RecyclerView feed
        rvFeed = findViewById(R.id.rvFeed);
        rvFeed.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<Publicacion> lista = crearMockPublicaciones();

        PublicacionAdapter adapter = new PublicacionAdapter(lista, publicacion ->
                Toast.makeText(MainActivity.this, "Detalle pendiente: " + publicacion.getTitulo(), Toast.LENGTH_SHORT).show()
        );

        rvFeed.setAdapter(adapter);
    }

    private void mostrarPantalla(int itemId) {
        pantallaInicio.setVisibility(View.GONE);
        pantallaMensajes.setVisibility(View.GONE);
        pantallaPerfil.setVisibility(View.GONE);

        if (itemId == R.id.nav_feed) {
            pantallaInicio.setVisibility(View.VISIBLE);
        } else if (itemId == R.id.nav_chats) {
            pantallaMensajes.setVisibility(View.VISIBLE);
        } else if (itemId == R.id.nav_profile) {
            pantallaPerfil.setVisibility(View.VISIBLE);
        }
    }

    private ArrayList<Publicacion> crearMockPublicaciones() {
        ArrayList<Publicacion> lista = new ArrayList<>();
        lista.add(new Publicacion("JuanCarlos", "hace 2 h", "Tortilla jugosa", 120, false));
        lista.add(new Publicacion("María", "hace 5 h", "Pasta cremosa con setas", 89, true));
        lista.add(new Publicacion("Alex", "ayer", "Pollo al horno con patatas", 230, false));
        lista.add(new Publicacion("Sofía", "ayer", "Ensalada fresca de verano", 45, false));
        lista.add(new Publicacion("Dani", "hace 3 días", "Croquetas caseras", 310, true));
        return lista;
    }
}