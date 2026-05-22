package com.sazon.proyectointegrador.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.util.RecipeImageHelper;

import java.util.ArrayList;

/**
 * Grid de recetas para la pestaña Perfil: cuadrículas cuadradas tipo Instagram
 * con imagen, título y likes superpuestos.
 */
public class PublicacionGridAdapter extends RecyclerView.Adapter<PublicacionGridAdapter.VH> {

    public interface OnPostClick {
        void onPostClick(Publicacion p);
    }

    private final ArrayList<Publicacion> data;
    private final OnPostClick listener;

    public PublicacionGridAdapter(ArrayList<Publicacion> data, OnPostClick listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publicacion_grid, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Publicacion p = data.get(position);
        h.tvTitle.setText(safe(p.getTitulo()));
        h.tvLikes.setText("❤ " + p.getLikes());
        String imageUrl = p.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            h.imgRecipe.setVisibility(View.VISIBLE);
            RecipeImageHelper.loadInto(h.imgRecipe, imageUrl);
        } else {
            h.imgRecipe.setVisibility(View.GONE);
            h.imgRecipe.setImageDrawable(null);
        }
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPostClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateData(ArrayList<Publicacion> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvLikes;
        final ImageView imgRecipe;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvGridTitle);
            tvLikes = itemView.findViewById(R.id.tvGridLikes);
            imgRecipe = itemView.findViewById(R.id.imgGridRecipe);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
