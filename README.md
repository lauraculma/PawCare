# PawCare

Sistema integral de monitoreo biometrico IoT y gestion clinica veterinaria. Este
repositorio contiene la plataforma web desarrollada para el proyecto academico
PawCare Technologies S.A.S.

La aplicacion centraliza la gestion de mascotas, las citas veterinarias, la agenda
clinica, el historial medico y las alertas generadas a partir de datos fisiologicos
del collar inteligente.

## Modulos

- **Autenticacion y roles**: registro e inicio de sesion con roles `ADMIN`,
  `VETERINARIAN` y `OWNER`.
- **Mascotas**: registro, consulta, edicion y eliminacion con aislamiento por
  propietario.
- **Citas**: agendamiento, reprogramacion, cancelacion y control de conflictos de
  horario.
- **Agenda veterinaria**: vista diaria y semanal de la agenda del profesional.
- **Historial clinico**: notas, diagnosticos y formulas asociadas a cada atencion.
- **Telemetria y alertas**: ingesta simulada de signos vitales y generacion de
  alertas por umbrales fisiologicos.
- **Collar inteligente**: pagina informativa del dispositivo (sin pasarela de pago
  en el alcance del MVP).

## Stack

| Capa | Tecnologia |
| --- | --- |
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| Backend | Spring Boot 4.1, Java 17, Spring Security, JPA |
| Base de datos | PostgreSQL 17, Flyway |
| Pruebas | JUnit 5, MockMvc, Testcontainers |
| Infraestructura | Docker, Docker Compose, Nginx |
| CI | GitHub Actions |

La decision de usar Spring Boot en lugar de NestJS se documenta en
[`docs/adr/0001-backend-spring-boot.md`](docs/adr/0001-backend-spring-boot.md).

## Estructura del repositorio

```text
.
├── backend/          API REST en Spring Boot
├── frontend/         Aplicacion web en Next.js
├── docs/             Documento academico, ADRs y guias
├── .github/          Plantillas y workflows de GitHub Actions
├── docker-compose.yml
└── Makefile
```

## Inicio rapido (desarrollo)

Requisitos: Java 17 o superior, Node.js 22 o superior, PostgreSQL 16 o superior
y Docker (opcional para la base de datos).

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env.local

# Base de datos local
docker compose up -d db

# Backend
cd backend && ./mvnw spring-boot:run

# Frontend (en otra terminal)
cd frontend && npm install && npm run dev
```

- Frontend: <http://localhost:3000>
- API: <http://localhost:8080/api>
- Swagger: <http://localhost:8080/swagger-ui.html>

## Despliegue con Docker

```bash
cp .env.example .env
docker compose up --build -d
```

La guia completa de despliegue, respaldo y solucion de problemas esta en
[`docs/deployment.md`](docs/deployment.md).

## Flujo de trabajo

Se trabaja con ramas `feat/*`, `fix/*` y `chore/*` que se integran a `dev` mediante
Pull Request. `main` solo recibe versiones estables etiquetadas. Consulta
[`CONTRIBUTING.md`](CONTRIBUTING.md) para las convenciones de commits, ramas y
pruebas.

## Documentacion

- [`docs/ConstruccionSoftware.pdf`](docs/ConstruccionSoftware.pdf): informe tecnico
  del proyecto.
- [`docs/api.md`](docs/api.md): referencia de endpoints.
- [`docs/deployment.md`](docs/deployment.md): despliegue y operacion.
- [`docs/adr/`](docs/adr/): decisiones de arquitectura.

## Equipo

- Laura Sofia Culma Ospina
- Brayan Camilo Gomez Rodriguez
- Juan Diego Peralta Barbosa

Universidad Distrital Francisco Jose de Caldas.
