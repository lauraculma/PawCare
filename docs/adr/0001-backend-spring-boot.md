# ADR 0001: Spring Boot como backend de PawCare

- Estado: aceptado
- Fecha: 2026-11-10

## Contexto

El informe tecnico propone NestJS con Prisma para la API. Sin embargo, el
repositorio ya contenia un esqueleto funcional de Spring Boot 4.1 con Java 17,
dependencias de JPA, PostgreSQL y una prueba de integracion del endpoint de salud.
El equipo dispone de Java y Docker en el entorno de desarrollo.

## Decision

Se conserva Spring Boot como framework del backend y se implementa la persistencia
con JPA/Hibernate y migraciones versionadas con Flyway. La API mantiene el contrato
REST propuesto en el informe: autenticacion JWT, DTOs validados, control de acceso
por roles y documentacion OpenAPI.

## Consecuencias

- Se reutiliza el esqueleto y las pruebas existentes, reduciendo el tiempo de
  entrega del MVP.
- El modelo de datos se define mediante migraciones SQL en lugar de Prisma.
- La separacion por capas se conserva (`controller`, `service`, `repository`,
  `entity`, `dto`).
- El documento academico puede mencionar NestJS como propuesta; la implementacion
  real y su justificacion quedan registradas en este ADR.
