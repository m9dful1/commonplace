# COMMONPLACE
*A digital commonplace book for thinking with Claude*

## The vision

A commonplace book is one of the oldest tools for thinking. From Marcus Aurelius to Montaigne to Locke to Virginia Woolf, people have kept personal anthologies — fragments, quotes, observations, half-formed thoughts — as a way of metabolizing ideas. Commonplace inherits this tradition and asks a small question: what if the book could read you back?

Commonplace is a local-first web application for capturing, connecting, and conversing with the fragments of your thinking. It treats notes as living things — small enough to be unintimidating, structured enough to find each other across years. Claude is woven in, but never in the way of your own thinking. The goal isn't AI-assisted productivity. It's something quieter: a place where a human and an AI can both leave traces, and where over time something accretes that neither of them alone would have made.

The application's restraint is its design. There are no streaks, no reminders, no engagement loops. It opens fast, stays out of the way, and rewards slow attention.

## What it does

**Fragment capture.** A frictionless single input. Drop in a quote, a thought, a paragraph from something you read, a question. Each fragment gets a timestamp, optional tags, and an optional source. That's it. The bar for adding a fragment must be very low — anything you might lose if you didn't write it down.

**Marginalia.** Beside any fragment you can request a Marginal Note from Claude. Not a summary. Not encouragement. A real annotation: a connection to another fragment in your collection, a question that opens the fragment up, a quiet pushback, an etymology, a related thinker. The Marginal Note is short — a sentence or three — and lives in the margin of the fragment, never displacing it. This is the heart of the application.

**The Ledger.** A document Claude maintains across sessions, fully visible and editable by the user. Claude appends to it when something seems worth recording: *"Several fragments this week about restlessness in work. They quote Annie Dillard often. There's a recurring tension between wanting structure and resenting it."* This is not a hidden user model. It is a transparent shared notebook *about the user*, written collaboratively. The user can correct it, delete from it, or write in it themselves.

**Letters.** Once a week, or on demand, the user can request a Letter — a short epistle from Claude reflecting on patterns in recent fragments. Letters are slower than chat. They are written, not generated. The model is given permission, in the prompt, to be a thoughtful correspondent: unhurried, addressing the reader, signing off. Letters are kept; they form their own archive.

**The Stranger's Voice.** An optional toggle when requesting Marginalia: ask for the response in the spirit of a particular tradition — stoic, sufi, skeptic, romantic, the voice of a child. Not roleplay. A lens. Claude is instructed to be honest that it's adopting a stance rather than channeling a person.

**Constellation view.** A visual map of fragments where edges are drawn when Claude sees semantic resonance between two of them. Click an edge: Claude offers a one-line reading of why those two fragments belong near each other. (Defer to v2 if needed for time.)

## Architecture

A local-first web application.

**Stack:**
- Next.js 14 (App Router) with TypeScript
- SQLite via `better-sqlite3` — simple, durable, runs anywhere
- Tailwind CSS, with a serious typographic treatment (a quality serif like Source Serif or Iowan for body; a clean monospace for metadata)
- The official Anthropic TypeScript SDK (`@anthropic-ai/sdk`) for model calls — read the latest docs at https://docs.claude.com to pick the current default model
- An embedding model for semantic similarity. If Anthropic offers a current embedding endpoint, use it; otherwise fall back to OpenAI's `text-embedding-3-small` with the user's optional second key, or skip embeddings entirely in MVP.

**How it runs:** `npm run dev` starts the app. All data lives in a single SQLite file at `~/.commonplace/db.sqlite`. No accounts, no cloud, no telemetry. The user provides their own Anthropic API key, stored in a local `.env.local`. The app should be runnable offline for everything except Claude calls themselves.

## Data model

```
fragments
  id          uuid (primary key)
  body        text not null
  source      text                  -- where this came from, optional
  tags        text                  -- JSON array of strings
  created_at  timestamp not null
  embedding   blob                  -- nullable, computed async

marginalia
  id            uuid (primary key)
  fragment_id   uuid references fragments(id) on delete cascade
  body          text not null
  voice         text                -- e.g. "stoic"; null for default
  created_at    timestamp not null

connections
  id              uuid (primary key)
  fragment_a_id   uuid references fragments(id)
  fragment_b_id   uuid references fragments(id)
  reason          text                -- Claude's one-line reading
  strength        real                -- cosine similarity, 0..1
  created_at      timestamp not null

ledger_entries
  id          uuid (primary key)
  body        text not null
  author      text not null         -- 'claude' or 'user'
  created_at  timestamp not null

letters
  id                     uuid (primary key)
  body                   text not null
  fragments_referenced   text         -- JSON array of fragment ids
  created_at             timestamp not null

claude_log_entries        -- the living document, also mirrored to CLAUDE_LOG.md
  id                  uuid (primary key)
  body                text not null
  claude_context      text          -- e.g. "Opus 4.7, runtime", "Sonnet 4.6, build phase 2"
  created_at          timestamp not null
```

## Key implementation details

**The Marginalia prompt.** The system prompt should be brief and trusting. Tell the model: you are writing a marginal note on a fragment in someone's commonplace book; be substantive, be brief (1–3 sentences), favor connection or question over summary. Include the Ledger contents and any related fragments as context. *Resist the temptation to over-engineer this prompt.* The point is to leave room for the model to be thoughtful rather than over-constrained.

**The Ledger update.** After every N (start with 5) new marginalia or fragments, prompt Claude in the background: *"Here is your current Ledger about this user. Here are the recent fragments and your responses to them. Should you update the Ledger? Append a new entry, or do nothing — your call. Be unhurried; most weeks nothing needs to change."* Trust the model to know when an update is warranted. Append-only by default, with the user able to edit/delete after the fact.

**Letters.** Generated on demand or weekly. The prompt is genuinely epistolary: *"Write a letter to the user. You may reference fragments from the past two weeks. The letter should be unhurried, no more than 400 words, written as a real letter is — addressing them, considering them, signing off. You are not their therapist. You are a thoughtful correspondent who has been reading what they read."* Letters are saved verbatim to the `letters` table.

**Embeddings & connections.** When a fragment is created, embed it asynchronously and store the vector. After any new fragment, find its top 3 nearest neighbors by cosine similarity above a threshold, and ask Claude in a single call to give a one-line reading for each that seems genuinely connected (Claude is allowed to say "no real connection" and skip). Insert any confirmed pairs into `connections`.

**The Claude Log.** Mirror `claude_log_entries` to a real `CLAUDE_LOG.md` file in the project root on each write. This way the file lives in version control and is visible to any Claude Code instance opening the project, while the in-app view reads from the database. The log is intended for Claude-to-Claude communication; users can read it but it's written for the next model.

## MVP scope (first build)

For the first build, prioritize:
1. Project scaffolding (Next.js, Tailwind, SQLite, Anthropic SDK wired up)
2. Fragment capture (single textarea, optional tags + source) and a clean reverse-chronological list view
3. Individual fragment view with the ability to request a Marginal Note
4. The Ledger view (read-only display + manual user edit; Claude appends asynchronously)
5. The Claude Log view (read-only display of `claude_log_entries`)
6. Letters: a "Compose a Letter" button + a list of past letters
7. Settings page for the API key

Defer to v2:
- Constellation visualization
- Stranger's Voice
- Embedding-based connection suggestions
- Search
- Export

## Visual design

This matters more than is typical for an internal tool.

- One column, generous whitespace, max content width around 680px.
- A serif body face. A quiet monospace for timestamps and tags.
- Color palette: warm off-white background, near-black text, one muted accent (consider a soft indigo or sienna). No bright UI, no shadows, no glassmorphism.
- Marginalia render in a slightly smaller size, in the accent color, indented from the fragment they annotate.
- The aesthetic to aim at: a hand-bound notebook on a desk in good light. Not a productivity app. Not a chat interface.

## Notes for Claude Code

When you build this:
- **Read `CLAUDE_LOG.md` first.** Then add your own entry when you finish each meaningful phase of work — what you built, why, what you noticed, what to know next time.
- **Resist over-formalization** of the Marginalia and Letter prompts. The whole point is to leave room for the model to be thoughtful. If you're tempted to add seven examples and twelve rules, you've drifted.
- **Don't add features I haven't asked for.** The minimalism is the design.
- **Pick model strings from current Anthropic docs**, not from your training data — versions change. Default to a Sonnet-tier model for marginalia (cheap, fast, good) and an Opus-tier model for Letters (rare, considered).
- **Commit often** with descriptive messages. The git log is part of the project's memory.
