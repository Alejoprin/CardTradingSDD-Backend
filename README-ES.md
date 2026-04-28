Markdown
# Plataforma de Intercambio de Cartas - API del Backend

Una interfaz de programación de aplicaciones (API) REST para una plataforma de intercambio de cartas coleccionables donde los usuarios pueden registrarse, gestionar su inventario, proponer intercambios y administrar su colección mediante el procesamiento de eventos asíncronos.

## ¿Qué hace esta plataforma?

### Para Comerciantes (Usuarios)

* **Registro y autenticación**: mediante sesiones seguras basadas en tokens JWT.
* **Exploración del catálogo**: búsqueda por nombre y filtros por rareza (Común, Rara, Épica, Legendaria), tipo (Monstruo, Hechizo, Trampa) o edición.
* **Gestión de inventario**: visualización de la colección personal, cantidades y registros de adquisición.
* **Creación de ofertas**: propuesta de intercambios seleccionando cartas propias y las deseadas de otro usuario.
* **Gestión de intercambios**: posibilidad de aceptar, rechazar o cancelar ofertas pendientes.
* **Notificaciones**: avisos por correo electrónico sobre el estado de las ofertas (creación, aceptación, rechazo o finalización).
* **Restablecimiento de contraseña**: proceso vía email con tokens de seguridad de tiempo limitado.

### Para Administradores

* **Gestión del catálogo**: creación, actualización y eliminación de cartas en el sistema.
* **Moderación**: capacidad para suspender o rehabilitar cuentas con registro de motivos.
* **Supervisión**: acceso a todos los intercambios, usuarios y métricas generales del sistema.

## Conceptos Principales

### Flujo de Intercambio

1. El **Usuario A** crea una oferta especificando qué cartas ofrece y cuáles solicita al **Usuario B**.
2. El **Usuario B** recibe un aviso por correo electrónico sobre la propuesta.
3. El **Usuario B** puede aceptar, rechazar o dejar que expire (se cancela automáticamente tras 7 días).
4. Si se acepta, el sistema transfiere las cartas automáticamente entre inventarios.
5. Ambos usuarios reciben una notificación con el resultado final.

### Reglas de Negocio

* No se permite el auto-intercambio (comerciar con uno mismo).
* Ambas partes deben poseer las cartas ofrecidas en el momento de crear la oferta.
* Límite máximo de 20 cartas por cada intercambio.
* Límite de 50 ofertas de intercambio creadas por usuario al día.
* Control de acceso: máximo 5 intentos de inicio de sesión por cada 15 minutos por correo.
* Limitación de la API: máximo 100 solicitudes por minuto por cada usuario autenticado.

## Descripción General de la API

| Grupo de Endpoints | Ruta Base        | Descripción                                                  |
|:-------------------|:-----------------|:-------------------------------------------------------------|
| **Auth**           | `/api/v1/auth`   | Registro, login, refresco de tokens y gestión de contraseñas |
| **Users**          | `/api/v1/users`  | Perfiles de usuario y gestión de inventarios                 |
| **Cards**          | `/api/v1/cards`  | Consulta y filtros del catálogo de cartas                    |
| **Trades**         | `/api/v1/trades` | Gestión completa del ciclo de vida de los intercambios       |
| **Admin**          | `/api/v1/admin`  | Control de usuarios, supervisión y estadísticas              |

La documentación técnica completa está disponible en **Swagger UI** a través de `/swagger-ui.html` cuando el servidor está activo.

## Autenticación

Todos los puntos de acceso (excepto registro y login) requieren un token JWT válido en el encabezado `Authorization`:

Authorization: Bearer <access_token>


* **Tokens de acceso**: duración de 1 hora.
* **Tokens de refresco**: duración de 7 días (usar `/api/v1/auth/refresh` para renovar).

## Stack Tecnológico

| Componente    | Tecnología                         |
|:--------------|:-----------------------------------|
| Entorno       | Java 17, Spring Boot 3.2           |
| Base de Datos | PostgreSQL 15                      |
| Caché         | Redis 7                            |
| Mensajería    | Apache Kafka                       |
| Seguridad     | JWT (JJWT), BCrypt                 |
| Correo        | Spring Mail + plantillas Thymeleaf |
| Documentación | SpringDoc OpenAPI (Swagger UI)     |
| Migraciones   | Flyway                             |

## Estructura del Proyecto

src/main/java/com/cardtrading/
auth/          Registro, login, JWT y perfiles
card/          Catálogo y administración de cartas
inventory/     Gestión de colecciones personales
trade/         Lógica de intercambios y caducidad
admin/         Funciones exclusivas de administrador
event/         Kafka: productores, consumidores y modelos
shared/        Seguridad, excepciones y configuración global



## Primeros Pasos

Consulta el archivo [DEVELOPMENT.md](DEVELOPMENT.md) para ver las instrucciones sobre cómo compilar y ejecutar el proyecto en un entorno local.

## Licencia

Este proyecto es de propiedad privada. Todos los derechos reservados.