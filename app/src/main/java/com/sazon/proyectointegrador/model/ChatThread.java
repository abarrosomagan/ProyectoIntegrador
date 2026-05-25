package com.sazon.proyectointegrador.model;

public class ChatThread {
    private final String id;
    private final String name;
    private final String lastMessage;
    private final String time;
    private final int unread;
    private final boolean presenceActive;
    private final boolean lastIsMine;
    private final boolean lastReadByOther;
    private final boolean muted;
    private final boolean pinned;

    public ChatThread(String id, String name, String lastMessage, String time, int unread) {
        this(id, name, lastMessage, time, unread, false, false, false, false, false);
    }

    public ChatThread(String id, String name, String lastMessage, String time, int unread,
                      boolean presenceActive, boolean lastIsMine, boolean lastReadByOther) {
        this(id, name, lastMessage, time, unread,
                presenceActive, lastIsMine, lastReadByOther, false, false);
    }

    public ChatThread(String id, String name, String lastMessage, String time, int unread,
                      boolean presenceActive, boolean lastIsMine, boolean lastReadByOther,
                      boolean muted, boolean pinned) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.unread = unread;
        this.presenceActive = presenceActive;
        this.lastIsMine = lastIsMine;
        this.lastReadByOther = lastReadByOther;
        this.muted = muted;
        this.pinned = pinned;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getUnread() { return unread; }
    public boolean isPresenceActive() { return presenceActive; }
    public boolean isLastIsMine() { return lastIsMine; }
    public boolean isLastReadByOther() { return lastReadByOther; }
    public boolean isMuted() { return muted; }
    public boolean isPinned() { return pinned; }
}
