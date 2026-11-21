import SwiftData
import SwiftUI

@main
struct FamilyBooksApp: App {
    private let modelContainer: ModelContainer
    @State private var store: LedgerStore

    init() {
        let schema = Schema([
            FamilyMember.self,
            LedgerTransaction.self,
            FamilyLiability.self
        ])
        let configuration = ModelConfiguration(
            "FamilyBooksOfflineStore",
            schema: schema,
            isStoredInMemoryOnly: false,
            cloudKitDatabase: .none
        )

        do {
            let container = try ModelContainer(for: schema, configurations: [configuration])
            self.modelContainer = container
            self._store = State(initialValue: LedgerStore(modelContext: container.mainContext))
        } catch {
            fatalError("Failed to initialize local SwiftData store: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            RootTabView(store: store)
                .preferredColorScheme(.light)
        }
        .modelContainer(modelContainer)
    }
}
