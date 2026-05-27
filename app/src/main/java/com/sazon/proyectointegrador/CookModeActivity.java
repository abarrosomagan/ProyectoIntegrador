package com.sazon.proyectointegrador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Modo cocinar paso a paso, pantalla completa.
 * Lee los pasos de la receta y los presenta uno a uno con botones Anterior /
 * Siguiente. Al llegar al último, "Siguiente" se convierte en "He terminado"
 * y cierra la pantalla. Pantalla siempre encendida mientras se cocina.
 */
public class CookModeActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "EXTRA_RECIPE_ID";

    private TextView tvStepLabel, tvStepTotal, tvRecipeTitle, tvStepBody;
    private ProgressBar progress;
    private MaterialButton btnPrev, btnNext;
    private ImageButton btnClose;

    private final List<String> pasos = new ArrayList<>();
    private int indice = 0;

    public static Intent intentFor(android.content.Context ctx, String recipeId) {
        Intent i = new Intent(ctx, CookModeActivity.class);
        i.putExtra(EXTRA_RECIPE_ID, recipeId);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cook_mode);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tvStepLabel = findViewById(R.id.tvCookStepLabel);
        tvStepTotal = findViewById(R.id.tvCookStepTotal);
        tvRecipeTitle = findViewById(R.id.tvCookRecipeTitle);
        tvStepBody = findViewById(R.id.tvCookStepBody);
        progress = findViewById(R.id.progressCook);
        btnPrev = findViewById(R.id.btnCookPrev);
        btnNext = findViewById(R.id.btnCookNext);
        btnClose = findViewById(R.id.btnCloseCook);

        btnClose.setOnClickListener(v -> finish());
        btnPrev.setOnClickListener(v -> ir(-1));
        btnNext.setOnClickListener(v -> {
            if (indice >= pasos.size() - 1) {
                Toast.makeText(this, "¡Buen provecho! 🍽", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                ir(1);
            }
        });

        String recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (recipeId == null || recipeId.isEmpty()) {
            Toast.makeText(this, "Receta no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        cargarReceta(recipeId);
    }

    private void cargarReceta(String recipeId) {
        SessionManager.db()
                .collection(RecipeRepository.COLLECTION_RECIPES)
                .document(recipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this, "Esta receta ya no existe",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    String titulo = doc.getString("titulo");
                    tvRecipeTitle.setText(titulo == null ? "" : titulo);

                    Object pasosObj = doc.get("pasos");
                    if (pasosObj instanceof List) {
                        for (Object o : (List<?>) pasosObj) {
                            if (o == null) continue;
                            String s = o.toString().trim();
                            if (!s.isEmpty()) pasos.add(s);
                        }
                    }

                    if (pasos.isEmpty()) {
                        Toast.makeText(this,
                                "Esta receta aún no tiene pasos paso a paso",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    indice = 0;
                    pintar();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No se pudo cargar la receta",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void ir(int delta) {
        int nuevo = indice + delta;
        if (nuevo < 0 || nuevo >= pasos.size()) return;
        indice = nuevo;
        pintar();
    }

    private void pintar() {
        int total = pasos.size();
        tvStepLabel.setText("Paso " + (indice + 1));
        tvStepTotal.setText((indice + 1) + " / " + total);
        tvStepBody.setText(pasos.get(indice));
        int pct = (int) (((indice + 1) / (float) total) * 100f);
        progress.setProgress(pct);

        btnPrev.setVisibility(indice == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(indice == total - 1 ? "He terminado" : "Siguiente");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
