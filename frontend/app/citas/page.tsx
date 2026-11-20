"use client";

import { useEffect, useState } from "react";
import { appointmentsApi, getErrorMessage } from "@/lib/api";
import type { Appointment, AppointmentPayload, AppointmentStatus } from "@/lib/types";
import { formatDateTime, statusLabels } from "@/lib/format";
import { Card, CardHeader } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import { buttonClasses } from "@/components/ui/Button";
import { useSession } from "@/components/providers/SessionProvider";
import Modal from "@/components/ui/Modal";
import { Field, Select, Textarea } from "@/components/ui/Field";
import Alert from "@/components/ui/Alert";
import Spinner from "@/components/ui/Spinner";
import Badge from "@/components/ui/Badge";
import CalendarAppointmentForm from "@/components/appointments/CalendarAppointmentForm";

export default function AppointmentsPage() {
  const { user } = useSession();
  const [data, setData] = useState<{
    content: Appointment[];
    totalElements: number;
    totalPages: number;
    number: number;
  } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const size = 12;
  const [status, setStatus] = useState<AppointmentStatus | "">("");
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Appointment | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [completionTarget, setCompletionTarget] = useState<number | null>(null);
  const [completionNotes, setCompletionNotes] = useState("");

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await appointmentsApi.list({
        veterinarianId: user?.role === "VETERINARIAN" ? user.internalId : undefined,
        status: status || undefined,
        page,
        size,
      });
      setData({
        content: res.content,
        totalElements: res.totalElements,
        totalPages: res.totalPages,
        number: res.number,
      });
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    Promise.resolve().then(() => void load());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status, user?.id]);

  async function handleCancel(id: number) {
    setError(null);
    try {
      await appointmentsApi.cancel(id);
      setData(current => current ? {
        ...current,
        content: current.content.map(appointment => appointment.id === id
          ? { ...appointment, status: "CANCELLED" }
          : appointment),
      } : current);
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function handleComplete(id: number) {
    setError(null);
    try {
      await appointmentsApi.updateStatus(id, "COMPLETED", completionNotes.trim());
      setCompletionTarget(null);
      setCompletionNotes("");
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function handleSave(payload: AppointmentPayload) {
    setSaving(true);
    setFormError(null);
    try {
      if (editing) {
        await appointmentsApi.update(editing.id, {
          scheduledAt: payload.scheduledAt,
          durationMinutes: payload.durationMinutes,
          reason: payload.reason,
          notes: payload.notes,
          veterinarianCedula: payload.veterinarianCedula,
        });
      } else {
        await appointmentsApi.create(payload);
      }
      setOpen(false);
      if (page !== 0) {
        setPage(0);
      } else {
        await load();
      }
    } catch (e) {
      setFormError(getErrorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeader
        title="Citas"
        description="Agenda, reprograma y cancela citas veterinarias."
        action={
          <button onClick={() => { setEditing(null); setOpen(true); }} className={buttonClasses("primary", "md")}>
            Nueva cita
          </button>
        }
      />

      <Card className="mt-6">
        <div className="flex flex-wrap items-center gap-3">
          <Field label="" className="m-0 w-full sm:w-64">
            <Select value={status} onChange={e => { setStatus(e.target.value as AppointmentStatus | ""); setPage(0); }}>
              <option value="">Todos los estados</option>
              {(["SCHEDULED","COMPLETED","CANCELLED"] as AppointmentStatus[]).map(s => (
                <option key={s} value={s}>{statusLabels[s]}</option>
              ))}
            </Select>
          </Field>
        </div>
      </Card>

      {error ? <Alert variant="error" className="mt-4">{error}</Alert> : null}
      {loading ? <Spinner label="Cargando citas..." /> : null}

      {!loading && data && data.content.length === 0 ? (
        <EmptyState title="No hay citas" description="Crea la primera cita para comenzar." />
      ) : null}

      {!loading && data && data.content.length > 0 ? (
        <div className="mt-6 grid gap-4">
          {data.content.map(a => (
            <Card key={a.id}>
              <div className="flex flex-wrap items-start justify-between gap-4">
                <CardHeader title={`${a.petName} (${a.petSpecies})`} description={`Propietario: ${a.owner.fullName}`} />
                <Badge tone={a.status === "COMPLETED" ? "emerald" : a.status === "CANCELLED" ? "zinc" : "sky"}>
                  {statusLabels[a.status]}
                </Badge>
              </div>
              <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                <div><p className="text-zinc-500">Veterinario</p><p className="font-medium">{a.veterinarianName}</p></div>
                <div><p className="text-zinc-500">Programada</p><p className="font-medium">{formatDateTime(a.scheduledAt)}</p></div>
                <div><p className="text-zinc-500">Duración</p><p className="font-medium">{a.durationMinutes} min</p></div>
                <div><p className="text-zinc-500">Motivo</p><p className="font-medium">{a.reason}</p></div>
              </div>
              <div className="mt-4 flex justify-end gap-2">
                {user?.role === "ADMIN" && a.status === "SCHEDULED" ? (
                  <button onClick={() => { setEditing(a); setOpen(true); }} className={buttonClasses("secondary", "sm")}>Editar</button>
                ) : null}
                {user?.role === "VETERINARIAN" && a.status === "SCHEDULED" ? (
                  <button disabled={new Date(a.endAt).getTime() > Date.now()} onClick={() => { setCompletionTarget(a.id); setCompletionNotes(a.notes ?? ""); }} className={buttonClasses("primary", "sm")}>
                    {new Date(a.endAt).getTime() > Date.now() ? "Al finalizar: marcar asistida" : "Marcar asistida"}
                  </button>
                ) : null}
                {a.status === "SCHEDULED" && user?.role !== undefined ? (
                  <button onClick={() => void handleCancel(a.id)} className={buttonClasses("danger", "sm")}>Cancelar</button>
                ) : null}
              </div>
              {completionTarget === a.id ? (
                <div className="mt-4 space-y-3 border-t border-zinc-200 pt-4">
                  <Field label="Notas de la cita" required>
                    <Textarea required maxLength={5000} value={completionNotes} onChange={event => setCompletionNotes(event.target.value)} />
                  </Field>
                  <div className="flex justify-end gap-2">
                    <button type="button" onClick={() => setCompletionTarget(null)} className={buttonClasses("secondary", "sm")}>Volver</button>
                    <button type="button" onClick={() => void handleComplete(a.id)} disabled={!completionNotes.trim()} className={buttonClasses("primary", "sm")}>Finalizar cita</button>
                  </div>
                </div>
              ) : null}
            </Card>
          ))}
        </div>
      ) : null}

      {data && (
        <div className="mt-6 flex items-center justify-between">
          <p className="text-sm text-zinc-600">Página {data.number + 1} de {data.totalPages || 1}</p>
          <div className="flex gap-2">
            <button disabled={data.number === 0 || loading} onClick={() => setPage(p=>Math.max(0,p-1))} className={buttonClasses("secondary","sm")}>Anterior</button>
            <button disabled={data.number + 1 >= data.totalPages || loading} onClick={() => setPage(p=>p+1)} className={buttonClasses("secondary","sm")}>Siguiente</button>
          </div>
        </div>
      )}

      <Modal open={open} onClose={()=>setOpen(false)} title={editing ? "Editar cita" : "Nueva cita"} contentClassName="max-w-5xl">
        <CalendarAppointmentForm initial={editing} onCancel={()=>setOpen(false)} onSave={handleSave} saving={saving} error={formError} canChangeVeterinarian={!editing || user?.role !== "OWNER"} />
      </Modal>
    </div>
  );
}
