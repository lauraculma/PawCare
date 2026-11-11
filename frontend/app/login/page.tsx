"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useSession } from "@/components/providers/SessionProvider";
import Alert from "@/components/ui/Alert";
import { buttonClasses } from "@/components/ui/Button";
import { Field, Input, Select } from "@/components/ui/Field";
import Spinner from "@/components/ui/Spinner";
import { getErrorMessage } from "@/lib/api";
import type { LoginPayload, RegisterPayload } from "@/lib/types";
import { useForm } from "react-hook-form";

type Mode = "login" | "register";

export default function AuthPage() {
  const router = useRouter();
  const [mode, setMode] = useState<Mode>("login");
  const [next] = useState<string>("/dashboard");
  const { user, login, register: registerUser, loading } = useSession();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm();

  useEffect(() => {
    if (user) {
      router.push(next);
      router.refresh();
    }
  }, [user, next, router]);

  useEffect(() => {
    Promise.resolve().then(() => {
      reset();
      setSubmitError(null);
    });
  }, [mode, reset]);

  async function onSubmit(data: Record<string, string>) {
    setSubmitError(null);
    setBusy(true);
    try {
      if (mode === "login") {
        const payload: LoginPayload = { email: data.email, password: data.password };
        await login(payload);
      } else {
        const role = (data.role as "OWNER" | "VETERINARIAN" | "ADMIN") ?? "OWNER";
        if (role === "ADMIN") {
          setSubmitError("El registro publico no permite crear usuarios ADMIN");
          setBusy(false);
          return;
        }
        const payload: RegisterPayload = {
          fullName: data.fullName,
          email: data.email,
          password: data.password,
          phone: data.phone?.trim() ? data.phone : undefined,
          role,
        };
        await registerUser(payload);
      }
      router.push(next);
      router.refresh();
    } catch (error) {
      setSubmitError(getErrorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return <Spinner label="Verificando sesion..." />;
  }

  return (
    <div className="mx-auto grid max-w-6xl gap-8 px-6 py-12 lg:grid-cols-2 lg:items-center">
      <div className="hidden lg:block">
        <h1 className="text-4xl font-bold tracking-tight text-zinc-900">Bienvenido a PawCare</h1>
        <p className="mt-4 max-w-xl text-lg text-zinc-600">
          Gestiona mascotas, citas, historias clinicas y alertas del collar inteligente en una sola
          plataforma segura.
        </p>
        <div className="mt-8 grid grid-cols-2 gap-3">
          <div className="rounded-xl border border-zinc-200 bg-white p-4">
            <p className="text-sm font-semibold text-zinc-900">Roles y permisos</p>
            <p className="mt-1 text-xs text-zinc-600">Propietario, veterinario y administrador.</p>
          </div>
          <div className="rounded-xl border border-zinc-200 bg-white p-4">
            <p className="text-sm font-semibold text-zinc-900">API REST + JWT</p>
            <p className="mt-1 text-xs text-zinc-600">Spring Boot 4 con autenticacion segura.</p>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm">
        <div className="mb-6 flex gap-2">
          <button
            type="button"
            onClick={() => setMode("login")}
            className={buttonClasses(mode === "login" ? "primary" : "secondary", "sm")}
          >
            Iniciar sesion
          </button>
          <button
            type="button"
            onClick={() => setMode("register")}
            className={buttonClasses(mode === "register" ? "primary" : "secondary", "sm")}
          >
            Crear cuenta
          </button>
        </div>

        <h2 className="text-xl font-semibold text-zinc-900">
          {mode === "login" ? "Accede a tu cuenta" : "Crea tu cuenta"}
        </h2>
        <p className="mt-1 text-sm text-zinc-600">
          {mode === "login"
            ? "Usa tu correo y contraseña para entrar."
            : "Registrate como propietario o veterinario."}
        </p>

        {submitError ? <div className="mt-4"><Alert variant="error">{submitError}</Alert></div> : null}

        <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
          {mode === "register" ? (
            <>
              <Field label="Nombre completo" required>
                <Input placeholder="Laura Gomez" {...register("fullName", { required: "Requerido" })} />
              </Field>
              {errors.fullName && <p className="text-xs text-red-600">{String(errors.fullName.message)}</p>}
            </>
          ) : null}

          <Field label="Correo electronico" required>
            <Input type="email" placeholder="correo@ejemplo.com" {...register("email", { required: "Requerido" })} />
          </Field>
          {errors.email && <p className="text-xs text-red-600">{String(errors.email.message)}</p>}

          <Field label="Contraseña" required>
            <Input type="password" placeholder="••••••••" {...register("password", { required: "Requerido", minLength: { value: 6, message: "Minimo 6 caracteres" } })} />
          </Field>
          {errors.password && <p className="text-xs text-red-600">{String(errors.password.message)}</p>}

          {mode === "register" ? (
            <>
              <Field label="Telefono" hint="Opcional">
                <Input placeholder="3001234567" {...register("phone")} />
              </Field>

              <Field label="Rol" required>
                <Select {...register("role", { required: "Requerido" })} defaultValue="OWNER">
                  <option value="OWNER">Propietario</option>
                  <option value="VETERINARIAN">Veterinario</option>
                </Select>
              </Field>
              {errors.role && <p className="text-xs text-red-600">{String(errors.role.message)}</p>}
            </>
          ) : null}

          <button
            type="submit"
            disabled={busy}
            className={buttonClasses("primary", "md", "w-full")}
          >
            {busy ? "Procesando..." : mode === "login" ? "Entrar" : "Crear cuenta"}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-zinc-600">
          {mode === "login" ? (
            <>
              ¿No tienes cuenta?{" "}
              <Link href="/login?mode=register" className="font-semibold text-emerald-700 hover:underline">
                Registrate
              </Link>
            </>
          ) : (
            <>
              ¿Ya tienes cuenta?{" "}
              <Link href="/login?mode=login" className="font-semibold text-emerald-700 hover:underline">
                Inicia sesion
              </Link>
            </>
          )}
        </p>
      </div>
    </div>
  );
}
