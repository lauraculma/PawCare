"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/providers/SessionProvider";
import Alert from "@/components/ui/Alert";
import { buttonClasses } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import Spinner from "@/components/ui/Spinner";
import { Select } from "@/components/ui/Field";
import { adminApi, getErrorMessage } from "@/lib/api";
import type { VeterinarianSchedule } from "@/lib/types";

const DAYS = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"];
const SHIFTS = Array.from({ length: 11 }, (_, index) => {
  const hour = index + 7;
  const endHour = hour + 5;
  const display = (value: number) => `${value % 12 || 12}:00 ${value < 12 ? "a. m." : "p. m."}`;
  return { value: `${String(hour).padStart(2, "0")}:00`, label: `${display(hour)} – ${display(endHour)}` };
});

export default function VeterinarianShiftsPage() {
  const { user, loading } = useSession();
  const [schedules, setSchedules] = useState<VeterinarianSchedule[]>([]);
  const [drafts, setDrafts] = useState<Record<number, Record<number, string>>>({});
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const result = await adminApi.veterinarianSchedules();
      setSchedules(result);
      setDrafts(Object.fromEntries(result.map(item => [item.veterinarian.internalId,
        Object.fromEntries(item.shifts.map(shift => [shift.dayOfWeek, shift.startTime.slice(0, 5)]))])));
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => { if (user?.role === "ADMIN") void load(); }, [user, load]);

  async function save(veterinarianId: number) {
    setBusyId(veterinarianId);
    setError(null);
    setSuccess(null);
    const shifts = Object.entries(drafts[veterinarianId] ?? {})
      .filter(([, startTime]) => Boolean(startTime))
      .map(([dayOfWeek, startTime]) => ({ dayOfWeek: Number(dayOfWeek), startTime }));
    try {
      await adminApi.saveVeterinarianSchedule(veterinarianId, shifts);
      setSuccess("Turno semanal actualizado. La disponibilidad de nuevas citas ya refleja el cambio.");
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  if (loading) return <Spinner label="Verificando sesión..." />;
  if (!user) return <div className="mx-auto max-w-3xl px-6 py-12"><Alert variant="warning">Inicia sesión para continuar. <Link href="/login" className="font-semibold underline">Ir al login</Link>.</Alert></div>;
  if (user.role !== "ADMIN") return <div className="mx-auto max-w-3xl px-6 py-12"><Alert variant="error">No tienes permisos para asignar turnos.</Alert></div>;

  return (
    <main className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-10">
      <PageHeader title="Turnos semanales" description="Asigna a cada veterinario un turno diario de cinco horas. Elige cualquier inicio por hora entre las 7:00 a. m. y las 5:00 p. m. La clínica atiende hasta las 10:00 p. m.; puedes cambiar los turnos cuando lo necesites." />
      {error ? <Alert variant="error">{error}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}
      {schedules.length === 0 ? <Card><p className="text-sm text-zinc-600">No hay veterinarios activos y aprobados para asignarles turnos.</p></Card> : null}
      <div className="grid gap-5">
        {schedules.map(({ veterinarian }) => (
          <Card key={veterinarian.internalId}>
            <div className="mb-4 flex flex-wrap items-start justify-between gap-2">
              <div>
                <h2 className="font-semibold text-zinc-900">{veterinarian.fullName}</h2>
                <p className="text-sm text-zinc-500">{veterinarian.email}</p>
              </div>
              <p className="text-xs text-zinc-500">Un turno diario de 5 horas · días sin turno: descanso</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              {DAYS.map((day, index) => (
                <label key={day} className="flex flex-col gap-1.5 text-sm font-medium text-zinc-700">
                  {day}
                  <Select value={drafts[veterinarian.internalId]?.[index + 1] ?? ""} onChange={e => setDrafts(current => ({
                    ...current,
                    [veterinarian.internalId]: { ...current[veterinarian.internalId], [index + 1]: e.target.value },
                  }))}>
                    <option value="">Descanso</option>
                    {SHIFTS.map(shift => <option key={shift.value} value={shift.value}>{shift.label}</option>)}
                  </Select>
                </label>
              ))}
            </div>
            <div className="mt-5 flex justify-end">
              <button type="button" disabled={busyId !== null} onClick={() => void save(veterinarian.internalId)} className={buttonClasses("primary", "md")}>
                {busyId === veterinarian.internalId ? "Guardando..." : "Guardar turno semanal"}
              </button>
            </div>
          </Card>
        ))}
      </div>
    </main>
  );
}
