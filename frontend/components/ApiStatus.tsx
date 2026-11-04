"use client";

import { useEffect, useState } from "react";
import { getHealth } from "@/lib/api";

type Status = "loading" | "online" | "offline";

export default function ApiStatus() {
  const [status, setStatus] = useState<Status>("loading");

  useEffect(() => {
    getHealth()
      .then(() => setStatus("online"))
      .catch(() => setStatus("offline"));
  }, []);

  const styles: Record<Status, string> = {
    loading: "bg-zinc-100 text-zinc-600",
    online: "bg-emerald-100 text-emerald-700",
    offline: "bg-amber-100 text-amber-700",
  };

  const labels: Record<Status, string> = {
    loading: "Verificando API...",
    online: "API conectada",
    offline: "API no disponible",
  };

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-sm font-medium ${styles[status]}`}
    >
      <span
        className={`h-2 w-2 rounded-full ${
          status === "online"
            ? "bg-emerald-500"
            : status === "offline"
              ? "bg-amber-500"
              : "bg-zinc-400"
        }`}
      />
      {labels[status]}
    </span>
  );
}
