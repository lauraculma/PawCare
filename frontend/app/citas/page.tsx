"use client";

import { useEffect, useState } from "react";
import { appointmentsApi, getErrorMessage } from "@/lib/api";
import type { Appointment, AppointmentPayload, AppointmentStatus } from "@/lib/types";
import { formatDateTime, formatDateTimeInput, statusLabels } from "@/lib/format";
import { Card, CardHeader } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import { buttonClasses } from "@/components/ui/Button";
import { useSession } from "@/components/providers/SessionProvider";
import Modal from "@/components/ui/Modal";
import { Field, Input, Select, Textarea } from "@/components/ui/Field";
import Alert from "@/components/ui/Alert";
import Spinner from "@/components/ui/Spinner";
import Badge from "@/components/ui/Badge";

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

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await appointmentsApi.list({
        veterinarianId: user?.role === "VETERINARIAN" ? user.id : undefined,
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
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status, user?.id]);

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
          veterinarianId: payload.veterinarianId,
        });
      } else {
        await appointmentsApi.create(payload);
      }
      setOpen(false);
      setPage(0);
      await load();
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
            <Select value={status} onChange={e => { setStatus(e.target.value as any); setPage(0); }}>
              <option value="">Todos los estados</option>
              {(["SCHEDULED","CONFIRMED","COMPLETED","CANCELLED","NO_SHOW"] as AppointmentStatus[]).map(s => (
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
                <button onClick={() => { setEditing(a); setOpen(true); }} className={buttonClasses("secondary", "sm")}>Editar</button>
                <button onClick={async () => { await appointmentsApi.cancel(a.id); await load(); }} className={buttonClasses("danger", "sm")}>Cancelar</button>
              </div>
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

      <Modal open={open} onClose={()=>setOpen(false)} title={editing ? "Editar cita" : "Nueva cita"}>
        <AppointmentForm initial={editing} onCancel={()=>setOpen(false)} onSave={handleSave} saving={saving} error={formError} />
      </Modal>
    </div>
  );
}

function AppointmentForm({
  initial,
  onCancel,
  onSave,
  saving,
  error,
}: {
  initial?: Appointment | null;
  onCancel: () => void;
  onSave: (data: AppointmentPayload) => void;
  saving: boolean;
  error: string | null;
}) {
  const [form, setForm] = useState<AppointmentPayload>({
    petId: initial?.petId ?? 0,
    veterinarianId: initial?.veterinarianId ?? null,
    scheduledAt: initial?.scheduledAt ? formatDateTimeInput(initial.scheduledAt) : "",
    durationMinutes: initial?.durationMinutes ?? 30,
    reason: initial?.reason ?? "",
    notes: initial?.notes ?? "",
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!form.petId) return;
        if (!form.scheduledAt) return;
        onSave({ ...form, scheduledAt: new Date(form.scheduledAt).toISOString() });
      }}
      className="space-y-4"
    >
      {error ? <Alert variant="error">{error}</Alert> : null}
      <Field label="Mascota ID" required>
        <Input type="number" value={form.petId || ""} onChange={e=>setForm(f=>({...f,petId: Number(e.target.value)}))} />
      </Field>
      <Field label="Veterinario ID">
        <Input type="number" value={form.veterinarianId ?? ""} onChange={e=>setForm(f=>({...f,veterinarianId: e.target.value ? Number(e.target.value) : null}))} />
      </Field>
      <Field label="Fecha y hora" required>
        <Input type="datetime-local" value={form.scheduledAt} onChange={e=>setForm(f=>({...f,scheduledAt: e.target.value}))} />
      </Field>
      <Field label="Duración minutos" required>
        <Input type="number" value={form.durationMinutes} onChange={e=>setForm(f=>({...f,durationMinutes: Number(e.target.value)}))} />
      </Field>
      <Field label="Motivo" required>
        <Input value={form.reason} onChange={e=>setForm(f=>({...f,reason: e.target.value}))} />
      </Field>
      <Field label="Notas">
        <Textarea value={form.notes ?? ""} onChange={e=>setForm(f=>({...f,notes: e.target.value}))} />
      </Field>
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onCancel} className={buttonClasses("secondary","md")}>Cancelar</button>
        <button type="submit" disabled={saving} className={buttonClasses("primary","md")}>{saving ? "Guardando..." : "Guardar"}</button>
      </div>
    </form>
  );
}
