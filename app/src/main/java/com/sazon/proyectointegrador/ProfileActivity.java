package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvName, tvEmail, tvBio;
    private Button btnEdit, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvBio = findViewById(R.id.tvBio);

        btnEdit = findViewById(R.id.btnEdit);
        btnLogout = findViewById(R.id.btnLogout);

        // ---- DATOS DE PRUEBA (sin Firebase) ----
        tvName.setText("Fernando");
        tvEmail.setText("ferna@email.com");
        tvBio.setText("Perfil de prueba sin Firebase. Aquí irá tu bio.");

        btnEdit.setOnClickListener(v -> {
            Toast.makeText(this, "Editar perfil (demo)", Toast.LENGTH_SHORT).show();
            // Aquí podrías abrir una pantalla EditProfileActivity
        });

        btnLogout.setOnClickListener(v -> {
            // ---- FUTURO CON FIREBASE (COMENTADO) ----
            // FirebaseAuth.getInstance().signOut();

            Toast.makeText(this, "Cerrar sesión (demo)", Toast.LENGTH_SHORT).show();

            // Vuelve a Login
            Intent i = new Intent(ProfileActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // ---- FUTURO CON FIREBASE (COMENTADO) ----
        // FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // if (user != null) {
        //     tvEmail.setText(user.getEmail());
        //     tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
        // }
    }
}