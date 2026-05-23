package com.sazon.proyectointegrador;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.sazon.proyectointegrador.adapters.ActivityAdapter;
import com.sazon.proyectointegrador.model.ActivityItem;
import com.sazon.proyectointegrador.util.ActivityRepository;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;

public class ActivityActivity extends AppCompatActivity {

    private RecyclerView rv;
    private View empty;
    private ActivityAdapter adapter;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_activity);

        rv = findViewById(R.id.rvActivity);
        empty = findViewById(R.id.emptyActivity);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ActivityAdapter(new ArrayList<>());
            rv.setAdapter(adapter);
        }

        ImageButton back = findViewById(R.id.btnBackActivity);
        if (back != null) back.setOnClickListener(v -> finish());

        MaterialButton markRead = findViewById(R.id.btnMarkRead);
        if (markRead != null) markRead.setOnClickListener(v -> markAllRead());

        listenActivity();
    }

    private void listenActivity() {
        String uid = SessionManager.currentUid();
        if (uid == null) {
            finish();
            return;
        }
        registration = ActivityRepository.query(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        Toast.makeText(this, "No se pudo cargar actividad", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ArrayList<ActivityItem> items = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        ActivityItem item = doc.toObject(ActivityItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            items.add(item);
                        }
                    }
                    if (adapter != null) adapter.updateData(items);
                    if (empty != null) empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    if (rv != null) rv.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void markAllRead() {
        String uid = SessionManager.currentUid();
        if (uid == null) return;
        ActivityRepository.markAllRead(uid,
                v -> Toast.makeText(this, "Actividad marcada como leida", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(this, "No se pudo actualizar", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        super.onDestroy();
    }
}
