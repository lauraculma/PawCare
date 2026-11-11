"use client";

import { useEffect, useState } from "react";
import { alertsApi, getErrorMessage } from "@/lib/api";
import type { Alert, PageResponse } from "@/lib/types";
import { formatDateTime } from "@/lib/format";
import { Card } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import { buttonClasses } from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";
import AlertBox from "@/components/ui/Alert";
import Spinner from "@/components/ui/Spinner";

export default function AlertsPage() {
  const [alerts, setAlerts] = useState<PageResponse<Alert> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [acknowledging, setAcknowledging] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await alertsApi.list({ acknowledged: false, size: 50 });
      setAlerts(res);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    Promise.resolve().then(() => void load());
  }, []);

  async function acknowledge(id: number) {
    setAcknowledging(id);
    try {
      await alertsApi.acknowledge(id);
      await load();
    } finally {
      setAcknowledging(null);
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeader title="Alertas de telemetría" description="Alertas activas generadas por umbrales fisiológicos." />
      {error ? <AlertBox variant="error" className="mt-4">{error}</AlertBox> : null}
      {loading ? <Spinner label="Cargando alertas..." /> : null}
      {!loading && (!alerts || alerts.content.length === 0) ? <EmptyState title="Sin alertas activas" description="No hay alertas pendientes por reconocer." /> : null}
      <div className="mt-6 grid gap-4">
        {alerts?.content.map(a => (
          <Card key={a.id}>
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="font-semibold text-zinc-900">{a.petName} — {a.type}</h3>
                <p className="text-sm text-zinc-600">{a.message}</p>
                <p className="mt-1 text-xs text-zinc-500">Creada: {formatDateTime(a.createdAt)}</p>
              </div>
              <div className="flex items-center gap-2">
                <Badge tone={a.severity === "CRITICAL" ? "red" : "amber"}>{a.severity}</Badge>
                <button disabled={acknowledging===a.id} onClick={()=>acknowledge(a.id)} className={buttonClasses("secondary","sm")}>{acknowledging===a.id ? "Reconociendo..." : "Reconocer"}</button>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
