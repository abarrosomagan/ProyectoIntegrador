package com.sazon.proyectointegrador.model;

public class ChatThread {
    private final String id;
    private final String name;
    private final String lastMessage;
    private final String time;
    private final int unread;

    public ChatThread(String id, String name, String lastMessage, String time, int unread) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.unread = unread;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getUnread() { return unread; }
}