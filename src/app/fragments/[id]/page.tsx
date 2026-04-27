import Link from "next/link";
import { notFound } from "next/navigation";
import { MarginaliaList } from "@/components/MarginaliaBlock";
import { MarginaliaRequester } from "@/components/MarginaliaRequester";
import { formatTimestamp } from "@/lib/format";
import { getFragment } from "@/lib/fragments";
import { listMarginaliaForFragment } from "@/lib/marginalia";
import { getAnthropicKey } from "@/lib/settings";

export const dynamic = "force-dynamic";

export default function FragmentPage({ params }: { params: { id: string } }) {
  const fragment = getFragment(params.id);
  if (!fragment) notFound();

  const marginalia = listMarginaliaForFragment(fragment.id);
  const hasApiKey = Boolean(getAnthropicKey());

  return (
    <article className="space-y-10">
      <div>
        <Link
          href="/"
          className="font-mono text-xs uppercase tracking-widest text-muted hover:text-ink"
        >
          ← all fragments
        </Link>
      </div>

      <div className="space-y-3">
        <p className="whitespace-pre-wrap font-serif text-xl leading-relaxed text-ink">
          {fragment.body}
        </p>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 font-mono text-xs uppercase tracking-widest text-muted">
          <time dateTime={fragment.createdAt}>
            {formatTimestamp(fragment.createdAt)}
          </time>
          {fragment.source && (
            <>
              <span aria-hidden>·</span>
              <span className="normal-case tracking-normal">{fragment.source}</span>
            </>
          )}
          {fragment.tags.length > 0 && (
            <>
              <span aria-hidden>·</span>
              <span>
                {fragment.tags.map((t, i) => (
                  <span key={t}>
                    #{t}
                    {i < fragment.tags.length - 1 ? " " : ""}
                  </span>
                ))}
              </span>
            </>
          )}
        </div>
      </div>

      <div className="space-y-6 border-t border-rule pt-8">
        <MarginaliaList items={marginalia} />
        <MarginaliaRequester
          fragmentId={fragment.id}
          hasExisting={marginalia.length > 0}
          hasApiKey={hasApiKey}
        />
      </div>
    </article>
  );
}
