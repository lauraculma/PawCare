# Despliegue de PawCare

## Requisitos
- Docker Engine 24+
- Docker Compose v2
- 4 GB RAM disponible
- Puerto 80 libre

## Variables de entorno
Crea un archivo `.env` en la raíz:
```
JWT_SECRET=un-secret-de-32-caracteres-minimo
```

## Despliegue local
```bash
docker compose up -d --build
```
Accesos:
- App: http://localhost
- API: http://localhost/api
- Swagger: http://localhost/api/swagger-ui.html
- DB: localhost:5432

## Smoke test
1. Crear usuario via `POST /api/auth/register`
2. Login `POST /api/auth/login` -> cookie `pawcare_token`
3. `GET /api/auth/me` debe retornar usuario autenticado
4. Crear mascota `POST /api/pets`
5. Crear cita `POST /api/appointments`
6. Ingestar vitales `POST /api/vitals` y verificar alertas en `GET /api/alerts`

## Respaldo
```bash
docker compose exec db pg_dump -U pawcare pawcare > backup.sql
```

## Recuperación
```bash
cat backup.sql | docker compose exec -T db psql -U pawcare -d pawcare
```

## Logs
```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f nginx
```

## Actualización
```bash
docker compose pull
docker compose up -d --build
```

## Solución de problemas
- **Base de datos no inicia**: verifica puertos y volumen `pgdata`.
- **JWT inválido**: asegura `JWT_SECRET` con al menos 32 caracteres.
- **CORS**: `CORS_ALLOWED_ORIGINS` debe coincidir con el dominio del frontend.
