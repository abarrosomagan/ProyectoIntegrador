# Sazon

Sazon es una aplicacion Android de comunidad gastronomica. Permite publicar recetas, descubrir platos de otros usuarios, seguir perfiles, guardar ideas, comentar, chatear y preparar una lista de la compra a partir de los ingredientes de cada receta.

El proyecto forma parte del Proyecto Integrador de Desarrollo de Aplicaciones Multiplataforma y esta construido con Java, Android SDK, Firebase Auth, Cloud Firestore, Material Design, AndroidX y Glide.

## Documentacion principal

La documentacion profesional completa del proyecto esta en:

- [docs/documentacion-profesional-sazon.md](docs/documentacion-profesional-sazon.md)

Documentos auxiliares:

- [docs/demo-checklist.md](docs/demo-checklist.md)
- [docs/firebase-security.md](docs/firebase-security.md)

## Estado del proyecto

La aplicacion ya cubre el flujo principal de una red social de cocina:

- Autenticacion con email y contrasena mediante Firebase Auth.
- Registro de usuario, aviso de correo no verificado y recuperacion de contrasena.
- Feed de recetas con modos Para ti, Siguiendo y Populares.
- Busqueda de recetas por titulo, descripcion, autor, dificultad y etiquetas.
- Creacion y edicion de recetas con imagen opcional, ingredientes, pasos, dificultad, tiempo, raciones y etiquetas.
- Detalle de receta con likes, guardados, comentarios, visualizaciones, compartir, enviar a chat, modo cocinar y lista de la compra.
- Perfil propio con recetas publicadas, guardadas y marcadas con me gusta.
- Perfil ajeno con seguimiento, estadisticas y navegacion a seguidores y seguidos.
- Explorador de recetas y chefs con filtros y ordenacion.
- Actividad social para likes, guardados, comentarios y follows.
- Chat en tiempo real con presencia, indicador de escritura, lectura, reacciones, edicion, borrado, fijado, silenciado, ocultado y borradores.
- Lista de la compra local con marcado, ordenacion, limpieza y compartir.
- Pantalla de ajustes y pantalla Acerca de.

## Cuentas de demo

El script de datos de demo prepara estas cuentas:

| Correo | Contrasena |
|---|---|
| `fer1@test.com` | `123456` |
| `fer2@test.com` | `123456` |
| `test1@sazon.com` | `123456` |

Pareja recomendada para probar chat en dos dispositivos o dos emuladores: `fer1@test.com` y `fer2@test.com`.

## Puesta en marcha

```bash
git clone https://github.com/abarrosomagan/ProyectoIntegrador.git
cd ProyectoIntegrador
```

1. Abrir el proyecto en Android Studio.
2. Sincronizar Gradle.
3. Usar un emulador con Google Play Services o un dispositivo Android fisico.
4. Configurar Firebase con el archivo `app/google-services.json`.
5. Habilitar Authentication con email/contrasena.
6. Habilitar Cloud Firestore.
7. Desplegar reglas e indices si se usa el proyecto Firebase real:

```bash
firebase deploy --only firestore
```

8. Cargar datos de demo si se necesita una presentacion estable:

```bash
node scripts/seed-demo.mjs
```

9. Compilar la app:

```bash
./gradlew :app:assembleDebug
```

En Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Decisiones tecnicas clave

- No se usa Firebase Storage para mantener el proyecto dentro de opciones gratuitas.
- Las imagenes se comprimen y se guardan como `data:image/jpeg;base64,...` en Firestore.
- Los contadores sociales se denormalizan para pintar rapido.
- Los estados por usuario, como likes y guardados, se almacenan tambien en subcolecciones del usuario.
- La coherencia visual entre feed, detalle y perfil se propaga en memoria con `RecipeStateBus`.
- La lista de la compra y los borradores de chat se guardan localmente en `SharedPreferences`.

## Diseno

El diseno base se trabajo en Figma:

https://www.figma.com/design/yHjedo6Y4G4Co4WEa7LY2A/Sin-t%C3%ADtulo?node-id=0-1&p=f&t=fkQQFC4GstUwoPPk-0

## Autores

- Alejandro Barroso
- Fernando Cecilia

Proyecto desarrollado con fines academicos.
