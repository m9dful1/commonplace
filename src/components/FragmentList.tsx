import Link from "next/link";
import type { Fragment } from "@/lib/fragments";
import { formatTimestamp } from "@/lib/format";

export function FragmentList({ fragments }: { fragments: Fragment[] }) {
  if (fragments.length === 0) {
    return (
      <p className="font-serif italic text-muted">
        Nothing yet. The first fragment goes above.
      </p>
    );
  }

  return (
    <ol className="space-y-10">
      {fragments.map((f) => (
        <li key={f.id} className="font-serif">
          <Link
            href={`/fragments/${f.id}`}
            className="group block no-underline"
          >
            <p className="whitespace-pre-wrap text-lg leading-relaxed text-ink group-hover:text-accent">
              {f.body}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 font-mono text-xs uppercase tracking-widest text-muted">
              <time dateTime={f.createdAt}>{formatTimestamp(f.createdAt)}</time>
              {f.source && (
                <>
                  <span aria-hidden>·</span>
                  <span className="normal-case tracking-normal">{f.source}</span>
                </>
              )}
              {f.tags.length > 0 && (
                <>
                  <span aria-hidden>·</span>
                  <span>
                    {f.tags.map((t, i) => (
                      <span key={t}>
                        #{t}
                        {i < f.tags.length - 1 ? " " : ""}
                      </span>
                    ))}
                  </span>
                </>
              )}
            </div>
          </Link>
        </li>
      ))}
    </ol>
  );
}
