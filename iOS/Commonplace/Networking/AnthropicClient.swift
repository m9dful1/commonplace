import Foundation

/// A minimal Anthropic Messages API client. URLSession only — no SDK.
/// Two surfaces: `sendMessage(...)` for whole-response calls (Ledger, Letter,
/// settings test) and `streamMessage(...)` for the streaming Marginalia call.
struct AnthropicClient {
    let apiKey: String

    private let endpoint = URL(string: "https://api.anthropic.com/v1/messages")!
    private let apiVersion = "2023-06-01"

    init?(apiKey: String?) {
        guard let key = apiKey?.trimmingCharacters(in: .whitespacesAndNewlines), !key.isEmpty else { return nil }
        self.apiKey = key
    }

    /// Send a non-streaming request and return the joined text content.
    /// Also returns the model id reported by the response (useful for the
    /// Settings "test key" surface).
    func sendMessage(
        model: String,
        maxTokens: Int,
        system: String,
        userMessage: String,
        timeout: TimeInterval = 120
    ) async throws -> (text: String, model: String) {
        var request = baseRequest(timeout: timeout)
        let body = AnthropicWire.MessageRequest(
            model: model,
            max_tokens: maxTokens,
            system: system,
            messages: [.init(role: "user", content: userMessage)],
            stream: nil
        )
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw AnthropicError.invalidResponse }
        guard (200..<300).contains(http.statusCode) else {
            let msg = String(data: data, encoding: .utf8) ?? "HTTP \(http.statusCode)"
            throw AnthropicError.http(status: http.statusCode, message: msg)
        }
        do {
            let decoded = try JSONDecoder().decode(AnthropicWire.MessageResponse.self, from: data)
            let text = decoded.content
                .compactMap { $0.type == "text" ? $0.text : nil }
                .joined()
            return (text, decoded.model ?? model)
        } catch {
            throw AnthropicError.decoding(error.localizedDescription)
        }
    }

    /// Stream a Messages API call and yield text deltas as they arrive.
    /// The stream completes normally on `event: message_stop` and throws on
    /// any error event or transport failure.
    func streamMessage(
        model: String,
        maxTokens: Int,
        system: String,
        userMessage: String
    ) -> AsyncThrowingStream<String, Error> {
        return AsyncThrowingStream { continuation in
            let task = Task {
                do {
                    var request = baseRequest(timeout: 120)
                    request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
                    let body = AnthropicWire.MessageRequest(
                        model: model,
                        max_tokens: maxTokens,
                        system: system,
                        messages: [.init(role: "user", content: userMessage)],
                        stream: true
                    )
                    request.httpBody = try JSONEncoder().encode(body)

                    let (bytes, response) = try await URLSession.shared.bytes(for: request)
                    guard let http = response as? HTTPURLResponse else {
                        throw AnthropicError.invalidResponse
                    }
                    if !(200..<300).contains(http.statusCode) {
                        // Read remaining bytes for an error message.
                        var collected = Data()
                        for try await byte in bytes {
                            collected.append(byte)
                            if collected.count > 4096 { break }
                        }
                        let msg = String(data: collected, encoding: .utf8) ?? "HTTP \(http.statusCode)"
                        throw AnthropicError.http(status: http.statusCode, message: msg)
                    }

                    var currentEvent: String? = nil
                    for try await line in bytes.lines {
                        if Task.isCancelled { break }
                        if line.isEmpty {
                            currentEvent = nil
                            continue
                        }
                        if line.hasPrefix("event:") {
                            currentEvent = String(line.dropFirst("event:".count)).trimmingCharacters(in: .whitespaces)
                            continue
                        }
                        if line.hasPrefix("data:") {
                            let payload = String(line.dropFirst("data:".count)).trimmingCharacters(in: .whitespaces)
                            switch currentEvent {
                            case "content_block_delta":
                                if let data = payload.data(using: .utf8),
                                   let parsed = try? JSONDecoder().decode(AnthropicWire.ContentBlockDelta.self, from: data),
                                   parsed.delta.type == "text_delta",
                                   let text = parsed.delta.text {
                                    continuation.yield(text)
                                }
                            case "message_stop":
                                continuation.finish()
                                return
                            case "error":
                                let msg = extractErrorMessage(payload) ?? payload
                                throw AnthropicError.streamInterrupted(message: msg)
                            default:
                                break
                            }
                        }
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }

    // MARK: - Internals

    private func baseRequest(timeout: TimeInterval) -> URLRequest {
        var req = URLRequest(url: endpoint)
        req.httpMethod = "POST"
        req.timeoutInterval = timeout
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue(apiKey, forHTTPHeaderField: "x-api-key")
        req.setValue(apiVersion, forHTTPHeaderField: "anthropic-version")
        return req
    }

    private func extractErrorMessage(_ payload: String) -> String? {
        guard let data = payload.data(using: .utf8) else { return nil }
        if let any = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
           let err = any["error"] as? [String: Any],
           let message = err["message"] as? String {
            return message
        }
        return nil
    }
}
