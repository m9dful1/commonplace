import SwiftUI

/// The web app's three-dot indigo pulse. Mirrors the same 1.4s ease-in-out
/// loop and 180ms staggered delays.
struct PulsingDots: View {
    var color: Color = Theme.accent

    var body: some View {
        HStack(spacing: 6) {
            Dot(color: color, delay: 0.0)
            Dot(color: color, delay: 0.18)
            Dot(color: color, delay: 0.36)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Claude is writing")
    }
}

private struct Dot: View {
    let color: Color
    let delay: Double
    @State private var phase: Double = 0.25

    var body: some View {
        Circle()
            .fill(color)
            .frame(width: 6, height: 6)
            .opacity(phase)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.7).repeatForever(autoreverses: true).delay(delay)) {
                    phase = 1.0
                }
            }
    }
}
