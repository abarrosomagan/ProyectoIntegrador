# Checklist de demo

Esta checklist prepara una presentación estable de Sazón sin depender de improvisación.

## Objetivo de la demo

Mostrar Sazón como una red social móvil de cocina con:

- Autenticación real.
- Feed de recetas.
- Creación, edición y detalle de recetas.
- Likes, guardados y comentarios.
- Perfil propio y perfil ajeno.
- Seguidores y siguiendo.
- Chat en tiempo real con presencia, escribiendo y lectura básica.

## Preparación previa

- Confirmar que el proyecto abre en Android Studio sin errores de Gradle.
- Ejecutar `./gradlew :app:assembleDebug` o `gradlew.bat :app:assembleDebug`.
- Usar un emulador con Google Play Services o un dispositivo físico.
- Confirmar que Firebase Auth tiene habilitado email/password.
- Confirmar que Firestore Database está habilitado.
- Desplegar reglas e índices si se va a usar Firebase real:

```bash
firebase deploy --only firestore
```

- Cargar datos de demo:

```bash
node scripts/seed-demo.mjs
```

- Tener dos sesiones disponibles para probar chat:
  - `fer1@test.com` / `123456`
  - `fer2@test.com` / `123456`

## Datos mínimos recomendados

Antes de presentar, conviene tener:

- Dos usuarios con nombre visible y bio.
- Al menos 14 recetas reales en Firestore.
- Una receta con foto.
- Una receta con comentarios.
- Una receta guardada por el usuario principal.
- Un usuario siguiendo a otro.
- Una conversación real entre `fer1@test.com` y `fer2@test.com`.

El script `scripts/seed-demo.mjs` prepara estos datos mínimos.

## Recorrido recomendado

1. Abrir la app desde Splash.
2. Iniciar sesión con `fer1@test.com`.
3. Mostrar el feed y el buscador.
4. Abrir una receta.
5. Dar like y guardar.
6. Añadir un comentario.
7. Entrar al perfil del autor.
8. Seguir o dejar de seguir.
9. Abrir listas de seguidores o siguiendo.
10. Volver al perfil propio.
11. Crear una receta con foto.
12. Editar esa receta.
13. Abrir la pestaña de chats.
14. Entrar a una conversación.
15. Enviar un mensaje.
16. En otro dispositivo o emulador, mostrar presencia, escribiendo y lectura.

## Prueba rápida antes de enseñar

- Login correcto.
- Feed carga sin quedarse vacío.
- Crear receta funciona.
- Editar receta funciona.
- Detalle muestra comentarios.
- Like cambia de estado.
- Guardado aparece en perfil.
- Seguir actualiza contador.
- Lista de seguidores abre.
- Explorar abre recetas y chefs.
- Actividad muestra eventos y contador.
- Desde Explorar se puede seguir a un chef.
- Explorar permite ordenar recetas por Para ti, Recientes y Populares.
- Explorar filtra chefs seguidos.
- Actividad permite ver solo eventos sin leer.
- Ajustes abre la cuenta y permite cerrar sesion.
- Chat envía y recibe.
- Indicador de escribiendo aparece entre dos sesiones.

## Plan B

Si Firebase va lento o no hay datos:

- El feed y chats tienen datos demo para que la interfaz no parezca rota.
- Evitar crear muchas imágenes nuevas, porque se guardan comprimidas en Firestore.
- Enseñar primero pantallas ya cargadas: feed, perfil, detalle y chat.
- Si falla una acción puntual, explicar que la app usa Firebase en tiempo real y pasar al siguiente flujo.

## Riesgos conocidos

- Login con Google está pendiente.
- La pantalla de ajustes completa está pendiente.
- No hay notificaciones push.
- Las imágenes se guardan en Firestore comprimidas para evitar servicios de pago; no es una solución pensada para escalar a miles de fotos.
- Los contadores sociales se actualizan desde cliente. Para producción estricta convendría moverlos a backend o funciones.

## Mensaje de presentación

Sazón no es sólo una app de recetas: está planteada como una red social gastronómica. El valor está en publicar, descubrir, guardar, comentar, seguir perfiles y conversar con otros usuarios alrededor de la cocina.
