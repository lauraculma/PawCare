# Release v1.0.0

## Auditoría y correcciones de mantenimiento (11 de noviembre de 2026)
- Recuperación de sesiones: el frontend ya no redirige al panel solo por encontrar una cookie; valida el JWT y expira cookies inválidas.
- El endpoint de logout queda disponible para borrar sesiones vencidas.
- Filtros de mascotas, citas, alertas e historias clínicas migrados a consultas dinámicas con Criteria/Specifications para evitar parámetros nulos sin tipo en PostgreSQL.
- Corrección de la validación de solapamiento de citas al crear y editar.
- Validación de JWT también comprueba que la cuenta siga activa.
- Ajustes de formularios: fecha de nacimiento enviada como `LocalDate`, validación de contraseña alineada con backend y permisos de acciones por rol.
- Docker Compose usa Nginx como única entrada web, configuración de puertos por `.env`, y contexto de build sin secretos ni dependencias locales.
- Dockerfile backend corrige permisos del Maven Wrapper; frontend y CI usan Node 22.
- Corrección del script SQL de demostración para el esquema actual.

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
