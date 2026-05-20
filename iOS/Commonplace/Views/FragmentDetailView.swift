import SwiftUI

struct FragmentDetailView: View {
    let fragment: Fragment

    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var marginalia: [Marginalia] = []
    @State private var showingDeleteAlert: Bool = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 36) {
                VStack(alignment: .leading, spacing: 12) {
                    Text(fragment.body)
                        .font(Theme.Font.serif(size: 20))
                        .foregroundStyle(Theme.ink)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .textSelection(.enabled)

                    HStack(alignment: .firstTextBaseline, spacing: 10) {
                        Text(DateFormatting.short(fragment.createdAt))
                            .font(Theme.Font.mono(size: 11))
                            .tracking(1.6)
                            .textCase(.uppercase)
                            .foregroundStyle(Theme.muted)
                        if let source = fragment.source, !source.isEmpty {
                            Text("·").foregroundStyle(Theme.muted)
                            Text(source)
                                .font(Theme.Font.mono(size: 11))
                                .foregroundStyle(Theme.muted)
                        }
                        if !fragment.tags.isEmpty {
                            Text("·").foregroundStyle(Theme.muted)
                            Text(fragment.tags.map { "#\($0)" }.joined(separator: " "))
                                .font(Theme.Font.mono(size: 11))
                                .foregroundStyle(Theme.muted)
                        }
                    }
                    .lineLimit(1)
                }

                Divider()
                    .background(Theme.rule)

                VStack(alignment: .leading, spacing: 24) {
                    MarginaliaList(items: marginalia)
                    MarginaliaRequester(fragment: fragment, hasExisting: !marginalia.isEmpty)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 32)
            .frame(maxWidth: Theme.pageMaxWidth, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Theme.paper)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button(role: .destructive) {
                        showingDeleteAlert = true
                    } label: {
                        Label("Delete fragment", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .foregroundStyle(Theme.muted)
                        .accessibilityLabel("More")
                }
            }
        }
        .alert("Delete this fragment?", isPresented: $showingDeleteAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive, action: deleteFragment)
        } message: {
            Text("Any margin notes Claude wrote on this fragment will be deleted with it. This can’t be undone.")
        }
        .onAppear { reload() }
        .onChange(of: store.refreshToken) { _, _ in reload() }
    }

    private func reload() {
        do {
            marginalia = try MarginaliaRepository.list(forFragment: fragment.id)
        } catch {
            marginalia = []
        }
    }

    private func deleteFragment() {
        Task {
            do {
                try FragmentRepository.delete(id: fragment.id)
                await MainActor.run {
                    store.bumpRefreshToken()
                    dismiss()
                }
            } catch {
                // best-effort
            }
        }
    }
}
