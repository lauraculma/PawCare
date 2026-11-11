import type {
  Alert,
  ApiErrorBody,
  Appointment,
  AppointmentPayload,
  AppointmentStatus,
  AppointmentUpdatePayload,
  AuthResponse,
  ClinicalRecord,
  ClinicalRecordPayload,
  LoginPayload,
  PageResponse,
  Pet,
  PetPayload,
  RegisterPayload,
  User,
  VitalSign,
  VitalSignPayload,
} from "@/lib/types";

export const API_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export class ApiError extends Error {
  status: number;
  details?: Record<string, string> | null;

  constructor(message: string, status: number, details?: Record<string, string> | null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const body = await response.json().catch(() => null);

  if (!response.ok) {
    const error = body as ApiErrorBody | null;
    throw new ApiError(
      error?.message ?? "No fue posible completar la solicitud",
      response.status,
      error?.details,
    );
  }

  return body as T;
}

function toQuery(params: Record<string, string | number | boolean | undefined | null>) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      query.set(key, String(value));
    }
  });
  const serialized = query.toString();
  return serialized ? `?${serialized}` : "";
}

export const authApi = {
  login: (payload: LoginPayload) =>
    request<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  register: (payload: RegisterPayload) =>
    request<AuthResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  me: () => request<User>("/auth/me"),
  logout: () => request<void>("/auth/logout", { method: "POST" }),
};

export interface PetQuery {
  search?: string;
  page?: number;
  size?: number;
}

export const petsApi = {
  list: (query: PetQuery = {}) =>
    request<PageResponse<Pet>>(
      `/pets${toQuery({ search: query.search, page: query.page ?? 0, size: query.size ?? 20 })}`,
    ),
  get: (id: number) => request<Pet>(`/pets/${id}`),
  create: (payload: PetPayload) =>
    request<Pet>("/pets", { method: "POST", body: JSON.stringify(payload) }),
  update: (id: number, payload: PetPayload) =>
    request<Pet>(`/pets/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  remove: (id: number) => request<void>(`/pets/${id}`, { method: "DELETE" }),
};

export interface AppointmentQuery {
  veterinarianId?: number;
  petId?: number;
  status?: AppointmentStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export const appointmentsApi = {
  list: (query: AppointmentQuery = {}) =>
    request<PageResponse<Appointment>>(
      `/appointments${toQuery({
        veterinarianId: query.veterinarianId,
        petId: query.petId,
        status: query.status,
        from: query.from,
        to: query.to,
        page: query.page ?? 0,
        size: query.size ?? 50,
      })}`,
    ),
  get: (id: number) => request<Appointment>(`/appointments/${id}`),
  create: (payload: AppointmentPayload) =>
    request<Appointment>("/appointments", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  update: (id: number, payload: AppointmentUpdatePayload) =>
    request<Appointment>(`/appointments/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  cancel: (id: number) =>
    request<Appointment>(`/appointments/${id}/cancel`, { method: "PATCH" }),
  updateStatus: (id: number, status: AppointmentStatus) =>
    request<Appointment>(`/appointments/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),
};

export interface ClinicalRecordQuery {
  petId?: number;
  appointmentId?: number;
  page?: number;
  size?: number;
}

export const clinicalRecordsApi = {
  list: (query: ClinicalRecordQuery = {}) =>
    request<PageResponse<ClinicalRecord>>(
      `/clinical-records${toQuery({
        petId: query.petId,
        appointmentId: query.appointmentId,
        page: query.page ?? 0,
        size: query.size ?? 50,
      })}`,
    ),
  get: (id: number) => request<ClinicalRecord>(`/clinical-records/${id}`),
  create: (payload: ClinicalRecordPayload) =>
    request<ClinicalRecord>("/clinical-records", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  update: (id: number, payload: ClinicalRecordPayload) =>
    request<ClinicalRecord>(`/clinical-records/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
};

export const vitalsApi = {
  list: (petId: number, from?: string, to?: string) =>
    request<PageResponse<VitalSign>>(
      `/pets/${petId}/vitals${toQuery({ from, to, size: 50 })}`,
    ),
  ingest: (payload: VitalSignPayload) =>
    request<VitalSign>("/vitals", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
};

export interface AlertQuery {
  petId?: number;
  acknowledged?: boolean;
  page?: number;
  size?: number;
}

export const alertsApi = {
  list: (query: AlertQuery = {}) =>
    request<PageResponse<Alert>>(
      `/alerts${toQuery({
        petId: query.petId,
        acknowledged: query.acknowledged,
        page: query.page ?? 0,
        size: query.size ?? 50,
      })}`,
    ),
  acknowledge: (id: number) =>
    request<Alert>(`/alerts/${id}/acknowledge`, { method: "PATCH" }),
};

export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.details) {
      const first = Object.values(error.details)[0];
      if (first) {
        return first;
      }
    }
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "Ocurrio un error inesperado";
}

export const healthApi = {
  check: () => request<{ status: string; service: string }>("/health"),
};
