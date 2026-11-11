export type Role = "ADMIN" | "VETERINARIAN" | "OWNER";

export type AppointmentStatus =
  | "SCHEDULED"
  | "CONFIRMED"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_SHOW";

export type AlertSeverity = "WARNING" | "CRITICAL";

export type AlertType =
  | "BRADYCARDIA"
  | "TACHYCARDIA"
  | "LOW_SPO2"
  | "HYPOTHERMIA"
  | "FEVER";

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  phone?: string | null;
  approved: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMinutes: number;
  user: User;
}

export interface RegistrationResponse {
  requiresApproval: boolean;
  token?: string | null;
  tokenType?: string | null;
  expiresInMinutes: number;
  user: User;
}

export interface RegisterPayload {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
  role?: Role;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface OwnerSummary {
  id: number;
  fullName: string;
  email: string;
}

export interface Pet {
  id: number;
  owner: OwnerSummary;
  name: string;
  species: string;
  breed?: string | null;
  birthDate?: string | null;
  weightKg?: number | null;
  sex?: string | null;
  collarId?: string | null;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface PetPayload {
  name: string;
  species: string;
  breed?: string;
  birthDate?: string;
  weightKg?: number | null;
  sex?: string;
  collarId?: string;
  notes?: string;
  ownerId?: number | null;
}

export interface Appointment {
  id: number;
  petId: number;
  petName: string;
  petSpecies: string;
  owner: OwnerSummary;
  veterinarianId: number;
  veterinarianName: string;
  scheduledAt: string;
  endAt: string;
  durationMinutes: number;
  status: AppointmentStatus;
  reason: string;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface AppointmentPayload {
  petId: number;
  veterinarianId?: number | null;
  scheduledAt: string;
  durationMinutes: number;
  reason: string;
  notes?: string;
}

export interface AppointmentUpdatePayload {
  scheduledAt: string;
  durationMinutes: number;
  reason: string;
  notes?: string;
  veterinarianId?: number | null;
}

export interface ClinicalRecord {
  id: number;
  appointmentId: number;
  petId: number;
  petName: string;
  owner: OwnerSummary;
  veterinarianId: number;
  veterinarianName: string;
  diagnosis: string;
  treatment?: string | null;
  prescription?: string | null;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ClinicalRecordPayload {
  appointmentId: number;
  diagnosis: string;
  treatment?: string;
  prescription?: string;
  notes?: string;
}

export interface VitalSign {
  id: number;
  petId: number;
  petName: string;
  collarId?: string | null;
  heartRate?: number | null;
  spo2?: number | null;
  temperatureC?: number | null;
  activityLevel?: string | null;
  recordedAt: string;
  createdAt?: string;
  alerts: Alert[];
}

export interface VitalSignPayload {
  petId: number;
  collarId?: string;
  heartRate?: number | null;
  spo2?: number | null;
  temperatureC?: number | null;
  activityLevel?: string;
  recordedAt?: string;
}

export interface Alert {
  id: number;
  petId: number;
  petName: string;
  vitalSignId?: number | null;
  type: AlertType;
  severity: AlertSeverity;
  message: string;
  acknowledged: boolean;
  acknowledgedBy?: string | null;
  acknowledgedAt?: string | null;
  createdAt?: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: Record<string, string> | null;
}
