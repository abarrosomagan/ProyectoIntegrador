package com.example.proyectointegrador;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectointegrador.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtEmail, edtPassword;
    private CheckBox chkTerms;
    private MaterialButton btnRegister;
    private TextView txtGoLogin, txtTermsLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        chkTerms = findViewById(R.id.chkTerms);
        btnRegister = findViewById(R.id.btnRegister);
        txtGoLogin = findViewById(R.id.txtGoLogin);
        txtTermsLink = findViewById(R.id.txtTermsLink);

        btnRegister.setOnClickListener(v -> attemptRegister());

        txtGoLogin.setOnClickListener(v -> {
            finish();
        });

        txtTermsLink.setOnClickListener(v -> {
            Toast.makeText(this, "Mostrar términos y condiciones", Toast.LENGTH_SHORT).show();
        });
    }

    private void attemptRegister() {
        String name = edtName.getText() != null ? edtName.getText().toString().trim() : "";
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
        String password = edtPassword.getText() != null ? edtPassword.getText().toString() : "";

        if (TextUtils.isEmpty(name)) {
            edtName.setError("Campo obligatorio");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Campo obligatorio");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Campo obligatorio");
            return;
        }
        if (!chkTerms.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Registro válido (pendiente de backend)", Toast.LENGTH_SHORT).show();
    }
}
