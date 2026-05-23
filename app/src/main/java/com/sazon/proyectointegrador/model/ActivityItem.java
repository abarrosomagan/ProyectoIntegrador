package com.sazon.proyectointegrador.model;

public class ActivityItem {
    private String id;
    private String type;
    private String actorId;
    private String actorName;
    private String recipeId;
    private String recipeTitle;
    private String message;
    private long createdAt;
    private boolean read;

    public ActivityItem() { }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public String getRecipeId() { return recipeId; }
    public String getRecipeTitle() { return recipeTitle; }
    public String getMessage() { return message; }
    public long getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }

    public void setId(String id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public void setRecipeTitle(String recipeTitle) { this.recipeTitle = recipeTitle; }
    public void setMessage(String message) { this.message = message; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setRead(boolean read) { this.read = read; }
}
