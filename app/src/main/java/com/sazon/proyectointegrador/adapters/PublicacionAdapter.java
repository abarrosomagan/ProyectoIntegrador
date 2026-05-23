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
import com.sazon.proyectointegrador.util.RecipeImageHelper;
import com.sazon.proyectointegrador.util.RecipeRepository;
import com.sazon.proyectointegrador.util.RecipeStateBus;
import com.sazon.proyectointegrador.util.SessionManager;

import java.util.ArrayList;

public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.VH> {

    public interface OnPostClick {
        void onPostClick(Publicacion p);
    }

    public interface OnAuthorClick {
        void onAuthorClick(Publicacion p);
    }

    private final ArrayList<Publicacion> data;
    private final OnPostClick onPostClick;
    private final OnAuthorClick onAuthorClick;

    public PublicacionAdapter(ArrayList<Publicacion> data, OnPostClick onPostClick, OnAuthorClick onAuthorClick) {
        this.data = data;
        this.onPostClick = onPostClick;
        this.onAuthorClick = onAuthorClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_publicacion, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Publicacion p = data.get(position);

        String autor = safe(p.getAutor());
        String titulo = safe(p.getTitulo());
        String tiempo = formatTime(p.getCreatedAt());
        int likes = p.getLikes();
        boolean guardada = p.isGuardada();
        boolean liked = p.isLiked();

        h.tvAutor.setText(autor);
        h.tvTiempo.setText(tiempo);
        h.tvTitulo.setText(titulo);
        h.tvLikes.setText((liked ? "♥ " : "♡ ") + likes);

        String imageUrl = p.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            h.imgReceta.setVisibility(View.VISIBLE);
            RecipeImageHelper.loadInto(h.imgReceta, imageUrl);
        } else {
            h.imgReceta.setVisibility(View.GONE);
            h.imgReceta.setImageDrawable(null);
        }

        // Click en autor/avatar -> perfil usuario
        View.OnClickListener authorClick = v -> {
            if (onAuthorClick != null) onAuthorClick.onAuthorClick(p);
        };
        h.imgAvatarAutor.setOnClickListener(authorClick);
        h.tvAutor.setOnClickListener(authorClick);

        // Click en card -> detalle receta
        h.cardPublicacion.setOnClickListener(v -> {
            if (onPostClick != null) onPostClick.onPostClick(p);
        });

        // Guardar: pinta icono + persiste en Firestore
        h.btnGuardar.setImageResource(
                guardada ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );

        h.btnGuardar.setOnClickListener(v -> {
            String uid = SessionManager.currentUid();
            String recipeId = p.getId();
            if (uid == null || recipeId == null || recipeId.isEmpty()) {
                // Sin sesión o item demo sin id real → toggle solo visual
                p.setGuardada(!p.isGuardada());
                notifyItemChanged(h.getAdapterPosition());
                return;
            }
            boolean nuevo = !p.isGuardada();
            p.setGuardada(nuevo);
            notifyItemChanged(h.getAdapterPosition());
            RecipeStateBus.publish(p, null, null, nuevo);
            RecipeRepository.toggleSaved(recipeId, uid, nuevo, null, e -> {
                // Si falla, revertimos visualmente
                p.setGuardada(!nuevo);
                notifyItemChanged(h.getAdapterPosition());
                RecipeStateBus.publish(p, null, null, !nuevo);
            });
        });

        // Click en likes: persiste y actualiza contador
        h.tvLikes.setOnClickListener(v -> {
            String uid = SessionManager.currentUid();
            String recipeId = p.getId();
            if (uid == null || recipeId == null || recipeId.isEmpty()) return;
            boolean nuevoLiked = !p.isLiked();
            int anteriorLikes = p.getLikes();
            int nuevoLikes = Math.max(0, anteriorLikes + (nuevoLiked ? 1 : -1));
            p.setLiked(nuevoLiked);
            p.setLikes(nuevoLikes);
            RecipeStateBus.publish(p, nuevoLiked, nuevoLikes, null);
            h.tvLikes.setText((nuevoLiked ? "♥ " : "♡ ") + nuevoLikes);
            RecipeRepository.toggleLike(recipeId, uid, nuevoLiked, null, e -> {
                p.setLiked(!nuevoLiked);
                p.setLikes(anteriorLikes);
                RecipeStateBus.publish(p, !nuevoLiked, anteriorLikes, null);
                h.tvLikes.setText((!nuevoLiked ? "♥ " : "♡ ") + anteriorLikes);
            });
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
        View cardPublicacion;
        ImageView imgAvatarAutor, imgReceta;
        TextView tvAutor, tvTiempo, tvTitulo, tvLikes;
        ImageButton btnGuardar;

        VH(@NonNull View itemView) {
            super(itemView);
            cardPublicacion = itemView.findViewById(R.id.cardPublicacion);
            imgAvatarAutor = itemView.findViewById(R.id.imgAvatarAutor);
            imgReceta = itemView.findViewById(R.id.imgReceta);
            tvAutor = itemView.findViewById(R.id.tvAutor);
            tvTiempo = itemView.findViewById(R.id.tvTiempo);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            btnGuardar = itemView.findViewById(R.id.btnGuardar);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // createdAt -> "hace X min/h", "ayer", "hace N días"
    private String formatTime(long createdAt) {
        if (createdAt <= 0) return "";

        long diff = System.currentTimeMillis() - createdAt;
        if (diff < 0) diff = 0;

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 60) return "hace " + minutes + " min";
        if (hours < 24) return "hace " + hours + " h";
        if (days == 1) return "ayer";
        return "hace " + days + " días";
    }
}
