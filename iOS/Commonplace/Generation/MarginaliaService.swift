import Foundation

/// Streaming marginalia generation. Yields text chunks as they arrive, then
/// inserts the full marginalia row into the database when the stream
/// completes successfully. Mid-stream errors discard the partial — same
/// rule the web app's Phase 2 build entry articulated: a half-finished
/// marginalia is a worse artifact than no marginalia.
enum MarginaliaService {
    enum StreamUpdate {
        case delta(String)
        case completed(Marginalia)
        case errored(message: String)
    }

    static func stream(forFragment fragment: Fragment) -> AsyncStream<StreamUpdate> {
        return AsyncStream { continuation in
            let task = Task {
                guard let client = AnthropicClient(apiKey: SettingsStore.getAnthropicKey()) else {
                    continuation.yield(.errored(message: "no API key set — visit Settings to add one."))
                    continuation.finish()
                    return
                }

                let priorMarginalia: [Marginalia]
                let recent: [(body: String, source: String?, createdAt: String)]
                do {
                    priorMarginalia = try MarginaliaRepository.list(forFragment: fragment.id)
                    recent = try FragmentRepository.recentForContext(excludeId: fragment.id, limit: 20)
                } catch {
                    continuation.yield(.errored(message: error.localizedDescription))
                    continuation.finish()
                    return
                }

                let userMessage = MarginaliaPrompt.userMessage(
                    fragment: fragment,
                    recentFragments: recent,
                    priorMarginalia: priorMarginalia
                )

                var collected = ""
                do {
                    let stream = client.streamMessage(
                        model: AnthropicModels.marginalia,
                        maxTokens: AnthropicModels.marginaliaMaxTokens,
                        system: MarginaliaPrompt.system,
                        userMessage: userMessage
                    )
                    for try await chunk in stream {
                        if Task.isCancelled { break }
                        collected += chunk
                        continuation.yield(.delta(chunk))
                    }
                    let trimmed = collected.trimmingCharacters(in: .whitespacesAndNewlines)
                    if !trimmed.isEmpty {
                        let inserted = try MarginaliaRepository.create(fragmentId: fragment.id, body: trimmed)
                        continuation.yield(.completed(inserted))
                    }
                    continuation.finish()
                } catch {
                    let msg = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
                    continuation.yield(.errored(message: msg))
                    continuation.finish()
                }
            }
            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }
}
