package com.sazon.proyectointegrador.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.Publicacion;

import java.util.ArrayList;

public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Publicacion publicacion);
    }

    private final ArrayList<Publicacion> lista;
    private final OnItemClickListener listener;

    public PublicacionAdapter(ArrayList<Publicacion> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PublicacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publicacion, parent, false);
        return new PublicacionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PublicacionViewHolder holder, int position) {
        Publicacion p = lista.get(position);

        holder.tvAutor.setText(p.getAutor());
        holder.tvTiempo.setText(p.getTiempo());
        holder.tvTitulo.setText(p.getTitulo());
        holder.tvLikes.setText("❤️ " + p.getLikes());

        // Estado guardado (placeholder con star on/off del sistema)
        holder.btnGuardar.setImageResource(
                p.isGuardada() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );

        holder.btnGuardar.setOnClickListener(v -> {
            p.setGuardada(!p.isGuardada());
            notifyItemChanged(holder.getAdapterPosition());
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class PublicacionViewHolder extends RecyclerView.ViewHolder {

        ImageView imgAvatarAutor;
        ImageButton btnGuardar;
        TextView tvAutor, tvTiempo, tvTitulo, tvLikes;

        PublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatarAutor = itemView.findViewById(R.id.imgAvatarAutor);
            btnGuardar = itemView.findViewById(R.id.btnGuardar);
            tvAutor = itemView.findViewById(R.id.tvAutor);
            tvTiempo = itemView.findViewById(R.id.tvTiempo);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvLikes = itemView.findViewById(R.id.tvLikes);
        }
    }
}