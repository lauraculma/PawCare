# Despliegue de PawCare

## Requisitos

- Docker Engine 24 o superior.
- Docker Compose v2.
- 4 GB de RAM disponible.
- Puertos `APP_PORT` y `DB_PORT` disponibles en el equipo anfitrión.

## Variables de entorno

Copia `.env.example` a `.env` y reemplaza `JWT_SECRET` por una cadena aleatoria de al menos 32 caracteres. Compose exige que esta variable esté definida. Mantén el mismo secreto entre reinicios para que los tokens emitidos sigan siendo verificables. Usa `COOKIE_SECURE=false` en HTTP local y `COOKIE_SECURE=true` cuando el sitio esté publicado con HTTPS.

La base de datos del contenedor utiliza `DOCKER_DB_USER` y `DOCKER_DB_PASSWORD`. PostgreSQL solo aplica estas variables cuando inicializa por primera vez el volumen `pgdata`; cambiar el archivo `.env` no altera las credenciales de un volumen ya creado.

## Despliegue local con Docker

```bash
docker compose up -d --build
docker compose ps
```

Accesos:

- Aplicación: `http://localhost` si `APP_PORT=80`, o `http://localhost:APP_PORT` si se usa otro puerto.
- API: `http://localhost/api`.
- Salud de la API: `http://localhost/api/health`.
- Swagger: `http://localhost/api/swagger-ui.html`.
- PostgreSQL: `localhost:DB_PORT` desde el equipo anfitrión; dentro de Docker el backend utiliza `db:5432`.

El frontend utiliza `/api` como URL relativa. Por eso el despliegue debe abrirse a través de Nginx y no directamente desde el puerto interno de Next.js.

## Prueba básica de autenticación

1. Registrar una cuenta con `POST /api/auth/register`.
2. Iniciar sesión con `POST /api/auth/login`; la respuesta establece la cookie HttpOnly `pawcare_token`.
3. Consultar `GET /api/auth/me`; debe devolver el usuario autenticado.
4. Cerrar sesión con `POST /api/auth/logout`; el backend expira la cookie.

## Respaldo y recuperación

Crear respaldo:

```bash
docker compose exec db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > backup.sql
```

Restaurar respaldo:

```bash
cat backup.sql | docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

## Operación

Ver logs:

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f nginx
```

Detener los servicios sin borrar la base de datos:

```bash
docker compose down
```

No utilices `docker compose down -v` a menos que quieras eliminar también el volumen de PostgreSQL.

Actualizar imágenes y reconstruir:

```bash
docker compose pull
docker compose up -d --build
```
