package com.sazon.proyectointegrador;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageButton btnBack = findViewById(R.id.btnBackAbout);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }
}