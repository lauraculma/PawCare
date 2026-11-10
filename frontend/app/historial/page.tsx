"use client";

import { useEffect, useState } from "react";
import { clinicalRecordsApi, getErrorMessage } from "@/lib/api";
import type { ClinicalRecord, ClinicalRecordPayload } from "@/lib/types";
import { formatDateTime } from "@/lib/format";
import { Card, CardHeader } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import { buttonClasses } from "@/components/ui/Button";
import Modal from "@/components/ui/Modal";
import { Field, Input, Textarea } from "@/components/ui/Field";
import Alert from "@/components/ui/Alert";
import Spinner from "@/components/ui/Spinner";
import { useSession } from "@/components/providers/SessionProvider";

export default function ClinicalRecordsPage() {
  const { user } = useSession();
  const [records, setRecords] = useState<ClinicalRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<ClinicalRecord | null>(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [petIdFilter, setPetIdFilter] = useState<number | "">("");

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await clinicalRecordsApi.list({ petId: petIdFilter ? Number(petIdFilter) : undefined, size: 50 });
      setRecords(res.content);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, [petIdFilter]);

  async function handleSave(data: ClinicalRecordPayload) {
    setSaving(true);
    setFormError(null);
    try {
      if (editing) {
        await clinicalRecordsApi.update(editing.id, data);
      } else {
        await clinicalRecordsApi.create(data);
      }
      setOpen(false);
      await load();
    } catch (e) {
      setFormError(getErrorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeader title="Historial clínico" description="Consulta y registra diagnósticos, tratamientos y prescripciones." action={<button onClick={()=>{setEditing(null); setOpen(true);}} className={buttonClasses("primary","md")}>Nuevo registro</button>} />

      <Card className="mt-6">
        <div className="flex flex-wrap items-center gap-3">
          <Field label="Filtrar por ID de mascota" className="w-full sm:w-64 m-0">
            <Input type="number" value={petIdFilter} onChange={e=>setPetIdFilter(e.target.value ? Number(e.target.value) : "")} placeholder="ID mascota" />
          </Field>
        </div>
      </Card>

      {error ? <Alert variant="error" className="mt-4">{error}</Alert> : null}
      {loading ? <Spinner label="Cargando historial..." /> : null}

      {!loading && records.length === 0 ? <EmptyState title="Sin registros clínicos" description="Crea el primer registro para esta mascota." /> : null}

      <div className="mt-6 grid gap-4">
        {records.map(r => (
          <Card key={r.id}>
            <CardHeader title={r.petName} description={`Mascota ID: ${r.petId} · Veterinario: ${r.veterinarianName} · ${formatDateTime(r.createdAt)}`} />
            <div className="mt-3 grid grid-cols-1 gap-2 text-sm">
              <p><span className="text-zinc-500">Diagnóstico:</span> <span className="font-medium">{r.diagnosis}</span></p>
              {r.treatment && <p><span className="text-zinc-500">Tratamiento:</span> {r.treatment}</p>}
              {r.prescription && <p><span className="text-zinc-500">Prescripción:</span> {r.prescription}</p>}
              {r.notes && <p><span className="text-zinc-500">Notas:</span> {r.notes}</p>}
            </div>
            <div className="mt-4 flex justify-end gap-2">
              <button onClick={()=>{setEditing(r); setOpen(true);}} className={buttonClasses("secondary","sm")}>Editar</button>
            </div>
          </Card>
        ))}
      </div>

      <Modal open={open} onClose={()=>setOpen(false)} title={editing ? "Editar registro clínico" : "Nuevo registro clínico"}>
        <ClinicalForm initial={editing} onCancel={()=>setOpen(false)} onSave={handleSave} saving={saving} error={formError} />
      </Modal>
    </div>
  );
}

function ClinicalForm({ initial, onCancel, onSave, saving, error }: { initial?: ClinicalRecord | null; onCancel: ()=>void; onSave:(d:ClinicalRecordPayload)=>void; saving:boolean; error:string|null }) {
  const [form, setForm] = useState<ClinicalRecordPayload>({ appointmentId: initial?.appointmentId ?? 0, diagnosis: initial?.diagnosis ?? "", treatment: initial?.treatment ?? "", prescription: initial?.prescription ?? "", notes: initial?.notes ?? "" });

  return (
    <form onSubmit={(e)=>{ e.preventDefault(); if(!form.appointmentId) return; onSave(form); }} className="space-y-4">
      {error ? <Alert variant="error">{error}</Alert> : null}
      <Field label="ID de cita" required>
        <Input type="number" value={form.appointmentId || ""} onChange={e=>setForm(f=>({...f, appointmentId: Number(e.target.value)}))} />
      </Field>
      <Field label="Diagnóstico" required>
        <Textarea value={form.diagnosis} onChange={e=>setForm(f=>({...f, diagnosis: e.target.value}))} />
      </Field>
      <Field label="Tratamiento">
        <Textarea value={form.treatment ?? ""} onChange={e=>setForm(f=>({...f, treatment: e.target.value}))} />
      </Field>
      <Field label="Prescripción">
        <Textarea value={form.prescription ?? ""} onChange={e=>setForm(f=>({...f, prescription: e.target.value}))} />
      </Field>
      <Field label="Notas">
        <Textarea value={form.notes ?? ""} onChange={e=>setForm(f=>({...f, notes: e.target.value}))} />
      </Field>
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onCancel} className={buttonClasses("secondary","md")}>Cancelar</button>
        <button type="submit" disabled={saving} className={buttonClasses("primary","md")}>{saving ? "Guardando..." : "Guardar"}</button>
      </div>
    </form>
  );
}
