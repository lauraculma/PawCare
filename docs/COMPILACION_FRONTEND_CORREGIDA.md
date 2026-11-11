# Correccion de compilacion del frontend

## Contrato de registro

El endpoint `POST /api/auth/register` devuelve `RegistrationResponse`, no `AuthResponse`.

- `frontend/lib/types.ts`: declara `RegistrationResponse` con `requiresApproval` y `user`.
- `frontend/lib/api.ts`: `authApi.register` devuelve `Promise<RegistrationResponse>`.
- `frontend/components/providers/SessionProvider.tsx`: `register` devuelve `Promise<RegistrationResponse>` y solo establece la sesion cuando `requiresApproval` es `false`.
- `frontend/app/login/page.tsx`: muestra el mensaje de solicitud pendiente para veterinarios.

Las anotaciones explicitas de retorno evitan que el registro sea inferido como `AuthResponse`. El login sigue utilizando `AuthResponse`.

## Construccion

Desde la carpeta raiz del proyecto:

```cmd
docker compose build --no-cache frontend
docker compose up -d
```

No ejecutar `docker compose down -v`: elimina el volumen de PostgreSQL.

## Nota de validacion

La coherencia estatica del contrato se reviso en los tres archivos. La compilacion completa de Next.js y las pruebas de backend deben ejecutarse en el equipo de desarrollo, ya que requieren descargar dependencias y/o disponer de Docker.
