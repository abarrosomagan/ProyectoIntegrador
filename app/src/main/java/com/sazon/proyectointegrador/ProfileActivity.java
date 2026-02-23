package com.sazon.proyectointegrador;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sazon.proyectointegrador.adapters.PublicacionAdapter;
import com.sazon.proyectointegrador.model.Publicacion;

import java.util.ArrayList;
import java.util.List;

// ===== Firebase imports (DEJAR COMENTADOS) =====
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.auth.FirebaseUser;
// import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.firebase.firestore.ListenerRegistration;
// import com.google.firebase.firestore.Query;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatar, tvUsername, tvBio;
    private TextView tvStatRecipes, tvStatFollowers, tvStatFollowing;

    private MaterialButton btnEditProfile;
    private MaterialButton btnTabMyRecipes, btnTabSaved;

    private RecyclerView rvProfileList;
    private PublicacionAdapter adapter;

    private final ArrayList<Publicacion> myRecipes = new ArrayList<>();
    private final ArrayList<Publicacion> savedRecipes = new ArrayList<>();

    private boolean showingMyRecipes = true;

    // ===== Firebase (DEJAR COMENTADO) =====
    // private FirebaseAuth firebaseAuth;
    // private FirebaseFirestore firestore;
    // private ListenerRegistration profileListener;
    // private ListenerRegistration myRecipesListener;
    // private ListenerRegistration savedListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        bindViews();
        setupRecycler();
        loadMockData();
        renderHeaderMock();
        showMyRecipes();

        setupListeners();

        // ===== Firebase listo (comentado) =====
        /*
        if (AppConfig.USE_FIREBASE) {
            initFirebase();
            listenProfileFirestore();
            listenMyRecipesFirestore();
            listenSavedFirestore();
        }
        */
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
    }

    private void setupRecycler() {
        rvProfileList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PublicacionAdapter(new ArrayList<>(), pub ->
                Toast.makeText(ProfileActivity.this,
                        "Detalle pendiente: " + pub.getTitulo(),
                        Toast.LENGTH_SHORT).show()
        );

        rvProfileList.setAdapter(adapter);
    }

    private void setupListeners() {
        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(this, "Editar perfil (pendiente)", Toast.LENGTH_SHORT).show()
        );

        btnTabMyRecipes.setOnClickListener(v -> showMyRecipes());
        btnTabSaved.setOnClickListener(v -> showSavedRecipes());
    }

    private void loadMockData() {
        myRecipes.clear();
        myRecipes.add(new Publicacion("Fernando", "hoy", "Tortilla de patatas", 120, false));
        myRecipes.add(new Publicacion("Fernando", "ayer", "Pollo al curry suave", 85, false));
        myRecipes.add(new Publicacion("Fernando", "hace 3 días", "Croquetas de jamón", 210, false));

        savedRecipes.clear();
        savedRecipes.add(new Publicacion("María", "hace 2 h", "Pasta cremosa con setas", 89, true));
        savedRecipes.add(new Publicacion("Sofía", "ayer", "Ensalada fresca de verano", 45, true));

        // stats mock
        tvStatRecipes.setText(String.valueOf(myRecipes.size()));
        tvStatFollowers.setText("12");
        tvStatFollowing.setText("8");
    }

    private void renderHeaderMock() {
        String name = "Fernando";
        tvUsername.setText(name);
        tvBio.setText("Amante de la cocina casera 🍳");
        tvAvatar.setText(name.substring(0, 1).toUpperCase());
    }

    private void showMyRecipes() {
        showingMyRecipes = true;
        // visual estado tab
        btnTabMyRecipes.setTextColor(getResources().getColor(R.color.texto_sobre_principal));
        btnTabMyRecipes.setBackgroundTintList(getColorStateList(R.color.color_principal_variante));

        btnTabSaved.setTextColor(getResources().getColor(R.color.texto_principal));
        btnTabSaved.setBackgroundTintList(getColorStateList(R.color.fondo_superficie));

        // cargar lista
        adapter = new PublicacionAdapter(new ArrayList<>(myRecipes), pub ->
                Toast.makeText(ProfileActivity.this, "Detalle pendiente: " + pub.getTitulo(), Toast.LENGTH_SHORT).show()
        );
        rvProfileList.setAdapter(adapter);
    }

    private void showSavedRecipes() {
        showingMyRecipes = false;

        btnTabSaved.setTextColor(getResources().getColor(R.color.texto_sobre_principal));
        btnTabSaved.setBackgroundTintList(getColorStateList(R.color.color_principal_variante));

        btnTabMyRecipes.setTextColor(getResources().getColor(R.color.texto_principal));
        btnTabMyRecipes.setBackgroundTintList(getColorStateList(R.color.fondo_superficie));

        adapter = new PublicacionAdapter(new ArrayList<>(savedRecipes), pub ->
                Toast.makeText(ProfileActivity.this, "Detalle pendiente: " + pub.getTitulo(), Toast.LENGTH_SHORT).show()
        );
        rvProfileList.setAdapter(adapter);
    }

    // =========================
    // ========== Firebase ======
    // =========================
    /*
    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    // Perfil: users/{uid}
    private void listenProfileFirestore() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        profileListener = firestore.collection("users")
                .document(user.getUid())
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    String name = doc.getString("name");
                    String bio = doc.getString("bio");

                    if (name != null) tvUsername.setText(name);
                    if (bio != null) tvBio.setText(bio);

                    if (name != null && !name.isEmpty()) {
                        tvAvatar.setText(("" + name.charAt(0)).toUpperCase());
                    }
                });
    }

    // Mis recetas: recipes where authorId == uid
    private void listenMyRecipesFirestore() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        myRecipesListener = firestore.collection("recipes")
                .whereEqualTo("authorId", user.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    myRecipes.clear();
                    // mapear doc -> Publicacion (o mejor tu modelo Recipe)
                    // actualizar stats y si estoy en la pestaña, refrescar adapter
                });
    }

    // Guardadas: users/{uid}/saved (o campo savedIds)
    private void listenSavedFirestore() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        savedListener = firestore.collection("users")
                .document(user.getUid())
                .collection("saved")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    savedRecipes.clear();
                    // mapear y refrescar si toca
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (profileListener != null) { profileListener.remove(); profileListener = null; }
        if (myRecipesListener != null) { myRecipesListener.remove(); myRecipesListener = null; }
        if (savedListener != null) { savedListener.remove(); savedListener = null; }
    }
    */
}