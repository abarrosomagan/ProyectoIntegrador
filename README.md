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

## Estado actual

El proyecto **no está finalizado**.

Actualmente se encuentra en una fase activa de desarrollo que incluye:
- Diseño completo de la interfaz en Figma
- Creación de la estructura base del proyecto Android
- Preparación de la arquitectura para futuras funcionalidades

Las funcionalidades se irán implementando y refinando progresivamente.

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


## Funcionalidades previstas

- Registro e inicio de sesión de usuarios
- Visualización de recetas en formato feed
- Publicación de recetas con imagen y etiquetas
- Guardado de recetas favoritas
- Perfil de usuario con publicaciones propias
- Sistema de mensajería entre usuarios
- Navegación mediante barra inferior

Estas funcionalidades se implementarán de forma gradual.

---

## Arquitectura y estructura

La aplicación está planteada con una estructura modular y escalable, orientada a facilitar el mantenimiento y la ampliación futura.

Estructura general prevista:

```
app/
 ├── ui/
 ├── screens/
 ├── components/
 ├── navigation/
 ├── res/
 └── MainActivity.kt
```

La arquitectura podrá adaptarse conforme avance el desarrollo.

---

## Tecnologías utilizadas

- Kotlin
- Android SDK
- Jetpack Compose y/o XML
- Material Design / Material 3
- Gradle (Kotlin DSL)
- Git
- GitHub
- Figma

---

## Instalación y ejecución

Para trabajar con el proyecto en local:

```
git clone https://github.com/USUARIO/REPOSITORIO.git
```

1. Abrir el proyecto en Android Studio
2. Sincronizar Gradle
3. Ejecutar en un emulador o dispositivo físico

---

## Evolución del proyecto

El proyecto se desarrollará de manera incremental, incorporando:
- Nuevas pantallas
- Implementación real de funcionalidades
- Mejoras de diseño y experiencia de usuario
- Refactorización progresiva del código

Cada avance quedará reflejado mediante commits sucesivos en el repositorio.

---

## Licencia

Proyecto desarrollado con fines **académicos**.

El contenido puede ser utilizado como referencia educativa citando a los autores.

