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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.sazon.proyectointegrador.adapters.UserListAdapter;
import com.sazon.proyectointegrador.model.UserListItem;
import com.sazon.proyectointegrador.util.FollowRepository;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class FollowListActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String MODE_FOLLOWERS = "followers";
    public static final String MODE_FOLLOWING = "following";

    private String userId;
    private String mode;
    private RecyclerView rv;
    private View emptyState;
    private TextView tvTitle, tvEmptyTitle, tvEmptySubtitle;
    private UserListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_follow_list);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Usuario no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!MODE_FOLLOWING.equals(mode)) mode = MODE_FOLLOWERS;

        bind();
        setupRecycler();
        cargarUsuarios();
    }

    private void bind() {
        ImageButton btnBack = findViewById(R.id.btnBackFollowList);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvFollowListTitle);
        tvEmptyTitle = findViewById(R.id.tvFollowListEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvFollowListEmptySubtitle);
        rv = findViewById(R.id.rvFollowList);
        emptyState = findViewById(R.id.emptyFollowList);

        boolean following = MODE_FOLLOWING.equals(mode);
        if (tvTitle != null) tvTitle.setText(following ? "Siguiendo" : "Seguidores");
        if (tvEmptyTitle != null) {
            tvEmptyTitle.setText(following ? "No sigue a nadie" : "Sin seguidores aún");
        }
        if (tvEmptySubtitle != null) {
            tvEmptySubtitle.setText(following
                    ? "Cuando siga a otros chefs aparecerán aquí."
                    : "Cuando otros chefs sigan este perfil aparecerán aquí.");
        }
    }

    private void setupRecycler() {
        if (rv == null) return;
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserListAdapter(new ArrayList<>(), this::abrirPerfil);
        rv.setAdapter(adapter);
    }

    private void cargarUsuarios() {
        String collection = MODE_FOLLOWING.equals(mode)
                ? FollowRepository.SUB_FOLLOWING
                : FollowRepository.SUB_FOLLOWERS;

        SessionManager.db()
                .collection(SessionManager.COLLECTION_USERS)
                .document(userId)
                .collection(collection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        pintarLista(new ArrayList<>());
                        return;
                    }
                    resolverUsuarios(snap.getDocuments());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No se pudo cargar la lista",
                            Toast.LENGTH_SHORT).show();
                    pintarLista(new ArrayList<>());
                });
    }

    private void resolverUsuarios(List<DocumentSnapshot> relationDocs) {
        ArrayList<UserListItem> result = new ArrayList<>();
        final int[] pending = { relationDocs.size() };

        for (DocumentSnapshot relationDoc : relationDocs) {
            String uid = relationDoc.getId();
            SessionManager.loadUserDoc(uid,
                    userDoc -> {
                        UserListItem item = parseUser(uid, userDoc);
                        result.add(item);
                        pending[0]--;
                        if (pending[0] == 0) pintarLista(result);
                    },
                    e -> {
                        result.add(new UserListItem(uid, "Chef", "", "", ""));
                        pending[0]--;
                        if (pending[0] == 0) pintarLista(result);
                    });
        }
    }

    private UserListItem parseUser(String uid, DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return new UserListItem(uid, "Chef", "", "", "");
        }
        return new UserListItem(
                uid,
                doc.getString("name"),
                doc.getString("email"),
                doc.getString("bio"),
                doc.getString("avatarUrl")
        );
    }

    private void pintarLista(ArrayList<UserListItem> users) {
        if (adapter != null) adapter.updateData(users);
        boolean empty = users.isEmpty();
        if (rv != null) rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void abrirPerfil(UserListItem user) {
        if (user == null || user.getUid() == null) return;
        String currentUid = SessionManager.currentUid();
        if (currentUid != null && currentUid.equals(user.getUid())) {
            finish();
            return;
        }
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_USER_ID, user.getUid());
        intent.putExtra(ProfileActivity.EXTRA_USERNAME, user.displayName());
        intent.putExtra(ProfileActivity.EXTRA_BIO, user.getBio());
        intent.putExtra(ProfileActivity.EXTRA_IS_OWN_PROFILE, false);
        startActivity(intent);
    }
}
