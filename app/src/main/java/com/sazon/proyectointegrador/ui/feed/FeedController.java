package com.sazon.proyectointegrador.ui.feed;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sazon.proyectointegrador.LoginActivity;
import com.sazon.proyectointegrador.ProfileActivity;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.adapters.PublicacionAdapter;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.util.FollowRepository;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.RecipeStateBus;
import com.sazon.proyectointegrador.util.SessionManager;
import com.sazon.proyectointegrador.util.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedController {

    private static final int MODE_FOR_YOU = 0;
    private static final int MODE_FOLLOWING = 1;
    private static final int MODE_POPULAR = 2;

    private final AppCompatActivity a;

    // Views
    private RecyclerView rvFeed;
    private SwipeRefreshLayout swipeFeed;
    private TextInputEditText etBuscar;
    private TextView tvSaludo;
    private TextView tvFeedEmptyTitle, tvFeedEmptySubtitle;
    private View emptyFeed;
    private ImageView imgAvatarFeed;
    private ImageButton btnMenuFeed;
    private MaterialButton btnFeedForYou, btnFeedFollowing, btnFeedPopular;

    // Adapter + data
    private PublicacionAdapter feedAdapter;
    private final ArrayList<Publicacion> currentData = new ArrayList<>();

    // Firebase
    private FirebaseAuth firebaseAuth;

    // Data source (hoy mock, mañana Firestore)
    private FeedRepository repository;
    private final RecipeStateBus.Listener recipeStateListener = this::onRecipeStateChanged;
    private int feedMode = MODE_FOR_YOU;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private int searchGeneration = 0;

    public FeedController(AppCompatActivity activity) {
        this.a = activity;
    }

    public void init() {
        firebaseAuth = FirebaseAuth.getInstance();

        // Firestore primero, mock como fallback si la colección está vacía.
        repository = new FirestoreFeedRepository();

        bind();
        setupHeader();
        setupRecycler();
        setupSearch();
        setupFeedTabs();
        setupRefresh();
        setupMenu();

        RecipeStateBus.register(recipeStateListener);
        loadFeed(false);
    }

    /** Recarga el feed desde fuera (por ejemplo, al volver de Crear receta). */
    public void refresh() {
        loadFeed(false);
    }

    public void onDestroy() {
        RecipeStateBus.unregister(recipeStateListener);
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        // Aquí en el futuro: repository.detach() para ListenerRegistration de Firestore
        // repository.detach();
    }

    private void onRecipeStateChanged(RecipeStateBus.RecipeState state) {
        boolean changed = false;
        for (Publicacion p : currentData) {
            if (state.recipeId.equals(p.getId())) {
                RecipeStateBus.apply(p, state);
                changed = true;
            }
        }
        if (!changed) return;
        String q = (etBuscar != null && etBuscar.getText() != null)
                ? etBuscar.getText().toString().trim()
                : "";
        applySearch(q);
    }

    private void bind() {
        rvFeed = a.findViewById(R.id.rvFeed);
        swipeFeed = a.findViewById(R.id.swipeFeed);
        etBuscar = a.findViewById(R.id.etBuscar);

        tvSaludo = a.findViewById(R.id.tvSaludo);
        tvFeedEmptyTitle = a.findViewById(R.id.tvFeedEmptyTitle);
        tvFeedEmptySubtitle = a.findViewById(R.id.tvFeedEmptySubtitle);
        emptyFeed = a.findViewById(R.id.emptyFeed);
        imgAvatarFeed = a.findViewById(R.id.imgAvatarFeed);
        btnFeedForYou = a.findViewById(R.id.btnFeedForYou);
        btnFeedFollowing = a.findViewById(R.id.btnFeedFollowing);
        btnFeedPopular = a.findViewById(R.id.btnFeedPopular);

        btnMenuFeed = a.findViewById(R.id.btnMenuFeed); // <- añade este id en el XML (te lo paso abajo)
    }

    private void setupHeader() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        String nombre = "Chef";
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                nombre = user.getDisplayName().trim();
            } else if (user.getEmail() != null && user.getEmail().contains("@")) {
                nombre = user.getEmail().substring(0, user.getEmail().indexOf("@"));
            }
        } else {
            // fallback al SessionManager si quieres
            String localName = new SessionManager(a).getUserName();
            if (localName != null && !localName.trim().isEmpty()) nombre = localName.trim();
        }

        if (tvSaludo != null) tvSaludo.setText("Hola, " + nombre + "!");
        // Avatar: de momento es icono fijo en el feed.
        if (imgAvatarFeed != null) {
            // placeholder ya está en xml
        }
    }

    private void setupRecycler() {
        if (rvFeed == null) {
            Toast.makeText(a, "rvFeed no encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        rvFeed.setLayoutManager(new LinearLayoutManager(a));

        feedAdapter = new PublicacionAdapter(
                new ArrayList<>(),
                pub -> openRecipeDetail(pub),
                pub -> openAuthorProfile(pub)
        );

        rvFeed.setAdapter(feedAdapter);
    }

    private void setupSearch() {
        if (etBuscar == null) return;

        etBuscar.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = (s != null) ? s.toString().trim() : "";
                scheduleSearch(query);
            }
        });
    }

    private void scheduleSearch(String query) {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        pendingSearch = () -> runSearch(query);
        searchHandler.postDelayed(pendingSearch, query != null && query.length() >= 2 ? 250 : 0);
    }

    private void runSearch(String query) {
        if (query == null || query.trim().length() < 2) {
            searchGeneration++;
            applySearch(query);
            return;
        }

        int generation = ++searchGeneration;
        setRefreshing(true);
        repository.searchFeed(query.trim(), new FeedRepository.Callback() {
            @Override
            public void onSuccess(List<Publicacion> data) {
                if (generation != searchGeneration) return;
                ArrayList<Publicacion> result = new ArrayList<>(data);
                if (feedAdapter != null) feedAdapter.updateData(result);
                paintEmpty(result.isEmpty(), "Sin resultados",
                        "Prueba con otra receta, ingrediente o chef.");
                setRefreshing(false);
            }

            @Override
            public void onError(Exception e) {
                if (generation != searchGeneration) return;
                applySearch(query);
                setRefreshing(false);
            }
        });
    }

    private void setupRefresh() {
        if (swipeFeed == null) return;

        swipeFeed.setOnRefreshListener(() -> loadFeed(true));
    }

    private void setupFeedTabs() {
        if (btnFeedForYou != null) {
            btnFeedForYou.setOnClickListener(v -> switchFeedMode(MODE_FOR_YOU));
        }
        if (btnFeedFollowing != null) {
            btnFeedFollowing.setOnClickListener(v -> switchFeedMode(MODE_FOLLOWING));
        }
        if (btnFeedPopular != null) {
            btnFeedPopular.setOnClickListener(v -> switchFeedMode(MODE_POPULAR));
        }
        paintFeedTabs();
    }

    private void switchFeedMode(int mode) {
        if (feedMode == mode) return;
        feedMode = mode;
        paintFeedTabs();
        searchGeneration++;
        loadFeed(false);
    }

    private void paintFeedTabs() {
        paintFeedTab(btnFeedForYou, feedMode == MODE_FOR_YOU);
        paintFeedTab(btnFeedFollowing, feedMode == MODE_FOLLOWING);
        paintFeedTab(btnFeedPopular, feedMode == MODE_POPULAR);
    }

    private void paintFeedTab(MaterialButton button, boolean selected) {
        if (button == null) return;
        button.setTextColor(a.getResources().getColor(selected
                ? R.color.texto_sobre_principal
                : R.color.texto_principal));
        button.setBackgroundTintList(a.getColorStateList(selected
                ? R.color.color_principal_variante
                : R.color.fondo_superficie));
        button.setTypeface(null, selected
                ? android.graphics.Typeface.BOLD
                : android.graphics.Typeface.NORMAL);
    }

    private void setupMenu() {
        if (btnMenuFeed == null) return;

        btnMenuFeed.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(a, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_feed, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();

                if (id == R.id.action_settings) {
                    a.startActivity(new Intent(a,
                            com.sazon.proyectointegrador.SettingsActivity.class));
                    return true;
                }

                if (id == R.id.action_activity) {
                    a.startActivity(new Intent(a,
                            com.sazon.proyectointegrador.ActivityActivity.class));
                    return true;
                }

                if (id == R.id.action_explore) {
                    a.startActivity(new Intent(a,
                            com.sazon.proyectointegrador.ExploreActivity.class));
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

    private void loadFeed(boolean fromRefresh) {
        setRefreshing(true);

        repository.fetchFeed(feedMode, new FeedRepository.Callback() {
            @Override
            public void onSuccess(List<Publicacion> data) {
                currentData.clear();
                currentData.addAll(data);

                // Si hay texto en buscador, lo aplicamos al resultado
                String q = (etBuscar != null && etBuscar.getText() != null)
                        ? etBuscar.getText().toString().trim()
                        : "";

                applySearch(q);
                setRefreshing(false);
            }

            @Override
            public void onError(Exception e) {
                setRefreshing(false);
                Toast.makeText(a, "Error cargando feed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applySearch(String query) {
        if (feedAdapter == null) return;

        if (query == null || query.isEmpty()) {
            ArrayList<Publicacion> copy = new ArrayList<>(currentData);
            feedAdapter.updateData(copy);
            paintEmpty(copy.isEmpty(), emptyTitleForMode(), emptySubtitleForMode());
            return;
        }

        ArrayList<Publicacion> filtered = filterByQuery(currentData, query);

        feedAdapter.updateData(filtered);
        paintEmpty(filtered.isEmpty(), "Sin resultados", "Prueba con otra receta, ingrediente o chef.");
    }

    private void paintEmpty(boolean isEmpty, String title, String subtitle) {
        if (rvFeed != null) rvFeed.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (emptyFeed != null) emptyFeed.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (tvFeedEmptyTitle != null) tvFeedEmptyTitle.setText(title);
        if (tvFeedEmptySubtitle != null) tvFeedEmptySubtitle.setText(subtitle);
    }

    private String emptyTitleForMode() {
        if (feedMode == MODE_FOLLOWING) return "Sin recetas de seguidos";
        if (feedMode == MODE_POPULAR) return "Sin populares todavia";
        return "Sin recetas";
    }

    private String emptySubtitleForMode() {
        if (feedMode == MODE_FOLLOWING) return "Sigue a otros chefs para construir tu feed.";
        if (feedMode == MODE_POPULAR) return "Cuando haya likes, las recetas destacadas apareceran aqui.";
        return "Cuando la comunidad publique recetas apareceran aqui.";
    }

    private void setRefreshing(boolean refreshing) {
        if (swipeFeed != null) swipeFeed.setRefreshing(refreshing);
    }

    private void openRecipeDetail(Publicacion publicacion) {
        String id = publicacion.getId();
        if (id == null || id.isEmpty()) {
            Toast.makeText(a, "Receta de demo (sin detalle)", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(a,
                com.sazon.proyectointegrador.RecipeDetailActivity.class);
        i.putExtra(com.sazon.proyectointegrador.RecipeDetailActivity.EXTRA_RECIPE_ID, id);
        a.startActivity(i);
    }

    private void openAuthorProfile(Publicacion publicacion) {
        if (publicacion.getAuthorId() == null || publicacion.getAuthorId().isEmpty()) {
            Toast.makeText(a, "Perfil no disponible para esta receta", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(a, ProfileActivity.class);
        i.putExtra(ProfileActivity.EXTRA_IS_OWN_PROFILE, false);
        i.putExtra(ProfileActivity.EXTRA_USER_ID, publicacion.getAuthorId());
        i.putExtra(ProfileActivity.EXTRA_USERNAME, publicacion.getAutor());
        i.putExtra(ProfileActivity.EXTRA_AVATAR_LETTER,
                (publicacion.getAutor() != null && !publicacion.getAutor().isEmpty())
                        ? publicacion.getAutor().substring(0, 1)
                        : "U");
        a.startActivity(i);
    }

    private void doLogout() {
        // Firebase logout real
        if (firebaseAuth != null) firebaseAuth.signOut();

        // Limpieza local
        new SessionManager(a).logout();

        Intent intent = new Intent(a, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(intent);
        a.finish();
    }

    // -------------------------
    // Repository (Firestore + fallback mock)
    // -------------------------

    public interface FeedRepository {
        interface Callback {
            void onSuccess(List<Publicacion> data);
            void onError(Exception e);
        }

        void fetchFeed(int mode, Callback cb);
        void searchFeed(String query, Callback cb);
        // En el futuro:
        // void listenFeed(Callback cb);
        // void detach();
        // void fetchNextPage(Callback cb);
    }

    public static class MockFeedRepository implements FeedRepository {

        @Override
        public void fetchFeed(int mode, Callback cb) {
            // Simula “red” para que SwipeRefresh se vea bien
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    cb.onSuccess(crearMockPublicaciones());
                } catch (Exception e) {
                    cb.onError(e);
                }
            }, 350);
        }

        @Override
        public void searchFeed(String query, Callback cb) {
            fetchFeed(MODE_FOR_YOU, new Callback() {
                @Override
                public void onSuccess(List<Publicacion> data) {
                    cb.onSuccess(filterByQuery(data, query));
                }

                @Override
                public void onError(Exception e) {
                    cb.onError(e);
                }
            });
        }

        private List<Publicacion> crearMockPublicaciones() {
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

    /**
     * Implementación real con Firestore. Si /recipes está vacío (primera ejecución,
     * cero recetas reales todavía), cae a mock para que el feed no parezca roto.
     */
    public static class FirestoreFeedRepository implements FeedRepository {

        private final MockFeedRepository fallback = new MockFeedRepository();

        @Override
        public void fetchFeed(int mode, Callback cb) {
            RecipeRepository.feed(80,
                    list -> {
                        if (list == null || list.isEmpty()) {
                            fallback.fetchFeed(mode, cb);
                        } else {
                            filtrarModo(mode, list, cb);
                        }
                    },
                    cb::onError);
        }

        @Override
        public void searchFeed(String query, Callback cb) {
            RecipeRepository.feed(150,
                    list -> marcarEstadoUsuario(filterByQuery(list, query), cb),
                    cb::onError);
        }

        private void filtrarModo(int mode, List<Publicacion> list, Callback cb) {
            if (mode == MODE_POPULAR) {
                Collections.sort(list, (a, b) -> {
                    int byLikes = Integer.compare(b.getLikes(), a.getLikes());
                    if (byLikes != 0) return byLikes;
                    return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                });
                marcarEstadoUsuario(list, cb);
                return;
            }

            if (mode == MODE_FOLLOWING) {
                String uid = SessionManager.currentUid();
                if (uid == null) {
                    cb.onSuccess(new ArrayList<>());
                    return;
                }
                FollowRepository.followingIds(uid,
                        followingIds -> {
                            ArrayList<Publicacion> filtered = new ArrayList<>();
                            for (Publicacion p : list) {
                                if (p.getAuthorId() != null && followingIds.contains(p.getAuthorId())) {
                                    filtered.add(p);
                                }
                            }
                            marcarEstadoUsuario(filtered, cb);
                        },
                        cb::onError);
                return;
            }

            marcarEstadoUsuario(list, cb);
        }

        private void marcarEstadoUsuario(List<Publicacion> list, Callback cb) {
            String uid = SessionManager.currentUid();
            if (uid == null) {
                cb.onSuccess(list);
                return;
            }

            RecipeRepository.savedIds(uid,
                    savedIds -> marcarLikesUsuario(uid, list, savedIds, cb),
                    e -> marcarLikesUsuario(uid, list, new HashSet<>(), cb));
        }

        private void marcarLikesUsuario(String uid,
                                        List<Publicacion> list,
                                        Set<String> savedIds,
                                        Callback cb) {
            RecipeRepository.likedIds(uid,
                    likedIds -> {
                        aplicarEstadoUsuario(list, savedIds, likedIds);
                        cb.onSuccess(list);
                    },
                    e -> {
                        aplicarEstadoUsuario(list, savedIds, new HashSet<>());
                        cb.onSuccess(list);
                    });
        }

        private void aplicarEstadoUsuario(List<Publicacion> list,
                                          Set<String> savedIds,
                                          Set<String> likedIds) {
            for (Publicacion p : list) {
                String id = p.getId();
                p.setGuardada(id != null && savedIds.contains(id));
                p.setLiked(id != null && likedIds.contains(id));
            }
        }
    }

    private static ArrayList<Publicacion> filterByQuery(List<Publicacion> data, String query) {
        ArrayList<Publicacion> filtered = new ArrayList<>();
        if (data == null) return filtered;
        String q = query == null ? "" : query.trim().toLowerCase();
        for (Publicacion p : data) {
            String title = (p.getTitulo() != null) ? p.getTitulo().toLowerCase() : "";
            String desc = (p.getDescripcion() != null) ? p.getDescripcion().toLowerCase() : "";
            String author = (p.getAutor() != null) ? p.getAutor().toLowerCase() : "";
            if (title.contains(q) || desc.contains(q) || author.contains(q)) {
                filtered.add(p);
            }
        }
        return filtered;
    }
}
