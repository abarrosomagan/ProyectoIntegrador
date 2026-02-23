package com.sazon.proyectointegrador.model;

public class ChatMessage {
    private final String text;
    private final boolean mine;

    public ChatMessage(String text, boolean mine) {
        this.text = text;
        this.mine = mine;
    }

    public String getText() { return text; }
    public boolean isMine() { return mine; }
}