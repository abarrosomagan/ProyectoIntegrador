package com.sazon.proyectointegrador.util;

import androidx.annotation.NonNull;

import com.sazon.proyectointegrador.model.Publicacion;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class RecipeStateBus {

    public interface Listener {
        void onRecipeStateChanged(@NonNull RecipeState state);
    }

    public static final class RecipeState {
        public final String recipeId;
        public final Publicacion recipe;
        public final Boolean liked;
        public final Integer likes;
        public final Boolean saved;

        private RecipeState(String recipeId,
                            Publicacion recipe,
                            Boolean liked,
                            Integer likes,
                            Boolean saved) {
            this.recipeId = recipeId;
            this.recipe = recipe;
            this.liked = liked;
            this.likes = likes;
            this.saved = saved;
        }
    }

    private static final Set<Listener> LISTENERS = new CopyOnWriteArraySet<>();

    private RecipeStateBus() { }

    public static void register(@NonNull Listener listener) {
        LISTENERS.add(listener);
    }

    public static void unregister(@NonNull Listener listener) {
        LISTENERS.remove(listener);
    }

    public static void publish(@NonNull Publicacion recipe,
                               Boolean liked,
                               Integer likes,
                               Boolean saved) {
        String id = recipe.getId();
        if (id == null || id.isEmpty()) return;
        RecipeState state = new RecipeState(id, copy(recipe), liked, likes, saved);
        for (Listener listener : LISTENERS) {
            listener.onRecipeStateChanged(state);
        }
    }

    public static void apply(@NonNull Publicacion recipe, @NonNull RecipeState state) {
        if (state.liked != null) recipe.setLiked(state.liked);
        if (state.likes != null) recipe.setLikes(state.likes);
        if (state.saved != null) recipe.setGuardada(state.saved);
    }

    private static Publicacion copy(Publicacion source) {
        Publicacion copy = new Publicacion(
                source.getId(),
                source.getAuthorId(),
                source.getAutor(),
                source.getCreatedAt(),
                source.getTitulo(),
                source.getDescripcion(),
                source.getImageUrl(),
                source.getLikes(),
                source.isGuardada());
        copy.setLiked(source.isLiked());
        return copy;
    }
}
