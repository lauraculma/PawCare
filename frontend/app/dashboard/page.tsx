"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useSession } from "@/components/providers/SessionProvider";
import Alert from "@/components/ui/Alert";
import Badge from "@/components/ui/Badge";
import { buttonClasses } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import Spinner from "@/components/ui/Spinner";
import { alertsApi, appointmentsApi, getErrorMessage, petsApi } from "@/lib/api";
import { roleLabels } from "@/lib/format";
import type { Role } from "@/lib/types";

type Stats = {
  pets: number;
  upcomingAppointments: number;
  activeAlerts: number;
};

const quickLinks: Record<Role, { href: string; label: string; description: string }[]> = {
  OWNER: [
    {
      href: "/mascotas",
      label: "Mis mascotas",
      description: "Registra mascotas y consulta sus datos.",
    },
    {
      href: "/citas",
      label: "Mis citas",
      description: "Agenda, reprograma o cancela una cita.",
    },
    {
      href: "/collar",
      label: "Collar inteligente",
      description: "Consulta la telemetria y alertas de tus mascotas.",
    },
  ],
  VETERINARIAN: [
    {
      href: "/agenda",
      label: "Agenda del dia",
      description: "Revisa las citas asignadas y su estado.",
    },
    {
      href: "/mascotas",
      label: "Pacientes",
      description: "Consulta las mascotas y su informacion clinica.",
    },
    {
      href: "/citas",
      label: "Citas",
      description: "Gestiona la programacion de citas.",
    },
  ],
  ADMIN: [
    {
      href: "/mascotas",
      label: "Mascotas",
      description: "Administra todas las mascotas registradas.",
    },
    {
      href: "/citas",
      label: "Citas",
      description: "Supervisa la programacion de la clinica.",
    },
    {
      href: "/agenda",
      label: "Agenda",
      description: "Consulta la agenda general.",
    },
  ],
};

export default function DashboardPage() {
  const { user, loading } = useSession();
  const [stats, setStats] = useState<Stats | null>(null);
  const [statsError, setStatsError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) {
      return;
    }

    let active = true;
    Promise.resolve().then(async () => {
      setStatsError(null);
      try {
        const from = new Date().toISOString();
        const [pets, appointments, alerts] = await Promise.all([
          petsApi.list({ size: 1 }),
          appointmentsApi.list({ from, size: 1 }),
          alertsApi.list({ acknowledged: false, size: 1 }),
        ]);
        if (active) {
          setStats({
            pets: pets.totalElements,
            upcomingAppointments: appointments.totalElements,
            activeAlerts: alerts.totalElements,
          });
        }
      } catch (error) {
        if (active) {
          setStatsError(getErrorMessage(error));
        }
      }
    });
    return () => {
      active = false;
    };
  }, [user]);

  if (loading) {
    return <Spinner label="Cargando sesion..." />;
  }

  if (!user) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16">
        <Alert variant="warning">
          Tu sesion no esta disponible.{" "}
          <Link href="/login" className="font-semibold underline">
            Inicia sesion nuevamente
          </Link>
          .
        </Alert>
      </div>
    );
  }

  const links = quickLinks[user.role];

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-8 px-6 py-10">
      <PageHeader
        title={`Hola, ${user.fullName.split(" ")[0]}`}
        description="Este es el resumen de tu actividad en PawCare."
        action={<Badge tone="emerald">{roleLabels[user.role]}</Badge>}
      />

      {statsError ? (
        <Alert variant="warning">
          No fue posible cargar el resumen: {statsError}
        </Alert>
      ) : null}

      <section className="grid gap-4 sm:grid-cols-3">
        <Card>
          <p className="text-sm font-medium text-zinc-500">Mascotas</p>
          <p className="mt-2 text-3xl font-bold text-zinc-900">
            {stats ? stats.pets : "-"}
          </p>
          <p className="mt-1 text-xs text-zinc-500">
            {user.role === "OWNER" ? "Registradas a tu nombre" : "En la clinica"}
          </p>
        </Card>
        <Card>
          <p className="text-sm font-medium text-zinc-500">Citas proximas</p>
          <p className="mt-2 text-3xl font-bold text-zinc-900">
            {stats ? stats.upcomingAppointments : "-"}
          </p>
          <p className="mt-1 text-xs text-zinc-500">Desde ahora en adelante</p>
        </Card>
        <Card>
          <p className="text-sm font-medium text-zinc-500">Alertas activas</p>
          <p className="mt-2 text-3xl font-bold text-zinc-900">
            {stats ? stats.activeAlerts : "-"}
          </p>
          <p className="mt-1 text-xs text-zinc-500">Sin reconocer</p>
        </Card>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold text-zinc-900">Accesos rapidos</h2>
        <div className="grid gap-4 sm:grid-cols-3">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="rounded-xl border border-zinc-200 bg-white p-6 transition-colors hover:border-emerald-500"
            >
              <h3 className="font-semibold text-zinc-900">{link.label}</h3>
              <p className="mt-2 text-sm text-zinc-600">{link.description}</p>
              <span className="mt-4 inline-block text-sm font-semibold text-emerald-700">
                Abrir modulo
              </span>
            </Link>
          ))}
        </div>
      </section>

      <section className="flex flex-wrap gap-3">
        <Link href="/mascotas" className={buttonClasses("primary", "md")}>
          Gestionar mascotas
        </Link>
        <Link href="/citas" className={buttonClasses("secondary", "md")}>
          Ver citas
        </Link>
      </section>
    </div>
  );
}
