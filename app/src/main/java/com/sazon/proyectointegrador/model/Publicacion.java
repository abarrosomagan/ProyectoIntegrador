package com.sazon.proyectointegrador.model;

public class Publicacion {

    private final String autor;
    private final String tiempo;
    private final String titulo;
    private final int likes;
    private boolean guardada;

    public Publicacion(String autor, String tiempo, String titulo, int likes, boolean guardada) {
        this.autor = autor;
        this.tiempo = tiempo;
        this.titulo = titulo;
        this.likes = likes;
        this.guardada = guardada;
    }

    public String getAutor() { return autor; }
    public String getTiempo() { return tiempo; }
    public String getTitulo() { return titulo; }
    public int getLikes() { return likes; }
    public boolean isGuardada() { return guardada; }

    public void setGuardada(boolean guardada) {
        this.guardada = guardada;
    }
}