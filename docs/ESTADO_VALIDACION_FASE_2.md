# Estado de validacion - Fase 2

## Revisiones realizadas

- Se reviso el flujo de registro, inicio de sesion, JWT y control de roles.
- Se agrego la migracion Flyway V7 para el estado de aprobacion de veterinarios.
- Se revisaron las rutas y servicios de administracion para aprobar veterinarios.
- Se valido la sintaxis YAML de `docker-compose.yml` con un parser YAML.
- Se reviso el SQL de datos demo y se corrigio el JOIN faltante para identificar al propietario en la insercion de signos vitales.

## Validaciones pendientes en un entorno con dependencias

En el entorno de preparacion no fue posible completar `mvn test` porque Maven Wrapper no pudo descargar Maven desde Maven Central. Tampoco se pudo completar la instalacion de dependencias del frontend, por lo que `tsc` no pudo resolver React/Next y sus tipos. No se dispuso de Docker Engine para construir las imagenes ni iniciar los contenedores.

En el computador de desarrollo, ejecutar desde la raiz:

```cmd
docker compose config
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

Para validar backend fuera de Docker:

```cmd
cd backend
mvnw.cmd test
```

Para validar frontend fuera de Docker:

```cmd
cd frontend
npm ci
npm run lint
npm run build
```

No se debe ejecutar `docker compose down -v` durante estas comprobaciones si se desea conservar los datos del volumen PostgreSQL.
