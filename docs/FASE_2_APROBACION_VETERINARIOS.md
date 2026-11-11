# Fase 2: ciclo de aprobacion de cuentas veterinarias

## Comportamiento implementado

1. Los propietarios se registran y pueden iniciar sesion inmediatamente.
2. Los veterinarios pueden registrarse, pero la cuenta se crea con `approved = false`.
3. El registro veterinario no emite JWT ni cookie de sesion.
4. Un veterinario pendiente recibe HTTP 403 con un mensaje claro al intentar iniciar sesion.
5. Solo un usuario con rol `ADMIN` puede consultar las solicitudes y aprobarlas.
6. Al aprobarse, la cuenta veterinaria puede iniciar sesion con normalidad.
7. Las cuentas existentes quedan aprobadas por defecto mediante la migracion V7, para no bloquear usuarios actuales.

## Primer administrador

La aplicacion no permite crear administradores mediante registro publico. Para crear el primer administrador, configura `BOOTSTRAP_ADMIN_ENABLED=true`, `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` (minimo 12 caracteres) en el archivo `.env`, luego reconstruye/inicia el backend. La cuenta solo se crea si el correo no existe. Una vez creada, cambia `BOOTSTRAP_ADMIN_ENABLED=false`.

## Interfaz

El administrador encuentra la opcion **Aprobaciones** en la navegacion y en el panel. La ruta directa es `/admin/veterinarios`.

## Migracion

Flyway ejecuta `V7__veterinarian_approval.sql`, que agrega `users.approved` con valor inicial `TRUE` para preservar el acceso de cuentas existentes.
