"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Alert from "@/components/ui/Alert";
import { buttonClasses } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { Field, Input, Select } from "@/components/ui/Field";
import { getErrorMessage, petsApi, vitalsApi } from "@/lib/api";
import type { Pet, PetPayload, VitalSign } from "@/lib/types";

type DeviceLocation = { latitude: number; longitude: number; capturedAt: string };
type DemoVitalSign = {
  heartRate: number;
  spo2: number;
  temperatureC: number;
  activityLevel: string;
  recordedAt: string;
};

function generateDemoVitalSign(): DemoVitalSign {
  return {
    heartRate: 96 + Math.floor(Math.random() * 25),
    spo2: 97 + Math.floor(Math.random() * 3),
    temperatureC: Number((38 + Math.random() * 0.8).toFixed(1)),
    activityLevel: "Moderada",
    recordedAt: new Date().toISOString(),
  };
}

function latestReadingForCollar(readings: VitalSign[], collarId?: string | null) {
  const collarKey = collarId?.trim().toLocaleLowerCase();
  return collarKey
    ? readings.find(reading => reading.collarId?.trim().toLocaleLowerCase() === collarKey) ?? null
    : null;
}

export default function OwnerCollarPanel() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [vitals, setVitals] = useState<Record<number, VitalSign | null>>({});
  const [demoVitals, setDemoVitals] = useState<Record<number, DemoVitalSign>>({});
  const [selectedPetId, setSelectedPetId] = useState("");
  const [collarId, setCollarId] = useState("");
  const [location, setLocation] = useState<DeviceLocation | null>(null);
  const [locationPanels, setLocationPanels] = useState<Record<number, boolean>>({});
  const [mapCenterVersions, setMapCenterVersions] = useState<Record<number, number>>({});
  const [locationError, setLocationError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [locating, setLocating] = useState(false);
  const [refreshingPets, setRefreshingPets] = useState<number[]>([]);

  const requestDeviceLocation = useCallback(() => {
    setLocationError(null);
    if (!window.isSecureContext) {
      setLocationError("La ubicación requiere HTTPS (o abrir PawCare en este mismo dispositivo mediante localhost).");
      return;
    }
    if (!navigator.geolocation) {
      setLocationError("Este navegador no permite consultar la ubicación del dispositivo.");
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      position => {
        setLocation({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          capturedAt: new Date().toISOString(),
        });
        setLocating(false);
      },
      positionError => {
        const message = positionError.code === positionError.PERMISSION_DENIED
          ? "No se concedió permiso para consultar la ubicación. Puedes habilitarlo en el navegador e intentarlo de nuevo."
          : "No fue posible obtener la ubicación. Verifica el permiso y la conexión del dispositivo.";
        setLocationError(message);
        setLocating(false);
      },
      { enableHighAccuracy: false, timeout: 12000, maximumAge: 60000 },
    );
  }, []);

  useEffect(() => {
    let active = true;
    Promise.resolve().then(async () => {
      try {
        const page = await petsApi.list({ size: 100 });
        const ownedPets = page.content;
        if (!active) return;
        setPets(ownedPets);
        setSelectedPetId(current => current || String(ownedPets.find(pet => !pet.collarId)?.id ?? ownedPets[0]?.id ?? ""));
        if (ownedPets.some(pet => pet.collarId)) requestDeviceLocation();
        const measurements = await Promise.all(
          ownedPets.filter(pet => pet.collarId).map(async pet => {
            try {
              const readings = await vitalsApi.list(pet.id);
              const reading = latestReadingForCollar(readings.content, pet.collarId);
              return { petId: pet.id, reading, demo: reading ? null : generateDemoVitalSign() };
            } catch {
              return { petId: pet.id, reading: null, demo: generateDemoVitalSign() };
            }
          }),
        );
        if (!active) return;
        setVitals(Object.fromEntries(measurements.map(({ petId, reading }) => [petId, reading] as const)));
        setDemoVitals(Object.fromEntries(measurements.flatMap(({ petId, demo }) => demo ? [[petId, demo] as const] : [])));
      } catch (e) {
        if (active) setError(getErrorMessage(e));
      } finally {
        if (active) setLoading(false);
      }
    });
    return () => { active = false; };
  }, [requestDeviceLocation]);

  async function refreshVitals(petId: number) {
    setRefreshingPets(current => [...current, petId]);
    try {
      const readings = await vitalsApi.list(petId);
      const pet = pets.find(item => item.id === petId);
      const latest = latestReadingForCollar(readings.content, pet?.collarId);
      setVitals(current => ({ ...current, [petId]: latest }));
      setDemoVitals(current => {
        const next = { ...current };
        if (latest) delete next[petId];
        else next[petId] = generateDemoVitalSign();
        return next;
      });
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setRefreshingPets(current => current.filter(id => id !== petId));
    }
  }

  async function registerCollar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const pet = pets.find(item => item.id === Number(selectedPetId));
    if (!pet) return;
    setSaving(true);
    setError(null);
    try {
      const payload: PetPayload = {
        name: pet.name,
        species: pet.species,
        breed: pet.breed ?? undefined,
        birthDate: pet.birthDate ?? undefined,
        weightKg: pet.weightKg ?? null,
        sex: pet.sex ?? undefined,
        collarId: collarId.trim(),
        notes: pet.notes ?? undefined,
      };
      const updatedPet = await petsApi.update(pet.id, payload);
      setPets(current => current.map(item => item.id === updatedPet.id ? updatedPet : item));
      const nextPetWithoutCollar = pets.find(item => item.id !== updatedPet.id && !item.collarId);
      setSelectedPetId(nextPetWithoutCollar ? String(nextPetWithoutCollar.id) : "");
      setCollarId("");
      if (!location && !locating && !locationError) requestDeviceLocation();
      try {
        const readings = await vitalsApi.list(updatedPet.id);
        const latest = latestReadingForCollar(readings.content, updatedPet.collarId);
        setVitals(current => ({ ...current, [updatedPet.id]: latest }));
        setDemoVitals(current => {
          if (latest) {
            const next = { ...current };
            delete next[updatedPet.id];
            return next;
          }
          return { ...current, [updatedPet.id]: generateDemoVitalSign() };
        });
      } catch {
        setVitals(current => ({ ...current, [updatedPet.id]: null }));
        setDemoVitals(current => ({ ...current, [updatedPet.id]: generateDemoVitalSign() }));
      }
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  const petsWithCollar = pets.filter(pet => pet.collarId);

  return (
    <section className="space-y-4">
      <div>
        <h2 className="text-lg font-semibold text-zinc-900">Collar inteligente</h2>
        <p className="mt-1 text-sm text-zinc-600">Vincula el identificador de un collar y consulta las últimas mediciones recibidas.</p>
      </div>

      {error ? <Alert variant="error">{error}</Alert> : null}

      {!loading && pets.some(pet => !pet.collarId) ? (
        <Card>
          <CardHeader title="Registrar collar en una mascota" description="Ingresa el identificador impreso en el collar." />
          <form onSubmit={registerCollar} className="mt-4 grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
            <Field label="Mascota" required>
              <Select required value={selectedPetId} onChange={event => setSelectedPetId(event.target.value)}>
                {pets.filter(pet => !pet.collarId).map(pet => <option key={pet.id} value={pet.id}>{pet.name}</option>)}
              </Select>
            </Field>
            <Field label="ID del collar" required>
              <Input required maxLength={60} value={collarId} onChange={event => setCollarId(event.target.value)} placeholder="Ej. PC-000123" />
            </Field>
            <button type="submit" disabled={saving || !collarId.trim()} className={buttonClasses("primary", "md")}>
              {saving ? "Vinculando..." : "Registrar collar"}
            </button>
          </form>
        </Card>
      ) : null}

      {petsWithCollar.length > 0 ? (
        <>
          <div className="grid items-start gap-4 lg:grid-cols-2">
            {petsWithCollar.map(pet => {
              const reading = vitals[pet.id] ?? demoVitals[pet.id] ?? null;
              const isDemoReading = !vitals[pet.id] && Boolean(demoVitals[pet.id]);
              return (
                <Card key={pet.id}>
                  <CardHeader title={pet.name} description={`Collar ${pet.collarId}`} />
                  {reading ? (
                    <>
                      <div className="mt-4 grid grid-cols-2 gap-3">
                        <Metric kind="heart" label="Frecuencia cardiaca" value={reading.heartRate == null ? "Sin dato" : `${reading.heartRate} lpm`} />
                        <Metric kind="oxygen" label="Oxígeno en sangre" value={reading.spo2 == null ? "Sin dato" : `${reading.spo2}%`} />
                        <Metric kind="temperature" label="Temperatura" value={reading.temperatureC == null ? "Sin dato" : `${reading.temperatureC} °C`} />
                        <Metric kind="activity" label="Actividad" value={reading.activityLevel || "Sin dato"} />
                      </div>
                      {isDemoReading ? <p className="mt-3 rounded-lg bg-amber-50 p-3 text-xs text-amber-900">Lectura simulada de demostración; no proviene de un sensor y no debe usarse para decisiones de salud.</p> : null}
                      <p className="mt-3 text-xs text-zinc-500">{isDemoReading ? "Lectura generada" : "Última medición recibida"}: {new Date(reading.recordedAt).toLocaleString("es-CO")}</p>
                    </>
                  ) : (
                    <p className="mt-4 rounded-lg bg-zinc-50 p-3 text-sm text-zinc-600">Aún no se reciben mediciones de este collar.</p>
                  )}
                  <div className="mt-4 flex flex-wrap gap-2">
                    <button type="button" onClick={() => void refreshVitals(pet.id)} disabled={refreshingPets.includes(pet.id)} className={buttonClasses("secondary", "sm")}>
                      {refreshingPets.includes(pet.id) ? "Actualizando..." : "Actualizar mediciones"}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        const willOpen = !locationPanels[pet.id];
                        setLocationPanels(current => ({ ...current, [pet.id]: willOpen }));
                        if (willOpen && !locating) requestDeviceLocation();
                      }}
                      className={buttonClasses("primary", "sm")}
                    >
                      {locationPanels[pet.id] ? "Ocultar ubicación" : "Ver ubicación"}
                    </button>
                  </div>
                  {locationPanels[pet.id] ? (
                    <div className="mt-4 border-t border-zinc-200 pt-4">
                      <p className="mb-3 text-sm font-semibold text-zinc-800">Mapa de ubicación del dispositivo para la ficha de {pet.name}</p>
                      {location ? (
                        <div className="relative overflow-hidden rounded-2xl border border-zinc-200 bg-zinc-100 shadow-sm">
                          <iframe
                            key={`${pet.id}-${mapCenterVersions[pet.id] ?? 0}`}
                            title={`Mapa de ubicación del dispositivo para ${pet.name}`}
                            src={`https://maps.google.com/maps?q=${location.latitude}%2C${location.longitude}&z=16&output=embed`}
                            className="h-64 w-full sm:h-80"
                            loading="lazy"
                            referrerPolicy="no-referrer"
                          />
                          <button
                            type="button"
                            onClick={() => setMapCenterVersions(current => ({
                              ...current,
                              [pet.id]: (current[pet.id] ?? 0) + 1,
                            }))}
                            className="absolute right-3 top-3 inline-flex items-center gap-2 rounded-lg border border-zinc-200 bg-white/95 px-3 py-2 text-sm font-semibold text-zinc-800 shadow-md transition hover:bg-white"
                            aria-label={`Centrar el mapa de ${pet.name} en la ubicación del dispositivo`}
                          >
                            <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" className="h-4 w-4 text-emerald-700">
                              <circle cx="12" cy="12" r="7" />
                              <circle cx="12" cy="12" r="2" />
                              <path d="M12 2v3m0 14v3M2 12h3m14 0h3" />
                            </svg>
                            Centrar mapa
                          </button>
                          <span className="absolute bottom-3 left-3 rounded-lg bg-white/95 px-3 py-1.5 text-xs font-medium text-zinc-700 shadow">
                            Ubicación del dispositivo
                          </span>
                        </div>
                      ) : (
                        <div role="status" className="flex h-56 items-center justify-center rounded-lg border border-dashed border-zinc-300 bg-zinc-50 px-5 text-center text-sm text-zinc-600">
                          {locating ? "Consultando la ubicación para cargar el mapa..." : locationError ?? "Preparando el mapa de ubicación..."}
                        </div>
                      )}
                      {location ? (
                        <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-zinc-700">
                          <span>{location.latitude.toFixed(5)}, {location.longitude.toFixed(5)}</span>
                          <a className="font-medium text-emerald-700 underline" href={`https://www.google.com/maps?q=${location.latitude},${location.longitude}`} target="_blank" rel="noreferrer">Abrir en Google Maps</a>
                          <span className="text-xs text-zinc-500">Actualizada {new Date(location.capturedAt).toLocaleTimeString("es-CO")}</span>
                        </div>
                      ) : null}
                      {locationError ? <p role="status" className="mt-3 text-sm text-amber-800">{locationError}</p> : null}
                      {!location || locationError ? (
                        <button type="button" onClick={requestDeviceLocation} disabled={locating} className={`${buttonClasses("secondary", "sm")} mt-3`}>
                          {locating ? "Consultando ubicación..." : "Intentar de nuevo"}
                        </button>
                      ) : null}
                      <p className="mt-3 text-xs text-zinc-500">Es la ubicación del dispositivo que usa PawCare, no la ubicación GPS del collar ni de la mascota.</p>
                    </div>
                  ) : null}
                </Card>
              );
            })}
          </div>
        </>
      ) : null}
    </section>
  );
}

function Metric({ kind, label, value }: { kind: "heart" | "oxygen" | "temperature" | "activity"; label: string; value: string }) {
  const icons = {
    heart: <><path d="M20.8 8.7c0 5.2-8.8 10.1-8.8 10.1S3.2 13.9 3.2 8.7A4.7 4.7 0 0 1 12 6.3a4.7 4.7 0 0 1 8.8 2.4Z" /><path d="M4 12h4l2-3 3.5 6 2-3H20" /></>,
    oxygen: <><path d="M12 21s7-4.4 7-11a7 7 0 1 0-14 0c0 6.6 7 11 7 11Z" /><path d="M8 12h2l1.2-2.3L13 15l1.2-3H16" /></>,
    temperature: <><path d="M14 14.8V5a3 3 0 0 0-6 0v9.8a5 5 0 1 0 6 0Z" /><path d="M11 12v6" /></>,
    activity: <><path d="M3 12h4l2.3-6 4.4 12 2.3-6H21" /><circle cx="12" cy="12" r="9" /></>,
  };
  return (
    <div className="flex min-h-28 items-center gap-3 rounded-xl border border-emerald-100 bg-gradient-to-br from-white to-emerald-50 p-3 shadow-sm">
      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className="h-6 w-6">{icons[kind]}</svg>
      </span>
      <span className="min-w-0">
        <span className="block text-xs text-zinc-600">{label}</span>
        <span className="mt-1 block text-lg font-bold text-zinc-900">{value}</span>
      </span>
    </div>
  );
}
