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
import com.sazon.proyectointegrador.util.ActivityRepository;
import com.sazon.proyectointegrador.util.FollowRepository;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;
import com.sazon.proyectointegrador.util.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExploreActivity extends AppCompatActivity {

    private static final int MODE_RECIPES = 0;
    private static final int MODE_CHEFS = 1;
    private static final int SORT_FOR_YOU = 0;
    private static final int SORT_RECENT = 1;
    private static final int SORT_POPULAR = 2;
    private static final int FILTER_ALL = 0;
    private static final int FILTER_QUICK = 1;
    private static final int FILTER_EASY = 2;

    private RecyclerView rv;
    private View empty;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private TextInputEditText etSearch;
    private MaterialButton btnRecipes, btnChefs;
    private MaterialButton btnForYou, btnRecent, btnPopular, btnAllChefs, btnFollowingChefs;
    private MaterialButton btnFilterAll, btnFilterQuick, btnFilterEasy;
    private View recipeSortRow, recipeFilterRow, chefFilterRow;
    private PublicacionAdapter recipeAdapter;
    private UserListAdapter userAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private int mode = MODE_RECIPES;
    private int recipeSortMode = SORT_FOR_YOU;
    private int recipeFilterMode = FILTER_ALL;
    private boolean onlyFollowingChefs = false;
    private int generation = 0;
    private final ArrayList<Publicacion> recipes = new ArrayList<>();
    private final ArrayList<UserListItem> chefs = new ArrayList<>();
    private final Set<String> followingIds = new HashSet<>();
    private final Set<String> likedIds = new HashSet<>();
    private final Set<String> savedIds = new HashSet<>();

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

        // Si llegamos con un tag prellenado (desde el detalle de receta) lo aplicamos
        String preset = getIntent().getStringExtra(EXTRA_QUERY);
        if (preset != null && !preset.isEmpty() && etSearch != null) {
            etSearch.setText(preset);
            etSearch.setSelection(etSearch.getText().length());
        }
    }

    public static final String EXTRA_QUERY = "EXTRA_QUERY";

    private void bind() {
        rv = findViewById(R.id.rvExplore);
        empty = findViewById(R.id.emptyExplore);
        tvEmptyTitle = findViewById(R.id.tvExploreEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvExploreEmptySubtitle);
        etSearch = findViewById(R.id.etExploreSearch);
        btnRecipes = findViewById(R.id.btnExploreRecipes);
        btnChefs = findViewById(R.id.btnExploreChefs);
        btnForYou = findViewById(R.id.btnExploreForYou);
        btnRecent = findViewById(R.id.btnExploreRecent);
        btnPopular = findViewById(R.id.btnExplorePopular);
        btnFilterAll = findViewById(R.id.btnRecipeFilterAll);
        btnFilterQuick = findViewById(R.id.btnRecipeFilterQuick);
        btnFilterEasy = findViewById(R.id.btnRecipeFilterEasy);
        btnAllChefs = findViewById(R.id.btnExploreAllChefs);
        btnFollowingChefs = findViewById(R.id.btnExploreFollowingChefs);
        recipeSortRow = findViewById(R.id.recipeSortRow);
        recipeFilterRow = findViewById(R.id.recipeFilterRow);
        chefFilterRow = findViewById(R.id.chefFilterRow);
        ImageButton back = findViewById(R.id.btnBackExplore);
        if (back != null) back.setOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        if (rv == null) return;
        rv.setLayoutManager(new LinearLayoutManager(this));
        recipeAdapter = new PublicacionAdapter(new ArrayList<>(),
                this::openRecipe,
                this::openAuthor);
        userAdapter = new UserListAdapter(new ArrayList<>(), this::openUser, this::toggleFollow);
        rv.setAdapter(recipeAdapter);
    }

    private void setupTabs() {
        if (btnRecipes != null) btnRecipes.setOnClickListener(v -> switchMode(MODE_RECIPES));
        if (btnChefs != null) btnChefs.setOnClickListener(v -> switchMode(MODE_CHEFS));
        if (btnForYou != null) btnForYou.setOnClickListener(v -> switchRecipeSort(SORT_FOR_YOU));
        if (btnRecent != null) btnRecent.setOnClickListener(v -> switchRecipeSort(SORT_RECENT));
        if (btnPopular != null) btnPopular.setOnClickListener(v -> switchRecipeSort(SORT_POPULAR));
        if (btnFilterAll != null) btnFilterAll.setOnClickListener(v -> switchRecipeFilter(FILTER_ALL));
        if (btnFilterQuick != null) btnFilterQuick.setOnClickListener(v -> switchRecipeFilter(FILTER_QUICK));
        if (btnFilterEasy != null) btnFilterEasy.setOnClickListener(v -> switchRecipeFilter(FILTER_EASY));
        if (btnAllChefs != null) btnAllChefs.setOnClickListener(v -> switchChefFilter(false));
        if (btnFollowingChefs != null) btnFollowingChefs.setOnClickListener(v -> switchChefFilter(true));
        paintTabs();
        paintRecipeSort();
        paintRecipeFilter();
        paintChefFilter();
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
        paintModeRows();
        if (rv != null) rv.setAdapter(mode == MODE_RECIPES ? recipeAdapter : userAdapter);
        String query = etSearch != null && etSearch.getText() != null
                ? etSearch.getText().toString().trim()
                : "";
        render(query);
    }

    private void switchRecipeSort(int sortMode) {
        if (recipeSortMode == sortMode) return;
        recipeSortMode = sortMode;
        paintRecipeSort();
        render(currentQuery());
    }

    private void switchChefFilter(boolean followingOnly) {
        if (onlyFollowingChefs == followingOnly) return;
        onlyFollowingChefs = followingOnly;
        paintChefFilter();
        render(currentQuery());
    }

    private void switchRecipeFilter(int filterMode) {
        if (recipeFilterMode == filterMode) return;
        recipeFilterMode = filterMode;
        paintRecipeFilter();
        render(currentQuery());
    }

    private void paintTabs() {
        paintTab(btnRecipes, mode == MODE_RECIPES);
        paintTab(btnChefs, mode == MODE_CHEFS);
        paintModeRows();
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

    private void paintModeRows() {
        if (recipeSortRow != null) {
            recipeSortRow.setVisibility(mode == MODE_RECIPES ? View.VISIBLE : View.GONE);
        }
        if (recipeFilterRow != null) {
            recipeFilterRow.setVisibility(mode == MODE_RECIPES ? View.VISIBLE : View.GONE);
        }
        if (chefFilterRow != null) {
            chefFilterRow.setVisibility(mode == MODE_CHEFS ? View.VISIBLE : View.GONE);
        }
    }

    private void paintRecipeSort() {
        paintTab(btnForYou, recipeSortMode == SORT_FOR_YOU);
        paintTab(btnRecent, recipeSortMode == SORT_RECENT);
        paintTab(btnPopular, recipeSortMode == SORT_POPULAR);
    }

    private void paintChefFilter() {
        paintTab(btnAllChefs, !onlyFollowingChefs);
        paintTab(btnFollowingChefs, onlyFollowingChefs);
    }

    private void paintRecipeFilter() {
        paintTab(btnFilterAll, recipeFilterMode == FILTER_ALL);
        paintTab(btnFilterQuick, recipeFilterMode == FILTER_QUICK);
        paintTab(btnFilterEasy, recipeFilterMode == FILTER_EASY);
    }

    private void loadInitialData() {
        loadRecipes();
        loadChefs();
        loadFollowingIds();
        loadRecipeState();
    }

    private void loadRecipes() {
        RecipeRepository.feed(150,
                list -> {
                    recipes.clear();
                    if (list != null) recipes.addAll(list);
                    applyRecipeState();
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
        UserListItem user = new UserListItem(
                doc.getId(),
                doc.getString("name"),
                doc.getString("email"),
                doc.getString("bio"),
                doc.getString("avatarUrl")
        );
        Long recipes = doc.getLong("recipes");
        Long followers = doc.getLong("followers");
        user.setRecipes(recipes == null ? 0 : recipes);
        user.setFollowers(followers == null ? 0 : followers);
        user.setFollowing(followingIds.contains(doc.getId()));
        return user;
    }

    private void loadFollowingIds() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;
        FollowRepository.followingIds(uid,
                ids -> {
                    followingIds.clear();
                    if (ids != null) followingIds.addAll(ids);
                    applyFollowingState();
                    applyRecipeState();
                    render(currentQuery());
                },
                e -> { });
    }

    private void loadRecipeState() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;
        RecipeRepository.savedIds(uid,
                ids -> {
                    savedIds.clear();
                    if (ids != null) savedIds.addAll(ids);
                    applyRecipeState();
                    render(currentQuery());
                },
                e -> { });
        RecipeRepository.likedIds(uid,
                ids -> {
                    likedIds.clear();
                    if (ids != null) likedIds.addAll(ids);
                    applyRecipeState();
                    render(currentQuery());
                },
                e -> { });
    }

    private void applyFollowingState() {
        for (UserListItem user : chefs) {
            user.setFollowing(user.getUid() != null && followingIds.contains(user.getUid()));
        }
    }

    private void applyRecipeState() {
        for (Publicacion recipe : recipes) {
            String id = recipe.getId();
            recipe.setGuardada(id != null && savedIds.contains(id));
            recipe.setLiked(id != null && likedIds.contains(id));
        }
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
            if (!matchesRecipeFilter(p)) continue;
            if (q.length() < 2 || recipeMatches(p, q)) filtered.add(p);
        }
        sortRecipes(filtered);
        if (recipeAdapter != null) recipeAdapter.updateData(filtered);
        paintEmpty(filtered.isEmpty(), "Sin recetas", "Prueba con otro ingrediente o plato.");
    }

    private void renderChefs(String query) {
        ArrayList<UserListItem> filtered = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase();
        for (UserListItem user : chefs) {
            if (onlyFollowingChefs && !user.isFollowing()) continue;
            if (q.length() < 2 || userMatches(user, q)) filtered.add(user);
        }
        Collections.sort(filtered, (a, b) -> {
            int byFollowers = Long.compare(b.getFollowers(), a.getFollowers());
            if (byFollowers != 0) return byFollowers;
            int byRecipes = Long.compare(b.getRecipes(), a.getRecipes());
            if (byRecipes != 0) return byRecipes;
            return a.displayName().compareToIgnoreCase(b.displayName());
        });
        if (userAdapter != null) userAdapter.updateData(filtered);
        paintEmpty(filtered.isEmpty(), onlyFollowingChefs ? "Sin chefs seguidos" : "Sin chefs",
                onlyFollowingChefs ? "Sigue chefs desde Explorar para construir este filtro."
                        : "Prueba con otro nombre o usuario.");
    }

    private void sortRecipes(ArrayList<Publicacion> filtered) {
        if (recipeSortMode == SORT_RECENT) {
            Collections.sort(filtered, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            return;
        }
        if (recipeSortMode == SORT_POPULAR) {
            Collections.sort(filtered, (a, b) -> {
                int byLikes = Integer.compare(b.getLikes(), a.getLikes());
                if (byLikes != 0) return byLikes;
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            });
            return;
        }
        Collections.sort(filtered, (a, b) -> Double.compare(scoreFor(b), scoreFor(a)));
    }

    private double scoreFor(Publicacion recipe) {
        double score = recipe.getLikes() * 6.0;
        if (recipe.isLiked()) score += 35.0;
        if (recipe.isGuardada()) score += 24.0;
        if (recipe.getAuthorId() != null && followingIds.contains(recipe.getAuthorId())) score += 80.0;
        long ageMillis = Math.max(0, System.currentTimeMillis() - recipe.getCreatedAt());
        double ageDays = ageMillis / (24.0 * 60.0 * 60.0 * 1000.0);
        score += Math.max(0.0, 45.0 - (ageDays * 3.0));
        return score;
    }

    private boolean recipeMatches(Publicacion p, String q) {
        return contains(p.getTitulo(), q)
                || contains(p.getDescripcion(), q)
                || contains(p.getAutor(), q)
                || contains(p.getDifficulty(), q)
                || contains(p.getTags(), q);
    }

    private boolean matchesRecipeFilter(Publicacion p) {
        if (recipeFilterMode == FILTER_QUICK) {
            return p.getPrepMinutes() > 0 && p.getPrepMinutes() <= 30;
        }
        if (recipeFilterMode == FILTER_EASY) {
            return contains(p.getDifficulty(), "facil")
                    || contains(p.getDifficulty(), "fácil")
                    || contains(p.getTags(), "facil")
                    || contains(p.getTags(), "fácil");
        }
        return true;
    }

    private boolean userMatches(UserListItem user, String q) {
        return contains(user.displayName(), q)
                || contains(user.handle(), q)
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

    private void toggleFollow(UserListItem user) {
        String meUid = SessionManager.currentUid();
        if (meUid == null || user == null || user.getUid() == null) {
            Toast.makeText(this, "Inicia sesion para seguir chefs", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean next = !user.isFollowing();
        user.setFollowing(next);
        if (next) {
            followingIds.add(user.getUid());
            user.setFollowers(user.getFollowers() + 1);
        } else {
            followingIds.remove(user.getUid());
            user.setFollowers(Math.max(0, user.getFollowers() - 1));
        }
        render(currentQuery());

        FollowRepository.toggleFollow(meUid, user.getUid(), next, v -> {
            if (next) {
                ActivityRepository.notifyFollow(user.getUid(), meUid, currentActorName(), null, null);
            }
        }, e -> {
            user.setFollowing(!next);
            if (next) {
                followingIds.remove(user.getUid());
                user.setFollowers(Math.max(0, user.getFollowers() - 1));
            } else {
                followingIds.add(user.getUid());
                user.setFollowers(user.getFollowers() + 1);
            }
            render(currentQuery());
            Toast.makeText(this, "No se pudo actualizar seguimiento", Toast.LENGTH_SHORT).show();
        });
    }

    private String currentActorName() {
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                return user.getDisplayName().trim();
            }
            if (user.getEmail() != null && user.getEmail().contains("@")) {
                return user.getEmail().substring(0, user.getEmail().indexOf("@"));
            }
        }
        String localName = new SessionManager(this).getUserName();
        return localName == null || localName.trim().isEmpty() ? "Chef" : localName.trim();
    }

    @Override
    protected void onDestroy() {
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        super.onDestroy();
    }
}
