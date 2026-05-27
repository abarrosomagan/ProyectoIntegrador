package com.sazon.proyectointegrador;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.sazon.proyectointegrador.adapters.PublicacionGridAdapter;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.util.ActivityRepository;
import com.sazon.proyectointegrador.util.FollowRepository;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;

/**
 * Perfil público de otro usuario: foto, nombre, bio, contadores reales,
 * sus recetas en grid, y botón Seguir / Siguiendo con persistencia.
 *
 * Reusa activity_profile.xml — escondemos lo que no aplica (editar, compartir,
 * menú ⋮) y mostramos el botón Seguir que está oculto por defecto.
 */
public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";
    public static final String EXTRA_BIO = "EXTRA_BIO";
    public static final String EXTRA_AVATAR_LETTER = "EXTRA_AVATAR_LETTER";
    public static final String EXTRA_IS_OWN_PROFILE = "EXTRA_IS_OWN_PROFILE";

    private String otherUid;

    private TextView tvAvatar, tvUsername, tvUserHandle, tvBio, tvChefRank;
    private ImageView ivAvatar;
    private TextView tvStatRecipes, tvStatFollowers, tvStatFollowing;
    private MaterialButton btnFollow;
    private RecyclerView rv;
    private PublicacionGridAdapter adapter;
    private View emptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private MaterialButton btnEmptyAction;

    private final ArrayList<Publicacion> recetas = new ArrayList<>();
    private boolean siguiendo = false;
    private long followersCount = 0;
    private boolean bloqueado = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        otherUid = getIntent().getStringExtra(EXTRA_USER_ID);
        if (otherUid == null || otherUid.isEmpty()) {
            Toast.makeText(this, "Usuario no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bind();
        configurarVistaParaPerfilAjeno();
        setupRecycler();
        setupCollapsibleHeader();
        setupListeners();

        // Pre-pintamos con lo que venga en el intent (rapidez)
        String preName = getIntent().getStringExtra(EXTRA_USERNAME);
        String preBio  = getIntent().getStringExtra(EXTRA_BIO);
        if (preName != null) tvUsername.setText(preName);
        if (preBio  != null) tvBio.setText(preBio);

        cargarDatosUsuario();
        cargarRecetas();
        comprobarSigo();
    }

    private void bind() {
        tvAvatar = findViewById(R.id.tvAvatar);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvUserHandle = findViewById(R.id.tvUserHandle);
        tvBio = findViewById(R.id.tvBio);
        tvChefRank = findViewById(R.id.tvChefRank);

        tvStatRecipes = findViewById(R.id.tvStatRecipes);
        tvStatFollowers = findViewById(R.id.tvStatFollowers);
        tvStatFollowing = findViewById(R.id.tvStatFollowing);

        btnFollow = findViewById(R.id.btnFollow);

        rv = findViewById(R.id.rvProfileList);

        emptyState = findViewById(R.id.emptyStateProfile);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnEmptyAction = findViewById(R.id.btnEmptyAction);
    }

    private void configurarVistaParaPerfilAjeno() {
        // En perfil ajeno escondemos Editar / Compartir / Menu, mostramos Follow,
        // y dejamos el back visible (en mi propio perfil estaba oculto).
        View btnBack = findViewById(R.id.btnBackProfile);
        if (btnBack != null) btnBack.setVisibility(View.VISIBLE);

        View btnEdit = findViewById(R.id.btnEditProfile);
        View btnShare = findViewById(R.id.btnShareProfile);
        View btnMore = findViewById(R.id.btnMoreProfile);
        if (btnEdit != null) btnEdit.setVisibility(View.GONE);
        if (btnShare != null) btnShare.setVisibility(View.GONE);
        // En perfil ajeno mantenemos el menú ⋮ para Bloquear / Desbloquear
        if (btnMore != null) {
            btnMore.setVisibility(View.VISIBLE);
            btnMore.setOnClickListener(v -> mostrarMenuPerfilAjeno());
        }
        if (btnFollow != null) btnFollow.setVisibility(View.VISIBLE);

        cargarEstadoBloqueo();

        // Pestañas: en perfil ajeno solo mostramos "Mis recetas" (sin guardadas).
        View btnTabSaved = findViewById(R.id.btnTabSaved);
        if (btnTabSaved != null) btnTabSaved.setVisibility(View.GONE);
        View btnTabLiked = findViewById(R.id.btnTabLiked);
        if (btnTabLiked != null) btnTabLiked.setVisibility(View.GONE);
    }

    private void setupRecycler() {
        if (rv == null) return;
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new PublicacionGridAdapter(
                new ArrayList<>(),
                this::abrirDetalleReceta
        );
        rv.setAdapter(adapter);
    }

    private void abrirDetalleReceta(Publicacion p) {
        if (p == null || p.getId() == null || p.getId().isEmpty()) {
            Toast.makeText(this, "Receta no disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.Intent i = new android.content.Intent(this, RecipeDetailActivity.class);
        i.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, p.getId());
        startActivity(i);
    }

    private void setupCollapsibleHeader() {
        AppBarLayout appBar = findViewById(R.id.profileAppBar);
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
        ImageButton btnBack = findViewById(R.id.btnBackProfile);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnFollow != null) btnFollow.setOnClickListener(v -> alternarSigo());
        if (tvStatFollowers != null) {
            tvStatFollowers.setOnClickListener(v ->
                    abrirListaFollow(FollowListActivity.MODE_FOLLOWERS));
        }
        if (tvStatFollowing != null) {
            tvStatFollowing.setOnClickListener(v ->
                    abrirListaFollow(FollowListActivity.MODE_FOLLOWING));
        }
    }

    // ===== Carga real =====

    private void cargarDatosUsuario() {
        SessionManager.loadUserDoc(otherUid,
                doc -> {
                    if (doc == null || !doc.exists()) return;
                    String name = doc.getString("name");
                    String bio  = doc.getString("bio");
                    String avatarUrl = doc.getString("avatarUrl");
                    String email = doc.getString("email");
                    Long followers = doc.getLong("followers");
                    Long following = doc.getLong("following");

                    if (name != null) tvUsername.setText(name);
                    if (bio != null && !bio.isEmpty()) tvBio.setText(bio);

                    if (tvUserHandle != null) {
                        String handle;
                        if (email != null && email.contains("@")) {
                            handle = email.substring(0, email.indexOf("@"));
                        } else {
                            handle = name != null ? name.toLowerCase().replaceAll("\\s+", "") : "chef";
                        }
                        tvUserHandle.setText("@" + handle);
                    }

                    if (followers != null) {
                        followersCount = followers;
                        tvStatFollowers.setText(String.valueOf(followers));
                    }
                    if (following != null) tvStatFollowing.setText(String.valueOf(following));

                    renderAvatar(avatarUrl, name);
                },
                e -> { /* nada */ });
    }

    private void cargarRecetas() {
        RecipeRepository.byAuthor(otherUid,
                list -> {
                    recetas.clear();
                    if (list != null) recetas.addAll(list);

                    tvStatRecipes.setText(String.valueOf(recetas.size()));
                    tvChefRank.setText(chefRankFor(recetas.size()));

                    if (adapter != null) adapter.updateData(new ArrayList<>(recetas));
                    actualizarEmpty();
                },
                e -> actualizarEmpty());
    }

    private void comprobarSigo() {
        String meUid = SessionManager.currentUid();
        if (meUid == null) return;
        if (meUid.equals(otherUid)) {
            btnFollow.setVisibility(View.GONE);
            return;
        }
        FollowRepository.isFollowing(meUid, otherUid,
                following -> {
                    siguiendo = Boolean.TRUE.equals(following);
                    pintarBotonFollow();
                },
                e -> { /* nada */ });
    }

    private void alternarSigo() {
        String meUid = SessionManager.currentUid();
        if (meUid == null) return;

        boolean nuevo = !siguiendo;
        siguiendo = nuevo;
        // UI optimista
        followersCount += nuevo ? 1 : -1;
        if (followersCount < 0) followersCount = 0;
        tvStatFollowers.setText(String.valueOf(followersCount));
        pintarBotonFollow();

        FollowRepository.toggleFollow(meUid, otherUid, nuevo, v -> {
            if (nuevo) {
                ActivityRepository.notifyFollow(otherUid, meUid, currentActorName(), null, null);
            }
        }, e -> {
            // Si falla, revertimos UI
            siguiendo = !nuevo;
            followersCount += nuevo ? -1 : 1;
            if (followersCount < 0) followersCount = 0;
            tvStatFollowers.setText(String.valueOf(followersCount));
            pintarBotonFollow();
            Toast.makeText(this, "No se pudo actualizar el seguimiento",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void pintarBotonFollow() {
        if (btnFollow == null) return;
        if (siguiendo) {
            btnFollow.setText("Siguiendo");
            btnFollow.setTextColor(getResources().getColor(R.color.color_principal_variante));
            btnFollow.setBackgroundTintList(
                    getColorStateList(R.color.fondo_superficie));
            btnFollow.setStrokeColor(
                    getColorStateList(R.color.color_principal_variante));
            btnFollow.setStrokeWidth(
                    (int) (1.5f * getResources().getDisplayMetrics().density));
        } else {
            btnFollow.setText("Seguir");
            btnFollow.setTextColor(getResources().getColor(R.color.texto_sobre_principal));
            btnFollow.setBackgroundTintList(
                    getColorStateList(R.color.color_principal_variante));
            btnFollow.setStrokeWidth(0);
        }
    }

    // ===== Bloqueo de usuario =====

    private void cargarEstadoBloqueo() {
        String meUid = SessionManager.currentUid();
        if (meUid == null || otherUid == null || meUid.equals(otherUid)) return;
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(meUid)
                .collection("blocked")
                .document(otherUid)
                .get()
                .addOnSuccessListener(doc -> bloqueado = doc != null && doc.exists());
    }

    private void mostrarMenuPerfilAjeno() {
        String optBlock = bloqueado ? "Desbloquear usuario" : "Bloquear usuario";
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setItems(new String[] { optBlock }, (d, which) -> {
                    if (which == 0) confirmarBloqueo();
                })
                .show();
    }

    private void confirmarBloqueo() {
        if (bloqueado) {
            cambiarBloqueo(false);
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Bloquear usuario")
                .setMessage("Sus mensajes y recetas se ocultarán de tu experiencia. "
                        + "Podrás desbloquearle más adelante desde su perfil.")
                .setPositiveButton("Bloquear", (d, w) -> cambiarBloqueo(true))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cambiarBloqueo(boolean nuevo) {
        String meUid = SessionManager.currentUid();
        if (meUid == null || otherUid == null) return;
        com.google.firebase.firestore.DocumentReference ref = SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(meUid)
                .collection("blocked")
                .document(otherUid);
        if (nuevo) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            ref.set(data)
                    .addOnSuccessListener(v -> {
                        bloqueado = true;
                        Toast.makeText(this, "Usuario bloqueado", Toast.LENGTH_SHORT).show();
                        // Si lo seguía, dejarlo de seguir como parte del bloqueo
                        if (siguiendo) alternarSigo();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "No se pudo bloquear",
                                    Toast.LENGTH_SHORT).show());
        } else {
            ref.delete()
                    .addOnSuccessListener(v -> {
                        bloqueado = false;
                        Toast.makeText(this, "Usuario desbloqueado", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "No se pudo desbloquear",
                                    Toast.LENGTH_SHORT).show());
        }
    }

    private void renderAvatar(String avatarUrl, String name) {
        if (ivAvatar == null || tvAvatar == null) return;
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            ivAvatar.setVisibility(View.VISIBLE);
            tvAvatar.setVisibility(View.GONE);
            com.sazon.proyectointegrador.util.RecipeImageHelper.loadInto(ivAvatar, avatarUrl);
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvAvatar.setVisibility(View.VISIBLE);
            if (name != null && !name.isEmpty()) {
                tvAvatar.setText(String.valueOf(Character.toUpperCase(name.charAt(0))));
            }
        }
    }

    private void abrirListaFollow(String mode) {
        Intent i = new Intent(this, FollowListActivity.class);
        i.putExtra(FollowListActivity.EXTRA_USER_ID, otherUid);
        i.putExtra(FollowListActivity.EXTRA_MODE, mode);
        startActivity(i);
    }

    private void actualizarEmpty() {
        if (emptyState == null || rv == null) return;
        boolean vacio = recetas.isEmpty();
        emptyState.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rv.setVisibility(vacio ? View.GONE : View.VISIBLE);
        if (vacio) {
            if (tvEmptyTitle != null) tvEmptyTitle.setText("Sin recetas aún");
            if (tvEmptySubtitle != null) tvEmptySubtitle.setText(
                    "Este chef todavía no ha publicado.");
            if (btnEmptyAction != null) btnEmptyAction.setVisibility(View.GONE);
        }
    }

    private static String chefRankFor(int recetas) {
        if (recetas <= 0)  return "🥄 Aprendiz de cocina";
        if (recetas < 4)   return "🍳 Cocinero novato";
        if (recetas < 11)  return "🍲 Cocinero";
        if (recetas < 26)  return "👨‍🍳 Chef de cocina";
        return "⭐ Chef estrella";
    }
    private static String currentActorName() {
        com.google.firebase.auth.FirebaseUser user = SessionManager.currentUser();
        if (user != null) {
            String display = user.getDisplayName();
            if (display != null && !display.trim().isEmpty()) return display.trim();
            String email = user.getEmail();
            if (email != null && email.contains("@")) return email.substring(0, email.indexOf("@"));
        }
        return "Chef";
    }
}
