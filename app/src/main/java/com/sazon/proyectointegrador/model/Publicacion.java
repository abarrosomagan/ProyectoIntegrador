package com.sazon.proyectointegrador.model;

import com.google.firebase.firestore.Exclude;

public class Publicacion {

    private String id;
    private String authorId;
    private String autor;
    private long createdAt;
    private String titulo;
    private String descripcion;
    private String imageUrl;
    private String difficulty;
    private String tags;
    private int prepMinutes;
    private int servings;
    private int likes;
    private boolean guardada;
    private boolean liked;

    // Constructor vacío (necesario para Firebase)
    public Publicacion() {
    }

    // Constructor completo
    public Publicacion(String id,
                       String authorId,
                       String autor,
                       long createdAt,
                       String titulo,
                       String descripcion,
                       String imageUrl,
                       int likes,
                       boolean guardada) {

        this.id = id;
        this.authorId = authorId;
        this.autor = autor;
        this.createdAt = createdAt;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.imageUrl = imageUrl;
        this.likes = likes;
        this.guardada = guardada;
    }

    // ===== Getters =====

    public String getId() { return id; }

    public String getAuthorId() { return authorId; }

    public String getAutor() { return autor; }

    public long getCreatedAt() { return createdAt; }

    public String getTitulo() { return titulo; }

    public String getDescripcion() { return descripcion; }

    public String getImageUrl() { return imageUrl; }

    public String getDifficulty() { return difficulty; }

    public String getTags() { return tags; }

    public int getPrepMinutes() { return prepMinutes; }

    public int getServings() { return servings; }

    public int getLikes() { return likes; }

    /**
     * "guardada" es estado por usuario, NO un campo del doc de la receta.
     * Lo excluimos de la serializacion Firestore — se computa al cargar las
     * recetas comparando con users/{uid}/saved.
     */
    @Exclude
    public boolean isGuardada() { return guardada; }

    @Exclude
    public boolean isLiked() { return liked; }

    // ===== Setters =====

    public void setId(String id) { this.id = id; }

    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public void setAutor(String autor) { this.autor = autor; }

    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public void setTitulo(String titulo) { this.titulo = titulo; }

    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public void setTags(String tags) { this.tags = tags; }

    public void setPrepMinutes(int prepMinutes) { this.prepMinutes = prepMinutes; }

    public void setServings(int servings) { this.servings = servings; }

    public void setLikes(int likes) { this.likes = likes; }

    @Exclude
    public void setGuardada(boolean guardada) { this.guardada = guardada; }

    @Exclude
    public void setLiked(boolean liked) { this.liked = liked; }
}
