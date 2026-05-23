package com.sazon.proyectointegrador.ui.feed;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sazon.proyectointegrador.LoginActivity;
import com.sazon.proyectointegrador.ProfileActivity;
import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.adapters.PublicacionAdapter;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;
import com.sazon.proyectointegrador.util.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedController {

    private final AppCompatActivity a;

    // Views
    private RecyclerView rvFeed;
    private SwipeRefreshLayout swipeFeed;
    private TextInputEditText etBuscar;
    private TextView tvSaludo;
    private ImageView imgAvatarFeed;
    private ImageButton btnMenuFeed;

    // Adapter + data
    private PublicacionAdapter feedAdapter;
    private final ArrayList<Publicacion> currentData = new ArrayList<>();

    // Firebase
    private FirebaseAuth firebaseAuth;

    // Data source (hoy mock, mañana Firestore)
    private FeedRepository repository;

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
        setupRefresh();
        setupMenu();

        loadFeed(false);
    }

    /** Recarga el feed desde fuera (por ejemplo, al volver de Crear receta). */
    public void refresh() {
        loadFeed(false);
    }

    public void onDestroy() {
        // Aquí en el futuro: repository.detach() para ListenerRegistration de Firestore
        // repository.detach();
    }

    private void bind() {
        rvFeed = a.findViewById(R.id.rvFeed);
        swipeFeed = a.findViewById(R.id.swipeFeed);
        etBuscar = a.findViewById(R.id.etBuscar);

        tvSaludo = a.findViewById(R.id.tvSaludo);
        imgAvatarFeed = a.findViewById(R.id.imgAvatarFeed);

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
                applySearch(query);
            }
        });
    }

    private void setupRefresh() {
        if (swipeFeed == null) return;

        swipeFeed.setOnRefreshListener(() -> loadFeed(true));
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

        repository.fetchFeed(new FeedRepository.Callback() {
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
            feedAdapter.updateData(new ArrayList<>(currentData));
            return;
        }

        ArrayList<Publicacion> filtered = new ArrayList<>();
        String q = query.toLowerCase();

        for (Publicacion p : currentData) {
            String title = (p.getTitulo() != null) ? p.getTitulo().toLowerCase() : "";
            String desc  = (p.getDescripcion() != null) ? p.getDescripcion().toLowerCase() : "";
            String author = (p.getAutor() != null) ? p.getAutor().toLowerCase() : "";

            if (title.contains(q) || desc.contains(q) || author.contains(q)) {
                filtered.add(p);
            }
        }

        feedAdapter.updateData(filtered);
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

        void fetchFeed(Callback cb);
        // En el futuro:
        // void listenFeed(Callback cb);
        // void detach();
        // void fetchNextPage(Callback cb);
    }

    public static class MockFeedRepository implements FeedRepository {

        @Override
        public void fetchFeed(Callback cb) {
            // Simula “red” para que SwipeRefresh se vea bien
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    cb.onSuccess(crearMockPublicaciones());
                } catch (Exception e) {
                    cb.onError(e);
                }
            }, 350);
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
        public void fetchFeed(Callback cb) {
            RecipeRepository.feed(50,
                    list -> {
                        if (list == null || list.isEmpty()) {
                            fallback.fetchFeed(cb);
                        } else {
                            marcarEstadoUsuario(list, cb);
                        }
                    },
                    cb::onError);
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
}
