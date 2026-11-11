"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/providers/SessionProvider";
import Alert from "@/components/ui/Alert";
import { buttonClasses } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import Spinner from "@/components/ui/Spinner";
import { adminApi, getErrorMessage } from "@/lib/api";
import type { User } from "@/lib/types";

export default function PendingVeterinariansPage() {
  const { user, loading } = useSession();
  const [pending, setPending] = useState<User[]>([]);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const loadPending = useCallback(async () => {
    setPageError(null);
    try {
      const result = await adminApi.pendingVeterinarians();
      setPending(result.content);
    } catch (error) {
      setPageError(getErrorMessage(error));
    }
  }, []);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      void loadPending();
    }
  }, [user, loadPending]);

  async function approve(id: number) {
    setBusyId(id);
    setPageError(null);
    setSuccess(null);
    try {
      const approved = await adminApi.approveVeterinarian(id);
      setPending((current) => current.filter((item) => item.id !== approved.id));
      setSuccess(`La cuenta de ${approved.fullName} fue aprobada. Ya puede iniciar sesion.`);
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setBusyId(null);
    }
  }

  if (loading) return <Spinner label="Verificando sesion..." />;

  if (!user) {
    return <div className="mx-auto max-w-3xl px-6 py-12"><Alert variant="warning">Inicia sesion para continuar. <Link href="/login" className="font-semibold underline">Ir al login</Link>.</Alert></div>;
  }

  if (user.role !== "ADMIN") {
    return <div className="mx-auto max-w-3xl px-6 py-12"><Alert variant="error">No tienes permisos para revisar solicitudes veterinarias.</Alert></div>;
  }

  return (
    <main className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-10">
      <PageHeader title="Aprobacion de veterinarios" description="Revisa las solicitudes de registro. Solo las cuentas aprobadas pueden iniciar sesion con el rol de veterinario." />
      {pageError ? <Alert variant="error">{pageError}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-zinc-600">Solicitudes pendientes: <strong>{pending.length}</strong></p>
        <button type="button" onClick={() => void loadPending()} className={buttonClasses("secondary", "sm")}>Actualizar</button>
      </div>
      {pending.length === 0 ? (
        <Card><p className="text-sm text-zinc-600">No hay solicitudes pendientes de aprobacion.</p></Card>
      ) : (
        <div className="grid gap-4">
          {pending.map((candidate) => (
            <Card key={candidate.id}>
              <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div>
                  <h2 className="font-semibold text-zinc-900">{candidate.fullName}</h2>
                  <p className="mt-1 text-sm text-zinc-600">{candidate.email}</p>
                  {candidate.phone ? <p className="mt-1 text-sm text-zinc-500">Telefono: {candidate.phone}</p> : null}
                  <p className="mt-1 text-xs text-amber-700">Pendiente de aprobacion</p>
                </div>
                <button type="button" disabled={busyId !== null} onClick={() => void approve(candidate.id)} className={buttonClasses("primary", "md")}>
                  {busyId === candidate.id ? "Aprobando..." : "Aprobar cuenta"}
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </main>
  );
}
