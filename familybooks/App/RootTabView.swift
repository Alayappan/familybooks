import SwiftUI
import UIKit

enum MainTab: Hashable {
    case dashboard
    case ledger
    case analytics
    case debts
    case profiles
}

struct RootTabView: View {
    @Bindable var store: LedgerStore

    @State private var selectedTab: MainTab = .dashboard
    @State private var ledgerProfileFilter: String? = nil
    @State private var analyticsScopeProfile: String? = nil

    // Sheet presentation state
    @State private var activeSheet: ActiveSheet?

    enum ActiveSheet: Identifiable {
        case addTransaction(initialType: String)
        case editTransaction(LedgerTransaction)
        case addLiability
        case splitExpense
        case addProfile
        case editProfile(FamilyMember)
        case refillCash
        case exportImport

        var id: String {
            switch self {
            case .addTransaction(let t): return "addTx-\(t)"
            case .editTransaction(let tx): return "editTx-\(tx.id)"
            case .addLiability: return "addLiability"
            case .splitExpense: return "splitExpense"
            case .addProfile: return "addProfile"
            case .editProfile(let m): return "editProfile-\(m.id)"
            case .refillCash: return "refillCash"
            case .exportImport: return "exportImport"
            }
        }
    }

    init(store: LedgerStore) {
        self.store = store
        Self.configureTabBarAppearance()
    }

    private static func configureTabBarAppearance() {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = BrandColors.uiNavy900
        appearance.shadowColor = BrandColors.uiNavy900

        let itemAppearance = UITabBarItemAppearance()
        itemAppearance.normal.iconColor = BrandColors.uiTabUnselected
        itemAppearance.normal.titleTextAttributes = [
            .foregroundColor: BrandColors.uiTabUnselected
        ]
        itemAppearance.selected.iconColor = BrandColors.uiTealAccent
        itemAppearance.selected.titleTextAttributes = [
            .foregroundColor: BrandColors.uiTealAccent
        ]

        appearance.stackedLayoutAppearance = itemAppearance
        appearance.inlineLayoutAppearance = itemAppearance
        appearance.compactInlineLayoutAppearance = itemAppearance

        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                TopHeaderBar(store: store) {
                    if store.profiles.isEmpty {
                        activeSheet = .addProfile
                    } else {
                        selectedTab = .profiles
                    }
                }

                TabView(selection: $selectedTab) {
                    DashboardView(
                        store: store,
                        onOpenAddTransaction: { type in
                            activeSheet = .addTransaction(initialType: type)
                        },
                        onEditTransaction: { tx in
                            activeSheet = .editTransaction(tx)
                        },
                        onOpenAddLiability: {
                            activeSheet = .addLiability
                        },
                        onOpenSplitExpense: {
                            activeSheet = .splitExpense
                        },
                        onOpenRefillCash: {
                            activeSheet = .refillCash
                        },
                        onSwitchToLedger: {
                            selectedTab = .ledger
                        },
                        onSwitchToAnalytics: {
                            selectedTab = .analytics
                        },
                        onSwitchToDebts: {
                            selectedTab = .debts
                        }
                    )
                    .tabItem {
                        Label("Dashboard", systemImage: "square.grid.2x2")
                    }
                    .tag(MainTab.dashboard)
                    .toolbarBackground(BrandColors.navy900, for: .tabBar)
                    .toolbarBackground(.visible, for: .tabBar)

                    LedgerView(
                        store: store,
                        selectedProfileFilter: $ledgerProfileFilter,
                        onOpenAddTransaction: {
                            activeSheet = .addTransaction(initialType: LedgerTransaction.typeExpense)
                        },
                        onEditTransaction: { tx in
                            activeSheet = .editTransaction(tx)
                        },
                        onOpenExportImport: {
                            activeSheet = .exportImport
                        }
                    )
                    .tabItem {
                        Label("Ledger", systemImage: "list.bullet.rectangle")
                    }
                    .tag(MainTab.ledger)
                    .toolbarBackground(BrandColors.navy900, for: .tabBar)
                    .toolbarBackground(.visible, for: .tabBar)

                    AnalyticsView(
                        store: store,
                        selectedScopeProfile: $analyticsScopeProfile
                    )
                    .tabItem {
                        Label("Analytics", systemImage: "chart.bar.xaxis")
                    }
                    .tag(MainTab.analytics)
                    .toolbarBackground(BrandColors.navy900, for: .tabBar)
                    .toolbarBackground(.visible, for: .tabBar)

                    DebtsView(
                        store: store,
                        onOpenAddLiability: {
                            activeSheet = .addLiability
                        },
                        onOpenSplitExpense: {
                            activeSheet = .splitExpense
                        }
                    )
                    .tabItem {
                        Label("Debts", systemImage: "banknote")
                    }
                    .tag(MainTab.debts)
                    .toolbarBackground(BrandColors.navy900, for: .tabBar)
                    .toolbarBackground(.visible, for: .tabBar)

                    ProfilesView(
                        store: store,
                        onOpenAddProfile: {
                            activeSheet = .addProfile
                        },
                        onEditProfile: { member in
                            activeSheet = .editProfile(member)
                        },
                        onAnalyzeProfile: { profileName in
                            analyticsScopeProfile = profileName
                            selectedTab = .analytics
                        },
                        onViewEntriesForProfile: { profileName in
                            ledgerProfileFilter = profileName
                            selectedTab = .ledger
                        },
                        onOpenExportImport: {
                            activeSheet = .exportImport
                        }
                    )
                    .tabItem {
                        Label("Profiles", systemImage: "person.2")
                    }
                    .tag(MainTab.profiles)
                    .toolbarBackground(BrandColors.navy900, for: .tabBar)
                    .toolbarBackground(.visible, for: .tabBar)
                }
                .tint(BrandColors.tealAccent)
            }

            // Toast notification banner
            if let message = store.toastMessage {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(BrandColors.tealAccent)
                    Text(message)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.white)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(BrandColors.navy900)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(BrandColors.tealAccent.opacity(0.7), lineWidth: 1)
                )
                .shadow(color: BrandColors.navy900.opacity(0.25), radius: 8, x: 0, y: 4)
                .padding(.bottom, 64)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .task(id: message) {
                    try? await Task.sleep(for: .seconds(2.2))
                    if store.toastMessage == message {
                        withAnimation(.easeOut(duration: 0.2)) {
                            store.toastMessage = nil
                        }
                    }
                }
            }
        }
        .animation(.spring(duration: 0.25), value: store.toastMessage)
        .preferredColorScheme(.light)
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .addTransaction(let initialType):
                AddTransactionSheet(store: store, initialType: initialType)
            case .editTransaction(let tx):
                AddTransactionSheet(store: store, initialType: tx.type, editingTransaction: tx)
            case .addLiability:
                AddLiabilitySheet(store: store)
            case .splitExpense:
                SplitExpenseSheet(store: store) {
                    activeSheet = .addProfile
                }
            case .addProfile:
                AddEditProfileSheet(store: store)
            case .editProfile(let member):
                AddEditProfileSheet(store: store, editingMember: member)
            case .refillCash:
                RefillCashSheet(store: store)
            case .exportImport:
                ExportImportSheet(store: store)
            }
        }
    }
}
