import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "Commonplace",
  description: "A digital commonplace book for thinking with Claude.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="bg-paper text-ink">
        <div className="mx-auto max-w-page px-6 py-12">
          <header className="mb-12 flex items-baseline justify-between border-b border-rule pb-4">
            <Link
              href="/"
              className="font-serif text-2xl tracking-tight text-ink no-underline"
            >
              Commonplace
            </Link>
            <nav className="flex gap-6 font-mono text-xs uppercase tracking-widest text-muted">
              <Link href="/" className="hover:text-ink">
                fragments
              </Link>
              <Link href="/log" className="hover:text-ink">
                log
              </Link>
              <Link href="/ledger" className="hover:text-ink">
                ledger
              </Link>
              <Link href="/letters" className="hover:text-ink">
                letters
              </Link>
              <Link href="/settings" className="hover:text-ink">
                settings
              </Link>
            </nav>
          </header>
          <main>{children}</main>
        </div>
      </body>
    </html>
  );
}
