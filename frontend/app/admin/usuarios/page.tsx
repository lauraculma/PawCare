"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import Link from "next/link";
import { useSession } from "@/components/providers/SessionProvider";
import Alert from "@/components/ui/Alert";
import { buttonClasses } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import Spinner from "@/components/ui/Spinner";
import Modal from "@/components/ui/Modal";
import { Field, Input } from "@/components/ui/Field";
import { adminApi, getErrorMessage } from "@/lib/api";
import type { AdminUserUpdatePayload, User } from "@/lib/types";

type UserFilter = "ALL" | "OWNER" | "VETERINARIAN";

export default function AdminUsersPage() {
  const { user, loading } = useSession();
  const [users, setUsers] = useState<User[]>([]);
  const [filter, setFilter] = useState<UserFilter>("ALL");
  const [search, setSearch] = useState("");
  const [busy, setBusy] = useState(false);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [editing, setEditing] = useState<User | null>(null);
  const [form, setForm] = useState<AdminUserUpdatePayload>({ fullName: "", email: "", cedula: "", phone: "" });

  const loadUsers = useCallback(async () => {
    setError(null);
    setLoadingUsers(true);
    try {
      const firstPage = await adminApi.users(0, 100);
      const allUsers = [...firstPage.content];
      for (let pageNumber = 1; pageNumber < firstPage.totalPages; pageNumber += 1) {
        const nextPage = await adminApi.users(pageNumber, 100);
        allUsers.push(...nextPage.content);
      }
      setUsers(allUsers);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoadingUsers(false);
    }
  }, []);

  useEffect(() => {
    if (user?.role === "ADMIN") void loadUsers();
  }, [user, loadUsers]);

  const visibleUsers = useMemo(() => {
    const term = search.trim().toLocaleLowerCase();
    return users.filter((item) => {
      const matchesRole = filter === "ALL" || item.role === filter;
      const matchesText = !term || [item.fullName, item.email, item.cedula ?? ""].some(value => value.toLocaleLowerCase().includes(term));
      return matchesRole && matchesText;
    });
  }, [users, filter, search]);

  function beginEdit(candidate: User) {
    setEditing(candidate);
    setForm({ fullName: candidate.fullName, email: candidate.email, cedula: candidate.cedula ?? "", phone: candidate.phone ?? "" });
    setError(null);
    setSuccess(null);
  }

  async function saveUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await adminApi.updateUser(editing.internalId, { ...form, phone: form.phone?.trim() || undefined });
      setUsers(current => current.map(item => item.internalId === updated.internalId ? updated : item));
      setEditing(null);
      setSuccess(`Se actualizaron los datos de ${updated.fullName}.`);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function removeUser(candidate: User) {
    const roleName = candidate.role === "OWNER" ? "propietario" : "veterinario";
    if (!window.confirm(`¿Eliminar la cuenta de ${candidate.fullName}? También se eliminarán sus mascotas, citas e historias clínicas asociadas.`)) return;
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      await adminApi.deleteUser(candidate.internalId);
      setUsers(current => current.filter(item => item.internalId !== candidate.internalId));
      setSuccess(`Se eliminó la cuenta del ${roleName} ${candidate.fullName} y sus datos asociados.`);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <Spinner label="Verificando sesión..." />;
  if (!user) return <div className="mx-auto max-w-3xl px-6 py-12"><Alert variant="warning">Inicia sesión para continuar. <Link href="/login" className="font-semibold underline">Ir al login</Link>.</Alert></div>;
  if (user.role !== "ADMIN") return <div className="mx-auto max-w-3xl px-6 py-12"><Alert variant="error">No tienes permisos para administrar usuarios.</Alert></div>;

  return (
    <main className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-10">
      <PageHeader title="Propietarios y veterinarios" description="Consulta y administra las cuentas registradas en PawCare." />
      {error ? <Alert variant="error">{error}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}
      <Card>
        <div className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-center">
          <Input value={search} onChange={e => setSearch(e.target.value)} placeholder="Buscar por nombre, correo o cédula" />
          <div className="flex flex-wrap gap-2">
            {(["ALL", "OWNER", "VETERINARIAN"] as UserFilter[]).map(value => (
              <button key={value} type="button" onClick={() => setFilter(value)} className={buttonClasses(filter === value ? "primary" : "secondary", "sm")}>
                {value === "ALL" ? "Todos" : value === "OWNER" ? "Propietarios" : "Veterinarios"}
              </button>
            ))}
          </div>
        </div>
      </Card>
      <p className="text-sm text-zinc-600">Cuentas visibles: <strong>{visibleUsers.length}</strong></p>
      {loadingUsers ? <Spinner label="Cargando usuarios..." /> : visibleUsers.length === 0 ? <Card><p className="text-sm text-zinc-600">No se encontraron cuentas.</p></Card> : (
        <div className="grid gap-4 sm:grid-cols-2">
          {visibleUsers.map(candidate => (
            <Card key={candidate.internalId}>
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <h2 className="truncate font-semibold text-zinc-900">{candidate.fullName}</h2>
                  <p className="mt-1 break-all text-sm text-zinc-600">{candidate.email}</p>
                  <p className="mt-1 text-sm text-zinc-600">Cédula: {candidate.cedula || "Sin registrar"}</p>
                  {candidate.phone ? <p className="mt-1 text-sm text-zinc-600">Teléfono: {candidate.phone}</p> : null}
                  <p className="mt-2 text-xs font-medium text-emerald-700">{candidate.role === "OWNER" ? "Propietario" : "Veterinario"}{candidate.role === "VETERINARIAN" && !candidate.approved ? " · Pendiente de aprobación" : ""}</p>
                </div>
              </div>
              <div className="mt-4 flex justify-end gap-2">
                <button type="button" disabled={busy} onClick={() => beginEdit(candidate)} className={buttonClasses("secondary", "sm")}>Editar</button>
                <button type="button" disabled={busy} onClick={() => void removeUser(candidate)} className={buttonClasses("danger", "sm")}>Eliminar</button>
              </div>
            </Card>
          ))}
        </div>
      )}
      <Modal open={editing !== null} onClose={() => !busy && setEditing(null)} title="Editar usuario">
        <form onSubmit={saveUser} className="space-y-4">
          {error ? <Alert variant="error">{error}</Alert> : null}
          <Field label="Nombre completo" required><Input required maxLength={120} value={form.fullName} onChange={e => setForm(value => ({ ...value, fullName: e.target.value }))} /></Field>
          <Field label="Correo electrónico" required><Input required type="email" maxLength={160} value={form.email} onChange={e => setForm(value => ({ ...value, email: e.target.value }))} /></Field>
          <Field label="Cédula" required><Input required inputMode="numeric" pattern="[0-9]{5,20}" value={form.cedula} onChange={e => setForm(value => ({ ...value, cedula: e.target.value }))} /></Field>
          <Field label="Teléfono"><Input maxLength={30} value={form.phone ?? ""} onChange={e => setForm(value => ({ ...value, phone: e.target.value }))} /></Field>
          <div className="flex justify-end gap-2">
            <button type="button" disabled={busy} onClick={() => setEditing(null)} className={buttonClasses("secondary", "md")}>Cancelar</button>
            <button type="submit" disabled={busy} className={buttonClasses("primary", "md")}>{busy ? "Guardando..." : "Guardar cambios"}</button>
          </div>
        </form>
      </Modal>
    </main>
  );
}
