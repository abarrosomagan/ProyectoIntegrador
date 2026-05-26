# Sazón

Sazón es una aplicación Android para personas aficionadas a la cocina que quieren publicar recetas, descubrir platos de otros usuarios, guardar ideas, seguir perfiles y conversar dentro de una comunidad gastronómica.

El proyecto forma parte del **Proyecto Integrador** del ciclo de **Desarrollo de Aplicaciones Multiplataforma (DAM)** y se desarrolla de forma incremental, con Firebase como base de autenticación, datos en tiempo real y almacenamiento.

---

## Diseño en Figma

El diseño de la aplicación se ha realizado en Figma y sirve como referencia visual para la implementación en Android.

https://www.figma.com/design/yHjedo6Y4G4Co4WEa7LY2A/Sin-t%C3%ADtulo?node-id=0-1&p=f&t=fkQQFC4GstUwoPPk-0

---

## Autores

**Alejandro Barroso**  
Developer y organizador del repositorio

**Fernando Cecilia**  
Developer y organizador del proyecto

Ambos autores participan en el diseño, la planificación y el desarrollo técnico de la aplicación.

---

## Reparto de tareas hasta la entrega del 9 de junio

> **Contexto:** la app la quiero dejar pulida para el **1 de junio**, dejando una semana de margen para integrar lo de Alejandro y preparar la defensa. El núcleo (Auth, feed, recetas, chat, perfil, seguidores) ya está cerrado.
>
> Para que **Alejandro** pueda aportar código real sin pisar el avance central, su trabajo se reparte en **pantallas nuevas totalmente aisladas**: cada tarea es una `Activity` o pantalla independiente con sus propios layouts y, como mucho, lecturas/escrituras en colecciones de Firestore que ya tienen reglas listas. Trabaja en la rama `Alejandro` (salida de `Fernando`) y entra a `Fernando` vía Pull Request.

### Calendario

| Fecha | Hito |
|---|---|
| 25 mayo | Cierre del reparto (este documento) |
| **1 junio** | Tareas 1–4 de Alejandro mergeadas en `Fernando`. App pulida por Fernando. |
| 1 – 7 junio | Tareas 5–8 de Alejandro y pulido final conjunto. |
| 8 junio | Ensayo de defensa con la app final. |
| **9 junio** | Entrega. |

### Tareas asignadas a Alejandro Barroso

Todas son tareas de desarrollo. Cada una es una pantalla nueva propia: layout XML + Activity Java en su propio paquete `com.sazon.proyectointegrador.alejandro.<feature>`. Fernando deja los puntos de enlace (botones en headers / menú) ya cableados con un `Intent` apuntando a la Activity vacía para que Alejandro solo tenga que rellenarla.

| # | Tarea | Qué tiene que hacer | Fecha tope |
|---|---|---|---|
| 1 | **`AboutActivity`** — Pantalla "Acerca de". | Layout con logo, versión (`BuildConfig.VERSION_NAME`), autores, link a GitHub y a `docs/legal/privacy.md`. Toolbar con flecha atrás. Sin Firestore. | **28 mayo** |
| 2 | **`HelpActivity`** — Centro de ayuda con FAQ. | `RecyclerView` con 8-12 preguntas-respuesta expandibles. Lista hardcodeada en un `ArrayList`. Sin Firestore. | **29 mayo** |
| 3 | **`SettingsActivity`** — Pantalla de ajustes. | Switches y filas para: silenciar todas las notificaciones (`SharedPreferences`), borrar borradores de chat (`SharedPreferences "chat_drafts"` que ya existe), cerrar sesión (`SessionManager.logout()`), borrar cuenta (con `AlertDialog` de confirmación → `FirebaseAuth.getCurrentUser().delete()`). | **31 mayo** |
| 4 | **`OnboardingActivity`** — 3 slides de bienvenida en el primer arranque. | `ViewPager2` con 3 fragments estáticos (texto + imagen + indicador de paso). Botón "Empezar" en el último slide que abre `LoginActivity` y guarda un flag `onboarding_done=true` en `SharedPreferences`. `SplashActivity` decide si mostrarla. | **1 junio** |
| 5 | **`ReportActivity`** — Reportar receta o usuario. | Pantalla con dropdown de motivo (spam, contenido inapropiado, abuso, otros), `EditText` para detalle, botón "Enviar". Escribe en la colección `/reports` (las reglas ya permiten `create` si `reporterId == uid`). Se abre desde el menú de receta y del perfil. | **3 junio** |
| 6 | **`StatsActivity`** — Estadísticas del perfil propio. | Activity que lee `users/{miUid}` y sus subcolecciones / consultas: total recetas publicadas, total likes recibidos sumando, total guardados que tengo, seguidores y siguiendo. Mostrar 4-6 tarjetas con número grande. Acceso desde un botón en el header del perfil propio. | **4 junio** |
| 7 | **`ExploreActivity`** — Explorar por categorías. | Chips horizontales (`Postres`, `Vegano`, `Rápido`, `Saludable`, `Carnes`, `Pescado`) que filtran un `RecyclerView` de recetas mediante una query `recipes.whereArrayContains("tagsList", ...)`. Si el campo no existe en la receta, filtra en cliente leyendo `tags` (string). Acceso desde un botón nuevo en el feed que Fernando enlazará. | **6 junio** |
| 8 | **`SearchActivity`** — Buscador global. | Buscador único con dos tabs (`TabLayout`): "Recetas" y "Usuarios". Recetas: query prefix-match sobre `recipes.titulo`. Usuarios: query prefix-match sobre `users.name`. Resultados en `RecyclerView` reutilizando `PublicacionAdapter` y `UserListAdapter`. Acceso desde el icono de lupa que Fernando añade al feed. | **7 junio** |

**Reglas básicas para Alejandro:**

- Trabajar siempre en rama `Alejandro` (salida de `Fernando`). Cada tarea se cierra con un Pull Request a `Fernando`.
- Crear todo el código nuevo dentro de **`com.sazon.proyectointegrador.alejandro.<feature>`** (paquete propio por tarea: `alejandro.about`, `alejandro.help`, `alejandro.settings`, etc.).
- Los layouts nuevos viven en `app/src/main/res/layout/` con prefijo `alej_` (ej. `alej_activity_about.xml`, `alej_item_faq.xml`).
- Puede usar libremente `SessionManager`, `RecipeImageHelper`, los adapters existentes y los modelos.
- **No tocar** archivos existentes en `com.sazon.proyectointegrador` salvo añadir su `<activity>` al `AndroidManifest.xml`. Los puntos de enlace los cablea Fernando.
- **No tocar** `firestore.rules`, `firebase.json`, `build.gradle.kts`, `colors.xml`, `themes.xml`, ni los layouts existentes.
- Cualquier nueva colección que necesite (no debería) debe pedirse a Fernando para añadir su regla.

### Tareas reservadas a Fernando Cecilia hasta el 1 de junio

Pulido final del código y QA propio. Ningún feature grande nuevo; solo redondear lo ya implementado.

- Revisión visual y ajustes finos del feed, chats, perfil y recetas.
- Bugs reportados por Alejandro durante el QA.
- Limpieza de logs y código muerto.
- Comprobación de las reglas de Firestore en producción.
- Mantenimiento del README y de `docs/`.
- Etiquetar la versión `v1.0` el 1 de junio.

---

## Cuentas de prueba

Para revisar la app sin registrarse hay cuentas de demostración en Firebase Authentication. Son cuentas de prueba con datos ficticios:

| Correo | Contraseña |
|---|---|
| `fer1@test.com` | `123456` |
| `fer2@test.com` | `123456` |
| `test1@sazon.com` | `123456` |

Pareja recomendada para probar el chat en dos dispositivos: `fer1@test.com` y `fer2@test.com`.

---

## Estado actual

El proyecto ya tiene implementado el flujo principal de una red social de recetas:

- **Autenticación** con Firebase Auth mediante email y contraseña.
- **Recuperación de contraseña** por correo.
- **Registro** con creación de usuario y perfil base en Firestore.
- **Feed de recetas** alimentado desde Firestore, con buscador y pull-to-refresh.
- **Creación de recetas** desde la app, con foto opcional comprimida y guardada en Firestore.
- **Edición de recetas propias** con actualización de título, descripción y foto.
- **Detalle de receta** con acciones de like, guardar, comentar, compartir, eliminar y acceso al perfil del autor.
- **Comentarios en recetas** en tiempo real mediante subcolecciones de Firestore.
- **Likes y guardados persistentes** en Firestore, con estado por usuario en feed y detalle.
- **Perfil propio** con foto comprimida guardada en Firestore, nombre, biografía, contadores y pestañas de recetas propias y guardadas.
- **Perfil ajeno** con carga de datos reales y botón de seguir o dejar de seguir.
- **Sistema de seguimiento** con contadores y listas navegables de seguidores y siguiendo.
- **Chats en tiempo real** con Firestore, lista de conversaciones y búsqueda de usuarios por correo.
- **Mensajes** con burbujas diferenciadas, separadores de fecha, hora, lectura básica, presencia, indicador de escribiendo y aviso interno de no leído.
- **Modo demo** para mostrar contenido cuando todavía no hay datos reales.

---

## Funcionalidades pendientes

Los siguientes puntos siguen abiertos para completar la experiencia final:

- Recorte de imagen de avatar.
- Notificaciones push.
- Pantalla de ajustes completa.
- Login con Google.
- Revisión de reglas de Firebase para un entorno de producción.

---

## Pantallas diseñadas

El diseño cubre las principales pantallas de navegación de la app.

### Pantalla de inicio

![Pantalla de inicio](images/splash.png)

### Registro

![Registro de usuario](images/register.png)

### Inicio de sesión

![Inicio de sesión](images/login.png)

### Feed principal

![Feed principal](images/feed.png)

### Perfil

![Perfil de usuario](images/profile.png)

### Mensajes

![Mensajes](images/messages.png)

### Recetas guardadas

![Recetas guardadas](images/saved.png)

---

## Arquitectura

La aplicación usa una arquitectura ligera basada en **Activities**, **Controllers**, **Adapters**, modelos y repositorios auxiliares.

```text
app/src/main/java/com/sazon/proyectointegrador/
 ├── SplashActivity
 ├── LoginActivity
 ├── RegisterActivity
 ├── MainActivity
 ├── ChatActivity
 ├── ProfileActivity
 ├── FollowListActivity
 ├── CreateRecipeActivity
 ├── RecipeDetailActivity
 ├── ui/
 │    ├── feed/FeedController
 │    ├── chats/ChatsController
 │    └── profile/ProfileController
 ├── adapters/
 │    ├── ChatThreadAdapter
 │    ├── ChatMessageAdapter
 │    ├── PublicacionAdapter
 │    ├── PublicacionGridAdapter
 │    ├── RecipeCommentAdapter
 │    └── UserListAdapter
 ├── model/
 │    ├── Publicacion
 │    ├── ChatThread
 │    ├── ChatMessage
 │    ├── ChatItem
 │    ├── ChatDateHeader
 │    ├── RecipeComment
 │    └── UserListItem
 └── util/
      ├── SessionManager
      ├── RecipeRepository
      ├── FollowRepository
      ├── AvatarHelper
      ├── RecipeImageHelper
      ├── DemoData
      └── SimpleTextWatcher
```

---

## Modelo de datos

### Firestore

```text
users/{uid}
  uid, name, email, bio, avatarUrl, createdAt
  followers, following, recipes

users/{uid}/followers/{followerUid}
users/{uid}/following/{followingUid}
users/{uid}/saved/{recipeId}

recipes/{recipeId}
  authorId, autor, titulo, descripcion, imageUrl, likes, createdAt

imageUrl guarda una URL antigua o una imagen JPEG comprimida en formato data URL.

recipes/{recipeId}/likes/{uid}
recipes/{recipeId}/comments/{commentId}
  recipeId, authorId, authorName, text, createdAt

chats/{chatId}
  participants
  participantsNames
  lastMessage, lastSenderId, lastMessageAt
  lastReadAt.{uid}
  presence.{uid}.active, presence.{uid}.typing, presence.{uid}.lastSeen

chats/{chatId}/messages/{msgId}
  text, senderId, createdAt, readAt
```

## Tecnologías

- Java
- Android SDK
- Firebase Auth
- Cloud Firestore
- Firebase Analytics
- Material Design 3
- AndroidX
- Glide
- Gradle Kotlin DSL
- Git y GitHub
- Figma

---

## Instalación

```bash
git clone https://github.com/abarrosomagan/ProyectoIntegrador.git
```

1. Abrir el proyecto en Android Studio.
2. Sincronizar Gradle.
3. Usar un emulador con Google Play Services o un dispositivo Android físico.
4. Configurar Firebase para el proyecto correspondiente.
5. Habilitar Authentication con email y contraseña.
6. Habilitar Firestore Database.
7. Ejecutar la app desde Android Studio.

---

## Seguridad Firebase

La configuración versionada está en:

- `firestore.rules`
- `firestore.indexes.json`
- `firebase.json`
- `docs/firebase-security.md`
- `docs/demo-checklist.md`
- `scripts/seed-demo.mjs`

El proyecto no usa Firebase Storage. Las imágenes se comprimen y se guardan en Firestore para mantener el despliegue dentro de opciones gratuitas.

## Demo

La guía de preparación para presentar la app está en `docs/demo-checklist.md`.

Para cargar datos de demo en Firebase:

```bash
node scripts/seed-demo.mjs
```

## Roadmap

1. Añadir pantalla de ajustes completa.
2. Mejorar búsqueda y exploración de recetas.
3. Añadir categorías, dificultad y tiempo de preparación.
4. Valorar notificaciones push si el proyecto final lo exige.

## Próximos avances propuestos

### Normales

- Pantalla de ajustes: perfil, sesión, privacidad básica y borrar cuenta.
- Búsqueda por título, autor y texto de receta con filtros rápidos.
- Categorías de recetas: dulce, salado, rápido, saludable, vegano, horno.
- Campos de receta más estructurados: tiempo, dificultad y raciones.
- Validación visual de pantallas principales antes de entrega.

### Ambiciosos

- Ingredientes y pasos estructurados por receta.
- Modo cocinar paso a paso.
- Colecciones personales de recetas.
- Ranking de chefs y recetas populares.
- Planificador semanal y lista de la compra.

---

## Licencia

Proyecto desarrollado con fines académicos.

El contenido puede utilizarse como referencia educativa citando a los autores.
