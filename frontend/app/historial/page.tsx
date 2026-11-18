"use client";

import { useEffect, useState } from "react";
import { clinicalRecordsApi, getErrorMessage, treatmentsApi } from "@/lib/api";
import type { ClinicalRecord, ClinicalRecordPayload, Treatment, TreatmentPayload } from "@/lib/types";
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
  const canManage = Boolean(user && user.role !== "OWNER");
  const canAddTreatment = user?.role === "VETERINARIAN";
  const [records, setRecords] = useState<ClinicalRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<ClinicalRecord | null>(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [petIdFilter, setPetIdFilter] = useState<number | "">("");
  const [treatmentsByRecord, setTreatmentsByRecord] = useState<Record<number, Treatment[]>>({});
  const [treatmentsLoading, setTreatmentsLoading] = useState<Record<number, boolean>>({});
  const [treatmentsError, setTreatmentsError] = useState<Record<number, string>>({});
  const [treatmentRecord, setTreatmentRecord] = useState<ClinicalRecord | null>(null);
  const [treatmentSaving, setTreatmentSaving] = useState(false);
  const [treatmentFormError, setTreatmentFormError] = useState<string | null>(null);

  const loadTreatments = async (recordsToLoad: ClinicalRecord[]) => {
    const loadingByRecord: Record<number, boolean> = {};
    recordsToLoad.forEach(r => { loadingByRecord[r.id] = true; });
    setTreatmentsLoading(loadingByRecord);
    setTreatmentsByRecord({});
    setTreatmentsError({});

    const results = await Promise.all(recordsToLoad.map(async (record) => {
      try {
        const res = await treatmentsApi.list({ clinicalRecordId: record.id, size: 100 });
        return { id: record.id, items: res.content, error: null as string | null };
      } catch (e) {
        return { id: record.id, items: [] as Treatment[], error: getErrorMessage(e) };
      }
    }));

    const byRecord: Record<number, Treatment[]> = {};
    const errors: Record<number, string> = {};
    results.forEach(result => {
      byRecord[result.id] = result.items;
      if (result.error) errors[result.id] = result.error;
    });
    setTreatmentsByRecord(byRecord);
    setTreatmentsError(errors);
    setTreatmentsLoading({});
  };

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await clinicalRecordsApi.list({ petId: petIdFilter ? Number(petIdFilter) : undefined, size: 50 });
      setRecords(res.content);
      void loadTreatments(res.content);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    Promise.resolve().then(() => void load());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [petIdFilter]);

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

  async function handleSaveTreatment(data: TreatmentPayload) {
    if (!treatmentRecord) return;
    setTreatmentSaving(true);
    setTreatmentFormError(null);
    try {
      await treatmentsApi.create(data);
      setTreatmentRecord(null);
      await load();
    } catch (e) {
      setTreatmentFormError(getErrorMessage(e));
    } finally {
      setTreatmentSaving(false);
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeader title="Historial clínico" description="Consulta y registra diagnósticos, tratamientos y prescripciones." action={canManage ? <button onClick={()=>{setEditing(null); setOpen(true);}} className={buttonClasses("primary","md")}>Nuevo registro</button> : null} />

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
            <div className="mt-4 border-t border-zinc-200 pt-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="text-sm font-semibold text-zinc-900">Fórmulas médicas</h3>
                {canAddTreatment ? (
                  <button onClick={()=>{setTreatmentRecord(r); setTreatmentFormError(null);}} className={buttonClasses("secondary","sm")}>Agregar tratamiento</button>
                ) : null}
              </div>
              {treatmentsLoading[r.id] ? <Spinner label="Cargando tratamientos..." /> : null}
              {!treatmentsLoading[r.id] && treatmentsError[r.id] ? <Alert variant="error">{treatmentsError[r.id]}</Alert> : null}
              {!treatmentsLoading[r.id] && !treatmentsError[r.id] && (!treatmentsByRecord[r.id] || treatmentsByRecord[r.id].length === 0) ? (
                <p className="mt-2 text-sm text-zinc-500">Sin tratamientos registrados.</p>
              ) : null}
              <div className="mt-2 grid gap-2">
                {(treatmentsByRecord[r.id] ?? []).map(t => (
                  <div key={t.id} className="rounded-lg border border-zinc-200 bg-zinc-50 p-3 text-sm">
                    <p className="font-semibold text-zinc-900">{t.medication}</p>
                    <div className="mt-1 grid gap-1 text-zinc-600">
                      <p><span className="text-zinc-500">Dosis:</span> {t.dosage}</p>
                      <p><span className="text-zinc-500">Frecuencia:</span> {t.frequency}</p>
                      {t.duration ? <p><span className="text-zinc-500">Duración:</span> {t.duration}</p> : null}
                      {t.instructions ? <p><span className="text-zinc-500">Indicaciones:</span> {t.instructions}</p> : null}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            {canManage ? (
              <div className="mt-4 flex justify-end gap-2">
                <button onClick={()=>{setEditing(r); setOpen(true);}} className={buttonClasses("secondary","sm")}>Editar</button>
              </div>
            ) : null}
          </Card>
        ))}
      </div>

      {canManage ? (
        <Modal open={open} onClose={()=>setOpen(false)} title={editing ? "Editar registro clínico" : "Nuevo registro clínico"}>
          <ClinicalForm initial={editing} onCancel={()=>setOpen(false)} onSave={handleSave} saving={saving} error={formError} />
        </Modal>
      ) : null}
      <Modal open={treatmentRecord !== null} onClose={()=>setTreatmentRecord(null)} title="Nuevo tratamiento">
        {treatmentRecord ? (
          <TreatmentForm
            clinicalRecordId={treatmentRecord.id}
            petName={treatmentRecord.petName}
            onCancel={()=>setTreatmentRecord(null)}
            onSave={handleSaveTreatment}
            saving={treatmentSaving}
            error={treatmentFormError}
          />
        ) : null}
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

function TreatmentForm({ clinicalRecordId, petName, onCancel, onSave, saving, error }: { clinicalRecordId: number; petName: string; onCancel: ()=>void; onSave:(d:TreatmentPayload)=>void; saving:boolean; error:string|null }) {
  const [form, setForm] = useState({ medication: "", dosage: "", frequency: "", duration: "", instructions: "" });

  return (
    <form onSubmit={(e)=>{
      e.preventDefault();
      if(!form.medication.trim() || !form.dosage.trim() || !form.frequency.trim()) return;
      onSave({
        clinicalRecordId,
        medication: form.medication.trim(),
        dosage: form.dosage.trim(),
        frequency: form.frequency.trim(),
        duration: form.duration.trim() || undefined,
        instructions: form.instructions.trim() || undefined,
      });
    }} className="space-y-4">
      {error ? <Alert variant="error">{error}</Alert> : null}
      <p className="text-sm text-zinc-600">Registro clínico de <span className="font-medium">{petName}</span>.</p>
      <Field label="Medicamento" required>
        <Input value={form.medication} onChange={e=>setForm(f=>({...f, medication: e.target.value}))} required />
      </Field>
      <Field label="Dosis" required>
        <Input value={form.dosage} onChange={e=>setForm(f=>({...f, dosage: e.target.value}))} required />
      </Field>
      <Field label="Frecuencia" required>
        <Input value={form.frequency} onChange={e=>setForm(f=>({...f, frequency: e.target.value}))} required />
      </Field>
      <Field label="Duración">
        <Input value={form.duration} onChange={e=>setForm(f=>({...f, duration: e.target.value}))} placeholder="Ej. 7 dias" />
      </Field>
      <Field label="Indicaciones">
        <Textarea value={form.instructions} onChange={e=>setForm(f=>({...f, instructions: e.target.value}))} />
      </Field>
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onCancel} className={buttonClasses("secondary","md")}>Cancelar</button>
        <button type="submit" disabled={saving} className={buttonClasses("primary","md")}>{saving ? "Guardando..." : "Guardar"}</button>
      </div>
    </form>
  );
}
