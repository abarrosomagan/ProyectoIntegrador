package com.sazon.proyectointegrador.model;

public class RecipeComment {

    private String id;
    private String recipeId;
    private String authorId;
    private String authorName;
    private String text;
    private long createdAt;

    public RecipeComment() {
    }

    public RecipeComment(String id,
                         String recipeId,
                         String authorId,
                         String authorName,
                         String text,
                         long createdAt) {
        this.id = id;
        this.recipeId = recipeId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }

    public String getRecipeId() { return recipeId; }

    public String getAuthorId() { return authorId; }

    public String getAuthorName() { return authorName; }

    public String getText() { return text; }

    public long getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }

    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public void setText(String text) { this.text = text; }

    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
