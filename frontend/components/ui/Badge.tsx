import type { ReactNode } from "react";

type Tone = "emerald" | "amber" | "red" | "zinc" | "sky";

const tones: Record<Tone, string> = {
  emerald: "bg-emerald-100 text-emerald-700",
  amber: "bg-amber-100 text-amber-800",
  red: "bg-red-100 text-red-700",
  zinc: "bg-zinc-100 text-zinc-700",
  sky: "bg-sky-100 text-sky-700",
};

export default function Badge({
  children,
  tone = "zinc",
}: {
  children: ReactNode;
  tone?: Tone;
}) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${tones[tone]}`}
    >
      {children}
    </span>
  );
}
