# Seguridad Firebase

Este proyecto usa Firebase Auth y Cloud Firestore. No usa Firebase Storage para mantener el despliegue dentro de opciones gratuitas.

## Archivos

- `firestore.rules`: reglas versionadas para Firestore.
- `firestore.indexes.json`: índices necesarios para consultas actuales.
- `firebase.json`: apunta a los archivos anteriores.

## Modelo protegido

- `users/{uid}`: lectura autenticada; escritura del propio usuario. Los contadores sociales quedan abiertos a cambios acotados porque el seguimiento se actualiza desde cliente.
- `users/{uid}/followers` y `users/{uid}/following`: sólo se puede escribir la relación que corresponde al usuario autenticado.
- `users/{uid}/saved`: sólo el dueño puede leer o escribir sus guardados.
- `recipes`: lectura autenticada; creación, edición y borrado sólo por autor.
- `recipes/{recipeId}/likes`: cada usuario sólo escribe su propio like.
- `recipes/{recipeId}/comments`: lectura autenticada; creación por el autor del comentario; borrado por el autor.
- `chats`: lectura y actualización sólo por participantes.
- `chats/{chatId}/messages`: lectura por participantes; creación por remitente; borrado sólo del propio mensaje.

## Decisiones escalables

- Las imágenes se guardan comprimidas como `data:image/jpeg;base64,...` en Firestore. Es suficiente para demo académica y evita servicios de pago. Si el proyecto creciera, habría que migrarlas a almacenamiento de objetos o CDN.
- Los contadores `followers`, `following`, `recipes` y `likes` están denormalizados para pintar rápido. En una app de producción estricta convendría mover esos contadores a Cloud Functions o a un backend propio.
- La presencia de chat se guarda en `chats/{chatId}.presence.{uid}`. Es simple y gratuita, aunque no sustituye a un sistema de presencia dedicado.
- Las reglas priorizan bloquear escrituras peligrosas, pero no reemplazan moderación ni validación profunda de contenido.

## Despliegue

Con Firebase CLI configurado para el proyecto:

```bash
firebase deploy --only firestore
```

Antes de presentar la app, conviene probar:

- Crear cuenta y editar perfil.
- Crear, editar, comentar, guardar y borrar receta propia.
- Dar y quitar like.
- Seguir y dejar de seguir.
- Abrir listas de seguidores y siguiendo.
- Crear chat, enviar mensajes, ver presencia y leer mensajes.
