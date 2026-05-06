package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sazon.proyectointegrador.adapters.PublicacionAdapter;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";
    public static final String EXTRA_BIO = "EXTRA_BIO";
    public static final String EXTRA_AVATAR_LETTER = "EXTRA_AVATAR_LETTER";
    public static final String EXTRA_IS_OWN_PROFILE = "EXTRA_IS_OWN_PROFILE";

    private TextView tvAvatar, tvUsername, tvBio;
    private TextView tvStatRecipes, tvStatFollowers, tvStatFollowing;

    private MaterialButton btnEditProfile;
    private MaterialButton btnTabMyRecipes, btnTabSaved;

    private RecyclerView rvProfileList;
    private PublicacionAdapter adapter;

    private ImageButton btnMoreProfile;

    private View emptyStateProfile;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private MaterialButton btnEmptyAction;

    private FirebaseAuth firebaseAuth;

    private final ArrayList<Publicacion> myRecipes = new ArrayList<>();
    private final ArrayList<Publicacion> savedRecipes = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        firebaseAuth = FirebaseAuth.getInstance();
        bindViews();

        ImageButton btnBack = findViewById(R.id.btnBackProfile);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupRecycler();
        applyIntentProfileMode();
        loadMockData();
        showMyRecipes();
        setupListeners();
        applyIntentProfileMode();
        renderHeaderFromFirebaseIfOwnProfile();
    }

    private void bindViews() {
        tvAvatar = findViewById(R.id.tvAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvBio = findViewById(R.id.tvBio);

        tvStatRecipes = findViewById(R.id.tvStatRecipes);
        tvStatFollowers = findViewById(R.id.tvStatFollowers);
        tvStatFollowing = findViewById(R.id.tvStatFollowing);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnTabMyRecipes = findViewById(R.id.btnTabMyRecipes);
        btnTabSaved = findViewById(R.id.btnTabSaved);

        rvProfileList = findViewById(R.id.rvProfileList);

        btnMoreProfile = findViewById(R.id.btnMoreProfile);

        emptyStateProfile = findViewById(R.id.emptyStateProfile);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnEmptyAction = findViewById(R.id.btnEmptyAction);
    }

    private void setupRecycler() {
        if (rvProfileList == null) return;

        rvProfileList.setLayoutManager(new LinearLayoutManager(this));

        // Si tu adapter tiene SOLO 1 callback, cámbialo aquí al constructor correcto.
        adapter = new PublicacionAdapter(
                new ArrayList<>(),
                pub -> Toast.makeText(this, "Detalle pendiente: " + pub.getTitulo(), Toast.LENGTH_SHORT).show(),
                pub -> Toast.makeText(this, "Autor: " + pub.getAutor(), Toast.LENGTH_SHORT).show()
        );

        rvProfileList.setAdapter(adapter);
    }

    private void setupListeners() {
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        }

        if (btnTabMyRecipes != null) btnTabMyRecipes.setOnClickListener(v -> showMyRecipes());
        if (btnTabSaved != null) btnTabSaved.setOnClickListener(v -> showSavedRecipes());

        if (btnMoreProfile != null) btnMoreProfile.setOnClickListener(this::showMoreMenu);

        if (tvStatFollowers != null) {
            tvStatFollowers.setOnClickListener(v ->
                    Toast.makeText(this, "Ver seguidores (pendiente)", Toast.LENGTH_SHORT).show()
            );
        }
        if (tvStatFollowing != null) {
            tvStatFollowing.setOnClickListener(v ->
                    Toast.makeText(this, "Ver seguidos (pendiente)", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void applyIntentProfileMode() {
        boolean isOwnProfile = getIntent().getBooleanExtra(EXTRA_IS_OWN_PROFILE, true);

        String username = getIntent().getStringExtra(EXTRA_USERNAME);
        String bio = getIntent().getStringExtra(EXTRA_BIO);
        String avatar = getIntent().getStringExtra(EXTRA_AVATAR_LETTER);

        if (username != null && !username.trim().isEmpty()) {
            tvUsername.setText(username.trim());

            String letter = (avatar != null && !avatar.trim().isEmpty())
                    ? avatar.trim().substring(0, 1)
                    : ("" + username.trim().charAt(0));

            tvAvatar.setText(letter.toUpperCase());
        } else {
            renderHeaderMock();
        }

        if (bio != null) tvBio.setText(bio);

        // Perfil público: normalmente no se ve Guardadas
        if (!isOwnProfile) {
            if (btnEditProfile != null) btnEditProfile.setVisibility(View.GONE);
            if (btnTabSaved != null) btnTabSaved.setVisibility(View.GONE);
        }
    }

    private void loadMockData() {
        long ahora = System.currentTimeMillis();

        myRecipes.clear();
        myRecipes.add(new Publicacion("1", "user_me", "Fernando",
                ahora - (2 * 60 * 60 * 1000L),
                "Tortilla de patatas",
                "Receta clásica y jugosa.",
                "",
                120,
                false
        ));
        myRecipes.add(new Publicacion("2", "user_me", "Fernando",
                ahora - (24 * 60 * 60 * 1000L),
                "Pollo al curry suave",
                "Curry suave para todos.",
                "",
                85,
                false
        ));

        savedRecipes.clear();
        savedRecipes.add(new Publicacion("10", "user_maria", "María",
                ahora - (2 * 60 * 60 * 1000L),
                "Pasta cremosa con setas",
                "Perfecta para cenas rápidas.",
                "",
                89,
                true
        ));

        if (tvStatRecipes != null) tvStatRecipes.setText(String.valueOf(myRecipes.size()));
        if (tvStatFollowers != null) tvStatFollowers.setText("12");
        if (tvStatFollowing != null) tvStatFollowing.setText("8");
    }

    private void renderHeaderMock() {
        String name = "Fernando";
        tvUsername.setText(name);
        tvBio.setText("Amante de la cocina casera 🍳");
        tvAvatar.setText(name.substring(0, 1).toUpperCase());
    }

    private void showMyRecipes() {
        paintTabs(true);
        List<Publicacion> data = new ArrayList<>(myRecipes);
        setListData(data);
        updateEmptyStateIfNeeded(data, true);
    }

    private void showSavedRecipes() {
        paintTabs(false);
        List<Publicacion> data = new ArrayList<>(savedRecipes);
        setListData(data);
        updateEmptyStateIfNeeded(data, false);
    }

    private void paintTabs(boolean myTab) {
        if (btnTabMyRecipes == null || btnTabSaved == null) return;

        if (myTab) {
            btnTabMyRecipes.setTextColor(getResources().getColor(R.color.texto_sobre_principal));
            btnTabMyRecipes.setBackgroundTintList(getColorStateList(R.color.color_principal_variante));

            btnTabSaved.setTextColor(getResources().getColor(R.color.texto_principal));
            btnTabSaved.setBackgroundTintList(getColorStateList(R.color.fondo_superficie));
        } else {
            btnTabSaved.setTextColor(getResources().getColor(R.color.texto_sobre_principal));
            btnTabSaved.setBackgroundTintList(getColorStateList(R.color.color_principal_variante));

            btnTabMyRecipes.setTextColor(getResources().getColor(R.color.texto_principal));
            btnTabMyRecipes.setBackgroundTintList(getColorStateList(R.color.fondo_superficie));
        }
    }

    private void setListData(List<Publicacion> data) {
        if (adapter == null) return;
        adapter.updateData(new ArrayList<>(data));
    }

    private void updateEmptyStateIfNeeded(List<Publicacion> data, boolean isMyRecipesTab) {
        if (emptyStateProfile == null || rvProfileList == null) return;

        boolean empty = (data == null || data.isEmpty());

        if (!empty) {
            emptyStateProfile.setVisibility(View.GONE);
            rvProfileList.setVisibility(View.VISIBLE);
            if (btnEmptyAction != null) btnEmptyAction.setVisibility(View.GONE);
            return;
        }

        rvProfileList.setVisibility(View.GONE);
        emptyStateProfile.setVisibility(View.VISIBLE);

        if (isMyRecipesTab) {
            if (tvEmptyTitle != null) tvEmptyTitle.setText("Aún no has publicado recetas");
            if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Cuando publiques, aparecerán aquí.");

            if (btnEmptyAction != null) {
                btnEmptyAction.setText("Crear receta");
                btnEmptyAction.setVisibility(View.VISIBLE);
                btnEmptyAction.setOnClickListener(v ->
                        Toast.makeText(this, "Ir a crear receta (pendiente)", Toast.LENGTH_SHORT).show()
                );
            }
        } else {
            if (tvEmptyTitle != null) tvEmptyTitle.setText("No tienes recetas guardadas");
            if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Guarda recetas para verlas aquí.");
            if (btnEmptyAction != null) btnEmptyAction.setVisibility(View.GONE);
        }
    }

    private void showMoreMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Ajustes");
        menu.getMenu().add(0, 2, 1, "Acerca de");
        menu.getMenu().add(0, 3, 2, "Cerrar sesión");

        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == 1) {
                Toast.makeText(this, "Ajustes (pendiente)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == 2) {
                Toast.makeText(this, "Recetas Social v1 (pendiente)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == 3) {
                // Logout real + volver a Login
                if (firebaseAuth != null) firebaseAuth.signOut();
                new SessionManager(this).logout();
                goToLoginAndClearBackstack();
                return true;
            }
            return false;
        });

        menu.show();
    }

    private void goToLoginAndClearBackstack() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // ===== Edición de perfil (SIN Activity extra) =====
    private void showEditProfileDialog() {
        final android.widget.EditText etName = new android.widget.EditText(this);
        etName.setHint("Nombre");
        etName.setText(tvUsername.getText().toString());

        final android.widget.EditText etBio = new android.widget.EditText(this);
        etBio.setHint("Bio");
        etBio.setText(tvBio.getText().toString());

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.addView(etName);
        container.addView(etBio);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Editar perfil")
                .setView(container)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newBio = etBio.getText().toString().trim();

                    if (!newName.isEmpty()) {
                        tvUsername.setText(newName);
                        tvAvatar.setText(("" + newName.charAt(0)).toUpperCase());
                    }
                    tvBio.setText(newBio);

                    Toast.makeText(this, "Perfil actualizado (mock)", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void renderHeaderFromFirebaseIfOwnProfile() {
        boolean isOwnProfile = getIntent().getBooleanExtra(EXTRA_IS_OWN_PROFILE, true);
        if (!isOwnProfile) return;

        if (firebaseAuth == null) firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        String name = user.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            // fallback a email si no hay displayName
            String email = user.getEmail();
            name = (email != null && email.contains("@")) ? email.substring(0, email.indexOf("@")) : "Usuario";
        }

        tvUsername.setText(name);
        tvAvatar.setText(("" + name.charAt(0)).toUpperCase());

        // Bio: de momento se queda como esté (mock o intent) hasta que metáis Firestore
    }
}