"use client";

import { useEffect, useState } from "react";
import { appointmentsApi, getErrorMessage } from "@/lib/api";
import type { Appointment } from "@/lib/types";
import { formatDateTime, statusLabels } from "@/lib/format";
import { Card, CardHeader } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import { buttonClasses } from "@/components/ui/Button";
import Spinner from "@/components/ui/Spinner";
import Badge from "@/components/ui/Badge";
import EmptyState from "@/components/ui/EmptyState";
import { useSession } from "@/components/providers/SessionProvider";

export default function AgendaPage() {
  const { user } = useSession();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [date, setDate] = useState<string>("");

  const load = async () => {
    if (!date) return;
    setLoading(true);
    setError(null);
    try {
      const from = new Date(date + "T00:00:00").toISOString();
      const to = new Date(date + "T23:59:59").toISOString();
      const res = await appointmentsApi.list({ from, to, veterinarianId: user?.role === "VETERINARIAN" ? user.id : undefined, size: 100 });
      setAppointments(res.content);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    Promise.resolve().then(() => {
      if (!date) {
        const today = new Date();
        const localDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
        setDate(localDate);
        return;
      }
      void load();
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date, user?.id]);

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeader title="Agenda del día" description="Consulta las citas programadas según fecha." />
      <Card className="mt-6">
        <div className="flex items-center gap-3">
          <label className="text-sm font-medium">Fecha</label>
          <input type="date" value={date} onChange={e=>setDate(e.target.value)} className="rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm" />
          <button onClick={()=>void load()} className={buttonClasses("secondary","md")}>Actualizar</button>
        </div>
      </Card>
      {error ? <div className="mt-4 text-red-600 text-sm">{error}</div> : null}
      {loading ? <Spinner label="Cargando agenda..." /> : null}
      {!loading && appointments.length === 0 ? <EmptyState title="Sin citas" description="No hay citas programadas para esta fecha." /> : null}
      <div className="mt-6 grid gap-4">
        {appointments.map(a => (
          <Card key={a.id}>
            <div className="flex items-start justify-between gap-4">
              <CardHeader title={`${a.petName} — ${a.petSpecies}`} description={`Propietario: ${a.owner.fullName}`} />
              <Badge tone="sky">{statusLabels[a.status]}</Badge>
            </div>
            <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
              <div><p className="text-zinc-500">Hora</p><p className="font-medium">{formatDateTime(a.scheduledAt)}</p></div>
              <div><p className="text-zinc-500">Veterinario</p><p className="font-medium">{a.veterinarianName}</p></div>
              <div className="col-span-2"><p className="text-zinc-500">Motivo</p><p className="font-medium">{a.reason}</p></div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
