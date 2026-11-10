# Release v1.0.0

## Cambios principales
- Backend Spring Boot 4.1.2 con autenticación JWT, roles ADMIN/VETERINARIAN/OWNER, Flyway migrations hasta V6.
- Endpoints implementados: `/api/auth`, `/api/pets`, `/api/appointments`, `/api/clinical-records`, `/api/vitals`, `/api/alerts`.
- Frontend Next.js 16 con autenticación, panel, mascotas, citas, agenda, historial y alertas.
- Contenerización completa con Docker Compose y Nginx reverse proxy.
- CI con GitHub Actions para tests backend y build frontend.

## Pruebas de humo
- Health check `GET /api/health` -> 200
- Registro y login con cookie HttpOnly
- CRUD mascotas con permisos por rol
- Creación de cita con validación de conflictos
- Ingesta de signos vitales y generación de alertas
- UI accesible vía Nginx en puerto 80

## Migración
No hay migraciones de datos entre versiones previas. Base nueva con Flyway.

## Notas
- Testcontainers desactivado sin Docker daemon local.
- ADMIN no puede registrarse públicamente.
