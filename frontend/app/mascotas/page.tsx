"use client";

import { useEffect, useState } from "react";
import { petsApi, getErrorMessage } from "@/lib/api";
import type { PageResponse, Pet, PetPayload } from "@/lib/types";
import { ageFromBirthDate, formatDateInput } from "@/lib/format";
import { Card, CardHeader } from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import { buttonClasses } from "@/components/ui/Button";
import { useSession } from "@/components/providers/SessionProvider";
import Modal from "@/components/ui/Modal";
import { Field, Input, Select, Textarea } from "@/components/ui/Field";
import Alert from "@/components/ui/Alert";
import Spinner from "@/components/ui/Spinner";

export default function PetsPage() {
  const { user } = useSession();
  const [pets, setPets] = useState<PageResponse<Pet> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const size = 12;
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Pet | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await petsApi.list({ search: query || undefined, page, size });
      setPets(data);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    Promise.resolve().then(() => void load());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  function openCreate() {
    setEditing(null);
    setOpen(true);
  }
  function openEdit(pet: Pet) {
    setEditing(pet);
    setOpen(true);
  }

  const canCreate = user && user.role !== "OWNER";

  async function handleSave(data: PetPayload) {
    setSaving(true);
    setFormError(null);
    try {
      if (editing) {
        await petsApi.update(editing.id, data);
      } else {
        await petsApi.create(data);
      }
      setOpen(false);
      await load();
    } catch (e) {
      setFormError(getErrorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeader
        title="Mascotas"
        description="Gestiona el registro y la información de cada paciente."
        action={
          canCreate ? (
            <button onClick={openCreate} className={buttonClasses("primary", "md")}>
              Registrar mascota
            </button>
          ) : null
        }
      />

      <Card className="mt-6">
        <div className="flex flex-wrap items-center gap-3">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Buscar por nombre, especie o raza"
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none focus:ring-2 focus:ring-emerald-600/20"
          />
          <button
            onClick={() => {
              setPage(0);
              void load();
            }}
            className={buttonClasses("secondary", "md")}
          >
            Buscar
          </button>
        </div>
      </Card>

      {error ? <Alert variant="error" className="mt-4">{error}</Alert> : null}

      {loading ? <Spinner label="Cargando mascotas..." /> : null}

      {!loading && pets && pets.content.length === 0 ? (
        <EmptyState title="No hay mascotas registradas" description="Crea la primera mascota para comenzar." />
      ) : null}

      {!loading && pets && pets.content.length > 0 ? (
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          {pets.content.map((pet) => (
            <Card key={pet.id}>
              <CardHeader
                title={pet.name}
                description={`${pet.species}${pet.breed ? ` · ${pet.breed}` : ""}`}
              />
              <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-zinc-500">Propietario</p>
                  <p className="font-medium text-zinc-900">{pet.owner.fullName}</p>
                </div>
                <div>
                  <p className="text-zinc-500">Edad aproximada</p>
                  <p className="font-medium text-zinc-900">{ageFromBirthDate(pet.birthDate)}</p>
                </div>
                <div>
                  <p className="text-zinc-500">Peso</p>
                  <p className="font-medium text-zinc-900">{pet.weightKg ? `${pet.weightKg} kg` : "Sin dato"}</p>
                </div>
                <div>
                  <p className="text-zinc-500">Collar</p>
                  <p className="font-medium text-zinc-900">{pet.collarId ?? "No asignado"}</p>
                </div>
              </div>
              <div className="mt-4 flex justify-end gap-2">
                <button onClick={() => openEdit(pet)} className={buttonClasses("secondary", "sm")}>
                  Editar
                </button>
              </div>
            </Card>
          ))}
        </div>
      ) : null}

      {pets && (
        <div className="mt-6 flex items-center justify-between">
          <p className="text-sm text-zinc-600">
            Página {pets.number + 1} de {pets.totalPages || 1}
          </p>
          <div className="flex gap-2">
            <button
              disabled={pets.first || loading}
              onClick={() => setPage(p => Math.max(0, p - 1))}
              className={buttonClasses("secondary", "sm")}
            >
              Anterior
            </button>
            <button
              disabled={pets.last || loading}
              onClick={() => setPage(p => p + 1)}
              className={buttonClasses("secondary", "sm")}
            >
              Siguiente
            </button>
          </div>
        </div>
      )}

      <Modal open={open} onClose={() => setOpen(false)} title={editing ? "Editar mascota" : "Registrar mascota"}>
        <PetForm
          initial={editing}
          onCancel={() => setOpen(false)}
          onSave={handleSave}
          saving={saving}
          error={formError}
          canSetOwner={user?.role !== "OWNER"}
        />
      </Modal>
    </div>
  );
}

function PetForm({
  initial,
  onCancel,
  onSave,
  saving,
  error,
  canSetOwner,
}: {
  initial?: Pet | null;
  onCancel: () => void;
  onSave: (data: PetPayload) => void;
  saving: boolean;
  error: string | null;
  canSetOwner: boolean;
}) {
  const [form, setForm] = useState<PetPayload>({
    name: initial?.name ?? "",
    species: initial?.species ?? "",
    breed: initial?.breed ?? "",
    birthDate: initial?.birthDate ? formatDateInput(initial.birthDate) : "",
    weightKg: initial?.weightKg ?? null,
    sex: initial?.sex ?? "",
    collarId: initial?.collarId ?? "",
    notes: initial?.notes ?? "",
    ownerId: initial?.owner?.id ?? undefined,
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSave({
          ...form,
          birthDate: form.birthDate ? new Date(form.birthDate).toISOString() : undefined,
          weightKg: form.weightKg ? Number(form.weightKg) : null,
        });
      }}
      className="space-y-4"
    >
      {error ? <Alert variant="error">{error}</Alert> : null}
      <Field label="Nombre" required>
        <Input
          value={form.name}
          onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
        />
      </Field>
      <Field label="Especie" required>
        <Input
          value={form.species}
          onChange={e => setForm(f => ({ ...f, species: e.target.value }))}
        />
      </Field>
      <div className="grid grid-cols-2 gap-4">
        <Field label="Raza">
          <Input value={form.breed ?? ""} onChange={e => setForm(f => ({ ...f, breed: e.target.value }))} />
        </Field>
        <Field label="Sexo">
          <Select value={form.sex ?? ""} onChange={e => setForm(f => ({ ...f, sex: e.target.value }))}>
            <option value="">Selecciona</option>
            <option value="M">Macho</option>
            <option value="F">Hembra</option>
          </Select>
        </Field>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <Field label="Fecha nacimiento">
          <Input type="date" value={form.birthDate ?? ""} onChange={e => setForm(f => ({ ...f, birthDate: e.target.value }))} />
        </Field>
        <Field label="Peso kg">
          <Input type="number" step="0.1" value={form.weightKg ?? ""} onChange={e => setForm(f => ({ ...f, weightKg: e.target.value ? Number(e.target.value) : null }))} />
        </Field>
      </div>
      <Field label="ID Collar">
        <Input value={form.collarId ?? ""} onChange={e => setForm(f => ({ ...f, collarId: e.target.value }))} />
      </Field>
      {canSetOwner && (
        <Field label="ID Propietario">
          <Input type="number" value={form.ownerId ?? ""} onChange={e => setForm(f => ({ ...f, ownerId: e.target.value ? Number(e.target.value) : undefined }))} />
        </Field>
      )}
      <Field label="Notas">
        <Textarea value={form.notes ?? ""} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
      </Field>
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onCancel} className={buttonClasses("secondary", "md")}>Cancelar</button>
        <button type="submit" disabled={saving} className={buttonClasses("primary", "md")}>
          {saving ? "Guardando..." : "Guardar"}
        </button>
      </div>
    </form>
  );
}
