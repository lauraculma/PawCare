# Corrección del cambio entre inicio de sesión y registro

## Problema

En la página `/login`, los enlaces inferiores actualizaban la URL (`?mode=register` o `?mode=login`), pero el formulario no cambiaba porque `AuthPage` solo leía el parámetro `mode` en un `useEffect` ejecutado al montar el componente. Durante la navegación cliente de Next.js, la página se reutilizaba y ese efecto no volvía a ejecutarse.

## Solución

- Se centralizó el cambio de modo en `changeMode()`.
- El cambio actualiza el estado React y la URL con `router.replace()`.
- Se conserva el parámetro `next` y cualquier otro parámetro existente.
- Los botones superiores y los controles inferiores utilizan el mismo mecanismo.
- Al terminar un registro veterinario pendiente, se actualiza también la URL a `mode=login` sin perder el mensaje de confirmación.

## Archivo modificado

`frontend/app/login/page.tsx`

## Aplicar en Docker

Desde la raíz del proyecto, reconstruir y recrear el frontend:

```bash
docker compose up -d --build frontend
```

Después abrir `http://localhost:3000/login` y comprobar ambos sentidos:

- `Registrate` debe mostrar el formulario de registro y la URL `?mode=register`.
- `Inicia sesion` debe mostrar el formulario de acceso y la URL `?mode=login`.
