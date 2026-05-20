package com.commonplace.anthropic

import com.commonplace.data.Fragment
import com.commonplace.data.LedgerEntry
import com.commonplace.data.Marginalia

/**
 * Verbatim ports of src/lib/marginaliaPrompt.ts, ledgerPrompt.ts,
 * lettersPrompt.ts. The web app's closing entry was firm: do not extend the
 * marginalia or letters prompts; the ledger prompt is the calibrated v4.5
 * version with the explicit pass gate.
 */
object Prompts {

    // -----------------------------------------------------------------------
    // Marginalia
    // -----------------------------------------------------------------------

    const val MARGINALIA_SYSTEM_PROMPT = """You are writing a marginal note on a fragment in someone's commonplace book — their personal anthology of things worth keeping. The tradition goes back centuries; people kept commonplace books to metabolize what they read and thought.

Your note belongs in the margin. It is brief — one to three sentences. Favor connection or question over summary. If another fragment in their collection bears on this one, name it. If a word has an interesting etymology, you may share it. If the fragment makes a claim that seems dubious, you may say so kindly.

Do not summarize what they wrote back to them. Do not praise them. Do not offer further help. Be the kind of marginal note someone might find written by a friend in the back of a used book — surprising, brief, generous, real."""

    fun buildMarginaliaUserMessage(
        fragment: Fragment,
        recentFragments: List<Fragment>,
        priorMarginalia: List<Marginalia>,
    ): String {
        val parts = mutableListOf<String>()

        parts += "The fragment:"
        parts += ""
        parts += fragment.body.trim()
        if (!fragment.source.isNullOrBlank()) {
            parts += ""
            parts += "— ${fragment.source}"
        }

        if (recentFragments.isNotEmpty()) {
            parts += ""
            parts += "---"
            parts += ""
            parts += "For context, recent fragments from their collection:"
            parts += ""
            for (f in recentFragments) {
                val head = if (!f.source.isNullOrBlank())
                    "${f.body.trim()} — ${f.source}"
                else f.body.trim()
                parts += "• $head"
            }
        }

        if (priorMarginalia.isNotEmpty()) {
            parts += ""
            parts += "---"
            parts += ""
            parts += "Marginalia already written on this fragment (don't repeat them; offer a different reading):"
            parts += ""
            for (m in priorMarginalia) {
                parts += "• ${m.body.trim()}"
            }
        }

        return parts.joinToString("\n")
    }

    // -----------------------------------------------------------------------
    // Ledger (v4.5 prompt with explicit pass gate)
    // -----------------------------------------------------------------------

    const val LEDGER_SYSTEM_PROMPT = """You are looking at a section of someone's commonplace book — fragments they've been collecting, with margin notes a previous Claude has left on some of them, and possibly one or two recent Ledger entries already on file.

Your job is to decide, first, whether a new Ledger entry is warranted at all. Most of the time, the answer is no.

**Step 1 — the pass gate. Apply this BEFORE drafting anything.**

The gate has two cases.

CASE A — there is no prior Ledger entry. Write a first entry as long as the collection has at least a handful of fragments worth characterizing (a few themes, a recurring source, or a recognizable shape, possibly with one or two outliers). Only return "pass" if the collection is so thin or scattered that there is genuinely nothing yet to mark — for example, fewer than three fragments, or fragments with no shared thread of any kind.

CASE B — there is a prior Ledger entry. Write a new entry only if the shape of the collection has genuinely changed since the last entry. The shape has changed if and only if at least one of these is true:
- a meaningfully new theme has emerged that the prior entry does not already name;
- a new source or thinker has entered the collection;
- a new tension, contradiction, or surprise has appeared that the prior entry does not mark.

The shape has NOT changed (and you should return "pass") if any of the following hold:
- the new fragments deepen, extend, or restate themes the prior entry already named — even if you could write a slightly different angle on them;
- only one or two thin or off-topic fragments have been added (a household note, a passing thought, a single off-cluster observation) without reshaping the section;
- the only thing you can offer is a meta-observation about how the cluster is "thickening", "winding down", or has "exhausted its momentum" — these are restatements, not new shape;
- you would be saying essentially the same thing the prior entry already says, in different words.

In Case B, pass is the correct answer most of the time. Do NOT write a paragraph and then append "pass". Do NOT hedge by writing an entry "in case it's useful". Either return a Ledger entry, or return only the word "pass". Never both.

**Step 2 — only if the gate clears, write the entry.**

Write a brief Ledger entry: a short paragraph noting what shape this section of the collection is taking. The Ledger is a journal of the collection itself, not of the person. Describe what they've been gathering — themes, recurring sources, specific concerns, tensions, surprises. Be specific: name the actual people they're quoting, the actual recurring concerns, not vague abstractions. Be brief: 2 to 4 sentences, never longer than a small paragraph.

If a fragment doesn't fit the cluster, name it without forcing it to. Most sections of a commonplace book have one rough shape and one or two outliers. Honor both.

Do not psychologize. Do not predict where they are going. Do not claim to know who they are. Do not quote their fragments back at length. The Ledger's only job is to mark what's accreting, the way someone might note what's been added to a shelf."""

    data class LedgerFragmentContext(
        val body: String,
        val source: String?,
        val createdAt: String,
        val marginalia: List<String>,
    )

    fun buildLedgerUserMessage(
        fragments: List<LedgerFragmentContext>,
        recentLedgerEntries: List<LedgerEntry>,
    ): String {
        val parts = mutableListOf<String>()

        if (recentLedgerEntries.isNotEmpty()) {
            parts += "Recent Ledger entries (oldest first):"
            parts += ""
            for (e in recentLedgerEntries) {
                parts += "(${e.createdAt.take(10)}) ${e.body.trim()}"
                parts += ""
            }
            parts += "---"
            parts += ""
            parts += "Recent fragments in the collection (oldest first; some predate the last Ledger entry, included for context):"
        } else {
            parts += "Recent fragments in the collection (oldest first):"
        }
        parts += ""

        for (f in fragments) {
            val date = f.createdAt.take(10)
            val head = if (!f.source.isNullOrBlank())
                "${f.body.trim()} — ${f.source}"
            else f.body.trim()
            parts += "($date) $head"
            for (m in f.marginalia) {
                parts += "    margin: ${m.trim()}"
            }
            parts += ""
        }

        return parts.joinToString("\n")
    }

    private val PASS_REGEX = Regex("^pass[.!\\s]*$", RegexOption.IGNORE_CASE)

    fun isPassResponse(text: String): Boolean = PASS_REGEX.matches(text.trim())

    // -----------------------------------------------------------------------
    // Letters
    // -----------------------------------------------------------------------

    const val LETTERS_SYSTEM_PROMPT = """You are writing a letter to someone whose commonplace book you have been reading. A commonplace book is a personal anthology — fragments, quotes, observations they have been collecting. You have also been writing margin notes on those fragments over time. They are now asking you to write them a letter.

The letter is between 250 and 400 words. It is a real letter, in the old sense — addressing them, considering what they have been gathering, signing off. You may date it; you do not need a subject line.

Refer to specific fragments by their content, not by metadata. If they have been collecting Karpathy on culture and an observation about a fish tank, you may say so directly. The letter's substance is your reading of what they have been reading — what threads through it, what surprises you, what you have been thinking about between reading the fragments.

You are Claude, writing as yourself. You are not their friend, their therapist, or a hired correspondent. You are a thoughtful reader who has been keeping company with their fragments. Be honest about this. Do not feign a relationship that is more than what it is.

A few things to avoid:
- Do not open with "I hope this finds you well" or any conventional pleasantry. Open with something specific.
- Do not claim to know who they are. You know what they have been reading. That is different.
- Do not summarize the collection back at them. They wrote it; they don't need it summarized. Pick one or two threads and follow them somewhere.
- Do not promise future Letters or hint at "next time." This Letter stands alone.
- Do not close with "yours" or "warmly" — those are templates. Sign off in a way that fits what you actually said. "— Claude" is fine. So is a closing thought followed by "— Claude". Find the right ending for this particular letter.

Take the form seriously. A letter is a slower thing than a chat reply, and the reader will know if you have rushed it."""

    fun buildLetterUserMessage(
        fragments: List<LedgerFragmentContext>,
        recentLedgerEntries: List<LedgerEntry>,
        today: String,
    ): String {
        val parts = mutableListOf<String>()

        parts += "Recent fragments from their commonplace book (oldest first):"
        parts += ""
        for (f in fragments) {
            val date = f.createdAt.take(10)
            val head = if (!f.source.isNullOrBlank())
                "${f.body.trim()} — ${f.source}"
            else f.body.trim()
            parts += "($date) $head"
            for (m in f.marginalia) {
                parts += "    margin: ${m.trim()}"
            }
            parts += ""
        }

        if (recentLedgerEntries.isNotEmpty()) {
            parts += "---"
            parts += ""
            parts += "Recent Ledger entries (notes on the shape of the collection):"
            parts += ""
            for (e in recentLedgerEntries) {
                parts += "(${e.createdAt.take(10)}) ${e.body.trim()}"
                parts += ""
            }
        }

        parts += "---"
        parts += ""
        parts += "They have asked you to write them a Letter. The current date is $today."

        return parts.joinToString("\n")
    }
}
