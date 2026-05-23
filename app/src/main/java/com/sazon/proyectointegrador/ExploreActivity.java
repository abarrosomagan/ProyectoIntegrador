package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.sazon.proyectointegrador.adapters.PublicacionAdapter;
import com.sazon.proyectointegrador.adapters.UserListAdapter;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.model.UserListItem;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;
import com.sazon.proyectointegrador.util.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.List;

public class ExploreActivity extends AppCompatActivity {

    private static final int MODE_RECIPES = 0;
    private static final int MODE_CHEFS = 1;

    private RecyclerView rv;
    private View empty;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private TextInputEditText etSearch;
    private MaterialButton btnRecipes, btnChefs;
    private PublicacionAdapter recipeAdapter;
    private UserListAdapter userAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private int mode = MODE_RECIPES;
    private int generation = 0;
    private final ArrayList<Publicacion> recipes = new ArrayList<>();
    private final ArrayList<UserListItem> chefs = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_explore);

        bind();
        setupRecycler();
        setupTabs();
        setupSearch();
        loadInitialData();
    }

    private void bind() {
        rv = findViewById(R.id.rvExplore);
        empty = findViewById(R.id.emptyExplore);
        tvEmptyTitle = findViewById(R.id.tvExploreEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvExploreEmptySubtitle);
        etSearch = findViewById(R.id.etExploreSearch);
        btnRecipes = findViewById(R.id.btnExploreRecipes);
        btnChefs = findViewById(R.id.btnExploreChefs);
        ImageButton back = findViewById(R.id.btnBackExplore);
        if (back != null) back.setOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        if (rv == null) return;
        rv.setLayoutManager(new LinearLayoutManager(this));
        recipeAdapter = new PublicacionAdapter(new ArrayList<>(),
                this::openRecipe,
                this::openAuthor);
        userAdapter = new UserListAdapter(new ArrayList<>(), this::openUser);
        rv.setAdapter(recipeAdapter);
    }

    private void setupTabs() {
        if (btnRecipes != null) btnRecipes.setOnClickListener(v -> switchMode(MODE_RECIPES));
        if (btnChefs != null) btnChefs.setOnClickListener(v -> switchMode(MODE_CHEFS));
        paintTabs();
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();
                scheduleSearch(query);
            }
        });
    }

    private void switchMode(int newMode) {
        if (mode == newMode) return;
        mode = newMode;
        paintTabs();
        if (rv != null) rv.setAdapter(mode == MODE_RECIPES ? recipeAdapter : userAdapter);
        String query = etSearch != null && etSearch.getText() != null
                ? etSearch.getText().toString().trim()
                : "";
        render(query);
    }

    private void paintTabs() {
        paintTab(btnRecipes, mode == MODE_RECIPES);
        paintTab(btnChefs, mode == MODE_CHEFS);
    }

    private void paintTab(MaterialButton button, boolean selected) {
        if (button == null) return;
        button.setTextColor(getResources().getColor(selected
                ? R.color.texto_sobre_principal
                : R.color.texto_principal));
        button.setBackgroundTintList(getColorStateList(selected
                ? R.color.color_principal_variante
                : R.color.fondo_superficie));
    }

    private void loadInitialData() {
        loadRecipes();
        loadChefs();
    }

    private void loadRecipes() {
        RecipeRepository.feed(150,
                list -> {
                    recipes.clear();
                    if (list != null) recipes.addAll(list);
                    render(currentQuery());
                },
                e -> Toast.makeText(this, "No se pudieron cargar recetas", Toast.LENGTH_SHORT).show());
    }

    private void loadChefs() {
        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .limit(120)
                .get()
                .addOnSuccessListener(snap -> {
                    chefs.clear();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            chefs.add(parseUser(doc));
                        }
                    }
                    render(currentQuery());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudieron cargar chefs", Toast.LENGTH_SHORT).show());
    }

    private UserListItem parseUser(DocumentSnapshot doc) {
        return new UserListItem(
                doc.getId(),
                doc.getString("name"),
                doc.getString("email"),
                doc.getString("bio"),
                doc.getString("avatarUrl")
        );
    }

    private void scheduleSearch(String query) {
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        int delay = query.length() >= 2 ? 200 : 0;
        int currentGeneration = ++generation;
        pendingSearch = () -> {
            if (currentGeneration == generation) render(query);
        };
        handler.postDelayed(pendingSearch, delay);
    }

    private String currentQuery() {
        return etSearch != null && etSearch.getText() != null
                ? etSearch.getText().toString().trim()
                : "";
    }

    private void render(String query) {
        if (mode == MODE_RECIPES) renderRecipes(query);
        else renderChefs(query);
    }

    private void renderRecipes(String query) {
        ArrayList<Publicacion> filtered = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase();
        for (Publicacion p : recipes) {
            if (q.length() < 2 || recipeMatches(p, q)) filtered.add(p);
        }
        if (recipeAdapter != null) recipeAdapter.updateData(filtered);
        paintEmpty(filtered.isEmpty(), "Sin recetas", "Prueba con otro ingrediente o plato.");
    }

    private void renderChefs(String query) {
        ArrayList<UserListItem> filtered = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase();
        for (UserListItem user : chefs) {
            if (q.length() < 2 || userMatches(user, q)) filtered.add(user);
        }
        if (userAdapter != null) userAdapter.updateData(filtered);
        paintEmpty(filtered.isEmpty(), "Sin chefs", "Prueba con otro nombre o usuario.");
    }

    private boolean recipeMatches(Publicacion p, String q) {
        return contains(p.getTitulo(), q)
                || contains(p.getDescripcion(), q)
                || contains(p.getAutor(), q);
    }

    private boolean userMatches(UserListItem user, String q) {
        return contains(user.displayName(), q)
                || contains(user.getEmail(), q)
                || contains(user.getBio(), q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private void paintEmpty(boolean isEmpty, String title, String subtitle) {
        if (rv != null) rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (empty != null) empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (tvEmptyTitle != null) tvEmptyTitle.setText(title);
        if (tvEmptySubtitle != null) tvEmptySubtitle.setText(subtitle);
    }

    private void openRecipe(Publicacion p) {
        if (p == null || p.getId() == null || p.getId().isEmpty()) return;
        Intent i = new Intent(this, RecipeDetailActivity.class);
        i.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, p.getId());
        startActivity(i);
    }

    private void openAuthor(Publicacion p) {
        if (p == null || p.getAuthorId() == null || p.getAuthorId().isEmpty()) return;
        Intent i = new Intent(this, ProfileActivity.class);
        i.putExtra(ProfileActivity.EXTRA_USER_ID, p.getAuthorId());
        i.putExtra(ProfileActivity.EXTRA_USERNAME, p.getAutor());
        i.putExtra(ProfileActivity.EXTRA_IS_OWN_PROFILE, false);
        startActivity(i);
    }

    private void openUser(UserListItem user) {
        if (user == null || user.getUid() == null) return;
        String currentUid = SessionManager.currentUid();
        if (currentUid != null && currentUid.equals(user.getUid())) {
            finish();
            return;
        }
        Intent i = new Intent(this, ProfileActivity.class);
        i.putExtra(ProfileActivity.EXTRA_USER_ID, user.getUid());
        i.putExtra(ProfileActivity.EXTRA_USERNAME, user.displayName());
        i.putExtra(ProfileActivity.EXTRA_BIO, user.getBio());
        i.putExtra(ProfileActivity.EXTRA_IS_OWN_PROFILE, false);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        super.onDestroy();
    }
}
