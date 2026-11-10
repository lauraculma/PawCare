.PHONY: help backend frontend test infra-up infra-down up down logs

help:
	@echo "Comandos disponibles:"
	@echo "  make backend   Ejecuta el backend en modo desarrollo"
	@echo "  make frontend  Ejecuta el frontend en modo desarrollo"
	@echo "  make test      Ejecuta pruebas de backend y build de frontend"
	@echo "  make infra-up  Levanta solo PostgreSQL"
	@echo "  make up        Levanta todo el stack con Docker Compose"
	@echo "  make down      Detiene el stack"
	@echo "  make logs      Sigue los logs del stack"

backend:
	cd backend && ./mvnw spring-boot:run

frontend:
	cd frontend && npm run dev

test:
	cd backend && ./mvnw verify
	cd frontend && npm run lint && npm run build

infra-up:
	docker compose up -d db

infra-down:
	docker compose stop db

up:
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f
