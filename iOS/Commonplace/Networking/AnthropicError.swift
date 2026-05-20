import Foundation

enum AnthropicError: LocalizedError {
    case missingApiKey
    case invalidResponse
    case http(status: Int, message: String)
    case decoding(String)
    case streamInterrupted(message: String)

    var errorDescription: String? {
        switch self {
        case .missingApiKey:
            return "Anthropic API key is not configured."
        case .invalidResponse:
            return "Unexpected response from the API."
        case .http(let status, let message):
            return "HTTP \(status): \(message)"
        case .decoding(let detail):
            return "Couldn't decode the response: \(detail)"
        case .streamInterrupted(let message):
            return message
        }
    }
}
