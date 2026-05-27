package com.sazon.proyectointegrador.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Catálogo de ingredientes alimentado desde TheMealDB
 * (https://www.themealdb.com/api/json/v1/1/list.php?i=list).
 *
 * Cachea la lista en SharedPreferences durante 7 días y la sirve sin volver a
 * pegar a la API. Si la red falla y no hay caché, devuelve una lista seed
 * pequeña en español para que el autocompletado nunca aparezca vacío.
 */
public final class IngredientCatalog {

    private static final String PREFS = "ingredient_catalog";
    private static final String KEY_ITEMS = "items_v1";
    private static final String KEY_FETCHED_AT = "fetched_at";
    private static final String SEP = "\n";

    private static final String ENDPOINT =
            "https://www.themealdb.com/api/json/v1/1/list.php?i=list";

    /** Tiempo que la caché se considera fresca: 7 días. */
    private static final long CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000;

    /** Seed mínimo en español por si no hay red ni caché. */
    private static final List<String> SEED_ES = Collections.unmodifiableList(Arrays.asList(
            "Aceite de oliva", "Sal", "Pimienta negra", "Ajo", "Cebolla",
            "Tomate", "Pimiento rojo", "Pimiento verde", "Patata", "Zanahoria",
            "Calabacín", "Berenjena", "Lechuga", "Huevo", "Leche",
            "Mantequilla", "Queso", "Harina", "Azúcar", "Levadura",
            "Pollo", "Ternera", "Cerdo", "Atún", "Bacalao",
            "Gambas", "Arroz", "Pasta", "Pan rallado", "Limón",
            "Perejil", "Albahaca", "Romero", "Tomillo", "Orégano",
            "Vinagre", "Vino blanco", "Caldo de pollo", "Nata", "Chocolate"
    ));

    public interface Callback {
        void onReady(List<String> ingredients);
    }

    private IngredientCatalog() { }

    /**
     * Devuelve la lista de ingredientes, refrescando desde la red si la caché
     * está caducada. Siempre llama al callback en el hilo principal.
     */
    public static void loadAsync(Context ctx, Callback cb) {
        if (cb == null) return;
        SharedPreferences prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String cached = prefs.getString(KEY_ITEMS, null);
        long fetchedAt = prefs.getLong(KEY_FETCHED_AT, 0L);
        long age = System.currentTimeMillis() - fetchedAt;
        boolean fresca = cached != null && !cached.isEmpty() && age < CACHE_TTL_MS;

        if (cached != null && !cached.isEmpty()) {
            cb.onReady(deserialize(cached));
        } else {
            // No hay nada — devolvemos seed mientras se descarga
            cb.onReady(new ArrayList<>(SEED_ES));
        }

        if (fresca) return;

        // Refresh asincrono
        Executors.newSingleThreadExecutor().execute(() -> {
            List<String> fetched = fetchFromNetwork();
            if (fetched != null && !fetched.isEmpty()) {
                prefs.edit()
                        .putString(KEY_ITEMS, serialize(fetched))
                        .putLong(KEY_FETCHED_AT, System.currentTimeMillis())
                        .apply();
                new Handler(Looper.getMainLooper()).post(() -> cb.onReady(fetched));
            }
        });
    }

    private static List<String> fetchFromNetwork() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(ENDPOINT);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            JSONObject root = new JSONObject(sb.toString());
            JSONArray meals = root.optJSONArray("meals");
            if (meals == null) return null;

            ArrayList<String> out = new ArrayList<>(meals.length());
            for (int i = 0; i < meals.length(); i++) {
                JSONObject item = meals.optJSONObject(i);
                if (item == null) continue;
                String name = item.optString("strIngredient", "").trim();
                if (!name.isEmpty()) out.add(name);
            }
            Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
            return out;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String serialize(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            if (sb.length() > 0) sb.append(SEP);
            sb.append(s);
        }
        return sb.toString();
    }

    private static List<String> deserialize(String raw) {
        ArrayList<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String s : raw.split(SEP)) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
