package com.sazon.proyectointegrador.ui.chats;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sazon.proyectointegrador.ChatActivity;
import com.sazon.proyectointegrador.model.ChatThread;
import com.sazon.proyectointegrador.adapters.ChatThreadAdapter;
import com.sazon.proyectointegrador.R;

import java.util.ArrayList;

public class ChatsController {

    private final AppCompatActivity a;

    private RecyclerView rvChats;
    private ChatThreadAdapter chatsAdapter;
    private final ArrayList<ChatThread> chatsData = new ArrayList<>();

    public ChatsController(AppCompatActivity activity) {
        this.a = activity;
    }

    public void init() {
        bind();
        setupRecycler();
        loadMock();
    }

    private void bind() {
        rvChats = a.findViewById(R.id.rvChats);
    }

    private void setupRecycler() {
        if (rvChats == null) return;
        rvChats.setLayoutManager(new LinearLayoutManager(a));
    }

    private void loadMock() {
        if (rvChats == null) return;

        chatsData.clear();
        chatsData.add(new ChatThread("1", "María", "¿Me pasas la receta?", "12:40", 2));
        chatsData.add(new ChatThread("2", "Alex", "Buenísima 😄", "ayer", 0));
        chatsData.add(new ChatThread("3", "Sofía", "¿Qué ingredientes usaste?", "lun", 1));

        chatsAdapter = new ChatThreadAdapter(chatsData, chat -> {
            Intent i = new Intent(a, ChatActivity.class);
            i.putExtra(ChatActivity.EXTRA_CHAT_ID, chat.getId());
            i.putExtra(ChatActivity.EXTRA_CHAT_NAME, chat.getName());
            a.startActivity(i);
        });

        rvChats.setAdapter(chatsAdapter);
    }
}