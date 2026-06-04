# Documentacion profesional de Sazon

Fecha de revision: 4 de junio de 2026  
Repositorio: `ProyectoIntegrador`  
Aplicacion: `Sazon`  
Tipo de producto: aplicacion Android social para personas aficionadas a la cocina

---

## 1. Resumen ejecutivo

Sazon es una aplicacion movil Android planteada como una red social gastronomica. Su objetivo es que una persona pueda crear una cuenta, publicar recetas, descubrir contenido de otros usuarios, guardar recetas, marcar me gusta, comentar, seguir perfiles, conversar mediante chat y preparar una lista de la compra a partir de los ingredientes de una receta.

La app no se limita a ser un recetario. El enfoque principal es social: cada receta pertenece a un autor, genera interacciones, aparece en feed, se puede compartir, puede recibir comentarios y construye la identidad del perfil del usuario. La aplicacion combina funcionalidades de contenido, comunidad y utilidad practica en cocina.

El proyecto esta desarrollado en Java para Android nativo. Usa Firebase Authentication para la identidad de usuario, Cloud Firestore para datos persistentes y en tiempo real, Material Design para la interfaz, Glide para imagenes y AndroidX como base de compatibilidad. Por decision de coste, no se usa Firebase Storage. Las imagenes se comprimen y se guardan en Firestore como `data URL`, suficiente para una demo academica y para mantener el despliegue dentro de opciones gratuitas.

---

## 2. Objetivos del proyecto

### 2.1 Objetivo funcional

Construir una aplicacion Android completa que permita vivir una experiencia social alrededor de recetas:

- Publicar recetas propias.
- Descubrir recetas de la comunidad.
- Interactuar con likes, guardados y comentarios.
- Seguir a otros usuarios.
- Consultar perfiles y estadisticas.
- Conversar mediante chat.
- Usar recetas como punto de partida para cocinar y comprar ingredientes.

### 2.2 Objetivo academico

Integrar en un proyecto unico las competencias principales de DAM:

- Desarrollo Android nativo.
- Diseno de interfaces.
- Navegacion entre pantallas.
- Persistencia remota.
- Autenticacion.
- Consultas y reglas de seguridad en Firebase.
- Gestion de estado local.
- Arquitectura por capas ligera.
- Uso de Gradle.
- Versionado con Git y GitHub.

### 2.3 Objetivo de producto

Ofrecer una experiencia coherente y presentable como red social de cocina. El usuario debe poder entender el recorrido sin explicaciones externas: entra, ve recetas, abre detalle, interactua, visita perfiles, crea contenido y conversa.

---

## 3. Alcance actual

### 3.1 Funcionalidades implementadas

La version actual cubre estos bloques:

- Splash e inicio de sesion.
- Registro de usuario.
- Recuperacion de contrasena.
- Aviso de correo no verificado y reenvio de verificacion.
- Feed principal con recetas reales y fallback demo.
- Feed con modos Para ti, Siguiendo y Populares.
- Busqueda por texto.
- Creacion de receta.
- Edicion de receta propia.
- Detalle de receta.
- Likes persistentes.
- Guardados persistentes.
- Comentarios en tiempo real.
- Visualizaciones de recetas.
- Perfil propio.
- Perfil ajeno.
- Pestañas de recetas propias, guardadas y con me gusta.
- Seguidores y seguidos.
- Explorador de recetas y chefs.
- Actividad social.
- Chat en tiempo real.
- Lista de la compra local.
- Modo cocinar paso a paso.
- Pantalla de ajustes.
- Pantalla Acerca de.
- Script de datos de demo para Firebase.
- Reglas de seguridad de Firestore versionadas.

### 3.2 Funcionalidades no incluidas todavia

Estas funcionalidades no forman parte estable del alcance actual:

- Login con Google completamente funcional.
- Notificaciones push.
- Moderacion avanzada de contenido.
- Recorte dedicado de avatar.
- Historias efimeras en cliente, aunque existen reglas preparadas para `/stories`.
- Reportes completos en interfaz, aunque existen reglas preparadas para `/reports`.
- Backend propio o Cloud Functions para contadores sociales.
- Almacenamiento de imagenes en Firebase Storage.

---

## 4. Arquitectura general

La aplicacion usa una arquitectura Android nativa ligera. No sigue un patron MVVM completo, pero separa responsabilidades en Activities, Controllers, Adapters, Models y utilidades/repositorios.

```text
app/src/main/java/com/sazon/proyectointegrador/
  Activities principales
    SplashActivity
    LoginActivity
    RegisterActivity
    MainActivity
    RecipeDetailActivity
    CreateRecipeActivity
    ProfileActivity
    FollowListActivity
    ExploreActivity
    ActivityActivity
    ChatActivity
    ShoppingListActivity
    CookModeActivity
    SettingsActivity
    AboutActivity

  ui/
    feed/FeedController
    chats/ChatsController
    profile/ProfileController

  adapters/
    PublicacionAdapter
    PublicacionGridAdapter
    RecipeCommentAdapter
    UserListAdapter
    ChatThreadAdapter
    ChatMessageAdapter
    ActivityAdapter

  model/
    Publicacion
    RecipeComment
    UserListItem
    ActivityItem
    ChatThread
    ChatMessage
    ChatItem
    ChatDateHeader

  util/
    SessionManager
    RecipeRepository
    FollowRepository
    ActivityRepository
    RecipeStateBus
    RecipeImageHelper
    AvatarHelper
    IngredientCatalog
    ShoppingList
    DemoData
    SimpleTextWatcher
```

### 4.1 Activities

Las Activities representan pantallas completas o flujos autonomos. Gestionan ciclo de vida, navegacion principal y acciones de interfaz que pertenecen a una pantalla concreta.

Ejemplos:

- `LoginActivity`: acceso con email y contrasena.
- `RegisterActivity`: creacion de cuenta.
- `MainActivity`: contenedor principal con bottom navigation.
- `RecipeDetailActivity`: detalle completo de una receta.
- `CreateRecipeActivity`: alta y edicion de receta.
- `ChatActivity`: conversacion individual.
- `CookModeActivity`: preparacion paso a paso.

### 4.2 Controllers

`MainActivity` delega sus tres areas principales a controllers:

- `FeedController`: feed, busqueda, tabs, menu y acciones de cabecera.
- `ChatsController`: lista de conversaciones, busqueda, nuevo chat y ajustes por chat.
- `ProfileController`: perfil propio, avatar, pestañas, estadisticas y edicion.

Esta separacion evita que `MainActivity` concentre toda la logica.

### 4.3 Repositories y utilidades

Los repositorios encapsulan operaciones repetidas contra Firebase o almacenamiento local:

- `RecipeRepository`: recetas, likes, guardados, comentarios y visualizaciones.
- `FollowRepository`: seguir, dejar de seguir y consultar relaciones.
- `ActivityRepository`: eventos sociales y contador de no leidos.
- `SessionManager`: sesion local, helpers de Firebase y documentos de usuario.
- `ShoppingList`: lista local de ingredientes.
- `RecipeImageHelper`: compresion y carga de imagenes.
- `IngredientCatalog`: catalogo de ingredientes y autocompletado.

### 4.4 Models

Los modelos representan datos de dominio. El principal es `Publicacion`, que mapea recetas de Firestore e incluye estado local por usuario (`liked` y `guardada`) excluido de serializacion con `@Exclude`.

---

## 5. Navegacion de usuario

### 5.1 Flujo de entrada

1. `SplashActivity` abre la aplicacion.
2. Si hay sesion valida, se navega a `MainActivity`.
3. Si no hay sesion, se navega a `LoginActivity`.
4. Desde login se puede ir a registro o recuperar contrasena.
5. Tras iniciar sesion, la app muestra el feed.

### 5.2 Navegacion principal

`MainActivity` tiene tres secciones mediante bottom navigation:

- Feed.
- Chats.
- Perfil.

El boton flotante abre `CreateRecipeActivity` para publicar una receta.

### 5.3 Navegacion social

Desde el feed o explorar se puede:

- Abrir detalle de receta.
- Abrir perfil del autor.
- Ir a actividad.
- Ir a explorar.
- Ir a lista de la compra.
- Ir a ajustes.

Desde detalle se puede:

- Dar like.
- Guardar.
- Comentar.
- Ver perfil del autor.
- Editar o borrar si el usuario es autor.
- Compartir dentro o fuera de Sazon.
- Entrar en modo cocinar.
- Anadir ingredientes a la lista de la compra.
- Explorar por etiquetas.

---

## 6. Modulos funcionales

## 6.1 Autenticacion y sesion

La autenticacion se apoya en Firebase Auth.

Funciones principales:

- Registro con email, nombre y contrasena.
- Login con email y contrasena.
- Recuperacion de contrasena por correo.
- Aviso si el correo no esta verificado.
- Reenvio del correo de verificacion.
- Cierre de sesion compatible con limpieza local.

La sesion local se complementa con `SessionManager`, que guarda datos basicos en `SharedPreferences` para poder recuperar nombre o uid sin depender siempre de una lectura remota inmediata.

Al cerrar sesion, `SessionManager.signOutCompat()` limpia:

- Sesion Firebase.
- Cache local de usuario.
- Borradores de chat.
- Lista de la compra local.

## 6.2 Feed principal

El feed es el punto de entrada social de la app.

Modos disponibles:

- Para ti: mezcla priorizada de recetas recientes y estado social.
- Siguiendo: recetas de usuarios seguidos.
- Populares: recetas ordenadas por likes y fecha.

Capacidades:

- Carga de recetas desde Firestore.
- Fallback demo si no hay recetas reales.
- Pull-to-refresh.
- Busqueda con retardo corto para evitar recalculos excesivos.
- Estado visual de like y guardado.
- Contador de actividad no leida.
- Acceso a explorar, actividad, lista de compra y ajustes.

La carga usa `RecipeRepository.feed()` y luego enriquece cada receta con `savedIds()` y `likedIds()` del usuario actual.

## 6.3 Creacion y edicion de recetas

`CreateRecipeActivity` permite crear o editar recetas.

Campos actuales:

- Titulo.
- Descripcion.
- Imagen opcional.
- Tiempo de preparacion.
- Raciones.
- Dificultad.
- Etiquetas.
- Ingredientes.
- Pasos.

La misma pantalla funciona en modo creacion y modo edicion. Si recibe `EXTRA_EDIT_RECIPE_ID`, carga la receta, verifica que el usuario actual sea autor y permite modificarla.

La imagen se selecciona con Photo Picker. Despues se transforma mediante `RecipeImageHelper` para guardarla como una cadena apta para Firestore. Si la imagen supera el limite practico definido por la compresion, la app avisa al usuario.

## 6.4 Detalle de receta

`RecipeDetailActivity` concentra la experiencia completa de una receta.

Muestra:

- Imagen principal.
- Titulo.
- Autor.
- Fecha relativa.
- Descripcion.
- Ingredientes.
- Pasos.
- Tiempo, raciones, dificultad y visualizaciones.
- Etiquetas.
- Comentarios.

Acciones:

- Like.
- Guardar.
- Comentar.
- Compartir.
- Enviar a chat.
- Abrir perfil del autor.
- Editar receta propia.
- Borrar receta propia.
- Ver lista de usuarios que dieron like con pulsacion larga.
- Abrir modo cocinar.
- Anadir ingredientes a lista de la compra.
- Abrir explorar filtrado por etiqueta.

Las visualizaciones no se cuentan para el propio autor. Para otros usuarios, `RecipeRepository.registerView()` crea un documento en `recipes/{recipeId}/views/{uid}` y solo incrementa si no existia previamente.

## 6.5 Likes, guardados y coherencia de estado

La app mantiene likes y guardados de forma persistente:

- Like global: `recipes/{recipeId}/likes/{uid}`.
- Like por usuario: `users/{uid}/liked/{recipeId}`.
- Guardado por usuario: `users/{uid}/saved/{recipeId}`.
- Contador de likes denormalizado en `recipes/{recipeId}.likes`.

Para que la interfaz sea coherente entre feed, detalle y perfil, se usa `RecipeStateBus`. Cuando se da like o se guarda una receta, el detalle publica el cambio. Los listeners en feed y perfil actualizan sus listas en memoria, de forma que:

- El icono del feed refleja el estado nuevo.
- El detalle mantiene contador e icono sincronizados.
- El perfil actualiza recetas guardadas y recetas con me gusta.
- Una receta recien marcada puede aparecer en la pestaña correspondiente del perfil sin esperar una recarga completa.

Esta decision replica el comportamiento esperado en apps sociales: la accion tiene respuesta inmediata y luego se confirma contra Firestore.

## 6.6 Comentarios

Los comentarios cuelgan de:

```text
recipes/{recipeId}/comments/{commentId}
```

Cada comentario incluye:

- `recipeId`
- `authorId`
- `authorName`
- `text`
- `createdAt`

La pantalla de detalle escucha la subcoleccion en tiempo real. El envio valida que el texto no este vacio y que no supere 500 caracteres.

## 6.7 Perfil propio

El perfil propio muestra:

- Avatar o inicial.
- Nombre.
- Handle.
- Biografia.
- Rango de chef.
- Numero de recetas.
- Seguidores.
- Siguiendo.
- Resumen de likes y visualizaciones de recetas propias.

Pestañas:

- Mis recetas.
- Guardados.
- Me gusta.

Acciones:

- Editar perfil.
- Cambiar avatar.
- Compartir perfil.
- Abrir ajustes.
- Abrir explorar.
- Abrir Acerca de.
- Cerrar sesion.
- Abrir seguidores o siguiendo.

## 6.8 Perfil ajeno y seguimiento

`ProfileActivity` permite abrir perfiles de otros usuarios. El usuario puede seguir o dejar de seguir si no es su propio perfil.

Las relaciones se guardan de forma doble:

```text
users/{miUid}/following/{otroUid}
users/{otroUid}/followers/{miUid}
```

Tambien se actualizan contadores denormalizados:

- `users/{uid}.followers`
- `users/{uid}.following`

El doble registro facilita consultas rapidas de listas de seguidores y seguidos.

## 6.9 Explorar

`ExploreActivity` permite descubrir recetas y chefs.

Modos:

- Recetas.
- Chefs.

Orden de recetas:

- Para ti.
- Recientes.
- Populares.

Filtros de recetas:

- Todas.
- Rapidas.
- Faciles.

Filtros de chefs:

- Todos.
- Siguiendo.

La puntuacion Para ti considera:

- Likes.
- Si el usuario ya marco like.
- Si la receta esta guardada.
- Si el autor es seguido.
- Recencia de la publicacion.

Desde explorar se puede abrir receta, abrir autor o seguir/dejar de seguir a un chef.

## 6.10 Actividad social

`ActivityActivity` presenta eventos sociales del usuario:

- Likes recibidos.
- Guardados.
- Comentarios.
- Follows.
- Respuestas o menciones preparadas para expansion.

La actividad vive en:

```text
users/{uid}/activity/{activityId}
```

Incluye filtro de todas/sin leer y accion para marcar todo como leido. El feed muestra un contador de actividad no leida.

## 6.11 Chat

El sistema de chat permite conversaciones 1-a-1.

Coleccion principal:

```text
chats/{chatId}
chats/{chatId}/messages/{msgId}
```

El `chatId` se construye de forma determinista con los dos uid ordenados, por lo que ambos usuarios llegan al mismo documento.

Funciones implementadas:

- Lista de conversaciones.
- Crear chat buscando por email o nombre.
- Conversacion en tiempo real.
- Mensajes con separadores de fecha.
- Indicador de presencia.
- Indicador de escritura.
- Lectura basica mediante `readBy` y `lastReadAt`.
- Borradores locales.
- Reacciones por mensaje.
- Edicion de mensajes propios.
- Borrado de mensajes propios.
- Fijar conversacion.
- Silenciar conversacion.
- Marcar como no leida.
- Ocultar conversacion para el usuario actual.
- Bloqueo considerado al pintar lista.
- Envio de receta a un chat desde el detalle.

Los ajustes privados de chat se guardan en:

```text
users/{uid}/chatSettings/{chatId}
```

## 6.12 Lista de la compra

La lista de la compra es local y se guarda en `SharedPreferences`, no en Firestore.

Capacidades:

- Anadir todos los ingredientes de una receta.
- Mantener el nombre de receta origen.
- Marcar ingredientes como comprados.
- Quitar marcados.
- Ordenar alfabeticamente.
- Compartir como texto.
- Limpiarse al cerrar sesion.

Esta decision reduce complejidad, coste y permisos. La lista de la compra se entiende como dato local del dispositivo.

## 6.13 Modo cocinar

`CookModeActivity` muestra los pasos de una receta uno a uno.

Caracteristicas:

- Pantalla completa.
- Mantiene la pantalla encendida.
- Progreso visual.
- Botones Anterior y Siguiente.
- Finalizacion con mensaje de cierre.

El modo solo se abre si la receta tiene pasos.

## 6.14 Ajustes

`SettingsActivity` muestra datos de cuenta:

- Nombre.
- Email.
- UID.
- Biografia.
- Recetas.
- Seguidores.
- Siguiendo.

Acciones:

- Refrescar datos.
- Enviar correo para cambiar contrasena.
- Borrar borradores de chat.
- Cerrar sesion.
- Eliminar cuenta de Firebase Auth.

La eliminacion de cuenta requiere sesion reciente, tal como exige Firebase. Si falla por seguridad, se cierra sesion y se pide volver a entrar.

## 6.15 Acerca de

`AboutActivity` es una pantalla informativa del proyecto. Explica el proposito de Sazon, sus funcionalidades principales y su naturaleza academica.

---

## 7. Modelo de datos Firestore

## 7.1 Usuarios

```text
users/{uid}
  uid: string
  name: string
  email: string
  bio: string
  avatarUrl: string
  followers: number
  following: number
  recipes: number
  createdAt: timestamp | number
```

Subcolecciones:

```text
users/{uid}/followers/{followerUid}
  createdAt

users/{uid}/following/{followingUid}
  createdAt

users/{uid}/saved/{recipeId}
  recipeId
  createdAt

users/{uid}/liked/{recipeId}
  uid
  recipeId
  createdAt

users/{uid}/activity/{activityId}
  type
  actorId
  actorName
  recipeId
  recipeTitle
  read
  createdAt

users/{uid}/settings/{docId}
users/{uid}/blocked/{otherUid}
users/{uid}/chatSettings/{chatId}
users/{uid}/tokens/{tokenId}
```

## 7.2 Recetas

```text
recipes/{recipeId}
  authorId: string
  autor: string
  titulo: string
  descripcion: string
  imageUrl: string
  difficulty: string
  tags: string
  prepMinutes: number
  servings: number
  ingredientes: string[]
  pasos: string[]
  likes: number
  views: number
  createdAt: number
  updatedAt: number
```

Subcolecciones:

```text
recipes/{recipeId}/likes/{uid}
  uid
  recipeId
  createdAt

recipes/{recipeId}/views/{uid}
  uid
  createdAt

recipes/{recipeId}/comments/{commentId}
  recipeId
  authorId
  authorName
  text
  createdAt

recipes/{recipeId}/comments/{commentId}/likes/{uid}
```

## 7.3 Chats

```text
chats/{chatId}
  participants: string[]
  participantsNames: map<uid, name>
  lastMessage: string
  lastSenderId: string
  lastMessageAt: timestamp
  lastReadAt: map<uid, timestamp>
  presence: map<uid, presenceState>
  typing: map
  theme: string
  pinned: map
```

Mensajes:

```text
chats/{chatId}/messages/{msgId}
  text: string
  senderId: string
  createdAt: timestamp
  readBy: string[]
  reactions: map<emoji, uid[]>
  recipeId: string
  edited: boolean
  editedAt: timestamp
```

## 7.4 Reports y stories

Las reglas incluyen soporte para:

```text
reports/{reportId}
stories/{storyId}
stories/{storyId}/views/{uid}
```

Estas areas estan preparadas a nivel de seguridad, pero no son funcionalidad principal implementada en cliente.

---

## 8. Reglas de seguridad

Las reglas versionadas estan en `firestore.rules`.

Principios:

- El usuario debe estar autenticado para leer datos sociales.
- Cada usuario solo puede escribir datos privados de su propio perfil o subcolecciones.
- Las recetas solo pueden ser creadas, editadas o borradas por su autor.
- Los likes y visualizaciones se escriben por usuario autenticado.
- Los comentarios validan autor y longitud de texto.
- Los chats solo son accesibles para participantes.
- Los mensajes solo pueden ser creados por su remitente.
- La edicion y borrado de mensajes quedan limitados al remitente.
- Los reports solo son visibles para quien los crea.
- Todo lo no definido cae en cierre por defecto.

### 8.1 Denormalizacion permitida

Algunos campos contadores pueden modificarse desde cliente de forma acotada:

- `followers`
- `following`
- `recipes`
- `likes`
- `views`
- `saves`
- `comments`

Esta decision facilita el proyecto academico. Para un entorno de produccion estricta, seria recomendable mover contadores a backend o Cloud Functions.

### 8.2 Indices

El archivo `firestore.indexes.json` define indice compuesto para recetas por autor y fecha:

```text
recipes
  authorId ASC
  createdAt DESC
```

Este indice soporta la consulta de recetas de un usuario ordenadas por fecha.

---

## 9. Imagenes y estrategia gratuita

El proyecto no usa Firebase Storage. Esta decision responde a una restriccion clara: mantener la aplicacion dentro de opciones gratuitas.

### 9.1 Funcionamiento actual

Las imagenes se procesan con `RecipeImageHelper`:

- Se seleccionan desde el dispositivo.
- Se comprimen.
- Se transforman en una cadena `data:image/jpeg;base64,...`.
- Se guardan en Firestore en el campo `imageUrl`.
- Glide las carga posteriormente en los `ImageView`.

### 9.2 Ventajas

- No se requiere Storage.
- No hay reglas adicionales de Storage.
- Es suficiente para una demo.
- Simplifica el despliegue.

### 9.3 Limitaciones

- Firestore no esta pensado para almacenar muchas imagenes.
- Los documentos tienen limites de tamano.
- A gran escala incrementaria coste y latencia.
- Para produccion real convendria migrar a almacenamiento de objetos.

---

## 10. Datos de demo

El script `scripts/seed-demo.mjs` prepara datos realistas en Firebase usando la API REST.

Incluye:

- Tres cuentas de prueba.
- Perfiles con nombre y biografia.
- Recetas de demo.
- Likes.
- Guardados.
- Follows.
- Comentarios.
- Chat entre dos usuarios.

Cuentas:

| Correo | Contrasena |
|---|---|
| `fer1@test.com` | `123456` |
| `fer2@test.com` | `123456` |
| `test1@sazon.com` | `123456` |

Ejecucion:

```bash
node scripts/seed-demo.mjs
```

El script lee `app/google-services.json` para extraer `projectId` y `apiKey`.

---

## 11. Compilacion y ejecucion

### 11.1 Requisitos

- Android Studio actualizado.
- JDK compatible con Java 11.
- Emulador con Google Play Services o dispositivo Android fisico.
- Proyecto Firebase configurado.
- `app/google-services.json` presente.

### 11.2 Configuracion Gradle

El modulo app usa:

- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 28`
- `applicationId = "com.sazon.proyectointegrador"`
- `versionCode = 1`
- `versionName = "1.0"`
- Java 11.

### 11.3 Dependencias principales

- AndroidX Core KTX.
- AppCompat.
- Material Components.
- Activity.
- ConstraintLayout.
- RecyclerView.
- SwipeRefreshLayout.
- Firebase BoM `31.5.0`.
- Firebase Auth.
- Cloud Firestore.
- Firebase Analytics.
- Glide `4.16.0`.
- Credentials y Google ID preparadas para evolucion futura.

### 11.4 Comandos utiles

Compilar en macOS/Linux:

```bash
./gradlew :app:assembleDebug
```

Compilar en Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Desplegar reglas e indices:

```bash
firebase deploy --only firestore
```

Cargar demo:

```bash
node scripts/seed-demo.mjs
```

---

## 12. Checklist de QA funcional

### 12.1 Autenticacion

- Registrar usuario nuevo.
- Iniciar sesion con usuario existente.
- Recuperar contrasena.
- Ver aviso de email no verificado.
- Reenviar email de verificacion.
- Cerrar sesion.
- Comprobar que se limpian borradores y lista local.

### 12.2 Feed

- Feed carga recetas reales.
- Fallback demo aparece si no hay datos.
- Pull-to-refresh funciona.
- Busqueda filtra por titulo.
- Busqueda filtra por autor.
- Busqueda filtra por etiqueta o dificultad.
- Modo Siguiendo filtra autores seguidos.
- Modo Populares ordena por likes.
- Contador de actividad no leida se actualiza.

### 12.3 Recetas

- Crear receta con campos obligatorios.
- Crear receta con imagen.
- Crear receta sin imagen.
- Crear receta con ingredientes.
- Crear receta con pasos.
- Editar receta propia.
- Intentar editar receta ajena no debe ser posible.
- Borrar receta propia.
- Abrir modo cocinar con pasos.
- No abrir modo cocinar si no hay pasos.
- Anadir ingredientes a lista de compra.

### 12.4 Interacciones sociales

- Dar like desde detalle.
- Quitar like desde detalle.
- Ver que el feed refleja el icono actualizado.
- Ver que el perfil refleja recetas con me gusta.
- Guardar receta.
- Quitar guardado.
- Ver que la pestaña Guardados se actualiza.
- Comentar receta.
- Ver comentarios en tiempo real.
- Abrir lista de usuarios que dieron like.

### 12.5 Perfil y seguimiento

- Editar nombre.
- Editar biografia.
- Cambiar avatar.
- Abrir recetas propias.
- Abrir guardados.
- Abrir me gusta.
- Abrir perfil ajeno.
- Seguir usuario.
- Dejar de seguir.
- Abrir seguidores.
- Abrir siguiendo.
- Ver contador actualizado.

### 12.6 Explorar

- Buscar recetas.
- Buscar chefs.
- Ordenar por Para ti.
- Ordenar por recientes.
- Ordenar por populares.
- Filtrar rapidas.
- Filtrar faciles.
- Ver todos los chefs.
- Ver solo seguidos.
- Seguir desde explorar.
- Abrir receta desde explorar.
- Abrir perfil desde explorar.

### 12.7 Actividad

- Generar like recibido.
- Generar comentario recibido.
- Generar follow recibido.
- Ver actividad en pantalla.
- Filtrar sin leer.
- Marcar todo como leido.
- Abrir destino de actividad.

### 12.8 Chat

- Crear conversacion por email.
- Crear conversacion por nombre.
- Enviar mensaje.
- Recibir mensaje en otro dispositivo.
- Ver indicador de escribiendo.
- Ver presencia.
- Ver lectura.
- Reaccionar a mensaje.
- Editar mensaje propio.
- Borrar mensaje propio.
- Fijar conversacion.
- Silenciar conversacion.
- Marcar como no leida.
- Ocultar conversacion.
- Recuperar borrador.
- Enviar receta a chat.

### 12.9 Lista de la compra

- Anadir ingredientes desde receta.
- Marcar ingrediente comprado.
- Limpiar marcados.
- Ordenar alfabeticamente.
- Compartir lista.
- Cerrar sesion y comprobar limpieza.

---

## 13. Riesgos y limitaciones

### 13.1 Imagenes en Firestore

Guardar imagenes como data URL funciona para demo, pero no es una solucion ideal para escala.

Riesgo:

- Documentos grandes.
- Carga mas lenta.
- Mayor consumo de Firestore.

Mitigacion actual:

- Compresion.
- Aviso si la imagen es demasiado grande.
- Uso limitado para presentacion academica.

### 13.2 Contadores desde cliente

Los contadores se actualizan desde cliente.

Riesgo:

- En produccion podria haber desajustes o manipulacion.

Mitigacion actual:

- Reglas acotadas.
- Subcolecciones como fuente de verdad parcial.

Mejora futura:

- Cloud Functions o backend propio para recalcular e incrementar contadores.

### 13.3 Busqueda simple

La busqueda actual se hace con datos cargados y comparacion de texto.

Riesgo:

- No escala bien a miles de recetas.

Mejora futura:

- Indices especificos.
- Busqueda por prefijos normalizados.
- Servicio externo de busqueda si el proyecto creciera.

### 13.4 Chat sin notificaciones push

El chat funciona en tiempo real cuando la app esta abierta, pero no avisa fuera de la app.

Mejora futura:

- Firebase Cloud Messaging.
- Tokens por usuario.
- Preferencias de notificacion.

### 13.5 Moderacion

No hay sistema avanzado de moderacion.

Mejora futura:

- Reportes en interfaz.
- Bloqueo ampliado.
- Revision de contenido.
- Ocultar recetas denunciadas.

---

## 14. Roadmap recomendado

### 14.1 Corto plazo

- Revisar textos y codificacion visible en todos los layouts.
- Terminar login con Google o retirar dependencias si no se entrega.
- Completar interfaz de reportes.
- Mejorar pantalla Acerca de con version, autores y enlaces.
- Crear pruebas manuales documentadas para entrega.
- Revisar reglas de Firestore contra todos los flujos reales.
- Anadir estados de carga y error mas finos.

### 14.2 Medio plazo

- Colecciones personales de recetas.
- Planificador semanal de comidas.
- Ingredientes con cantidades estructuradas.
- Temporizadores en modo cocinar.
- Filtros por dieta, alergenos y tipo de plato.
- Busqueda avanzada por ingredientes.
- Ranking de chefs.
- Recetas destacadas por tendencia.
- Sistema de reportes visible.
- Bloqueo completo entre usuarios.

### 14.3 Largo plazo

- Notificaciones push.
- Backend para contadores y moderacion.
- Migracion de imagenes a almacenamiento externo si se acepta coste.
- Feed paginado.
- Recomendaciones personalizadas.
- Historias efimeras.
- Comunidades o grupos de cocina.
- Recetas colaborativas.
- Version web o multiplataforma.

---

## 15. Criterios de entrega profesional

Para considerar Sazon lista para una presentacion solida:

- El proyecto compila sin errores.
- Firebase Auth y Firestore estan operativos.
- Las reglas estan desplegadas.
- Los datos de demo estan cargados.
- Al menos dos usuarios pueden iniciar sesion.
- El feed muestra recetas.
- Like y guardado se reflejan entre detalle, feed y perfil.
- El perfil muestra recetas propias, guardadas y liked.
- El chat funciona entre dos cuentas.
- La lista de la compra se puede demostrar desde una receta.
- La documentacion del proyecto esta accesible desde README.
- No hay archivos locales innecesarios versionados.
- No se suben caches, builds ni configuraciones privadas fuera de lo necesario.

---

## 16. Guia de presentacion

Recorrido recomendado para demo:

1. Abrir la app y mostrar splash.
2. Iniciar sesion con `fer1@test.com`.
3. Explicar que Sazon es una red social de cocina.
4. Mostrar feed Para ti.
5. Cambiar a Populares.
6. Buscar una receta.
7. Abrir detalle.
8. Dar like y guardar.
9. Anadir comentario.
10. Mostrar ingredientes y pasos.
11. Abrir modo cocinar.
12. Anadir ingredientes a lista de la compra.
13. Abrir perfil del autor.
14. Seguir o dejar de seguir.
15. Abrir perfil propio y mostrar pestañas.
16. Entrar en Explorar y filtrar.
17. Abrir Actividad.
18. Entrar en Chats.
19. Enviar un mensaje.
20. Cerrar con Ajustes y Acerca de.

Mensaje sugerido:

> Sazon es una red social gastronomica. La app permite publicar recetas, descubrir contenido, guardar ideas, interactuar con otros usuarios y llevar la receta hasta el momento real de cocinar, con lista de compra y modo paso a paso.

---

## 17. Estructura de archivos relevante

```text
README.md
docs/
  documentacion-profesional-sazon.md
  demo-checklist.md
  firebase-security.md
scripts/
  seed-demo.mjs
firestore.rules
firestore.indexes.json
firebase.json
app/
  build.gradle.kts
  google-services.json
  src/main/AndroidManifest.xml
  src/main/java/com/sazon/proyectointegrador/
  src/main/res/layout/
  src/main/res/drawable/
  src/main/res/values/
```

---

## 18. Glosario

**Feed**: lista principal de recetas publicadas por la comunidad.  
**Guardado**: receta marcada para recuperarla despues en el perfil.  
**Like**: interaccion social positiva sobre una receta.  
**Perfil ajeno**: vista publica de otro usuario.  
**Chef**: nombre usado en la app para referirse a usuarios creadores de recetas.  
**Modo cocinar**: pantalla paso a paso para preparar una receta.  
**Firestore**: base de datos NoSQL en la nube usada por la app.  
**Data URL**: cadena que contiene una imagen codificada en base64.  
**Denormalizacion**: guardar contadores o copias de datos para leer mas rapido.  
**Subcoleccion**: coleccion hija dentro de un documento Firestore.

---

## 19. Conclusiones

Sazon ya tiene una base funcional amplia y coherente para una red social de cocina. La aplicacion combina identidad, contenido, interaccion social, mensajeria y utilidad practica. La arquitectura es ligera pero suficiente para el alcance actual, y las decisiones tecnicas respetan la restriccion de no depender de Firebase Storage.

El proyecto esta en buen punto para seguir una progresion incremental: reforzar seguridad, pulir experiencia, completar moderacion, mejorar busqueda y preparar una demo estable con datos reales. La siguiente etapa deberia centrarse en calidad de entrega, correccion de textos, pruebas manuales, reglas y limpieza de pequenos riesgos antes de ampliar con funcionalidades mas ambiciosas.
