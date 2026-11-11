# API de PawCare

Documento en construccion. La referencia interactiva se publica en
`/swagger-ui.html` cuando el backend esta en ejecucion.

## Convenciones

- Base URL: `/api`
- Autenticacion: `Authorization: Bearer <token>` o cookie `pawcare_token`.
- Formato: JSON.
- Errores: `{ "timestamp", "status", "error", "message", "path", "details" }`.

## Endpoints previstos

| Metodo | Ruta | Rol | Descripcion |
| --- | --- | --- | --- |
| GET | `/api/health` | publico | Estado del servicio |
| POST | `/api/auth/register` | publico | Registro de propietario (sesion inmediata) o solicitud veterinaria pendiente |
| POST | `/api/auth/login` | publico | Inicio de sesion |
| GET | `/api/auth/me` | autenticado | Perfil actual |
| POST | `/api/auth/logout` | autenticado | Cierre de sesion |
| GET/POST | `/api/pets` | segun rol | Listado y registro de mascotas |
| GET/PUT/DELETE | `/api/pets/{id}` | segun rol | Detalle, edicion y borrado |
| GET/POST | `/api/appointments` | segun rol | Listado y agendamiento |
| PUT | `/api/appointments/{id}` | segun rol | Reprogramacion |
| PATCH | `/api/appointments/{id}/cancel` | segun rol | Cancelacion |
| GET/POST | `/api/clinical-records` | veterinario/admin | Historial clinico |
| POST | `/api/vitals` | dispositivo/admin | Ingesta de telemetria |
| GET | `/api/pets/{id}/vitals` | segun rol | Signos vitales |
| GET | `/api/alerts` | segun rol | Alertas generadas |
| PATCH | `/api/alerts/{id}/acknowledge` | veterinario/admin | Reconocer alerta |
| GET | `/api/admin/veterinarians/pending` | admin | Listar solicitudes veterinarias pendientes |
| PATCH | `/api/admin/veterinarians/{id}/approve` | admin | Aprobar una cuenta veterinaria |


### Flujo de aprobacion veterinaria

- El registro con `role: "VETERINARIAN"` crea la cuenta con `approved: false` y devuelve `requiresApproval: true`. No emite cookie ni token.
- El registro con `role: "OWNER"` crea una cuenta aprobada y devuelve los datos de autenticacion para iniciar sesion inmediatamente.
- El inicio de sesion de un veterinario pendiente devuelve HTTP 403 con un mensaje de aprobacion pendiente.
- Un administrador consulta las solicitudes pendientes y aprueba una con los endpoints indicados arriba.
