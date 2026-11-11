"use client";

import { createElement, useEffect, useMemo, useRef, useState, type FormEvent, type HTMLAttributes } from "react";
import Button from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";

const PRICE = 200_000;
const STORAGE_KEY = "pawcare-demo-sales-v1";
const MODEL_URL = "/models/collar-pawcare.glb";

type DemoSale = {
  id: string;
  date: string;
  buyer: string;
  email: string;
  phone: string;
  city: string;
  address: string;
  quantity: number;
  total: number;
};

const money = (value: number) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(value);

export default function CollarPage() {
  const [quantity, setQuantity] = useState(1);
  const [buyer, setBuyer] = useState({
    name: "",
    email: "",
    phone: "",
    city: "",
    address: "",
  });
  const [sales, setSales] = useState<DemoSale[]>([]);
  const [completedSale, setCompletedSale] = useState<DemoSale | null>(null);
  const [modelReady, setModelReady] = useState(false);
  const [modelError, setModelError] = useState(false);
  const modelRef = useRef<HTMLElement | null>(null);

  const total = useMemo(() => quantity * PRICE, [quantity]);

  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(STORAGE_KEY);
      if (saved) setSales(JSON.parse(saved) as DemoSale[]);
    } catch {
      setSales([]);
    }
  }, []);

  useEffect(() => {
    const tag = "pawcare-model-viewer";
    if (customElements.get("model-viewer")) {
      setModelReady(true);
      return;
    }
    let script = document.querySelector<HTMLScriptElement>(`script[data-${tag}]`);
    if (!script) {
      script = document.createElement("script");
      script.type = "module";
      script.src =
        "https://cdn.jsdelivr.net/npm/@google/model-viewer/dist/model-viewer.min.js";
      script.setAttribute(`data-${tag}`, "true");
      document.head.appendChild(script);
    }
    const onLoad = () => setModelReady(true);
    const onError = () => setModelError(true);
    script.addEventListener("load", onLoad);
    script.addEventListener("error", onError);
    return () => {
      script?.removeEventListener("load", onLoad);
      script?.removeEventListener("error", onError);
    };
  }, []);

  useEffect(() => {
    const element = modelRef.current;
    if (!element || !modelReady) return;
    const onModelError = () => setModelError(true);
    const onModelLoad = () => setModelError(false);
    element.addEventListener("error", onModelError);
    element.addEventListener("load", onModelLoad);
    return () => {
      element.removeEventListener("error", onModelError);
      element.removeEventListener("load", onModelLoad);
    };
  }, [modelReady]);

  function updateBuyer(field: keyof typeof buyer, value: string) {
    setBuyer((current) => ({ ...current, [field]: value }));
  }

  function confirmDemoSale(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const sale: DemoSale = {
      id: `PC-DEMO-${Date.now().toString().slice(-8)}`,
      date: new Date().toISOString(),
      buyer: buyer.name.trim(),
      email: buyer.email.trim(),
      phone: buyer.phone.trim(),
      city: buyer.city.trim(),
      address: buyer.address.trim(),
      quantity,
      total,
    };
    const next = [sale, ...sales];
    setSales(next);
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    } catch {
      // La confirmación de esta sesión sigue disponible aunque el navegador
      // no permita almacenamiento local.
    }
    setCompletedSale(sale);
    setQuantity(1);
    setBuyer({ name: "", email: "", phone: "", city: "", address: "" });
  }

  function clearDemoSales() {
    setSales([]);
    window.localStorage.removeItem(STORAGE_KEY);
  }

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-10 px-4 py-8 sm:px-6 sm:py-12">
      <section className="grid items-center gap-8 lg:grid-cols-[1.05fr_0.95fr]">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-700">
            Tecnología para su bienestar
          </p>
          <h1 className="mt-3 text-4xl font-bold tracking-tight text-zinc-950 sm:text-5xl">
            Collar inteligente PawCare
          </h1>
          <p className="mt-4 max-w-xl text-lg leading-8 text-zinc-600">
            Una solución vestible diseñada para acompañar el cuidado de tu
            mascota y conectar su información con el ecosistema PawCare.
          </p>
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <span className="text-3xl font-bold text-emerald-700">
              {money(PRICE)}
            </span>
            <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800">
              Precio de demostración
            </span>
          </div>
          <p className="mt-2 text-sm text-zinc-500">
            Precio por unidad · Pesos colombianos (COP)
          </p>
          <a
            href="#simulador-venta"
            className="mt-7 inline-flex rounded-lg bg-emerald-700 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-800"
          >
            Simular compra
          </a>
        </div>

        <div className="overflow-hidden rounded-3xl border border-zinc-200 bg-gradient-to-br from-emerald-50 via-white to-sky-50 p-3 shadow-sm">
          <div className="relative flex min-h-[320px] items-center justify-center overflow-hidden rounded-2xl bg-white sm:min-h-[410px]">
            {createElement("model-viewer", {
              ref: modelRef,
              src: MODEL_URL,
              alt: "Modelo tridimensional del collar inteligente PawCare",
              "camera-controls": true,
              "auto-rotate": true,
              "rotation-per-second": "18deg",
              "shadow-intensity": "1",
              exposure: "1",
              "interaction-prompt": "auto",
              ar: true,
              style: {
                width: "100%",
                height: "410px",
                backgroundColor: "transparent",
                display: modelReady ? "block" : "none",
              },
            } as HTMLAttributes<HTMLElement>)}
            {!modelReady && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 px-6 text-center">
                <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100 text-3xl">
                  3D
                </div>
                <p className="font-semibold text-zinc-800">
                  {modelError
                    ? "No se pudo cargar el visor 3D"
                    : "Preparando modelo tridimensional..."}
                </p>
                <p className="max-w-xs text-sm text-zinc-500">
                  {modelError
                    ? "Puedes abrir el archivo del modelo directamente."
                    : "El modelo puede tardar unos segundos en cargar."}
                </p>
                {modelError && (
                  <a className="text-sm font-semibold text-emerald-700 underline" href={MODEL_URL}>
                    Abrir modelo 3D
                  </a>
                )}
              </div>
            )}
          </div>
          <p className="px-3 py-3 text-center text-xs text-zinc-500">
            Arrastra para girar · Usa la rueda o pellizca para acercar y alejar
          </p>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          ["Monitoreo biométrico", "Sensores orientados al seguimiento de variables fisiológicas."],
          ["Actividad y descanso", "Registro de movimiento y patrones de actividad."],
          ["Conectividad", "Diseñado para integrarse con la plataforma PawCare."],
          ["Seguimiento centralizado", "Información disponible para apoyar el cuidado veterinario."],
        ].map(([title, description]) => (
          <Card key={title} className="p-5">
            <div className="mb-3 h-2 w-10 rounded-full bg-emerald-500" />
            <h2 className="font-semibold text-zinc-900">{title}</h2>
            <p className="mt-2 text-sm leading-6 text-zinc-600">{description}</p>
          </Card>
        ))}
      </section>

      <section id="simulador-venta" className="scroll-mt-8">
        <div className="mb-5">
          <p className="text-sm font-semibold uppercase tracking-wider text-emerald-700">
            E-commerce · Demostración
          </p>
          <h2 className="mt-1 text-2xl font-bold text-zinc-950 sm:text-3xl">
            Simulador de venta
          </h2>
          <p className="mt-2 text-zinc-600">
            Completa los datos y confirma un pedido ficticio. No se realiza
            ningún cobro ni se envía un pedido real.
          </p>
        </div>

        <div className="grid items-start gap-6 lg:grid-cols-[1fr_0.85fr]">
          <Card>
            <CardHeader
              title="Datos del comprador"
              description="Información utilizada únicamente para generar el comprobante de demostración."
            />
            <form onSubmit={confirmDemoSale} className="mt-6 grid gap-4 sm:grid-cols-2">
              <label className="grid gap-1.5 text-sm font-medium text-zinc-700 sm:col-span-2">
                Nombre completo
                <input
                  required
                  minLength={3}
                  value={buyer.name}
                  onChange={(e) => updateBuyer("name", e.target.value)}
                  placeholder="Ej. María Rodríguez"
                  className="rounded-lg border border-zinc-300 px-3 py-2.5 font-normal outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                />
              </label>
              <label className="grid gap-1.5 text-sm font-medium text-zinc-700">
                Correo electrónico
                <input
                  required
                  type="email"
                  value={buyer.email}
                  onChange={(e) => updateBuyer("email", e.target.value)}
                  placeholder="correo@ejemplo.com"
                  className="rounded-lg border border-zinc-300 px-3 py-2.5 font-normal outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                />
              </label>
              <label className="grid gap-1.5 text-sm font-medium text-zinc-700">
                Teléfono
                <input
                  required
                  type="tel"
                  value={buyer.phone}
                  onChange={(e) => updateBuyer("phone", e.target.value)}
                  placeholder="300 000 0000"
                  className="rounded-lg border border-zinc-300 px-3 py-2.5 font-normal outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                />
              </label>
              <label className="grid gap-1.5 text-sm font-medium text-zinc-700">
                Ciudad
                <input
                  required
                  value={buyer.city}
                  onChange={(e) => updateBuyer("city", e.target.value)}
                  placeholder="Bogotá"
                  className="rounded-lg border border-zinc-300 px-3 py-2.5 font-normal outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                />
              </label>
              <label className="grid gap-1.5 text-sm font-medium text-zinc-700">
                Dirección de entrega (simulada)
                <input
                  required
                  value={buyer.address}
                  onChange={(e) => updateBuyer("address", e.target.value)}
                  placeholder="Dirección de ejemplo"
                  className="rounded-lg border border-zinc-300 px-3 py-2.5 font-normal outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                />
              </label>

              <div className="sm:col-span-2">
                <label className="mb-2 block text-sm font-medium text-zinc-700">
                  Cantidad de collares
                </label>
                <div className="flex w-fit items-center gap-4 rounded-lg border border-zinc-300 p-1.5">
                  <button
                    type="button"
                    onClick={() => setQuantity((n) => Math.max(1, n - 1))}
                    aria-label="Disminuir cantidad"
                    className="h-9 w-9 rounded-md bg-zinc-100 text-lg font-semibold hover:bg-zinc-200"
                  >
                    −
                  </button>
                  <span className="min-w-6 text-center font-semibold tabular-nums">{quantity}</span>
                  <button
                    type="button"
                    onClick={() => setQuantity((n) => Math.min(10, n + 1))}
                    aria-label="Aumentar cantidad"
                    className="h-9 w-9 rounded-md bg-zinc-100 text-lg font-semibold hover:bg-zinc-200"
                  >
                    +
                  </button>
                  <span className="pr-2 text-xs text-zinc-500">Máximo 10 unidades</span>
                </div>
              </div>

              <div className="sm:col-span-2 rounded-xl bg-amber-50 p-4 text-sm text-amber-900">
                <strong>Modo demostración:</strong> el botón genera un comprobante
                ficticio en este navegador. No se procesan pagos, no se contacta
                una transportadora y no se crea una orden comercial real.
              </div>
              <Button type="submit" size="lg" className="sm:col-span-2">
                Confirmar venta simulada · {money(total)}
              </Button>
            </form>
          </Card>

          <div className="grid gap-6">
            <Card>
              <CardHeader title="Resumen del pedido" description="Valores de demostración en COP." />
              <div className="mt-5 flex items-center justify-between gap-4 border-b border-zinc-100 pb-4">
                <div>
                  <p className="font-semibold text-zinc-900">Collar inteligente PawCare</p>
                  <p className="mt-1 text-sm text-zinc-500">{money(PRICE)} por unidad</p>
                </div>
                <span className="rounded-lg bg-emerald-50 px-3 py-1.5 text-sm font-semibold text-emerald-800">
                  × {quantity}
                </span>
              </div>
              <div className="mt-4 flex justify-between text-sm text-zinc-600">
                <span>Subtotal</span><span>{money(total)}</span>
              </div>
              <div className="mt-3 flex justify-between text-sm text-zinc-600">
                <span>Envío</span><span>Sin cobro (simulado)</span>
              </div>
              <div className="mt-4 flex justify-between border-t border-zinc-200 pt-4 text-lg font-bold">
                <span>Total</span><span className="text-emerald-700">{money(total)}</span>
              </div>
            </Card>

            {completedSale && (
              <Card className="border-emerald-300 bg-emerald-50">
                <div className="flex items-center gap-2 text-emerald-800">
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-100">✓</span>
                  <h3 className="font-bold">Venta simulada confirmada</h3>
                </div>
                <p className="mt-3 text-sm text-emerald-900">
                  Se generó el comprobante ficticio para {completedSale.buyer}.
                </p>
                <div className="mt-3 rounded-lg bg-white p-3 text-sm">
                  <p><strong>Referencia:</strong> {completedSale.id}</p>
                  <p><strong>Unidades:</strong> {completedSale.quantity}</p>
                  <p><strong>Total:</strong> {money(completedSale.total)}</p>
                  <p><strong>Fecha:</strong> {new Date(completedSale.date).toLocaleString("es-CO")}</p>
                </div>
                <p className="mt-3 text-xs text-emerald-800">
                  Este comprobante no es una factura ni acredita un pago real.
                </p>
              </Card>
            )}
          </div>
        </div>
      </section>

      <section>
        <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-2xl font-bold text-zinc-950">Historial de ventas de demostración</h2>
            <p className="mt-1 text-sm text-zinc-600">
              Registros locales de este navegador: {sales.length}
            </p>
          </div>
          {sales.length > 0 && (
            <Button type="button" variant="secondary" size="sm" onClick={clearDemoSales}>
              Limpiar historial
            </Button>
          )}
        </div>
        {sales.length === 0 ? (
          <div className="rounded-xl border border-dashed border-zinc-300 bg-white p-8 text-center text-sm text-zinc-500">
            Aún no hay ventas simuladas. Completa el formulario para generar la primera.
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-zinc-200 bg-white">
            <table className="w-full min-w-[620px] text-left text-sm">
              <thead className="bg-zinc-50 text-xs uppercase tracking-wide text-zinc-500">
                <tr>
                  <th className="px-4 py-3">Referencia</th>
                  <th className="px-4 py-3">Comprador</th>
                  <th className="px-4 py-3">Unidades</th>
                  <th className="px-4 py-3">Total</th>
                  <th className="px-4 py-3">Fecha</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {sales.map((sale) => (
                  <tr key={sale.id}>
                    <td className="px-4 py-3 font-medium text-emerald-700">{sale.id}</td>
                    <td className="px-4 py-3">{sale.buyer}</td>
                    <td className="px-4 py-3">{sale.quantity}</td>
                    <td className="px-4 py-3 font-semibold">{money(sale.total)}</td>
                    <td className="px-4 py-3 text-zinc-500">
                      {new Date(sale.date).toLocaleDateString("es-CO")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
