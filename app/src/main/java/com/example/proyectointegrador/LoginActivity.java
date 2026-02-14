package com.example.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usoCampoCorreo;
    private TextInputEditText usoCampoContrasena;
    private MaterialButton usoBotonIniciarSesion;
    private MaterialButton usoBotonGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usoCampoCorreo = findViewById(R.id.uso_campo_correo);
        usoCampoContrasena = findViewById(R.id.uso_campo_contrasena);
        usoBotonIniciarSesion = findViewById(R.id.uso_boton_iniciar_sesion);
        usoBotonGoogle = findViewById(R.id.uso_boton_google);

//        findViewById(R.id.uso_texto_registro).setOnClickListener(v ->
//                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
//        );

        findViewById(R.id.uso_olvidaste_contrasena).setOnClickListener(v ->
                Toast.makeText(this, "Recuperación de contraseña (pendiente)", Toast.LENGTH_SHORT).show()
        );

        usoBotonGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Login con Google (pendiente)", Toast.LENGTH_SHORT).show()
        );

        usoBotonIniciarSesion.setOnClickListener(v -> {
            String correo = usoCampoCorreo.getText() != null ? usoCampoCorreo.getText().toString().trim() : "";
            String contrasena = usoCampoContrasena.getText() != null ? usoCampoContrasena.getText().toString().trim() : "";

            if (TextUtils.isEmpty(correo)) {
                usoCampoCorreo.setError("Introduce el correo");
                usoCampoCorreo.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(contrasena)) {
                usoCampoContrasena.setError("Introduce la contraseña");
                usoCampoContrasena.requestFocus();
                return;
            }

            // Login simulado (de momento)
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }
}