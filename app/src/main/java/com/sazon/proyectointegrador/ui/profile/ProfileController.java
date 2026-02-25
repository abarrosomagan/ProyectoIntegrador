package com.sazon.proyectointegrador.ui.profile;

import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sazon.proyectointegrador.LoginActivity;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.adapters.PublicacionAdapter;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ProfileController {

    private final AppCompatActivity a;

    private TextView tvAvatar, tvUsername, tvBio;
    private TextView tvStatRecipes, tvStatFollowers, tvStatFollowing;

    private MaterialButton btnEdit, btnTabMy, btnTabSaved;
    private ImageButton btnMore;

    private RecyclerView rv;
    private PublicacionAdapter adapter;

    private View emptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private MaterialButton btnEmptyAction;

    private final ArrayList<Publicacion> my = new ArrayList<>();
    private final ArrayList<Publicacion> saved = new ArrayList<>();

    public ProfileController(AppCompatActivity activity) {
        this.a = activity;
    }

    public void init() {
        bind();
        setupRecycler();
        loadMock();
        renderHeaderMock();
        setupListeners();

        ImageButton btnBack = a.findViewById(R.id.btnBackProfile);
        if (btnBack != null) btnBack.setVisibility(View.GONE);

        paintTabs(true);
        showMy();
    }

    private void bind() {
        tvAvatar = a.findViewById(R.id.tvAvatar);
        tvUsername = a.findViewById(R.id.tvUsername);
        tvBio = a.findViewById(R.id.tvBio);

        tvStatRecipes = a.findViewById(R.id.tvStatRecipes);
        tvStatFollowers = a.findViewById(R.id.tvStatFollowers);
        tvStatFollowing = a.findViewById(R.id.tvStatFollowing);

        btnEdit = a.findViewById(R.id.btnEditProfile);
        btnTabMy = a.findViewById(R.id.btnTabMyRecipes);
        btnTabSaved = a.findViewById(R.id.btnTabSaved);

        rv = a.findViewById(R.id.rvProfileList);
        btnMore = a.findViewById(R.id.btnMoreProfile);

        emptyState = a.findViewById(R.id.emptyStateProfile);
        tvEmptyTitle = a.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = a.findViewById(R.id.tvEmptySubtitle);
        btnEmptyAction = a.findViewById(R.id.btnEmptyAction);
    }

    private void setupRecycler() {
        if (rv == null) return;
        rv.setLayoutManager(new LinearLayoutManager(a));

        adapter = new PublicacionAdapter(
                new ArrayList<>(),
                p -> Toast.makeText(a, "Detalle pendiente: " + p.getTitulo(), Toast.LENGTH_SHORT).show(),
                p -> Toast.makeText(a, "Autor: " + p.getAutor(), Toast.LENGTH_SHORT).show()
        );
        rv.setAdapter(adapter);
    }

    private void setupListeners() {
        if (btnTabMy != null) btnTabMy.setOnClickListener(v -> { paintTabs(true); showMy(); });
        if (btnTabSaved != null) btnTabSaved.setOnClickListener(v -> { paintTabs(false); showSaved(); });

        if (btnEdit != null) btnEdit.setOnClickListener(v ->
                Toast.makeText(a, "Editar perfil (pendiente)", Toast.LENGTH_SHORT).show()
        );

        if (btnMore != null) btnMore.setOnClickListener(this::showMoreMenu);

        if (tvStatFollowers != null) tvStatFollowers.setOnClickListener(v ->
                Toast.makeText(a, "Seguidores (pendiente)", Toast.LENGTH_SHORT).show()
        );
        if (tvStatFollowing != null) tvStatFollowing.setOnClickListener(v ->
                Toast.makeText(a, "Siguiendo (pendiente)", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadMock() {
        long ahora = System.currentTimeMillis();

        my.clear();
        my.add(new Publicacion("p1","user_me","Fernando",
                ahora-(2*60*60*1000L),
                "Tortilla de patatas","Receta clásica y jugosa.","",120,false));

        my.add(new Publicacion("p2","user_me","Fernando",
                ahora-(24*60*60*1000L),
                "Pollo al curry suave","Curry suave para todos.","",85,false));

        saved.clear();
        saved.add(new Publicacion("s1","user_maria","María",
                ahora-(2*60*60*1000L),
                "Pasta cremosa con setas","Perfecta para cenas rápidas.","",89,true));

        if (tvStatRecipes != null) tvStatRecipes.setText(String.valueOf(my.size()));
        if (tvStatFollowers != null) tvStatFollowers.setText("12");
        if (tvStatFollowing != null) tvStatFollowing.setText("8");
    }

    private void renderHeaderMock() {
        String name = "Fernando";
        if (tvUsername != null) tvUsername.setText(name);
        if (tvBio != null) tvBio.setText("Amante de la cocina casera 🍳");
        if (tvAvatar != null) tvAvatar.setText(name.substring(0,1).toUpperCase());
    }

    private void showMy() {
        if (adapter != null) adapter.updateData(new ArrayList<>(my));
        updateEmpty(my, true);
    }

    private void showSaved() {
        if (adapter != null) adapter.updateData(new ArrayList<>(saved));
        updateEmpty(saved, false);
    }

    private void paintTabs(boolean myTab) {
        if (btnTabMy == null || btnTabSaved == null) return;

        if (myTab) {
            btnTabMy.setTextColor(a.getResources().getColor(R.color.texto_sobre_principal));
            btnTabMy.setBackgroundTintList(a.getColorStateList(R.color.color_principal_variante));

            btnTabSaved.setTextColor(a.getResources().getColor(R.color.texto_principal));
            btnTabSaved.setBackgroundTintList(a.getColorStateList(R.color.fondo_superficie));
        } else {
            btnTabSaved.setTextColor(a.getResources().getColor(R.color.texto_sobre_principal));
            btnTabSaved.setBackgroundTintList(a.getColorStateList(R.color.color_principal_variante));

            btnTabMy.setTextColor(a.getResources().getColor(R.color.texto_principal));
            btnTabMy.setBackgroundTintList(a.getColorStateList(R.color.fondo_superficie));
        }
    }

    private void updateEmpty(List<Publicacion> data, boolean myTab) {
        if (emptyState == null || rv == null) return;

        boolean empty = data == null || data.isEmpty();

        if (!empty) {
            emptyState.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            if (btnEmptyAction != null) btnEmptyAction.setVisibility(View.GONE);
            return;
        }

        rv.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);

        if (tvEmptyTitle != null) tvEmptyTitle.setText(myTab ? "Aún no has publicado recetas" : "No tienes recetas guardadas");
        if (tvEmptySubtitle != null) tvEmptySubtitle.setText(myTab ? "Cuando publiques, aparecerán aquí." : "Guarda recetas para verlas aquí.");

        if (btnEmptyAction != null) {
            if (myTab) {
                btnEmptyAction.setVisibility(View.VISIBLE);
                btnEmptyAction.setText("Crear receta");
                btnEmptyAction.setOnClickListener(v ->
                        Toast.makeText(a, "Ir a crear receta (pendiente)", Toast.LENGTH_SHORT).show()
                );
            } else {
                btnEmptyAction.setVisibility(View.GONE);
            }
        }
    }

    private void showMoreMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(a, anchor);
        menu.getMenu().add(0, 1, 0, "Ajustes");
        menu.getMenu().add(0, 2, 1, "Acerca de");
        menu.getMenu().add(0, 3, 2, "Cerrar sesión");

        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == 1) {
                Toast.makeText(a, "Ajustes (pendiente)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == 2) {
                Toast.makeText(a, "Recetas Social v1 (pendiente)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == 3) {
                confirmLogout();
                return true;
            }

            return false;
        });

        menu.show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(a)
                .setPositiveButton("Salir", (d, w) -> {
                    new SessionManager(a).logout();

                    Intent i = new Intent(a, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    a.startActivity(i);

                    // IMPORTANTE: no finishAffinity() aquí
                    a.finish();
                })
                .show();
    }
}