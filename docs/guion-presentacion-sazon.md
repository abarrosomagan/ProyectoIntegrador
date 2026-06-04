# Guion de presentacion de Sazon

Documento de apoyo para exponer la presentacion `Presentacion Sazon`.

## Diapositiva 1. Portada

Buenos dias.

Somos Fernando Cecilia y Alejandro Barroso, y hoy vamos a presentar nuestro proyecto: Sazon.

Sazon nace como una aplicacion movil pensada para acercar la cocina al dia a dia de los usuarios. No queriamos hacer solo un recetario tradicional, sino una comunidad donde compartir, descubrir y conectar a traves de la gastronomia.

La idea es que la cocina no se quede unicamente en buscar una receta, sino que tambien pueda convertirse en una experiencia social.

## Diapositiva 2. Que es Sazon

Sazon es una red social para amantes de la cocina.

La aplicacion permite compartir recetas, descubrir nuevos platos, guardar recetas favoritas, conectar con otros usuarios y completar una lista de la compra a partir de los ingredientes.

Por tanto, unimos tres partes importantes:

- La parte creativa, porque el usuario puede publicar sus propias recetas.
- La parte social, porque puede seguir perfiles, comentar y comunicarse.
- La parte practica, porque puede guardar recetas y organizar ingredientes.

El objetivo es que Sazon sea una aplicacion util tanto para descubrir ideas como para cocinar realmente.

## Diapositiva 3. El problema y la solucion

Actualmente, cuando una persona busca recetas, normalmente tiene que consultar muchas fuentes distintas: paginas web, videos, redes sociales o incluso notas personales.

Esto provoca varios problemas. El contenido queda disperso, cuesta encontrarlo de nuevo, no siempre esta bien organizado y, ademas, muchas plataformas no estan centradas especificamente en crear comunidad alrededor de la cocina.

Sazon propone resolver esto reuniendo en una unica aplicacion la busqueda, publicacion y almacenamiento de recetas.

Ademas, gracias al sistema de perfiles, seguidores y mensajeria, los usuarios pueden interactuar entre ellos, compartir experiencias y conectar con personas con intereses culinarios similares.

La solucion no es solo guardar recetas, sino construir un espacio comun para compartirlas y vivirlas.

## Diapositiva 4. Identidad visual

Para la identidad visual hemos buscado una estetica calida, cercana y relacionada con el mundo de la cocina.

La paleta principal se basa en tonos naranjas, porque transmiten energia, creatividad y cercania. Tambien usamos blanco para mantener limpieza visual y tonos oscuros para asegurar una buena legibilidad.

La intencion era que la aplicacion se sintiera moderna, pero tambien familiar. Queremos que visualmente recuerde a un entorno gastronomico: cercano, apetecible y facil de usar.

## Diapositiva 5. Navegacion

La aplicacion esta organizada en tres secciones principales: Feed, Chats y Perfil.

Esta estructura permite que el usuario entienda rapidamente como moverse por la app.

El Feed es la entrada principal para descubrir recetas. Chats permite la comunicacion directa entre usuarios. Perfil funciona como el espacio personal donde el usuario gestiona su identidad, sus recetas y sus interacciones.

Esta navegacion sencilla ayuda a que la experiencia sea clara desde el primer uso.

## Diapositiva 6. Login y registro

Las pantallas de Login y Registro permiten acceder a todas las funcionalidades de Sazon de forma segura.

El usuario puede crear una cuenta nueva o iniciar sesion con una cuenta existente. Una vez dentro, ya puede publicar recetas, guardar contenido, comentar, seguir a otros usuarios y utilizar la mensajeria.

Esta parte esta conectada con Firebase Authentication, que se encarga de gestionar la autenticacion de usuarios.

Tambien se contempla la recuperacion de contrasena y el aviso de correo no verificado, para que la gestion de cuenta sea mas completa.

## Diapositiva 7. Feed principal

El Feed principal muestra las publicaciones de la comunidad en un formato visual que facilita la exploracion.

Desde esta pantalla el usuario puede descubrir recetas, buscar contenido concreto, abrir recetas en detalle, acceder a perfiles de otros usuarios y guardar recetas para consultarlas mas adelante.

El Feed es una de las pantallas mas importantes porque da vida a la comunidad. Es donde se ve el contenido nuevo, las recetas populares y la actividad de otros usuarios.

En la aplicacion tambien se han planteado modos como recetas recomendadas, recetas de usuarios seguidos y recetas populares, para que la experiencia de descubrimiento sea mas rica.

## Diapositiva 8. Receta

Cuando el usuario selecciona una receta desde el Feed, accede a una vista detallada.

En esta pantalla se reune toda la informacion del plato: imagen, titulo, descripcion, ingredientes, pasos de preparacion, datos del autor y metadatos como tiempo, dificultad o raciones.

Tambien se pueden realizar acciones sociales, como dar me gusta, guardar la receta o comentar.

Ademas, la receta no se queda solo en la lectura. Desde el detalle se puede enviar a un chat, compartir fuera de la aplicacion, anadir ingredientes a la lista de la compra o abrir el modo cocinar paso a paso.

La idea es que el detalle sea el centro de la experiencia de cada receta.

## Diapositiva 9. Crear receta

La pantalla de creacion de recetas permite que cualquier usuario se convierta en creador de contenido dentro de Sazon.

A traves de un formulario sencillo, el usuario puede anadir una imagen, escribir el titulo y descripcion, introducir ingredientes, explicar los pasos de elaboracion, indicar dificultad, tiempo de preparacion, raciones y etiquetas.

Esta funcionalidad es clave porque convierte la aplicacion en una comunidad activa. Los usuarios no solo consumen recetas, sino que tambien aportan contenido propio.

Tambien se ha contemplado la edicion de recetas propias, para que el usuario pueda corregir o mejorar una receta despues de publicarla.

## Diapositiva 10. Perfil

El perfil actua como la identidad digital del usuario dentro de Sazon.

Desde esta pantalla se pueden consultar las recetas publicadas, las recetas guardadas y las recetas marcadas con me gusta.

Ademas, incorpora funcionalidades propias de una red social, como seguidores y seguidos. Esto permite que los usuarios construyan una comunidad y puedan seguir a personas cuyos platos les interesan.

El perfil tambien incluye informacion personal, estadisticas y opciones de gestion, como editar datos o acceder a ajustes.

Por eso, el perfil no es solo una pantalla informativa, sino una parte central de la experiencia social.

## Diapositiva 11. Mensajeria

Sazon incorpora un sistema de mensajeria privada entre usuarios.

Gracias a esta funcionalidad, los usuarios pueden establecer conversaciones directas, intercambiar consejos, resolver dudas sobre recetas o crear nuevas conexiones dentro de la comunidad gastronomica.

La mensajeria aporta una capa social mas profunda. No se limita a reaccionar con likes o comentarios, sino que permite una comunicacion directa.

En el desarrollo se ha trabajado con mensajes en tiempo real, indicadores de lectura, presencia, escritura y opciones de gestion de conversaciones.

## Diapositiva 12. Guardados y favoritos

Una de las funcionalidades mas utiles de Sazon es guardar recetas para consultarlas posteriormente.

Esto permite que cada usuario cree su propia coleccion personalizada de recetas favoritas y pueda acceder a ellas rapidamente desde su perfil.

Es una funcion sencilla, pero muy importante en el uso real de la aplicacion. Muchas veces encontramos una receta interesante y no queremos perderla.

Con los guardados, Sazon facilita que el usuario pueda organizar sus ideas y volver a ellas cuando quiera cocinar.

## Diapositiva 13. Video publicitario

En este punto presentamos el video publicitario de la aplicacion.

El objetivo del video es transmitir la esencia de Sazon: una aplicacion calida, visual, social y practica, pensada para personas que disfrutan cocinando y compartiendo recetas.

Aqui dejamos que el video refuerce el ambiente de la app y muestre su proyeccion como producto.

## Diapositiva 14. Cierre

Para terminar, Sazon es una aplicacion que combina recetas, comunidad y utilidad practica.

Nuestro objetivo ha sido crear una experiencia completa: desde descubrir una receta, guardarla y comentarla, hasta hablar con otros usuarios y llevarla al momento real de cocinar.

Con este proyecto hemos integrado diseno de interfaces, navegacion, autenticacion, base de datos, interaccion social y funcionalidades pensadas para un uso real.

Muchas gracias por vuestra atencion. Estamos disponibles para responder cualquier pregunta.

## Cierre breve alternativo

Si necesitamos cerrar de forma mas rapida, podemos decir:

Sazon es una red social gastronomica que permite publicar recetas, descubrir platos, guardar favoritos, seguir usuarios y comunicarse dentro de una comunidad de cocina.

La aplicacion une la parte social y la parte practica: no solo ayuda a encontrar recetas, sino tambien a organizarlas, compartirlas y llevarlas al momento de cocinar.

Muchas gracias.
