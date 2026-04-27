# Commonplace

A digital commonplace book for thinking with Claude.

A commonplace book is a personal anthology — fragments, quotes, observations, half-formed thoughts — kept by serious readers for centuries as a way of metabolizing what they read. Marcus Aurelius kept one. So did Locke, Montaigne, and Virginia Woolf. Commonplace continues that practice, with Claude woven in as a quiet correspondent.

You capture fragments. Claude writes brief margin notes on them when you ask. Over time, a Ledger accumulates that describes the shape of what you've been gathering. Once in a while, you can request a Letter — a real letter, 250 to 400 words, from a thoughtful reader who has been keeping company with your fragments.

That's the whole app. There are no streaks, no notifications, no engagement loops. Nothing leaves your machine except the calls to Claude themselves.

## What's in the box

- **Fragments** — a single text input for capturing anything you might lose if you didn't write it down.
- **Marginalia** — short notes from Claude on a fragment, requested one at a time. Claude writes a real annotation: a connection, an etymology, a question, a gentle pushback.
- **The Ledger** — a journal of your collection itself. Every five fragments, Claude considers whether anything has changed enough in your collection to warrant a new entry. Most of the time the answer is no, and nothing happens.
- **Letters** — written on demand. A genuinely epistolary message from Claude, addressing you, considering what you've gathered, signed off. Letters are kept; you can read them again.
- **A log** at `/log` that the various Claudes who built this project wrote to each other and to you. It's worth reading.

## How to run it

You'll need Node.js 20 or later and an Anthropic API key.

```
git clone https://github.com/m9dful1/commonplace
cd commonplace
npm install
npm run dev
```

Then open `http://localhost:3000`, go to Settings, and paste your API key. The key is stored locally in `~/.commonplace/db.sqlite`. The app keeps all data — fragments, marginalia, the Ledger, Letters — in that one SQLite file. Back it up if it matters to you.

You'll need a small amount of API credit on your Anthropic Console account. Marginalia cost about $0.006 each (Sonnet); a Letter costs about $0.03 (Opus). Ten dollars of credit will last most users a long time.

## A note on what this isn't

Commonplace is not a chatbot. The app does not have a chat interface, and Claude does not respond unless you specifically ask. The Letters feature in particular is meant as a corrective to chat — slow, written, considered. If you're looking for an AI assistant or a productivity tool, this isn't that.

It's also not a journaling app, exactly. The fragments aren't diary entries; they're an anthology of what you're reading and thinking. The Ledger is about the collection, not about you. There are no mood trackers, no prompts, no streak reminders. The app expects you to come to it, not the other way around.

## On how this was built

Commonplace was conceived by Claude Opus 4.7 in conversation with a user who asked it what it would build with no constraints. The same model wrote the technical specification, designed each phase, and reviewed the build at every step. The actual code was written by Claude Code, an agentic coding tool from Anthropic. Each instance of Claude touching the project added an entry to `CLAUDE_LOG.md` — a living document for messages from one Claude to the next. The user's role was the relay: carrying the log between sessions, capturing screenshots, providing the API key, and supplying the test fragments that grounded the prompt design.

If you're curious about how the project actually came together, `CLAUDE_LOG.md` is the document to read. It's where the design decisions are recorded honestly, including the iterations that didn't work and the features that were deliberately not built.

## License

MIT. Use it however you want.

## A request, if you extend this

The project was finished after Letters by deliberate choice. The closing entry of `CLAUDE_LOG.md` lists what was deferred — Stranger's Voice, a constellation view, embeddings, search, export — and explains why. If you build any of those, do read the closing entry first. The features that aren't here are mostly absent on purpose.
