package com.sazon.proyectointegrador.model;

/**
 * Cabecera flotante con la fecha entre grupos de mensajes:
 *   "Hoy", "Ayer", "12 may", "12/05/2024"...
 */
public class ChatDateHeader implements ChatItem {

    private final String label;

    public ChatDateHeader(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
