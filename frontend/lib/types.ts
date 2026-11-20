export type Role = "ADMIN" | "VETERINARIAN" | "OWNER";

export type AppointmentStatus =
  | "SCHEDULED"
  | "COMPLETED"
  | "CANCELLED";

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
  id: string;
  internalId: number;
  fullName: string;
  email: string;
  role: Role;
  phone?: string | null;
  cedula: string;
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
  cedula: string;
  role?: Role;
}

export interface AdminUserUpdatePayload {
  fullName: string;
  email: string;
  cedula: string;
  phone?: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface OwnerSummary {
  id: string;
  fullName: string;
  email: string;
  cedula?: string | null;
}

export interface Pet {
  id: number;
  publicId: string;
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
  ownerCedula?: string | null;
}

export interface Appointment {
  id: number;
  petId: number;
  petPublicId: string;
  petName: string;
  petSpecies: string;
  owner: OwnerSummary;
  veterinarianId: string;
  veterinarianInternalId: number;
  veterinarianCedula: string;
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
  petPublicId: string;
  veterinarianCedula?: string | null;
  scheduledAt: string;
  durationMinutes: number;
  reason: string;
  notes?: string;
}

export interface VeterinarianOption {
  cedula: string;
  fullName: string;
}

export interface AvailabilitySlot {
  time: string;
  veterinarianCedula: string;
  veterinarianName: string;
}

export interface NearestAvailability extends AvailabilitySlot { date: string; }

export interface VeterinarianSchedule {
  veterinarian: User;
  shifts: { dayOfWeek: number; startTime: string }[];
}

export interface AppointmentUpdatePayload {
  scheduledAt: string;
  durationMinutes: number;
  reason: string;
  notes?: string;
  veterinarianCedula?: string | null;
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

export interface Treatment {
  id: number;
  clinicalRecordId: number;
  petId: number;
  petName: string;
  owner: OwnerSummary;
  veterinarianId: number;
  veterinarianName: string;
  medication: string;
  dosage: string;
  frequency: string;
  duration?: string | null;
  instructions?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface TreatmentPayload {
  clinicalRecordId: number;
  medication: string;
  dosage: string;
  frequency: string;
  duration?: string;
  instructions?: string;
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
