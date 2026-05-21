# Sazón

Sazón es un **proyecto académico en desarrollo** que consiste en una aplicación móvil Android orientada a la **gestión, publicación y descubrimiento de recetas**, incorporando un componente social mediante un feed visual, perfiles de usuario, sistema de guardados y mensajería.

El proyecto se desarrolla de forma progresiva y se irá **actualizando poco a poco**, tanto a nivel de diseño como de implementación, siguiendo un flujo de trabajo incremental.

---

## Diseño en Figma

El diseño completo de la aplicación ha sido realizado en Figma y sirve como referencia visual y funcional para la implementación del proyecto.

https://www.figma.com/design/yHjedo6Y4G4Co4WEa7LY2A/Sin-t%C3%ADtulo?node-id=0-1&p=f&t=fkQQFC4GstUwoPPk-0

---

## Contexto del proyecto

Este proyecto forma parte del **Proyecto Integrador** del ciclo formativo de **Desarrollo de Aplicaciones Multiplataforma (DAM)**.

El objetivo es aplicar de manera práctica y conjunta conocimientos de:
- Diseño de interfaces
- Programación Android
- Organización y planificación de proyectos
- Control de versiones con Git
- Trabajo colaborativo

---

## Autores y roles

**Alejandro Barroso**  
Developer y organizador del repositorio

**Fernando Cecilia**  
Developer y organizador del proyecto

Ambos autores participan en el diseño, la planificación y el desarrollo técnico de la aplicación.

---

## Cuentas de prueba

Para revisar la app sin tener que registrarse, hay tres cuentas de demostración cargadas en Firebase Authentication. **Son cuentas de prueba con datos ficticios**, no contienen información real:

| Correo | Contraseña |
|---|---|
| `fer1@test.com` | `123456` |
| `fer2@test.com` | `123456` |
| `test1@sazon.com` | `123456` |

> Pareja recomendada para probar el chat en dos dispositivos a la vez: `fer1@test.com` ↔ `fer2@test.com`. Las usamos para enseñar mensajes en tiempo real, separadores de fecha y *read receipts*.

---

## Estado actual

El proyecto se encuentra en una fase avanzada con la mayor parte del flujo principal funcional sobre Firebase. A día de hoy ya están implementados de forma real:

- **Autenticación** con Firebase Auth (email/contraseña), persistencia de sesión y recuperación de contraseña por correo.
- **Perfil real**: nombre, biografía, **foto de perfil** (Firebase Storage), rango de chef calculado a partir del número de recetas y contadores reales de seguidores y siguiendo.
- **Recetas reales**: creación desde la app (pantalla *Nueva receta*) y persistencia en Firestore. Cada receta tiene autor, título, descripción, likes y fecha.
- **Feed real** alimentado desde Firestore con buscador, *pull-to-refresh* y fallback a contenido demo cuando aún no hay recetas reales.
- **Likes y guardados persistentes**: cada toque sobre el corazón o la estrella se guarda en Firestore en `recipes/{id}/likes/{uid}` y `users/{uid}/saved/{recipeId}`.
- **Sistema de seguimiento** con `FollowRepository`: botón *Seguir / Siguiendo* sobre los perfiles ajenos y actualización transaccional de contadores en ambos usuarios.
- **Mensajería en tiempo real** con Firestore: listado de conversaciones, mensajes con separadores de fecha, burbujas asimétricas y diálogo *Nueva conversación* buscando por correo.
- **Perfil ajeno** (`ProfileActivity`): carga datos reales del usuario al pinchar sobre el autor de una receta.
- **Navegación inferior** con indicador *pill* estilo Material 3 y **FAB** flotante para crear receta.

Quedan elementos no implementados todavía (documentados al final del README).

---

## Diseño de la aplicación (Figma)

El diseño de la aplicación ha sido realizado previamente en **Figma**, y constituye la referencia principal para la implementación en Android.

El diseño define:
- Tema claro con paleta cromática cálida
- Jerarquía visual clara y consistente
- Espaciados homogéneos
- Componentes reutilizables
- Resolución base de diseño: **720 × 1280**

### Pantallas diseñadas

A continuación se muestran las principales pantallas diseñadas en Figma, que definen el flujo de navegación y la estructura visual de la aplicación.

#### Pantalla de inicio (splash)
![Pantalla de inicio](images/splash.png)

#### Registro de usuario
![Registro de usuario](images/register.png)

#### Inicio de sesión
![Inicio de sesión](images/login.png)

#### Feed principal de recetas
![Feed principal](images/feed.png)

#### Perfil de usuario
![Perfil de usuario](images/profile.png)

#### Mensajes
![Mensajes](images/messages.png)

#### Recetas guardadas
![Recetas guardadas](images/saved.png)

El diseño cubre el flujo completo de navegación principal de la aplicación, desde la pantalla de inicio hasta la interacción social y la gestión de contenido por parte del usuario.

---


## Funcionalidades

### Implementadas

- **Splash** con redirección automática a Main si la sesión Firebase está vigente.
- **Login** real (Firebase Auth) con mensajes de error en castellano y recuperar contraseña por email.
- **Registro** con creación del documento de usuario en Firestore + `displayName` en Auth.
- **Feed** desde Firestore con buscador, *pull-to-refresh* y avatar del usuario logueado en la cabecera.
- **Perfil propio** con avatar editable (Storage), nombre y bio editables, contadores reales de recetas/seguidores/siguiendo, rango de chef calculado, tabs con icono (Mis recetas / Guardadas) y header colapsable por *drag*.
- **Perfil ajeno** con datos reales y botón *Seguir / Siguiendo* con transacción que actualiza contadores en ambos usuarios.
- **Mensajería**: lista de conversaciones en tiempo real, *Nueva conversación* por email, mensajes con burbujas asimétricas, separadores de fecha, hora dentro de cada burbuja y avatar del otro usuario en la cabecera.
- **Crear receta** desde la app y persistencia en Firestore (sin foto de receta todavía).
- **Likes y guardados** persistentes con transacciones de Firestore.
- **Navegación inferior** estilo Material 3 con indicador *pill*.
- **Modo demo**: cuando aún no hay datos reales, la app muestra chats y recetas de muestra para que la interfaz no parezca vacía.

### Pendientes

- Foto de receta (subida a Storage).
- Pantalla de detalle de receta + comentarios.
- Recorte / *crop* de imagen de avatar.
- Listas de seguidores y siguiendo (ahora sólo contadores).
- Notificaciones push (FCM).
- *Read receipts*, indicador de *escribiendo* y presencia en línea en el chat (Realtime DB).
- Login con Google (botón presente pero deshabilitado).

---

## Arquitectura y estructura

La aplicación sigue una arquitectura ligera basada en **Activities + Controllers** para mantener `MainActivity` limpio.

```
app/src/main/java/com/sazon/proyectointegrador/
 ├── SplashActivity                  → ruta inicial según sesión
 ├── LoginActivity / RegisterActivity → auth con Firebase
 ├── MainActivity                    → host de las 3 pestañas
 ├── ChatActivity                    → conversación 1-a-1 en tiempo real
 ├── ProfileActivity                 → perfil ajeno
 ├── CreateRecipeActivity            → crear nueva receta
 ├── ui/
 │    ├── feed/FeedController        → feed + buscador + refresh
 │    ├── chats/ChatsController      → lista de conversaciones
 │    └── profile/ProfileController  → pestaña Perfil dentro del Main
 ├── adapters/
 │    ├── ChatThreadAdapter          → ítems de lista de chats
 │    ├── ChatMessageAdapter         → burbujas + separadores de fecha
 │    ├── PublicacionAdapter         → cards del feed con likes/guardados
 │    └── PublicacionGridAdapter     → grid 3 columnas del perfil
 ├── model/
 │    ├── Publicacion                → receta
 │    ├── ChatThread / ChatMessage   → conversación + mensaje
 │    ├── ChatItem / ChatDateHeader  → unión + cabecera de fecha
 └── util/
      ├── SessionManager             → wrappers de FirebaseAuth y Firestore
      ├── RecipeRepository           → CRUD de recetas + likes + guardados
      ├── FollowRepository           → seguir / dejar de seguir
      ├── AvatarHelper               → picker + subida a Storage
      ├── DemoData                   → datos de muestra para chats/recetas
      └── SimpleTextWatcher          → TextWatcher abstracto
```

### Modelo de datos en Firestore

```
users/{uid}
  uid, name, email, bio, avatarUrl, createdAt
  followers (number), following (number), recipes (number)

users/{uid}/followers/{followerUid}
users/{uid}/following/{followingUid}
users/{uid}/saved/{recipeId}

recipes/{recipeId}
  authorId, autor, titulo, descripcion, imageUrl, likes, createdAt

recipes/{recipeId}/likes/{uid}

chats/{chatId}                       chatId = "uidA_uidB" ordenado
  participants: [uidA, uidB]
  participantsNames: { uidA: nameA, uidB: nameB }
  lastMessage, lastSenderId, lastMessageAt

chats/{chatId}/messages/{msgId}
  text, senderId, createdAt
```

### Storage

```
avatars/{uid}.jpg
```

---

## Tecnologías utilizadas

- **Java** (Android nativo)
- Android SDK (compileSdk 36, minSdk 28)
- **Firebase**: Auth, Firestore, Storage, Analytics (BoM 31.5.0)
- Material Design 3 (Material Components 1.13)
- AndroidX: AppCompat, ConstraintLayout, RecyclerView, SwipeRefreshLayout, Activity Result API
- **Glide** 4.16 para carga de imágenes remotas
- Gradle Kotlin DSL
- Git / GitHub
- Figma (diseño)

---

## Instalación y ejecución

Para trabajar con el proyecto en local:

```
git clone https://github.com/abarrosomagan/ProyectoIntegrador.git
```

1. Abrir el proyecto en Android Studio.
2. Sincronizar Gradle.
3. **Importante**: usar un emulador con **Google Play Services** (no AOSP) o un dispositivo Android físico, porque Firebase Auth lo requiere.
4. En la consola de Firebase del proyecto **sazon-cc95a** habilitar:
   - Authentication → proveedor **Email/Password**
   - **Firestore Database** (modo prueba o producción con reglas).
   - **Cloud Storage** (modo prueba o reglas equivalentes).
5. Ejecutar la app desde Android Studio.

### Reglas mínimas de Firestore para desarrollo

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == uid;
      match /{sub=**} {
        allow read, write: if request.auth != null;
      }
    }
    match /recipes/{recipeId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null
        && request.auth.uid == request.resource.data.authorId;
      allow update, delete: if request.auth != null
        && request.auth.uid == resource.data.authorId;
      match /likes/{uid} {
        allow read: if request.auth != null;
        allow write: if request.auth != null && request.auth.uid == uid;
      }
    }
    match /chats/{chatId} {
      allow read, update: if request.auth != null
        && request.auth.uid in resource.data.participants;
      allow create: if request.auth != null
        && request.auth.uid in request.resource.data.participants;
      match /messages/{msgId} {
        allow read: if request.auth != null
          && request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.participants;
        allow create: if request.auth != null
          && request.auth.uid == request.resource.data.senderId;
      }
    }
  }
}
```

### Reglas mínimas de Storage

```js
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /avatars/{uid}.jpg {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

---

## Evolución del proyecto

El proyecto se ha desarrollado de manera incremental:

1. Diseño en Figma y maquetación inicial de pantallas.
2. Splash, Login, Registro y estructura base con XML.
3. Refactor a `Controllers` para limpiar `MainActivity`.
4. Integración Firebase Auth (login real + sesión persistente).
5. Firestore para perfil, mensajería y *Nueva conversación*.
6. Mejora visual: BottomNav rediseñado, perfil con hero gradient colapsable, chat estilo WhatsApp con burbujas asimétricas, perfil estilo Instagram con grid.
7. Funcionalidad real completa: foto de perfil con Storage, recetas reales con CRUD, likes y guardados persistentes, sistema de seguidores.

Cada avance queda reflejado mediante commits sucesivos en el repositorio.

---

## Licencia

Proyecto desarrollado con fines **académicos**.

El contenido puede ser utilizado como referencia educativa citando a los autores.

