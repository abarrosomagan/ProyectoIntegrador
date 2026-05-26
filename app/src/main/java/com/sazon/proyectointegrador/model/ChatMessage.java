package com.sazon.proyectointegrador.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ChatMessage implements ChatItem {

    private final String text;
    private final boolean mine;
    private final long createdAt;
    private final String docId;
    private final boolean readByOther;
    /** Emoji → lista de uids que han reaccionado con ese emoji. */
    private final Map<String, List<String>> reactions;
    /** Si el mensaje es una receta compartida, su id. */
    private final String recipeId;
    /** Si fue editado por el autor. */
    private final boolean edited;

    public ChatMessage(String text, boolean mine) {
        this(text, mine, System.currentTimeMillis(), null, false, null, null, false);
    }

    public ChatMessage(String text, boolean mine, long createdAt) {
        this(text, mine, createdAt, null, false, null, null, false);
    }

    public ChatMessage(String text, boolean mine, long createdAt,
                       String docId, boolean readByOther) {
        this(text, mine, createdAt, docId, readByOther, null, null, false);
    }

    public ChatMessage(String text, boolean mine, long createdAt,
                       String docId, boolean readByOther,
                       Map<String, List<String>> reactions) {
        this(text, mine, createdAt, docId, readByOther, reactions, null, false);
    }

    public ChatMessage(String text, boolean mine, long createdAt,
                       String docId, boolean readByOther,
                       Map<String, List<String>> reactions,
                       String recipeId, boolean edited) {
        this.text = text;
        this.mine = mine;
        this.createdAt = createdAt;
        this.docId = docId;
        this.readByOther = readByOther;
        this.reactions = reactions == null
                ? Collections.emptyMap()
                : reactions;
        this.recipeId = recipeId;
        this.edited = edited;
    }

    public String getText() { return text; }
    public boolean isMine() { return mine; }
    public long getCreatedAt() { return createdAt; }
    public String getDocId() { return docId; }
    public boolean isReadByOther() { return readByOther; }
    public Map<String, List<String>> getReactions() { return reactions; }
    public String getRecipeId() { return recipeId; }
    public boolean isEdited() { return edited; }

    /** ¿He reaccionado yo con este emoji? */
    public boolean reactedByMe(String emoji, String myUid) {
        if (emoji == null || myUid == null) return false;
        List<String> uids = reactions.get(emoji);
        return uids != null && uids.contains(myUid);
    }
}
