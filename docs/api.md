# API de PawCare

Referencia de los endpoints implementados. La documentacion interactiva se publica en
`/swagger-ui.html` cuando el backend esta en ejecucion.

## Convenciones

- Base URL: `/api`
- Autenticacion: `Authorization: Bearer <token>` o cookie `pawcare_token`.
- Formato: JSON.
- Paginacion: parametros `page`, `size` y `sort`. Los listados devuelven `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last` y `empty`.
- Errores: `{ "timestamp", "status", "error", "message", "path", "details" }`.

## Endpoints

### Salud y autenticacion

| Metodo | Ruta | Rol | Descripcion |
| --- | --- | --- | --- |
| GET | `/api/health` | publico | Estado del servicio |
| POST | `/api/auth/register` | publico | Registro de propietario (sesion inmediata) o solicitud veterinaria pendiente |
| POST | `/api/auth/login` | publico | Inicio de sesion |
| GET | `/api/auth/me` | autenticado | Perfil del usuario actual |
| POST | `/api/auth/logout` | publico | Expira la cookie `pawcare_token` |

### Mascotas

| Metodo | Ruta | Rol | Descripcion |
| --- | --- | --- | --- |
| GET | `/api/pets` | segun rol | Listado paginado; filtros `search`, `page`, `size` |
| POST | `/api/pets` | autenticado | Registro de mascota |
| GET | `/api/pets/{id}` | segun rol | Detalle de la mascota |
| PUT | `/api/pets/{id}` | segun rol | Edicion de la mascota |
| DELETE | `/api/pets/{id}` | segun rol | Baja logica de la mascota |

### Citas

| Metodo | Ruta | Rol | Descripcion |
| --- | --- | --- | --- |
| GET | `/api/appointments` | segun rol | Listado paginado; filtros `veterinarianId`, `petId`, `status`, `from`, `to` |
| GET | `/api/appointments/{id}` | segun rol | Detalle de la cita |
| POST | `/api/appointments` | segun rol | Agendamiento con validacion de solapamiento |
| PUT | `/api/appointments/{id}` | segun rol | Reprogramacion de la cita |
| PATCH | `/api/appointments/{id}/cancel` | segun rol | Cancelacion de la cita |
| PATCH | `/api/appointments/{id}/status` | veterinario/admin | Cambio de estado (`SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`) |

### Historial clinico y formulas medicas

| Metodo | Ruta | Rol | Descripcion |
| --- | --- | --- | --- |
| GET | `/api/clinical-records` | segun rol | Listado paginado; filtros `petId`, `appointmentId` |
| GET | `/api/clinical-records/{id}` | segun rol | Detalle del registro clinico |
| POST | `/api/clinical-records` | veterinario/admin | Creacion de registro clinico |
| PUT | `/api/clinical-records/{id}` | veterinario/admin | Edicion del registro clinico |
| GET | `/api/treatments` | segun rol | Listado paginado; filtros `clinicalRecordId`, `petId` |
| GET | `/api/treatments/{id}` | segun rol | Detalle del tratamiento |
| POST | `/api/treatments` | veterinario/admin | Alta de medicamento, dosis, frecuencia, duracion e indicaciones |
| PUT | `/api/treatments/{id}` | veterinario/admin | Edicion del tratamiento |

### Telemetria y alertas

| Metodo | Ruta | Rol | Descripcion |
| --- | --- | --- | --- |
| POST | `/api/vitals` | autenticado | Ingesta de signos vitales y generacion de alertas por umbrales |
| GET | `/api/pets/{id}/vitals` | segun rol | Signos vitales; filtros `from`, `to` |
| GET | `/api/alerts` | segun rol | Alertas; filtros `petId`, `acknowledged` |
| PATCH | `/api/alerts/{id}/acknowledge` | veterinario/admin | Reconocer una alerta |

### Administracion

| Metodo | Ruta | Rol | Descripcion |
| --- | --- | --- | --- |
| GET | `/api/admin/veterinarians/pending` | admin | Listar solicitudes veterinarias pendientes |
| PATCH | `/api/admin/veterinarians/{id}/approve` | admin | Aprobar una cuenta veterinaria |

### Flujo de aprobacion veterinaria

- El registro con `role: "VETERINARIAN"` crea la cuenta con `approved: false` y devuelve `requiresApproval: true`. No emite cookie ni token.
- El registro con `role: "OWNER"` crea una cuenta aprobada y devuelve los datos de autenticacion para iniciar sesion inmediatamente.
- El inicio de sesion de un veterinario pendiente devuelve HTTP 403 con un mensaje de aprobacion pendiente.
- Un administrador consulta las solicitudes pendientes y aprueba una con los endpoints indicados arriba.
