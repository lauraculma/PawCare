# PawCare con Docker Compose

## Requisitos

- Docker Desktop actualizado (Docker Engine 24 o superior).
- Docker Compose v2.
- Al menos 4 GB de RAM disponible.

## Preparar el entorno

En la carpeta raíz del proyecto, crea el archivo `.env` a partir de la plantilla:

```cmd
copy .env.example .env
```

En PowerShell también puedes usar `Copy-Item .env.example .env`.

Antes de iniciar, revisa:

- `APP_PORT`: puerto desde el que abrirás PawCare. Por defecto es `80`.
- `DB_PORT`: puerto de PostgreSQL publicado en Windows. Usa `5434` si ya tienes PostgreSQL local en `5432`.
- `DB_USERNAME` y `DB_PASSWORD`: credenciales internas de PostgreSQL (tambien se aceptan `DOCKER_DB_USER` y `DOCKER_DB_PASSWORD` como alias). Si el volumen `pgdata` ya existe, deben coincidir con las credenciales con las que fue creado.
- `JWT_SECRET`: reemplaza el valor de ejemplo por una cadena aleatoria de al menos 32 caracteres y mantenla estable entre reinicios.
- `COOKIE_SECURE`: déjalo en `false` para HTTP local; configúralo en `true` cuando publiques el sistema detrás de HTTPS.

## Iniciar PawCare

Desde la raíz del proyecto:

```cmd
docker compose up -d --build
```

Comprueba el estado:

```cmd
docker compose ps
```

Abre la aplicación en `http://localhost` si `APP_PORT=80`. Si configuraste otro puerto, úsalo en la dirección, por ejemplo `http://localhost:3000`.

La aplicación entra por Nginx. Nginx enruta `/` al frontend y `/api/` al backend. No abras directamente el frontend en el puerto 3000 para el despliegue Docker.

## Ver registros

```cmd
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f nginx
```

## Detener y reiniciar

Detener los contenedores sin borrar los datos:

```cmd
docker compose down
```

Volver a iniciar:

```cmd
docker compose up -d
```

> No uses `docker compose down -v` salvo que quieras eliminar permanentemente el volumen `pgdata` y todos los datos de PostgreSQL.

## Base de datos y respaldo

Listar las tablas:

```cmd
docker compose exec db sh -c "psql -U $POSTGRES_USER -d $POSTGRES_DB -c '\\dt'"
```

Crear respaldo:

```cmd
docker compose exec db sh -c "pg_dump -U $POSTGRES_USER -d $POSTGRES_DB" > backup.sql
```

Restaurar un respaldo:

```cmd
type backup.sql | docker compose exec -T db sh -c "psql -U $POSTGRES_USER -d $POSTGRES_DB"
```

Guarda `backup.sql` fuera de la carpeta del proyecto y no lo compartas si contiene información personal.

## Solución de problemas

- **Puerto ocupado:** cambia `APP_PORT` o `DB_PORT` en `.env` y vuelve a iniciar Compose.
- **Base de datos no inicia:** revisa `docker compose logs db`. No borres `pgdata` como primer intento.
- **Error de autenticación PostgreSQL:** confirma que `DOCKER_DB_USER` y `DOCKER_DB_PASSWORD` coinciden con los valores usados al crear el volumen. Cambiar `.env` no cambia automáticamente un usuario ya existente.
- **Sesión no válida:** la aplicación expira la cookie `pawcare_token` cuando `/api/auth/me` devuelve `401`. Si fuera necesario, borra esa cookie desde las herramientas del navegador y vuelve a iniciar sesión.
- **API no encontrada:** accede por Nginx usando `http://localhost` (o el puerto `APP_PORT`), no por el puerto directo del frontend.


## Primer administrador y aprobacion de veterinarios

El registro publico crea cuentas de propietario activas. Las cuentas con rol veterinario quedan guardadas como pendientes y no reciben una cookie de sesion ni pueden iniciar sesion hasta que un administrador las apruebe.

Para crear el primer administrador, configura en `.env` estas variables antes del primer `docker compose up`:

```env
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@tudominio.com
BOOTSTRAP_ADMIN_PASSWORD=una-clave-larga-de-al-menos-12-caracteres
BOOTSTRAP_ADMIN_FULL_NAME=Administrador PawCare
```

Al iniciar, PawCare crea esa cuenta solo si el correo no existe. No cambia la clave ni el rol de una cuenta existente. Inicia sesion con esa cuenta y abre **Aprobaciones** en el menu o visita `/admin/veterinarios`. Desde alli puedes aprobar solicitudes veterinarias.

Despues de crear el administrador inicial, cambia `BOOTSTRAP_ADMIN_ENABLED=false` en `.env` y reinicia el backend. No compartas el archivo `.env`.
