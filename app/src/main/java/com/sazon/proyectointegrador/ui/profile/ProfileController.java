package com.sazon.proyectointegrador.ui.profile;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.sazon.proyectointegrador.LoginActivity;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.adapters.PublicacionGridAdapter;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileController {

    private final AppCompatActivity a;

    private TextView tvAvatar, tvUsername, tvUserHandle, tvBio, tvChefRank;
    private TextView tvStatRecipes, tvStatFollowers, tvStatFollowing;

    private MaterialButton btnEdit, btnShare, btnTabMy, btnTabSaved;
    private ImageButton btnMore;

    private RecyclerView rv;
    private PublicacionGridAdapter adapter;

    private View emptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private MaterialButton btnEmptyAction;

    private final ArrayList<Publicacion> my = new ArrayList<>();
    private final ArrayList<Publicacion> saved = new ArrayList<>();

    // Caché del perfil actual (lo que se ve en la UI)
    private String nombreActual = "";
    private String bioActual = "";

    public ProfileController(AppCompatActivity activity) {
        this.a = activity;
    }

    public void init() {
        bind();
        setupRecycler();
        loadMock();
        setupListeners();
        setupCollapsibleHeader();

        ImageButton btnBack = a.findViewById(R.id.btnBackProfile);
        if (btnBack != null) btnBack.setVisibility(View.GONE);

        paintTabs(true);
        showMy();

        cargarPerfilDesdeFirestore();
    }

    private void bind() {
        tvAvatar = a.findViewById(R.id.tvAvatar);
        tvUsername = a.findViewById(R.id.tvUsername);
        tvUserHandle = a.findViewById(R.id.tvUserHandle);
        tvBio = a.findViewById(R.id.tvBio);
        tvChefRank = a.findViewById(R.id.tvChefRank);

        tvStatRecipes = a.findViewById(R.id.tvStatRecipes);
        tvStatFollowers = a.findViewById(R.id.tvStatFollowers);
        tvStatFollowing = a.findViewById(R.id.tvStatFollowing);

        btnEdit = a.findViewById(R.id.btnEditProfile);
        btnShare = a.findViewById(R.id.btnShareProfile);
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
        rv.setLayoutManager(new GridLayoutManager(a, 3));

        adapter = new PublicacionGridAdapter(
                new ArrayList<>(),
                p -> Toast.makeText(a, "Detalle pendiente: " + p.getTitulo(), Toast.LENGTH_SHORT).show()
        );
        rv.setAdapter(adapter);
    }

    /**
     * Permite arrastrar el AppBarLayout directamente para colapsar/expandir el
     * header del perfil, aunque la lista esté vacía o no tenga scroll real.
     * Sin esto el drag solo funciona cuando el hijo scrollable puede moverse.
     */
    private void setupCollapsibleHeader() {
        AppBarLayout appBar = a.findViewById(R.id.profileAppBar);
        if (appBar == null) return;
        if (!(appBar.getLayoutParams() instanceof CoordinatorLayout.LayoutParams)) return;

        CoordinatorLayout.LayoutParams lp =
                (CoordinatorLayout.LayoutParams) appBar.getLayoutParams();

        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) lp.getBehavior();
        if (behavior == null) {
            behavior = new AppBarLayout.Behavior();
            lp.setBehavior(behavior);
        }

        behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
            @Override
            public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                return true;
            }
        });
    }

    private void setupListeners() {
        if (btnTabMy != null) btnTabMy.setOnClickListener(v -> { paintTabs(true); showMy(); });
        if (btnTabSaved != null) btnTabSaved.setOnClickListener(v -> { paintTabs(false); showSaved(); });

        if (btnEdit != null) btnEdit.setOnClickListener(v -> mostrarDialogoEditarPerfil());

        if (btnShare != null) btnShare.setOnClickListener(v ->
                Toast.makeText(a, "Compartir perfil (pendiente)", Toast.LENGTH_SHORT).show());

        if (btnMore != null) btnMore.setOnClickListener(this::showMoreMenu);

        if (tvStatFollowers != null) tvStatFollowers.setOnClickListener(v ->
                Toast.makeText(a, "Seguidores (pendiente)", Toast.LENGTH_SHORT).show()
        );
        if (tvStatFollowing != null) tvStatFollowing.setOnClickListener(v ->
                Toast.makeText(a, "Siguiendo (pendiente)", Toast.LENGTH_SHORT).show()
        );
    }

    // ===== Carga real desde Firestore =====

    private void cargarPerfilDesdeFirestore() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;

        SessionManager.loadUserDoc(uid,
                doc -> {
                    String name;
                    String bio;

                    if (doc.exists()) {
                        name = doc.getString("name");
                        bio  = doc.getString("bio");
                    } else {
                        // Usuario en Auth sin doc en Firestore (cuentas antiguas).
                        // Lo creamos al vuelo con lo que tengamos.
                        FirebaseUser u = SessionManager.currentUser();
                        name = u != null ? u.getDisplayName() : "";
                        String email = u != null ? u.getEmail() : "";
                        if (name == null) name = "";
                        if (email == null) email = "";
                        SessionManager.createUserDoc(uid, name, email, x -> {}, e -> {});
                        bio = "";
                    }

                    if (name == null || name.isEmpty()) {
                        FirebaseUser u = SessionManager.currentUser();
                        name = u != null && u.getDisplayName() != null ? u.getDisplayName() : "";
                    }
                    if (name == null) name = "";
                    if (bio == null) bio = "";

                    nombreActual = name;
                    bioActual = bio;

                    pintarHeader(name, bio);
                },
                e -> {
                    // Si Firestore falla, al menos pintamos lo que sabemos de Auth
                    FirebaseUser u = SessionManager.currentUser();
                    String fallback = u != null && u.getDisplayName() != null ? u.getDisplayName() : "Usuario";
                    nombreActual = fallback;
                    pintarHeader(fallback, "");
                });
    }

    private void pintarHeader(String name, String bio) {
        if (tvUsername != null) tvUsername.setText(name.isEmpty() ? "Sin nombre" : name);
        if (tvBio != null) tvBio.setText(
                bio == null || bio.isEmpty() ? "Aún no has escrito una biografía." : bio);
        if (tvAvatar != null && !name.isEmpty()) {
            tvAvatar.setText(String.valueOf(Character.toUpperCase(name.charAt(0))));
        }
        if (tvUserHandle != null) tvUserHandle.setText("@" + buildHandle(name));
        if (tvChefRank != null) tvChefRank.setText(chefRankFor(my.size()));
    }

    /** Genera un handle simple a partir del nombre o del email. */
    private String buildHandle(String name) {
        String email = SessionManager.currentEmail();
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@")).toLowerCase();
        }
        if (name == null || name.isEmpty()) return "chef";
        return name.toLowerCase().replaceAll("\\s+", "");
    }

    /** Rango de chef en función del número de recetas publicadas. */
    private static String chefRankFor(int recetas) {
        if (recetas <= 0)  return "🥄 Aprendiz de cocina";
        if (recetas < 4)   return "🍳 Cocinero novato";
        if (recetas < 11)  return "🍲 Cocinero";
        if (recetas < 26)  return "👨‍🍳 Chef de cocina";
        return "⭐ Chef estrella";
    }

    // ===== Editar perfil real =====

    private void mostrarDialogoEditarPerfil() {
        View view = LayoutInflater.from(a).inflate(R.layout.dialog_editar_perfil, null, false);
        TextInputEditText etNombre = view.findViewById(R.id.etEditNombre);
        TextInputEditText etBio    = view.findViewById(R.id.etEditBio);

        etNombre.setText(nombreActual);
        etBio.setText(bioActual);

        new AlertDialog.Builder(a)
                .setTitle("Editar perfil")
                .setView(view)
                .setPositiveButton("Guardar", (d, w) -> {
                    String nuevoNombre = etNombre.getText() != null
                            ? etNombre.getText().toString().trim() : "";
                    String nuevaBio = etBio.getText() != null
                            ? etBio.getText().toString().trim() : "";
                    guardarCambiosPerfil(nuevoNombre, nuevaBio);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarCambiosPerfil(String nuevoNombre, String nuevaBio) {
        String uid = SessionManager.currentUid();
        if (uid == null) return;

        if (nuevoNombre.isEmpty()) {
            Toast.makeText(a, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", nuevoNombre);
        updates.put("bio", nuevaBio);

        SessionManager.updateUserDoc(uid, updates,
                unused -> {
                    nombreActual = nuevoNombre;
                    bioActual = nuevaBio;
                    pintarHeader(nuevoNombre, nuevaBio);

                    // Mantener displayName de Auth en sintonía
                    FirebaseUser user = SessionManager.currentUser();
                    if (user != null) {
                        user.updateProfile(new UserProfileChangeRequest.Builder()
                                .setDisplayName(nuevoNombre)
                                .build());
                    }

                    // Y el cache local para que el feed muestre el nuevo nombre
                    new SessionManager(a).login(uid, nuevoNombre);

                    Toast.makeText(a, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(a, "No se pudo guardar", Toast.LENGTH_SHORT).show());
    }

    // ===== Datos mock (recetas/guardadas — pendiente Firestore) =====

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
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres salir?")
                .setPositiveButton("Salir", (d, w) -> {
                    SessionManager.signOutCompat(a);

                    Intent i = new Intent(a, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    a.startActivity(i);
                    a.finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
