# PawCare - Docker Compose

## Requisitos
- Docker Engine 24+
- Docker Compose v2

## Variables de entorno
Copia `.env.example` a `.env` y ajusta:
```
JWT_SECRET=change-me-please-32-chars
```

## Comandos

Levantar servicios:
```bash
docker compose up -d --build
```

Ver logs:
```bash
docker compose logs -f backend
docker compose logs -f frontend
```

Bajar servicios:
```bash
docker compose down -v
```

### Notas
- Frontend expone http://localhost:3000
- Backend expone http://localhost:8080/api
- Nginx proxy en http://localhost
- DB Postgres en localhost:5432
```

```bash
docker compose ps
docker compose exec db psql -U pawcare -d pawcare -c "\dt"
```

