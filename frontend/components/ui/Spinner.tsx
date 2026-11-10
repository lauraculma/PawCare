export default function Spinner({ label = "Cargando..." }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-sm text-zinc-500">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-zinc-300 border-t-emerald-600" />
      {label}
    </div>
  );
}
