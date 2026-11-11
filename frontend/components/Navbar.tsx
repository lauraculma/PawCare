"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { useSession } from "@/components/providers/SessionProvider";
import { buttonClasses } from "@/components/ui/Button";
import { roleLabels } from "@/lib/format";

const publicLinks = [{ href: "/collar", label: "Collar inteligente" }];

const privateLinks = [
  { href: "/dashboard", label: "Panel" },
  { href: "/mascotas", label: "Mascotas" },
  { href: "/citas", label: "Citas" },
  { href: "/agenda", label: "Agenda" },
];

export default function Navbar() {
  const { user, loading, logout } = useSession();
  const pathname = usePathname();
  const router = useRouter();
  const [signingOut, setSigningOut] = useState(false);

  const links = user
    ? [...privateLinks, ...(user.role === "ADMIN" ? [{ href: "/admin/veterinarios", label: "Aprobaciones" }] : []), ...publicLinks]
    : publicLinks;

  async function handleLogout() {
    setSigningOut(true);
    try {
      await logout();
      router.push("/login");
      router.refresh();
    } finally {
      setSigningOut(false);
    }
  }

  function isActive(href: string) {
    return pathname === href || pathname.startsWith(`${href}/`);
  }

  return (
    <header className="border-b border-zinc-200 bg-white">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-4">
        <Link
          href={user ? "/dashboard" : "/"}
          className="text-xl font-bold tracking-tight text-emerald-700"
        >
          PawCare
        </Link>

        <nav className="flex flex-wrap items-center gap-4 text-sm font-medium text-zinc-600">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={
                isActive(link.href)
                  ? "text-emerald-700"
                  : "hover:text-emerald-700"
              }
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-3 text-sm font-medium">
          {loading ? (
            <span className="h-8 w-28 animate-pulse rounded-lg bg-zinc-100" />
          ) : user ? (
            <>
              <div className="hidden text-right sm:block">
                <p className="text-sm font-semibold text-zinc-900">
                  {user.fullName}
                </p>
                <p className="text-xs text-zinc-500">{roleLabels[user.role]}</p>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                disabled={signingOut}
                className={buttonClasses("secondary", "sm")}
              >
                {signingOut ? "Saliendo..." : "Salir"}
              </button>
            </>
          ) : (
            <>
              <Link
                href="/login"
                className="text-zinc-600 hover:text-emerald-700"
              >
                Iniciar sesion
              </Link>
              <Link
                href="/register"
                className={buttonClasses("primary", "md")}
              >
                Registrarse
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
