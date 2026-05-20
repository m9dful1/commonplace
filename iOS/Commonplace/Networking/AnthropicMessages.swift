import Foundation

/// Wire types for the Anthropic Messages API. Only the fields we actually
/// read are decoded; the rest are tolerated by `decodeIfPresent`.
enum AnthropicWire {
    struct MessageRequest: Encodable {
        let model: String
        let max_tokens: Int
        let system: String
        let messages: [InputMessage]
        let stream: Bool?
    }

    struct InputMessage: Encodable {
        let role: String
        let content: String
    }

    /// Non-streaming response shape.
    struct MessageResponse: Decodable {
        let model: String?
        let content: [ContentBlock]
    }

    struct ContentBlock: Decodable {
        let type: String
        let text: String?
    }

    /// Decoded SSE event payloads we care about.
    enum StreamEvent {
        case textDelta(String)
        case messageStop
        case error(String)
        case other
    }

    /// Decoded payload for `event: content_block_delta`.
    struct ContentBlockDelta: Decodable {
        let type: String
        let delta: Delta

        struct Delta: Decodable {
            let type: String
            let text: String?
        }
    }
}
