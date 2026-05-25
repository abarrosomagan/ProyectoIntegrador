package com.sazon.proyectointegrador;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sazon.proyectointegrador.util.ShoppingList;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de la compra del usuario. Datos en SharedPreferences (vía ShoppingList).
 * Checkbox por item, "Quitar marcados" arriba para limpiar lo ya comprado.
 */
public class ShoppingListActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView tvEmpty;
    private MaterialButton btnClear;
    private final List<ShoppingList.Item> data = new ArrayList<>();
    private Adapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shopping_list);

        ImageButton btnBack = findViewById(R.id.btnBackShopping);
        btnBack.setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvShopping);
        tvEmpty = findViewById(R.id.tvShoppingEmpty);
        btnClear = findViewById(R.id.btnClearCheckedShopping);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        rv.setAdapter(adapter);

        btnClear.setOnClickListener(v -> {
            ShoppingList.clearChecked(this);
            recargar();
        });

        ImageButton btnSort = findViewById(R.id.btnSortShopping);
        if (btnSort != null) btnSort.setOnClickListener(v -> ordenarAlfabeticamente());

        ImageButton btnShare = findViewById(R.id.btnShareShopping);
        if (btnShare != null) btnShare.setOnClickListener(v -> compartirLista());

        recargar();
    }

    private void ordenarAlfabeticamente() {
        if (data.isEmpty()) return;
        java.util.Collections.sort(data, (a, b) -> {
            if (a.checked != b.checked) return a.checked ? 1 : -1;
            return a.text.compareToIgnoreCase(b.text);
        });
        ShoppingList.update(this, data);
        adapter.notifyDataSetChanged();
    }

    private void compartirLista() {
        if (data.isEmpty()) return;
        StringBuilder sb = new StringBuilder("🛒 Lista de la compra (Sazón):\n");
        for (ShoppingList.Item it : data) {
            sb.append(it.checked ? "✓ " : "• ").append(it.text);
            if (it.recipe != null && !it.recipe.isEmpty()) {
                sb.append("  (").append(it.recipe).append(")");
            }
            sb.append("\n");
        }
        android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(android.content.Intent.EXTRA_SUBJECT, "Mi lista de la compra");
        share.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
        startActivity(android.content.Intent.createChooser(share, "Compartir lista"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        recargar();
    }

    private void recargar() {
        data.clear();
        data.addAll(ShoppingList.getAll(this));
        adapter.notifyDataSetChanged();
        boolean vacio = data.isEmpty();
        tvEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rv.setVisibility(vacio ? View.GONE : View.VISIBLE);
        btnClear.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void persistirCambios() {
        ShoppingList.update(this, data);
    }

    // ===== Adapter interno =====

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_shopping, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ShoppingList.Item item = data.get(position);
            h.tvText.setText(item.text);
            h.cb.setOnCheckedChangeListener(null);
            h.cb.setChecked(item.checked);
            pintarTachado(h.tvText, item.checked);

            if (item.recipe != null && !item.recipe.isEmpty()) {
                h.tvRecipe.setText("de " + item.recipe);
                h.tvRecipe.setVisibility(View.VISIBLE);
            } else {
                h.tvRecipe.setVisibility(View.GONE);
            }

            h.cb.setOnCheckedChangeListener((b, isChecked) -> {
                item.checked = isChecked;
                pintarTachado(h.tvText, isChecked);
                persistirCambios();
            });
            h.itemView.setOnClickListener(v -> h.cb.setChecked(!h.cb.isChecked()));
        }

        @Override
        public int getItemCount() { return data.size(); }

        private void pintarTachado(TextView tv, boolean tachado) {
            if (tachado) {
                tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tv.setAlpha(0.6f);
            } else {
                tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                tv.setAlpha(1f);
            }
        }

        class VH extends RecyclerView.ViewHolder {
            final CheckBox cb;
            final TextView tvText, tvRecipe;
            VH(@NonNull View item) {
                super(item);
                cb = item.findViewById(R.id.cbShoppingItem);
                tvText = item.findViewById(R.id.tvShoppingText);
                tvRecipe = item.findViewById(R.id.tvShoppingRecipe);
            }
        }
    }
}
