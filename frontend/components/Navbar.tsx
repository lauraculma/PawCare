import Link from "next/link";

const links = [
  { href: "/mascotas", label: "Mascotas" },
  { href: "/citas", label: "Citas" },
  { href: "/agenda", label: "Agenda" },
  { href: "/collar", label: "Collar inteligente" },
];

export default function Navbar() {
  return (
    <header className="border-b border-zinc-200 bg-white">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-4">
        <Link href="/" className="text-xl font-bold tracking-tight text-emerald-700">
          PawCare
        </Link>
        <nav className="flex flex-wrap items-center gap-5 text-sm font-medium text-zinc-600">
          {links.map((link) => (
            <Link key={link.href} href={link.href} className="hover:text-emerald-700">
              {link.label}
            </Link>
          ))}
        </nav>
        <div className="flex items-center gap-3 text-sm font-medium">
          <Link href="/login" className="text-zinc-600 hover:text-emerald-700">
            Iniciar sesión
          </Link>
          <Link
            href="/register"
            className="rounded-lg bg-emerald-600 px-4 py-2 text-white hover:bg-emerald-700"
          >
            Registrarse
          </Link>
        </div>
      </div>
    </header>
  );
}
