import SwiftUI

struct FragmentsView: View {
    @Environment(AppStore.self) private var store
    @State private var fragments: [Fragment] = []
    @State private var loadError: String?
    @State private var pendingDelete: Fragment?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                FragmentForm()
                    .padding(.bottom, 28)
                    .overlay(alignment: .bottom) {
                        Rectangle()
                            .fill(Theme.rule)
                            .frame(height: 1)
                    }
                    .padding(.bottom, 28)

                if let err = loadError {
                    Text(err)
                        .font(Theme.Font.serif(size: 16, italic: true))
                        .foregroundStyle(Theme.muted)
                } else if fragments.isEmpty {
                    Text("Nothing yet. The first fragment goes above.")
                        .font(Theme.Font.serif(size: 16, italic: true))
                        .foregroundStyle(Theme.muted)
                } else {
                    LazyVStack(alignment: .leading, spacing: 36) {
                        ForEach(fragments) { fragment in
                            NavigationLink(value: fragment) {
                                FragmentRow(fragment: fragment)
                            }
                            .buttonStyle(.plain)
                            .contextMenu {
                                Button(role: .destructive) {
                                    pendingDelete = fragment
                                } label: {
                                    Label("Delete fragment", systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)
            .padding(.bottom, 32)
            .frame(maxWidth: Theme.pageMaxWidth, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Theme.paper)
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Commonplace")
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(for: Fragment.self) { fragment in
            FragmentDetailView(fragment: fragment)
        }
        .onAppear { reload() }
        .onChange(of: store.refreshToken) { _, _ in reload() }
        .alert("Delete this fragment?", isPresented: deleteAlertBinding, presenting: pendingDelete) { fragment in
            Button("Cancel", role: .cancel) { pendingDelete = nil }
            Button("Delete", role: .destructive) {
                delete(fragment)
            }
        } message: { _ in
            Text("Any margin notes Claude wrote on this fragment will be deleted with it. This can’t be undone.")
        }
    }

    private var deleteAlertBinding: Binding<Bool> {
        Binding(
            get: { pendingDelete != nil },
            set: { if !$0 { pendingDelete = nil } }
        )
    }

    private func reload() {
        do {
            fragments = try FragmentRepository.list()
            loadError = nil
        } catch {
            loadError = "Couldn't load fragments — \(error.localizedDescription)"
            fragments = []
        }
    }

    private func delete(_ fragment: Fragment) {
        Task {
            do {
                try FragmentRepository.delete(id: fragment.id)
                await MainActor.run {
                    pendingDelete = nil
                    store.bumpRefreshToken()
                }
            } catch {
                await MainActor.run { pendingDelete = nil }
            }
        }
    }
}

private struct FragmentRow: View {
    let fragment: Fragment

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(fragment.body)
                .font(Theme.Font.serif(size: 18))
                .foregroundStyle(Theme.ink)
                .lineLimit(nil)
                .fixedSize(horizontal: false, vertical: true)
                .multilineTextAlignment(.leading)

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
        .padding(.vertical, 4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }
}
