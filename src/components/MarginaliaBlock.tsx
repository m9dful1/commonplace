import { renderLightMarkdown } from "@/lib/lightMarkdown";
import type { Marginalia } from "@/lib/marginalia";

export function MarginaliaBlock({ body }: { body: string }) {
  return (
    <p className="ml-6 border-l-2 border-accent/30 pl-4 font-serif text-base italic leading-relaxed text-accent">
      {renderLightMarkdown(body)}
    </p>
  );
}

export function MarginaliaList({ items }: { items: Marginalia[] }) {
  if (items.length === 0) return null;
  return (
    <div className="space-y-4">
      {items.map((m) => (
        <MarginaliaBlock key={m.id} body={m.body} />
      ))}
    </div>
  );
}
