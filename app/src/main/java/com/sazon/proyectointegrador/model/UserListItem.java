package com.sazon.proyectointegrador.model;

public class UserListItem {

    private String uid;
    private String name;
    private String email;
    private String bio;
    private String avatarUrl;

    public UserListItem() {
    }

    public UserListItem(String uid,
                        String name,
                        String email,
                        String bio,
                        String avatarUrl) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }

    public String getUid() { return uid; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getBio() { return bio; }

    public String getAvatarUrl() { return avatarUrl; }

    public void setUid(String uid) { this.uid = uid; }

    public void setName(String name) { this.name = name; }

    public void setEmail(String email) { this.email = email; }

    public void setBio(String bio) { this.bio = bio; }

    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String displayName() {
        if (name != null && !name.trim().isEmpty()) return name.trim();
        if (email != null && email.contains("@")) return email.substring(0, email.indexOf("@"));
        return "Chef";
    }

    public String handle() {
        if (email != null && email.contains("@")) {
            return "@" + email.substring(0, email.indexOf("@")).toLowerCase();
        }
        String base = displayName().toLowerCase().replaceAll("\\s+", "");
        return "@" + (base.isEmpty() ? "chef" : base);
    }
}
