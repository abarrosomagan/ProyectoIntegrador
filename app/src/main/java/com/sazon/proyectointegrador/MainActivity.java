package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sazon.proyectointegrador.adapters.PublicacionAdapter;
import com.sazon.proyectointegrador.model.Publicacion;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Pantallas
    private LinearLayout pantallaInicio;
    private LinearLayout pantallaMensajes;
    private LinearLayout pantallaPerfil;

    // Feed
    private RecyclerView rvFeed;
    private PublicacionAdapter adapter;

    // (Incluido en include_feed)
    private ImageButton btnMenuFeed;

    // (Preparado para Firebase)
    // private FirebaseAuth firebaseAuth;
    // private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bindViews();
        // initFirebase(); // <- activar cuando metas dependencias + google-services.json

        setupBottomNav();
        setupFeed();
        setupFeedMenu();
    }

    private void bindViews() {
        // IDs de activity_main.xml
        pantallaInicio = findViewById(R.id.pantallaInicio);
        pantallaMensajes = findViewById(R.id.pantallaMensajes);
        pantallaPerfil = findViewById(R.id.pantallaPerfil);

        // IDs que vienen del include_feed (deben existir en include_feed.xml)
        rvFeed = findViewById(R.id.rvFeed);
        btnMenuFeed = findViewById(R.id.btnMenuFeed);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setSelectedItemId(R.id.nav_feed);
        mostrarPantalla(R.id.nav_feed);

        bottomNav.setOnItemSelectedListener(item -> {
            mostrarPantalla(item.getItemId());
            return true;
        });
    }

    private void setupFeed() {
        if (rvFeed == null) {
            // Si esto pasa, es porque el id rvFeed no está en include_feed.xml
            Toast.makeText(this, "rvFeed no encontrado. Revisa include_feed.xml", Toast.LENGTH_SHORT).show();
            return;
        }

        rvFeed.setLayoutManager(new LinearLayoutManager(this));

        // MODO ACTUAL: Mock
        List<Publicacion> lista = crearMockPublicaciones();

        adapter = new PublicacionAdapter(new ArrayList<>(lista), publicacion ->
                Toast.makeText(MainActivity.this,
                        "Detalle pendiente: " + publicacion.getTitulo(),
                        Toast.LENGTH_SHORT).show()
        );

        rvFeed.setAdapter(adapter);

        // FUTURO: Firebase (cuando lo actives)
        // loadFeedFromFirestore();
    }

    private void setupFeedMenu() {
        if (btnMenuFeed == null) {
            // Si esto pasa, es porque btnMenuFeed no está en include_feed.xml
            return;
        }

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
                    doLogout();
                    return true;
                }

                return false;
            });

            popupMenu.show();
        });
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

    private void doLogout() {
        // MODO ACTUAL: simplemente volvemos a Login y limpiamos la pila.
        // (No cambia tu flujo)
        //
        // FUTURO: Firebase
        // if (firebaseAuth != null) firebaseAuth.signOut();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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

    // =========================
    // ========== Firebase ======
    // =========================

    /*
    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void loadFeedFromFirestore() {
        // Ejemplo (a futuro):
        // firestore.collection("posts")
        //     .orderBy("createdAt", Query.Direction.DESCENDING)
        //     .limit(50)
        //     .get()
        //     .addOnSuccessListener(snapshot -> {
        //         ArrayList<Publicacion> lista = new ArrayList<>();
        //         for (DocumentSnapshot doc : snapshot.getDocuments()) {
        //             // Mapear doc -> Publicacion (según tu modelo)
        //         }
        //         adapter.updateData(lista); // necesitarías método en el adapter
        //     })
        //     .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    */
}