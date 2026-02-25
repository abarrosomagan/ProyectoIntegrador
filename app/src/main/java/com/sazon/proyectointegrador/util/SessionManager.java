package com.sazon.proyectointegrador.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF = "session_prefs";
    private static final String KEY_LOGGED = "logged_in";
    private static final String KEY_UID = "user_id";
    private static final String KEY_NAME = "user_name";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return sp.getBoolean("logged_in", false);
    }

    public void login(String userId, String userName) {
        sp.edit()
                .putBoolean("logged_in", true)
                .putString("user_id", userId)
                .putString("user_name", userName)
                .commit();
    }

    public void logout() {
        sp.edit()
                .putBoolean("logged_in", false)
                .remove("user_id")
                .remove("user_name")
                .commit(); // inmediato
    }
    public String getUserId() {
        return sp.getString(KEY_UID, "");
    }

    public String getUserName() {
        return sp.getString(KEY_NAME, "");
    }
}