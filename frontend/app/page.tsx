import Link from "next/link";
import ApiStatus from "@/components/ApiStatus";

const modules = [
  {
    title: "Mascotas",
    description: "Registra y consulta la información de cada paciente.",
    href: "/mascotas",
  },
  {
    title: "Citas",
    description: "Agenda, reprograma y cancela citas veterinarias.",
    href: "/citas",
  },
  {
    title: "Agenda veterinaria",
    description: "Consulta la agenda del día e historial de atención.",
    href: "/agenda",
  },
  {
    title: "Collar inteligente",
    description: "Conoce las características del collar para mascotas.",
    href: "/collar",
  },
];

export default function Home() {
  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-16 px-6 py-16">
      <section className="flex flex-col gap-6">
        <ApiStatus />
        <h1 className="max-w-3xl text-4xl font-bold tracking-tight text-zinc-900 sm:text-5xl">
          Cuidado veterinario organizado en un solo lugar
        </h1>
        <p className="max-w-2xl text-lg text-zinc-600">
          PawCare centraliza la gestión de mascotas, citas y la agenda de los
          veterinarios para brindar una atención más clara y oportuna.
        </p>
        <div className="flex flex-wrap gap-3">
          <Link
            href="/register"
            className="rounded-lg bg-emerald-600 px-5 py-3 text-sm font-semibold text-white hover:bg-emerald-700"
          >
            Crear cuenta
          </Link>
          <Link
            href="/collar"
            className="rounded-lg border border-zinc-300 bg-white px-5 py-3 text-sm font-semibold text-zinc-700 hover:border-emerald-600 hover:text-emerald-700"
          >
            Ver collar inteligente
          </Link>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2">
        {modules.map((module) => (
          <Link
            key={module.href}
            href={module.href}
            className="rounded-xl border border-zinc-200 bg-white p-6 transition-colors hover:border-emerald-500"
          >
            <h2 className="text-lg font-semibold text-zinc-900">{module.title}</h2>
            <p className="mt-2 text-sm text-zinc-600">{module.description}</p>
          </Link>
        ))}
      </section>
    </div>
  );
}
