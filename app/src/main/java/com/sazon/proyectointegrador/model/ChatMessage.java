package com.sazon.proyectointegrador.model;

public class ChatMessage implements ChatItem {

    private final String text;
    private final boolean mine;
    private final long createdAt;
    private final String docId;       // id del documento Firestore (para borrar)
    private final boolean readByOther; // ¿lo ha leído el otro participante?

    /** Constructor legacy: para mocks sin timestamp ni metadatos. */
    public ChatMessage(String text, boolean mine) {
        this(text, mine, System.currentTimeMillis(), null, false);
    }

    public ChatMessage(String text, boolean mine, long createdAt) {
        this(text, mine, createdAt, null, false);
    }

    public ChatMessage(String text, boolean mine, long createdAt,
                       String docId, boolean readByOther) {
        this.text = text;
        this.mine = mine;
        this.createdAt = createdAt;
        this.docId = docId;
        this.readByOther = readByOther;
    }

    public String getText() { return text; }
    public boolean isMine() { return mine; }
    public long getCreatedAt() { return createdAt; }
    public String getDocId() { return docId; }
    public boolean isReadByOther() { return readByOther; }
}
