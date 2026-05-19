package com.sazon.proyectointegrador.model;

public class ChatMessage implements ChatItem {

    private final String text;
    private final boolean mine;
    private final long createdAt;

    /** Constructor legacy: para mocks sin timestamp. */
    public ChatMessage(String text, boolean mine) {
        this(text, mine, System.currentTimeMillis());
    }

    public ChatMessage(String text, boolean mine, long createdAt) {
        this.text = text;
        this.mine = mine;
        this.createdAt = createdAt;
    }

    public String getText() { return text; }
    public boolean isMine() { return mine; }
    public long getCreatedAt() { return createdAt; }
}
