import SwiftUI

/// The "request a marginal note" affordance for a fragment. State machine:
/// idle → loading (pulsing dots) → streaming (live partial) → idle (refresh).
/// Errors land as a quiet inline line with a "try again" link, mirroring
/// the web's `<MarginaliaRequester>` state machine.
struct MarginaliaRequester: View {
    @Environment(AppStore.self) private var store
    let fragment: Fragment
    let hasExisting: Bool

    @State private var state: RequestState = .idle
    @State private var streamTask: Task<Void, Never>?

    enum RequestState {
        case idle
        case loading
        case streaming(String)
        case error(String, partial: String?)
    }

    var body: some View {
        Group {
            switch state {
            case .idle:
                idleView
            case .loading:
                PulsingDots()
                    .padding(.leading, 18)
            case .streaming(let text):
                MarginaliaBlock(text: text)
            case .error(let message, let partial):
                VStack(alignment: .leading, spacing: 12) {
                    if let partial, !partial.isEmpty {
                        MarginaliaBlock(text: partial)
                    }
                    Text("couldn’t reach Claude — \(message)")
                        .font(Theme.Font.serif(size: 14, italic: true))
                        .foregroundStyle(Theme.muted)
                        .padding(.leading, 18)
                    Button(action: request) {
                        Text("try again")
                            .font(Theme.Font.mono(size: 11))
                            .tracking(1.6)
                            .textCase(.uppercase)
                            .foregroundStyle(Theme.accent)
                    }
                    .padding(.leading, 18)
                }
            }
        }
        .onDisappear { streamTask?.cancel() }
    }

    @ViewBuilder
    private var idleView: some View {
        if !store.hasApiKey {
            Text("Add an Anthropic API key in Settings to request a marginal note.")
                .font(Theme.Font.serif(size: 14, italic: true))
                .foregroundStyle(Theme.muted)
        } else {
            Button(action: request) {
                Text(hasExisting ? "another reading" : "request a marginal note")
                    .font(Theme.Font.mono(size: 11))
                    .tracking(1.6)
                    .textCase(.uppercase)
                    .foregroundStyle(Theme.accent)
            }
        }
    }

    private func request() {
        state = .loading
        streamTask?.cancel()
        let frag = fragment
        streamTask = Task {
            var collected = ""
            let stream = MarginaliaService.stream(forFragment: frag)
            for await update in stream {
                if Task.isCancelled { return }
                switch update {
                case .delta(let chunk):
                    collected += chunk
                    let snapshot = collected
                    await MainActor.run {
                        state = .streaming(snapshot)
                    }
                case .completed:
                    await MainActor.run {
                        state = .idle
                        store.bumpRefreshToken()
                    }
                    return
                case .errored(let message):
                    let partial = collected.trimmingCharacters(in: .whitespacesAndNewlines)
                    let cleanPartial = partial.isEmpty ? nil : partial
                    await MainActor.run {
                        state = .error(message, partial: cleanPartial)
                    }
                    return
                }
            }
        }
    }
}
