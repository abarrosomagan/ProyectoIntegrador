package com.sazon.proyectointegrador.ui.profile;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
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
import com.sazon.proyectointegrador.util.AvatarHelper;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileController {

    private final AppCompatActivity a;

    private TextView tvAvatar, tvUsername, tvUserHandle, tvBio, tvChefRank;
    private ImageView ivAvatar;
    private View cardProfileHeader;
    private TextView tvStatRecipes, tvStatFollowers, tvStatFollowing;

    private MaterialButton btnEdit, btnShare, btnFollow, btnTabMy, btnTabSaved;
    private ImageButton btnMore;

    private RecyclerView rv;
    private PublicacionGridAdapter adapter;

    private View emptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private MaterialButton btnEmptyAction;
    private SwipeRefreshLayout swipeProfile;

    private final ArrayList<Publicacion> my = new ArrayList<>();
    private final ArrayList<Publicacion> saved = new ArrayList<>();
    private final Set<String> savedIds = new HashSet<>();

    // Caché del perfil actual (lo que se ve en la UI)
    private String nombreActual = "";
    private String bioActual = "";
    private boolean showingMyTab = true;

    private AvatarHelper avatarHelper;

    public ProfileController(AppCompatActivity activity) {
        this.a = activity;
    }

    public void init() {
        bind();
        setupRecycler();
        setupAvatarPicker();
        setupListeners();
        setupCollapsibleHeader();

        ImageButton btnBack = a.findViewById(R.id.btnBackProfile);
        if (btnBack != null) btnBack.setVisibility(View.GONE);
        // En mi propio perfil, el botón "Seguir" no tiene sentido.
        if (btnFollow != null) btnFollow.setVisibility(View.GONE);

        paintTabs(true);
        cargarPerfilDesdeFirestore();
        cargarRecetas();
    }

    /** Refresca recetas y datos del perfil (llamado desde MainActivity.onResume). */
    public void refresh() {
        cargarPerfilDesdeFirestore();
        cargarRecetas();
    }

    private void setupAvatarPicker() {
        avatarHelper = AvatarHelper.attach(a, url -> {
            if (url == null) return;
            renderAvatar(url, nombreActual);
            Toast.makeText(a, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
        });
    }

    private void bind() {
        tvAvatar = a.findViewById(R.id.tvAvatar);
        ivAvatar = a.findViewById(R.id.ivAvatar);
        cardProfileHeader = a.findViewById(R.id.cardProfileHeader);
        tvUsername = a.findViewById(R.id.tvUsername);
        tvUserHandle = a.findViewById(R.id.tvUserHandle);
        tvBio = a.findViewById(R.id.tvBio);
        tvChefRank = a.findViewById(R.id.tvChefRank);

        tvStatRecipes = a.findViewById(R.id.tvStatRecipes);
        tvStatFollowers = a.findViewById(R.id.tvStatFollowers);
        tvStatFollowing = a.findViewById(R.id.tvStatFollowing);

        btnEdit = a.findViewById(R.id.btnEditProfile);
        btnShare = a.findViewById(R.id.btnShareProfile);
        btnFollow = a.findViewById(R.id.btnFollow);
        btnTabMy = a.findViewById(R.id.btnTabMyRecipes);
        btnTabSaved = a.findViewById(R.id.btnTabSaved);

        rv = a.findViewById(R.id.rvProfileList);
        swipeProfile = a.findViewById(R.id.swipeProfile);
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
                this::abrirDetalleReceta);
        rv.setAdapter(adapter);

        if (swipeProfile != null) {
            swipeProfile.setColorSchemeResources(
                    R.color.color_principal_variante, R.color.color_principal);
            swipeProfile.setOnRefreshListener(() -> cargarRecetas());
        }
    }

    private void abrirDetalleReceta(Publicacion p) {
        if (p == null || p.getId() == null || p.getId().isEmpty()) {
            Toast.makeText(a, "Receta de demo (sin detalle)", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(a, com.sazon.proyectointegrador.RecipeDetailActivity.class);
        i.putExtra(com.sazon.proyectointegrador.RecipeDetailActivity.EXTRA_RECIPE_ID, p.getId());
        a.startActivity(i);
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

        if (btnShare != null) btnShare.setOnClickListener(v -> compartirPerfil());

        if (btnMore != null) btnMore.setOnClickListener(this::showMoreMenu);

        // Tap en el avatar → galería para cambiar la foto
        if (cardProfileHeader != null) {
            cardProfileHeader.setOnClickListener(v -> {
                if (avatarHelper != null) avatarHelper.launchPicker();
            });
        }

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
                    String avatarUrl = "";
                    long followers = 0;
                    long following = 0;

                    if (doc.exists()) {
                        name = doc.getString("name");
                        bio  = doc.getString("bio");
                        avatarUrl = doc.getString("avatarUrl");
                        Long f = doc.getLong("followers");
                        Long g = doc.getLong("following");
                        if (f != null) followers = f;
                        if (g != null) following = g;
                    } else {
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
                    if (avatarUrl == null) avatarUrl = "";

                    nombreActual = name;
                    bioActual = bio;

                    pintarHeader(name, bio);
                    renderAvatar(avatarUrl, name);
                    if (tvStatFollowers != null) tvStatFollowers.setText(String.valueOf(followers));
                    if (tvStatFollowing != null) tvStatFollowing.setText(String.valueOf(following));
                },
                e -> {
                    FirebaseUser u = SessionManager.currentUser();
                    String fallback = u != null && u.getDisplayName() != null ? u.getDisplayName() : "Usuario";
                    nombreActual = fallback;
                    pintarHeader(fallback, "");
                    renderAvatar("", fallback);
                });
    }

    /**
     * Carga las recetas reales del usuario desde Firestore. Si la base aún
     * está vacía (primera ejecución) cae a las recetas demo para que la
     * pestaña Perfil no parezca rota.
     */
    private void cargarRecetas() {
        String uid = SessionManager.currentUid();
        if (uid == null) {
            if (swipeProfile != null) swipeProfile.setRefreshing(false);
            return;
        }

        // 1) Set de IDs guardados para marcar el icono "estrella"
        RecipeRepository.savedIds(uid,
                ids -> {
                    savedIds.clear();
                    savedIds.addAll(ids);
                    // 2) Recetas propias
                    RecipeRepository.byAuthor(uid,
                            list -> {
                                my.clear();
                                if (list != null && !list.isEmpty()) {
                                    for (Publicacion p : list) {
                                        p.setGuardada(savedIds.contains(p.getId()));
                                        my.add(p);
                                    }
                                } else if (com.sazon.proyectointegrador.util.DemoData.ENABLED) {
                                    my.addAll(com.sazon.proyectointegrador.util.DemoData.recetasPropias());
                                }
                                if (tvStatRecipes != null)
                                    tvStatRecipes.setText(String.valueOf(my.size()));
                                if (tvChefRank != null)
                                    tvChefRank.setText(chefRankFor(my.size()));
                                if (showingMyTab) showMy();
                                if (swipeProfile != null) swipeProfile.setRefreshing(false);
                            },
                            e -> {
                                if (swipeProfile != null) swipeProfile.setRefreshing(false);
                            });

                    // 3) Recetas guardadas
                    RecipeRepository.savedBy(uid,
                            list -> {
                                saved.clear();
                                if (list != null && !list.isEmpty()) {
                                    saved.addAll(list);
                                } else if (com.sazon.proyectointegrador.util.DemoData.ENABLED) {
                                    saved.addAll(com.sazon.proyectointegrador.util.DemoData.recetasGuardadas());
                                }
                                if (!showingMyTab) showSaved();
                            },
                            e -> { /* idem */ });
                },
                e -> {
                    if (swipeProfile != null) swipeProfile.setRefreshing(false);
                });
    }

    private void renderAvatar(String avatarUrl, String name) {
        if (ivAvatar == null || tvAvatar == null) return;
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            ivAvatar.setVisibility(View.VISIBLE);
            tvAvatar.setVisibility(View.GONE);
            Glide.with(a)
                    .load(avatarUrl)
                    .centerCrop()
                    .into(ivAvatar);
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvAvatar.setVisibility(View.VISIBLE);
            if (name != null && !name.isEmpty()) {
                tvAvatar.setText(String.valueOf(Character.toUpperCase(name.charAt(0))));
            }
        }
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

    private void compartirPerfil() {
        String handle = buildHandle(nombreActual);
        String texto = "Mira el perfil de @" + handle + " en Sazón 🍳";
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, texto);
        a.startActivity(Intent.createChooser(share, "Compartir perfil"));
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
        my.clear();
        saved.clear();

        if (com.sazon.proyectointegrador.util.DemoData.ENABLED) {
            my.addAll(com.sazon.proyectointegrador.util.DemoData.recetasPropias());
            saved.addAll(com.sazon.proyectointegrador.util.DemoData.recetasGuardadas());
        }

        if (tvStatRecipes != null) tvStatRecipes.setText(String.valueOf(my.size()));
        if (tvStatFollowers != null) tvStatFollowers.setText(
                String.valueOf(com.sazon.proyectointegrador.util.DemoData.demoFollowers()));
        if (tvStatFollowing != null) tvStatFollowing.setText(
                String.valueOf(com.sazon.proyectointegrador.util.DemoData.demoFollowing()));
    }

    private void showMy() {
        showingMyTab = true;
        if (adapter != null) adapter.updateData(new ArrayList<>(my));
        updateEmpty(my, true);
    }

    private void showSaved() {
        showingMyTab = false;
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
                        a.startActivity(new Intent(a, com.sazon.proyectointegrador.CreateRecipeActivity.class))
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
