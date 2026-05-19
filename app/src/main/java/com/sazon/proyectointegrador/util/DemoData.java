package com.sazon.proyectointegrador.util;

import com.sazon.proyectointegrador.model.ChatDateHeader;
import com.sazon.proyectointegrador.model.ChatItem;
import com.sazon.proyectointegrador.model.ChatMessage;
import com.sazon.proyectointegrador.model.ChatThread;
import com.sazon.proyectointegrador.model.Publicacion;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Datos de demostración para que la app se vea poblada sin depender de
 * que Firestore ya tenga contenido. Los ids de los chats demo empiezan
 * por "demo:" para que ChatActivity pueda detectarlos y no se suscriba
 * a Firestore con un id que no existe.
 *
 * Si quieres apagarlo en una build "limpia", pon ENABLED a false.
 */
public final class DemoData {

    public static final boolean ENABLED = true;

    public static final String DEMO_PREFIX = "demo:";

    private DemoData() { /* util estática */ }

    public static boolean isDemoChatId(String chatId) {
        return chatId != null && chatId.startsWith(DEMO_PREFIX);
    }

    // ============================
    // ===== Lista de chats =======
    // ============================

    public static List<ChatThread> chats() {
        List<ChatThread> list = new ArrayList<>();
        list.add(new ChatThread("demo:maria",
                "María", "¡Gracias por la receta!", "12:40", 2));
        list.add(new ChatThread("demo:alex",
                "Alex", "Buenísima 😄 perfecta para domingo", "11:25", 0));
        list.add(new ChatThread("demo:dani",
                "Dani", "👌 el rey de la cena", "08:31", 1));
        list.add(new ChatThread("demo:sofia",
                "Sofía", "Voy a pillarme uno entonces", "lun", 0));
        list.add(new ChatThread("demo:javier",
                "Javier", "¿Te ha quedado bien la masa?", "12 may", 0));
        return list;
    }

    // ============================
    // ===== Mensajes por chat ====
    // ============================

    public static List<ChatItem> messages(String chatId) {
        ArrayList<ChatItem> items = new ArrayList<>();
        long now = System.currentTimeMillis();
        long startOfToday = startOfDayMs(now);

        if ("demo:maria".equals(chatId)) {
            long ayer = startOfToday - day(1);
            items.add(new ChatDateHeader("Ayer"));
            items.add(new ChatMessage(
                    "¡Esa tortilla que subiste pinta increíble! 😍", false,
                    ayer + h(19) + m(28)));
            items.add(new ChatMessage(
                    "¿La haces con cebolla?", false,
                    ayer + h(19) + m(28) + 30_000));
            items.add(new ChatMessage(
                    "Con cebolla pochada lenta sí, es la clave", true,
                    ayer + h(19) + m(32)));
            items.add(new ChatMessage(
                    "Y huevo M ecológico, hace bastante diferencia.", true,
                    ayer + h(19) + m(33)));

            items.add(new ChatDateHeader("Hoy"));
            items.add(new ChatMessage(
                    "¿Me pasas la receta detallada cuando puedas?", false,
                    startOfToday + h(10) + m(15)));
            items.add(new ChatMessage(
                    "Sí, te la mando esta tarde con fotos del paso a paso", true,
                    startOfToday + h(10) + m(22)));
            items.add(new ChatMessage(
                    "¡Gracias por la receta!", false,
                    startOfToday + h(12) + m(40)));
            return items;
        }

        if ("demo:alex".equals(chatId)) {
            items.add(new ChatDateHeader("Hoy"));
            items.add(new ChatMessage(
                    "Probé tus croquetas anoche", false,
                    startOfToday + h(9) + m(45)));
            items.add(new ChatMessage(
                    "¿Qué tal salieron?", true,
                    startOfToday + h(9) + m(58)));
            items.add(new ChatMessage(
                    "Buenísima 😄 perfecta para domingo", false,
                    startOfToday + h(11) + m(25)));
            return items;
        }

        if ("demo:dani".equals(chatId)) {
            items.add(new ChatDateHeader("Hoy"));
            items.add(new ChatMessage(
                    "Tu risotto fue el rey de la cena anoche", false,
                    startOfToday + h(8) + m(28)));
            items.add(new ChatMessage(
                    "Me alegro mucho 😊", true,
                    startOfToday + h(8) + m(30)));
            items.add(new ChatMessage(
                    "👌 el rey de la cena", false,
                    startOfToday + h(8) + m(31)));
            return items;
        }

        if ("demo:sofia".equals(chatId)) {
            long lunes = startOfToday - day(daysSinceMonday(now));
            items.add(new ChatDateHeader(labelFor(lunes)));
            items.add(new ChatMessage(
                    "¿Qué horno usas? El mío reparte mal", false,
                    lunes + h(18) + m(10)));
            items.add(new ChatMessage(
                    "El Bosch de siempre, calor envolvente.", true,
                    lunes + h(18) + m(14)));
            items.add(new ChatMessage(
                    "Voy a pillarme uno entonces", false,
                    lunes + h(18) + m(16)));
            return items;
        }

        if ("demo:javier".equals(chatId)) {
            long hace7 = startOfToday - day(7);
            items.add(new ChatDateHeader(labelFor(hace7)));
            items.add(new ChatMessage(
                    "Vi tu post del bizcocho de zanahoria, te quedó precioso", false,
                    hace7 + h(20) + m(0)));
            items.add(new ChatMessage(
                    "Gracias Javi! Esta semana voy a hacer la versión vegana", true,
                    hace7 + h(20) + m(5)));
            items.add(new ChatMessage(
                    "¿Te ha quedado bien la masa?", false,
                    hace7 + h(20) + m(40)));
            return items;
        }

        // chat demo desconocido → estado vacío
        return items;
    }

    // ============================
    // ===== Perfil — recetas =====
    // ============================

    public static ArrayList<Publicacion> recetasPropias() {
        ArrayList<Publicacion> r = new ArrayList<>();
        long now = System.currentTimeMillis();

        r.add(rec("p01","user_me","Fernando", now - h(2),
                "Tortilla de patatas jugosa", 312));
        r.add(rec("p02","user_me","Fernando", now - h(6),
                "Pasta cremosa con setas", 178));
        r.add(rec("p03","user_me","Fernando", now - day(1),
                "Pollo al curry rojo", 224));
        r.add(rec("p04","user_me","Fernando", now - day(2),
                "Ensalada de quinoa y aguacate", 96));
        r.add(rec("p05","user_me","Fernando", now - day(3),
                "Salmón al horno con eneldo", 145));
        r.add(rec("p06","user_me","Fernando", now - day(4),
                "Croquetas de jamón ibérico", 410));
        r.add(rec("p07","user_me","Fernando", now - day(6),
                "Sopa fría de tomate y albahaca", 88));
        r.add(rec("p08","user_me","Fernando", now - day(8),
                "Tarta de queso al horno", 502));
        r.add(rec("p09","user_me","Fernando", now - day(10),
                "Bizcocho de zanahoria casero", 267));
        r.add(rec("p10","user_me","Fernando", now - day(12),
                "Risotto de boletus", 198));
        r.add(rec("p11","user_me","Fernando", now - day(15),
                "Bowl asiático con tofu", 73));
        r.add(rec("p12","user_me","Fernando", now - day(20),
                "Hummus de remolacha", 64));
        return r;
    }

    public static ArrayList<Publicacion> recetasGuardadas() {
        ArrayList<Publicacion> r = new ArrayList<>();
        long now = System.currentTimeMillis();

        r.add(rec("s01","user_maria","María", now - h(4),
                "Galette de manzana", 156));
        r.add(rec("s02","user_alex","Alex", now - day(1),
                "Ramen casero con miso", 289));
        r.add(rec("s03","user_sofia","Sofía", now - day(2),
                "Smoothie verde detox", 47));
        r.add(rec("s04","user_dani","Dani", now - day(3),
                "Pizza napolitana de masa madre", 612));
        r.add(rec("s05","user_maria","María", now - day(5),
                "Coca de espinacas y piñones", 102));
        r.add(rec("s06","user_alex","Alex", now - day(9),
                "Tarte tatin clásica", 234));
        return r;
    }

    // ============================
    // ===== Stats =====
    // ============================

    public static int demoFollowers() { return 245; }
    public static int demoFollowing() { return 87; }

    // ============================
    // ===== Helpers privados =====
    // ============================

    private static Publicacion rec(String id, String authorId, String autor,
                                   long createdAt, String titulo, int likes) {
        return new Publicacion(id, authorId, autor, createdAt, titulo, "", "", likes, false);
    }

    private static long h(int hours) { return hours * 60L * 60_000L; }
    private static long m(int minutes) { return minutes * 60_000L; }
    private static long day(int days) { return days * 24L * 60L * 60_000L; }

    private static long startOfDayMs(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static int daysSinceMonday(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        int dow = c.get(Calendar.DAY_OF_WEEK); // domingo=1, lunes=2, ...
        int dist = (dow + 5) % 7; // distancia hasta lunes
        // si hoy es lunes, dist=0 → pongo 0 (mismo día) → no quiero eso para demo
        return dist == 0 ? 7 : dist;
    }

    /**
     * Devuelve la misma etiqueta que ChatActivity.formatDayLabel para fechas
     * "lejanas" (Hoy/Ayer no aplican aquí porque ya usamos esos).
     * Mantengo la lógica acoplada para que el chip de fecha aparezca igual
     * que lo que pinta el flujo normal de Firestore.
     */
    private static String labelFor(long timeMs) {
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(timeMs);
        Calendar now = Calendar.getInstance();

        long diff = now.getTimeInMillis() - then.getTimeInMillis();
        if (diff < 7L * 24 * 60 * 60 * 1000
                && then.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            return new java.text.SimpleDateFormat("EEE",
                    new java.util.Locale("es", "ES")).format(then.getTime());
        }
        if (then.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            return new java.text.SimpleDateFormat("d 'de' MMMM",
                    new java.util.Locale("es", "ES")).format(then.getTime());
        }
        return new java.text.SimpleDateFormat("d MMM yyyy",
                new java.util.Locale("es", "ES")).format(then.getTime());
    }
}
