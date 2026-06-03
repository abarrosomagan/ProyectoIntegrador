package com.sazon.proyectointegrador.adapters;

import android.content.Context;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sazon.proyectointegrador.R;
import com.sazon.proyectointegrador.model.Publicacion;
import com.sazon.proyectointegrador.util.ActivityRepository;
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
        pintarColorLike(h.tvLikes, liked);

        bindMeta(h, p);

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

        // Tap → detalle, doble tap → like + corazón flotante (estilo Instagram)
        attachTapAndDoubleTap(h, p);

        // Guardar: pinta icono + persiste en Firestore
        h.btnGuardar.setImageResource(
                guardada ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_empty
        );

        h.btnGuardar.setOnClickListener(v -> {
            String uid = SessionManager.currentUid();
            String recipeId = p.getId();
            if (uid == null || recipeId == null || recipeId.isEmpty()) {
                // Sin sesión o item demo sin id real → toggle solo visual
                p.setGuardada(!p.isGuardada());
                animateButtonPop(h.btnGuardar);
                notifyItemChanged(h.getAdapterPosition());
                return;
            }
            boolean nuevo = !p.isGuardada();
            p.setGuardada(nuevo);
            animateButtonPop(h.btnGuardar);
            notifyItemChanged(h.getAdapterPosition());
            RecipeStateBus.publish(p, null, null, nuevo);
            RecipeRepository.toggleSaved(recipeId, uid, nuevo, unused -> {
                if (nuevo) {
                    ActivityRepository.notifyRecipe(p.getAuthorId(),
                            ActivityRepository.TYPE_SAVE, uid, currentActorName(),
                            recipeId, safe(p.getTitulo()), null, null);
                }
            }, e -> {
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
            pintarColorLike(h.tvLikes, nuevoLiked);
            if (nuevoLiked) animateLikePulse(h.tvLikes);
            RecipeRepository.toggleLike(recipeId, uid, nuevoLiked, unused -> {
                if (nuevoLiked) {
                    ActivityRepository.notifyRecipe(p.getAuthorId(),
                            ActivityRepository.TYPE_LIKE, uid, currentActorName(),
                            recipeId, safe(p.getTitulo()), null, null);
                }
            }, e -> {
                p.setLiked(!nuevoLiked);
                p.setLikes(anteriorLikes);
                RecipeStateBus.publish(p, !nuevoLiked, anteriorLikes, null);
                h.tvLikes.setText((!nuevoLiked ? "♥ " : "♡ ") + anteriorLikes);
                pintarColorLike(h.tvLikes, !nuevoLiked);
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
        TextView tvAutor, tvTiempo, tvTitulo, tvLikes, tvMeta;
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
            tvMeta = itemView.findViewById(R.id.tvRecipeCardMeta);
            btnGuardar = itemView.findViewById(R.id.btnGuardar);
        }
    }

    private static void bindMeta(@NonNull VH h, Publicacion p) {
        if (h.tvMeta == null) return;
        StringBuilder meta = new StringBuilder();
        if (p.getPrepMinutes() > 0) appendMeta(meta, p.getPrepMinutes() + " min");
        if (p.getServings() > 0) appendMeta(meta, p.getServings() + " rac.");
        if (p.getDifficulty() != null && !p.getDifficulty().trim().isEmpty()) {
            appendMeta(meta, p.getDifficulty().trim());
        }
        if (p.getTags() != null && !p.getTags().trim().isEmpty()) {
            appendMeta(meta, firstTag(p.getTags()));
        }
        h.tvMeta.setText(meta.toString());
        h.tvMeta.setVisibility(meta.length() == 0 ? View.GONE : View.VISIBLE);
    }

    private static void appendMeta(StringBuilder out, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (out.length() > 0) out.append(" · ");
        out.append(value);
    }

    private static String firstTag(String tags) {
        String first = tags.split(",")[0].trim();
        return first.isEmpty() ? "" : "#" + first.replace(" ", "");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Detecta tap simple (abre detalle) y doble tap (da like + corazón flotante).
     * GestureDetector espera ~300ms para confirmar single tap, así que el detalle
     * abre tras una breve pausa — comportamiento idéntico a Instagram.
     */
    @SuppressWarnings("ClickableViewAccessibility")
    private void attachTapAndDoubleTap(@NonNull VH h, @NonNull Publicacion p) {
        Context ctx = h.cardPublicacion.getContext();
        GestureDetector gd = new GestureDetector(ctx,
                new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (onPostClick != null) onPostClick.onPostClick(p);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!p.isLiked()) togglearLikeDesdeDobleTap(h, p);
                animarCorazonExplosivo(h.cardPublicacion);
                return true;
            }
        });
        h.cardPublicacion.setOnTouchListener((v, ev) -> gd.onTouchEvent(ev));
    }

    private void togglearLikeDesdeDobleTap(@NonNull VH h, @NonNull Publicacion p) {
        String uid = SessionManager.currentUid();
        String recipeId = p.getId();
        if (uid == null || recipeId == null || recipeId.isEmpty()) return;

        int anteriorLikes = p.getLikes();
        int nuevoLikes = anteriorLikes + 1;
        p.setLiked(true);
        p.setLikes(nuevoLikes);
        RecipeStateBus.publish(p, true, nuevoLikes, null);
        h.tvLikes.setText("♥ " + nuevoLikes);
        pintarColorLike(h.tvLikes, true);
        animateLikePulse(h.tvLikes);

        RecipeRepository.toggleLike(recipeId, uid, true, unused -> {
            ActivityRepository.notifyRecipe(p.getAuthorId(),
                    ActivityRepository.TYPE_LIKE, uid, currentActorName(),
                    recipeId, safe(p.getTitulo()), null, null);
        }, e -> {
            p.setLiked(false);
            p.setLikes(anteriorLikes);
            RecipeStateBus.publish(p, false, anteriorLikes, null);
            h.tvLikes.setText("♡ " + anteriorLikes);
            pintarColorLike(h.tvLikes, false);
        });
    }

    /** Corazón rojo grande aparece, crece y se desvanece sobre la card. */
    private static void animarCorazonExplosivo(View target) {
        if (!(target instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) target;
        Context ctx = parent.getContext();

        ImageView heart = new ImageView(ctx);
        heart.setImageResource(R.drawable.ic_heart_filled);
        int size = (int) (130 * ctx.getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        lp.gravity = Gravity.CENTER;
        heart.setLayoutParams(lp);
        heart.setScaleX(0f);
        heart.setScaleY(0f);
        heart.setAlpha(0f);

        parent.addView(heart);
        heart.animate()
                .scaleX(1f).scaleY(1f)
                .alpha(0.95f)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator(2f))
                .withEndAction(() -> heart.animate()
                        .scaleX(1.35f).scaleY(1.35f)
                        .alpha(0f)
                        .setDuration(320)
                        .withEndAction(() -> parent.removeView(heart))
                        .start())
                .start();
    }

    private static void pintarColorLike(TextView tv, boolean liked) {
        if (tv == null) return;
        int colorRes = liked ? R.color.color_like : R.color.texto_secundario;
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), colorRes));
    }

    /** Animación rebote tipo Instagram al dar like. */
    private static void animateLikePulse(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.animate()
                .scaleX(1.45f).scaleY(1.45f)
                .setDuration(120)
                .withEndAction(() -> v.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(180)
                        .setInterpolator(new OvershootInterpolator(3f))
                        .start())
                .start();
    }

    /** Pulso suave del botón guardar. */
    private static void animateButtonPop(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.animate()
                .scaleX(1.25f).scaleY(1.25f)
                .rotationBy(8f)
                .setDuration(110)
                .withEndAction(() -> v.animate()
                        .scaleX(1f).scaleY(1f)
                        .rotationBy(-8f)
                        .setDuration(140)
                        .setInterpolator(new OvershootInterpolator(2f))
                        .start())
                .start();
    }

    private static String currentActorName() {
        com.google.firebase.auth.FirebaseUser user = SessionManager.currentUser();
        if (user != null) {
            String display = user.getDisplayName();
            if (display != null && !display.trim().isEmpty()) return display.trim();
            String email = user.getEmail();
            if (email != null && email.contains("@")) return email.substring(0, email.indexOf("@"));
        }
        return "Chef";
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
