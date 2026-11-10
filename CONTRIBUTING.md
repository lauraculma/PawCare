# Guia de contribucion

## Ramas

- `main`: versiones estables y etiquetadas.
- `dev`: rama de integracion.
- `feat/<nombre>`: nueva funcionalidad.
- `fix/<nombre>`: correccion de errores.
- `chore/<nombre>`: configuracion, infraestructura o mantenimiento.
- `docs/<nombre>`: documentacion.

Crea la rama desde `dev` y abre un Pull Request hacia `dev`. Solo los releases se
integran de `dev` a `main`.

## Commits

Se usa Conventional Commits:

```text
feat(auth): agregar inicio de sesion con JWT
fix(citas): evitar solapamiento de horarios
chore(docker): agregar healthcheck al backend
docs(api): documentar endpoints de mascotas
```

Tipos permitidos: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`,
`build`, `perf`, `style`.

## Pull Requests

Cada PR debe incluir:

1. Descripcion del cambio y modulo afectado.
2. Como se probo (comandos ejecutados).
3. Capturas si hay cambios visibles de interfaz.
4. Checklist de pruebas y documentacion.

## Pruebas antes de abrir un PR

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm run build
```

Si el cambio afecta infraestructura:

```bash
docker compose up --build -d
docker compose ps
```

## Estilo de codigo

- Backend: Java 17, capas `controller`, `service`, `repository`, DTOs con records y
  validacion con Jakarta Validation.
- Frontend: TypeScript estricto, componentes en `components`, acceso a datos en
  `lib`, sin llamadas `fetch` directas desde componentes de presentacion.
- No se admiten secretos en el repositorio. Usa `.env.example` como plantilla.
