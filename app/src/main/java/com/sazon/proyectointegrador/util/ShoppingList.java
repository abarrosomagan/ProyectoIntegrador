package com.sazon.proyectointegrador.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de la compra local del usuario, persistida en SharedPreferences.
 * Cada item lleva texto, origen (nombre de receta) y estado marcado/no marcado.
 *
 * Formato JSON serializado:
 *   [
 *     { "t": "200 g de harina", "r": "Bizcocho de limón", "c": false },
 *     { "t": "3 huevos",        "r": "Bizcocho de limón", "c": true  }
 *   ]
 */
public final class ShoppingList {

    private static final String PREFS = "shopping_list";
    private static final String KEY = "items_v1";

    public static class Item {
        public String text;
        public String recipe;
        public boolean checked;

        public Item(String text, String recipe, boolean checked) {
            this.text = text;
            this.recipe = recipe;
            this.checked = checked;
        }
    }

    private ShoppingList() { }

    public static List<Item> getAll(Context ctx) {
        SharedPreferences prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY, "[]");
        ArrayList<Item> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Item(
                        o.optString("t", ""),
                        o.optString("r", ""),
                        o.optBoolean("c", false)));
            }
        } catch (Exception ignored) { }
        return out;
    }

    public static void addAll(Context ctx, List<String> ingredients, String fromRecipe) {
        if (ingredients == null || ingredients.isEmpty()) return;
        List<Item> current = getAll(ctx);
        for (String ing : ingredients) {
            if (ing == null) continue;
            String t = ing.trim();
            if (t.isEmpty()) continue;
            // Evitar duplicados exactos del mismo texto si aún no están marcados
            boolean ya = false;
            for (Item it : current) {
                if (it.text.equalsIgnoreCase(t) && !it.checked) { ya = true; break; }
            }
            if (!ya) current.add(new Item(t, fromRecipe == null ? "" : fromRecipe, false));
        }
        persist(ctx, current);
    }

    public static void update(Context ctx, List<Item> items) {
        persist(ctx, items);
    }

    public static void clear(Context ctx) {
        ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY).apply();
    }

    public static void clearChecked(Context ctx) {
        List<Item> all = getAll(ctx);
        List<Item> kept = new ArrayList<>();
        for (Item it : all) if (!it.checked) kept.add(it);
        persist(ctx, kept);
    }

    private static void persist(Context ctx, List<Item> items) {
        JSONArray arr = new JSONArray();
        for (Item it : items) {
            try {
                JSONObject o = new JSONObject();
                o.put("t", it.text);
                o.put("r", it.recipe);
                o.put("c", it.checked);
                arr.put(o);
            } catch (Exception ignored) { }
        }
        ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, arr.toString()).apply();
    }
}
