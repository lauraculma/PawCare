type PagePlaceholderProps = {
  title: string;
  description: string;
};

export default function PagePlaceholder({ title, description }: PagePlaceholderProps) {
  return (
    <div className="mx-auto max-w-4xl px-6 py-16">
      <h1 className="text-3xl font-bold tracking-tight text-zinc-900">{title}</h1>
      <p className="mt-3 max-w-2xl text-zinc-600">{description}</p>
      <div className="mt-8 rounded-xl border border-dashed border-zinc-300 bg-white p-10 text-center text-sm text-zinc-500">
        Sección preparada. La funcionalidad se implementará en una etapa posterior.
      </div>
    </div>
  );
}
