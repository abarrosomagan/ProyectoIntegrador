package com.sazon.proyectointegrador.ui.feed;

import android.content.Intent;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sazon.proyectointegrador.LoginActivity;
import com.sazon.proyectointegrador.ProfileActivity;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.adapters.PublicacionAdapter;
import com.sazon.proyectointegrador.R;

import java.util.ArrayList;

public class FeedController {

    private final AppCompatActivity a;

    private RecyclerView rvFeed;
    private PublicacionAdapter feedAdapter;
    private ImageButton btnMenuFeed;

    public FeedController(AppCompatActivity activity) {
        this.a = activity;
    }

    public void init() {
        bind();
        setupRecycler();
        setupMenu();
    }

    private void bind() {
        rvFeed = a.findViewById(R.id.rvFeed);
    }

    private void setupRecycler() {
        if (rvFeed == null) {
            Toast.makeText(a, "rvFeed no encontrado. Revisa include_feed.xml", Toast.LENGTH_SHORT).show();
            return;
        }

        rvFeed.setLayoutManager(new LinearLayoutManager(a));

        ArrayList<Publicacion> lista = crearMockPublicaciones();

        feedAdapter = new PublicacionAdapter(
                new ArrayList<>(lista),
                publicacion -> Toast.makeText(a, "Detalle pendiente: " + publicacion.getTitulo(), Toast.LENGTH_SHORT).show(),
                publicacion -> {
                    Intent i = new Intent(a, ProfileActivity.class);
                    i.putExtra(ProfileActivity.EXTRA_IS_OWN_PROFILE, false);
                    i.putExtra(ProfileActivity.EXTRA_USER_ID, publicacion.getAuthorId());
                    i.putExtra(ProfileActivity.EXTRA_USERNAME, publicacion.getAutor());
                    i.putExtra(ProfileActivity.EXTRA_BIO, "Bio del usuario (pendiente)");
                    i.putExtra(ProfileActivity.EXTRA_AVATAR_LETTER, publicacion.getAutor().substring(0, 1));
                    a.startActivity(i);
                }
        );

        rvFeed.setAdapter(feedAdapter);
    }

    private void setupMenu() {
        if (btnMenuFeed == null) return;

        btnMenuFeed.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(a, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_feed, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();

                if (id == R.id.action_settings) {
                    Toast.makeText(a, "Ajustes (pendiente)", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (id == R.id.action_logout) {
                    // si prefieres, lo podemos “inyectar” desde MainActivity
                    Intent intent = new Intent(a, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    a.startActivity(intent);
                    a.finish();
                    return true;
                }

                return false;
            });

            popupMenu.show();
        });
    }

    private ArrayList<Publicacion> crearMockPublicaciones() {
        ArrayList<Publicacion> lista = new ArrayList<>();
        long ahora = System.currentTimeMillis();

        lista.add(new Publicacion("1", "user1", "JuanCarlos",
                ahora - (2 * 60 * 60 * 1000L),
                "Tortilla jugosa",
                "Receta tradicional con cebolla.",
                "",
                120,
                false));

        lista.add(new Publicacion("2", "user2", "María",
                ahora - (5 * 60 * 60 * 1000L),
                "Pasta cremosa con setas",
                "Ideal para cenas rápidas.",
                "",
                89,
                true));

        lista.add(new Publicacion("3", "user3", "Alex",
                ahora - (24 * 60 * 60 * 1000L),
                "Pollo al horno con patatas",
                "Crujiente por fuera y jugoso por dentro.",
                "",
                230,
                false));

        lista.add(new Publicacion("4", "user4", "Sofía",
                ahora - (24 * 60 * 60 * 1000L),
                "Ensalada fresca de verano",
                "Ligera y saludable.",
                "",
                45,
                false));

        lista.add(new Publicacion("5", "user5", "Dani",
                ahora - (3L * 24 * 60 * 60 * 1000L),
                "Croquetas caseras",
                "Receta clásica.",
                "",
                310,
                true));

        return lista;
    }
}