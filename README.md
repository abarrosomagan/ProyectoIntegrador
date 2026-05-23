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
