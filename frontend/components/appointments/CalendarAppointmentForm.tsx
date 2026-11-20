"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import { appointmentsApi, getErrorMessage } from "@/lib/api";
import type { Appointment, AppointmentPayload, AvailabilitySlot, VeterinarianOption } from "@/lib/types";
import { formatDateTimeInput } from "@/lib/format";
import { buttonClasses } from "@/components/ui/Button";
import { Field, Input, Select, Textarea } from "@/components/ui/Field";
import Alert from "@/components/ui/Alert";

type AssignMode = "NEAREST" | "DATE" | "VETERINARIAN";
const WEEKDAYS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];

function todayLocal() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

function currentMonth() { return todayLocal().slice(0, 7); }

export default function CalendarAppointmentForm({
  initial, onCancel, onSave, saving, error, canChangeVeterinarian,
}: {
  initial?: Appointment | null;
  onCancel: () => void;
  onSave: (data: AppointmentPayload) => void;
  saving: boolean;
  error: string | null;
  canChangeVeterinarian: boolean;
}) {
  const initialLocal = initial?.scheduledAt ? formatDateTimeInput(initial.scheduledAt) : "";
  const [petPublicId, setPetPublicId] = useState(initial?.petPublicId ?? "");
  const [mode, setMode] = useState<AssignMode>(initial ? "DATE" : "NEAREST");
  const [date, setDate] = useState(initialLocal.slice(0, 10));
  const [time, setTime] = useState(initialLocal.slice(11, 16));
  const [month, setMonth] = useState(initialLocal.slice(0, 7) || currentMonth());
  const [vets, setVets] = useState<VeterinarianOption[]>([]);
  const [slots, setSlots] = useState<AvailabilitySlot[]>([]);
  const [availableDates, setAvailableDates] = useState<string[]>([]);
  const [selectedVetCedula, setSelectedVetCedula] = useState(initial?.veterinarianCedula ?? "");
  const [reason, setReason] = useState(initial?.reason ?? "");
  const [notes, setNotes] = useState(initial?.notes ?? "");
  const [loadingAvailability, setLoadingAvailability] = useState(false);
  const [loadingDates, setLoadingDates] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    void appointmentsApi.veterinarians().then(setVets).catch(e => setFormError(getErrorMessage(e)));
  }, []);

  useEffect(() => {
    if (mode !== "NEAREST") return;
    let active = true;
    Promise.resolve().then(async () => {
      if (!active) return;
      setLoadingAvailability(true);
      setFormError(null);
      try {
        const result = await appointmentsApi.nearestAvailability(todayLocal(), initial?.id);
        if (!active) return;
        setDate(result.date);
        setMonth(result.date.slice(0, 7));
        setTime(result.time);
        setSelectedVetCedula(result.veterinarianCedula);
      } catch (e) {
        if (active) setFormError(getErrorMessage(e));
      } finally {
        if (active) setLoadingAvailability(false);
      }
    });
    return () => { active = false; };
  }, [mode, initial?.id]);

  useEffect(() => {
    let active = true;
    Promise.resolve().then(async () => {
      if (!active) return;
      const requiresVet = mode === "VETERINARIAN" || (!canChangeVeterinarian && mode !== "NEAREST");
      const vetFilter = requiresVet ? selectedVetCedula || undefined : undefined;
      if (requiresVet && !vetFilter) {
        setAvailableDates([]);
        setLoadingDates(false);
        return;
      }
      setLoadingDates(true);
      try {
        const result = await appointmentsApi.availableDates(month, vetFilter, initial?.id);
        if (active) setAvailableDates(result);
      } catch (e) {
        if (active) setFormError(getErrorMessage(e));
      } finally {
        if (active) setLoadingDates(false);
      }
    });
    return () => { active = false; };
  }, [month, mode, selectedVetCedula, initial?.id, canChangeVeterinarian]);

  useEffect(() => {
    let active = true;
    Promise.resolve().then(async () => {
      if (!active) return;
      setSlots([]);
      if (!date) { setLoadingAvailability(false); return; }
      const requiresVet = mode === "VETERINARIAN" || mode === "NEAREST" || !canChangeVeterinarian;
      const vetFilter = requiresVet ? selectedVetCedula || undefined : undefined;
      if (requiresVet && !vetFilter) { setLoadingAvailability(false); return; }
      setLoadingAvailability(true);
      try {
        const result = await appointmentsApi.availability(date, vetFilter, initial?.id);
        if (active) { setSlots(result); setFormError(null); }
      } catch (e) {
        if (active) setFormError(getErrorMessage(e));
      } finally {
        if (active) setLoadingAvailability(false);
      }
    });
    return () => { active = false; };
  }, [date, mode, selectedVetCedula, initial?.id, canChangeVeterinarian]);

  const times = useMemo(() => [...new Set(slots.map(slot => slot.time))].sort(), [slots]);
  const vetsForTime = useMemo(() => {
    const unique = new Map<string, AvailabilitySlot>();
    slots.filter(slot => slot.time === time).forEach(slot => unique.set(slot.veterinarianCedula, slot));
    return [...unique.values()];
  }, [slots, time]);
  const timeSelectionEnabled = mode !== "NEAREST" && Boolean(date) && !loadingAvailability
    && (mode !== "VETERINARIAN" || Boolean(selectedVetCedula)) && times.length > 0;
  const vetSelectionDisabled = !canChangeVeterinarian || mode === "NEAREST"
    || (mode === "DATE" && (!time || vetsForTime.length === 0));

  function changeMode(value: AssignMode) {
    setMode(value);
    setTime("");
    setSelectedVetCedula("");
    if (value === "NEAREST") setDate("");
  }

  function chooseDate(value: string) {
    if (mode === "NEAREST") setMode("DATE");
    setDate(value);
    setMonth(value.slice(0, 7));
    setTime("");
    if (mode === "DATE") setSelectedVetCedula("");
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const slot = slots.find(item => item.time === time && item.veterinarianCedula === selectedVetCedula);
    if (!petPublicId || !date || !time || !slot) return;
    onSave({
      petPublicId,
      veterinarianCedula: slot.veterinarianCedula,
      scheduledAt: new Date(`${date}T${time}:00-05:00`).toISOString(),
      durationMinutes: 60,
      reason,
      notes,
    });
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <div className="grid gap-5 lg:grid-cols-2">
        <div className="space-y-4">
          {error ? <Alert variant="error">{error}</Alert> : null}
          {formError ? <Alert variant="error">{formError}</Alert> : null}
          <Field label="ID de mascota (5 dígitos)" required>
            <Input inputMode="numeric" pattern="[0-9]{5}" maxLength={5} required value={petPublicId}
              onChange={e => setPetPublicId(e.target.value.replace(/\D/g, "").slice(0, 5))} />
          </Field>
          <Field label="¿Cómo quieres asignar la cita?" required>
            <Select disabled={!canChangeVeterinarian} value={mode} onChange={e => changeMode(e.target.value as AssignMode)}>
              <option value="NEAREST">Cita más cercana</option>
              <option value="DATE">Elegir por fecha y hora</option>
              <option value="VETERINARIAN">Elegir por veterinario</option>
            </Select>
          </Field>
          {mode === "NEAREST" ? (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">
              {loadingAvailability ? "Buscando la cita más cercana..." : date && time
                ? <span>Primera disponibilidad: <strong>{new Date(`${date}T12:00:00-05:00`).toLocaleDateString("es-CO", { dateStyle: "long", timeZone: "America/Bogota" })} a las {time}</strong></span>
                : "No se encontró disponibilidad cercana."}
              {selectedVetCedula ? <p className="mt-1">Veterinario: {vets.find(vet => vet.cedula === selectedVetCedula)?.fullName ?? selectedVetCedula}</p> : null}
            </div>
          ) : null}
          <Field label="Fecha seleccionada" required>
            <Input type="date" value={date} readOnly disabled />
          </Field>
          <Field label="Veterinario" required>
            <Select required disabled={vetSelectionDisabled} value={selectedVetCedula}
              onChange={e => {
                setSelectedVetCedula(e.target.value);
                if (mode === "VETERINARIAN") setTime("");
              }}>
              <option value="">{mode === "DATE" ? "Elige una hora para ver veterinarios" : "Selecciona un veterinario"}</option>
              {(mode === "DATE" ? vetsForTime.map(slot => ({ cedula: slot.veterinarianCedula, fullName: slot.veterinarianName })) : vets)
                .map(vet => <option key={vet.cedula} value={vet.cedula}>{vet.fullName} · {vet.cedula}</option>)}
            </Select>
          </Field>
          <Field label="Hora disponible" required>
            <Select required disabled={!timeSelectionEnabled} value={time}
              onChange={e => { setTime(e.target.value); if (mode === "DATE") setSelectedVetCedula(""); }}>
              <option value="">{loadingAvailability ? "Buscando horarios..." : mode === "NEAREST" ? "Asignada automáticamente" : "Selecciona una hora"}</option>
              {times.map(value => <option key={value} value={value}>{value}</option>)}
            </Select>
          </Field>
          {mode !== "NEAREST" && date && !loadingAvailability && times.length === 0
            ? <p className="text-sm text-zinc-500">No hay horarios disponibles para esta fecha y selección.</p> : null}
          <p className="text-sm text-zinc-600">Duración: <strong>1 hora</strong>. La clínica atiende de 7:00 a. m. a 10:00 p. m.</p>
          <Field label="Motivo" required><Input required maxLength={240} value={reason} onChange={e => setReason(e.target.value)} /></Field>
          <Field label="Notas"><Textarea value={notes} onChange={e => setNotes(e.target.value)} /></Field>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={onCancel} className={buttonClasses("secondary", "md")}>Cancelar</button>
            <button type="submit" disabled={saving || loadingAvailability || !date || !time || !selectedVetCedula} className={buttonClasses("primary", "md")}>{saving ? "Guardando..." : "Guardar"}</button>
          </div>
        </div>
        <div>
          <AvailabilityCalendar
            month={month}
            availableDates={availableDates}
            selectedDate={date}
            loading={loadingDates}
            onMonthChange={setMonth}
            onSelect={chooseDate}
          />
        </div>
      </div>
    </form>
  );
}

function AvailabilityCalendar({
  month, availableDates, selectedDate, loading, onMonthChange, onSelect,
}: {
  month: string;
  availableDates: string[];
  selectedDate: string;
  loading: boolean;
  onMonthChange: (month: string) => void;
  onSelect: (date: string) => void;
}) {
  const [year, monthNumber] = month.split("-").map(Number);
  const startWeekday = (new Date(Date.UTC(year, monthNumber - 1, 1)).getUTCDay() + 6) % 7;
  const daysInMonth = new Date(Date.UTC(year, monthNumber, 0)).getUTCDate();
  const available = new Set(availableDates);
  const title = new Intl.DateTimeFormat("es-CO", { month: "long", year: "numeric", timeZone: "UTC" })
    .format(new Date(Date.UTC(year, monthNumber - 1, 1)));

  function moveMonth(delta: number) {
    const next = new Date(Date.UTC(year, monthNumber - 1 + delta, 1));
    onMonthChange(`${next.getUTCFullYear()}-${String(next.getUTCMonth() + 1).padStart(2, "0")}`);
  }

  return (
    <div className="rounded-xl border border-zinc-200 p-3">
      <div className="mb-3 flex items-center justify-between">
        <button type="button" aria-label="Mes anterior" onClick={() => moveMonth(-1)} className={buttonClasses("secondary", "sm")}>‹</button>
        <h3 className="font-semibold capitalize text-zinc-900">{title}</h3>
        <button type="button" aria-label="Mes siguiente" onClick={() => moveMonth(1)} className={buttonClasses("secondary", "sm")}>›</button>
      </div>
      <div className="grid grid-cols-7 gap-1 text-center text-xs text-zinc-500">
        {WEEKDAYS.map(day => <div key={day} className="py-1 font-medium">{day}</div>)}
        {Array.from({ length: startWeekday }, (_, index) => <div key={`blank-${index}`} />)}
        {Array.from({ length: daysInMonth }, (_, index) => {
          const day = index + 1;
          const date = `${month}-${String(day).padStart(2, "0")}`;
          const hasAvailability = available.has(date);
          const past = date < todayLocal();
          const selected = date === selectedDate;
          return <button key={date} type="button" disabled={!hasAvailability || past || loading}
            aria-label={`${day}${hasAvailability ? ", disponible" : ", sin disponibilidad"}`}
            onClick={() => onSelect(date)}
            className={`aspect-square rounded-full text-sm transition ${selected ? "bg-emerald-700 font-bold text-white" : hasAvailability && !past ? "bg-emerald-100 font-semibold text-emerald-900 hover:bg-emerald-200" : "text-zinc-300"}`}>
            {day}
          </button>;
        })}
      </div>
      <div className="mt-3 flex items-center justify-between text-xs text-zinc-500">
        <span className="inline-flex items-center gap-1.5"><span className="h-3 w-3 rounded-full bg-emerald-100 ring-1 ring-emerald-300" /> Fecha disponible</span>
        <span>{loading ? "Actualizando disponibilidad..." : `${availableDates.length} días disponibles`}</span>
      </div>
    </div>
  );
}
