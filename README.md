# FamilyBooks

**FamilyBooks** is a 100% offline native iOS individual-family ledger application built with **Swift**, **SwiftUI**, **SwiftData**, and the **Observation (`@Observable`)** framework targeting **iOS 18.5+**.

Designed for privacy and clarity, FamilyBooks runs entirely on-device with zero networking, zero cloud sync, zero user accounts, and zero pre-populated sample data—starting cleanly at `$0.00` on first launch.

---

## Screenshots

### Onboarding & Dashboard

| Clean First Launch (`$0.00` Baseline) | Add Family Profile | Dashboard (Active Profile) | Quick Actions & Categories |
| :---: | :---: | :---: | :---: |
| <img src="familybooks/artifact/Screenshot%20iPhone%2017%2021-11-2026%20at%208.09.32%E2%80%AFPM.png" width="200" alt="Clean First Launch Dashboard" /> | <img src="familybooks/artifact/Screenshot%20iPhone%2017%2021-11-2026%20at%208.09.48%E2%80%AFPM.png" width="200" alt="Add Family Profile Sheet" /> | <img src="familybooks/artifact/Screenshot%20iPhone%2017%2021-11-2026%20at%208.09.54%E2%80%AFPM.png" width="200" alt="Dashboard with Active Profile" /> | <img src="familybooks/artifact/Screenshot%20iPhone%2017%2021-11-2026%20at%208.10.38%E2%80%AFPM.png" width="200" alt="Dashboard Quick Actions and Expense Categories" /> |

### Ledger, Analytics & Profiles

| Ledger (All Entries) | Ledger (Expense Filter) | Month-on-Month Analytics | Family Profiles & Standing |
| :---: | :---: | :---: | :---: |
| <img src="familybooks/artifact/Screenshot%20iPhone%2017%2021-11-2026%20at%208.10.42%E2%80%AFPM.png" width="200" alt="Ledger All Entries" /> | <img src="familybooks/artifact/Screenshot%20iPhone%2017%2021-11-2026%20at%208.10.46%E2%80%AFPM.png" width="200" alt="Ledger Filtered by Expense" /> | <img src="familybooks/artifact/Screenshot%20iPhone%2017%2021-11-2026%20at%208.10.51%E2%80%AFPM.png" width="200" alt="Month-on-Month Analytics" /> | <img src="familybooks/artifact/Screenshot%20iPhone%2017%2021-11-2026%20at%208.11.00%E2%80%AFPM.png" width="200" alt="Family Profiles Overview" /> |

---

## Core Features

- **100% Offline & Private**
  - Local-only SwiftData persistence (`cloudKitDatabase: .none`).
  - Zero networking, zero cloud sync, zero P2P/Bonjour code, zero login/accounts, and zero API keys.
  - **Clean First Launch**: Starts completely empty with a `$0.00` cash baseline and helpful empty states—every profile, transaction, and liability is entered by the user.

- **5-Tab Navigation & Fixed Brand Header**
  - **Dashboard**: Monthly net cash flow hero card, live Cash in Hand wallet with `+ Refill / ATM` & baseline calibration, Analytics shortcut, 4 Quick Action tiles (`+ Expense`, `+ Income`, `+ Debt`, `Split Bill`), active debts summary banner, monthly top expense categories with top payer, and recent entries.
  - **Ledger**: Full-text search across category, notes, payer, beneficiary, source, contributor, payment method, and amount; type & profile filter chips; live summary strip (`Out` / `In` totals); edit/delete actions; and offline backup access.
  - **Analytics**: Switch between **All Family** and individual member scopes; Month-on-Month (`MoM`) comparison with percentage delta badges and net savings; attribution, share contributions, and liability tiles; category-by-category MoM progress bars; and payment method breakdowns.
  - **Debts & Liabilities**: Track **Family IOUs**, **3rd-Party Debts (Payables)**, and **3rd-Party Receivables**; view Pending, Settled, and By Profile net standing; record partial payments (`UNPAID` -> `PARTIAL` -> `SETTLED`); or settle in one tap.
  - **Profiles**: Create and manage household members with roles, 8 avatar colors, default active profile switching, 6-metric per-member financial cards, net liability standing banners, and deep-links directly into filtered Analytics and Ledger views.

- **Bill Splitting & Cash Wallet**
  - **Split Expense**: Select upfront payer and participants, preview rounded equal shares live, optionally log the full expense to the ledger, and automatically generate `FAMILY_IOU` records for each participant.
  - **Cash in Hand**: Automatically tracks cash income minus cash expenses on top of an adjustable baseline (`+$50`, `+$100`, `+$200`, `+$500` quick refills or direct baseline reconciliation).

- **Cross-Platform Offline Backup & Restore (JSON v2.0 & CSV)**
  - Export and import by **Full Year**, **Particular Month**, or **Multiple Months**.
  - Share via iOS Share Sheet (`ShareLink`), Files app (`.fileExporter` / `.fileImporter`), or clipboard copy/paste.
  - Deduplicated merge by `id` / `updatedAt` (and case-insensitive profile name matching) with soft-delete awareness.

---

## Cross-Platform Backup Schemas

FamilyBooks exports and imports standardized schemas so backups remain interchangeable across iOS and Android:

### JSON v2.0
- Top-level fields: `app` (`"FamilyBooks"`), `exportVersion` (`"2.0"`), `year`, `months` (**0-based integers**, `[]` for Full Year), `summary`, `profiles`, `transactions` (including `formattedDate`), and `invoices`.

### CSV (RFC-4180)
- 13-column header with `"yyyy-MM-dd HH:mm:ss"` (`en_US_POSIX`) timestamps and full RFC-4180 quoting/unescaping (preserving commas, quotes, and embedded newlines):
  ```text
  ID,Date,Type,Amount,Category,PaymentMethod,PaidBy,ForWhom,FromWhom,ContributedBy,ContributedAmount,Notes,CreatedAt
  ```

---

## Visual Design System

FamilyBooks enforces a high-contrast light color scheme (`.preferredColorScheme(.light)`) built on a Deep Navy & Teal palette:

| Token | Hex | Usage |
| :--- | :--- | :--- |
| `Navy900` | `#0A2540` | Primary headers, dark hero cards, primary text |
| `TealAccent` | `#00D4B2` | Active pills, tab highlights, brand accent |
| `TealDark` | `#00A88D` | Secondary accents, positive badges |
| `TealLight` | `#E0FAF5` | Soft callouts and highlight surfaces |
| `BackgroundLight` | `#F5F7FA` | Screen canvas background |
| `SurfaceLight` | `#FFFFFF` | Cards and modal sheets |
| `CardBorderLight` | `#D5DEE8` | Card dividers and borders |
| `ExpenseRed` / `Bg` | `#D32F2F` / `#FFEBEE` | Expense values and status pills |
| `IncomeGreen` / `Bg` | `#2E7D32` / `#E8F5E9` | Income values and settled badges |
| `PendingOrange` / `Bg` | `#ED6C02` / `#FFF3E0` | Pending / partial debt indicators |

---

## Project Structure

```text
familybooks/
├── README.md
├── familybooks.xcodeproj
├── familybooks/
│   ├── App/
│   │   ├── FamilyBooksApp.swift          # @main entry point & local-only SwiftData container
│   │   └── RootTabView.swift             # Fixed header, 5-tab navigation, sheet router, toast banner
│   ├── Models/
│   │   ├── FamilyMember.swift            # @Model family profile with soft-delete support
│   │   ├── LedgerTransaction.swift       # @Model expense/income with attribution & share fields
│   │   ├── FamilyLiability.swift         # @Model Family IOU & 3rd-party payable/receivable
│   │   └── AnalyticsModels.swift         # Aggregated metrics & JSON v2.0 / CSV DTOs
│   ├── Services/
│   │   ├── LedgerStore.swift             # @Observable state store, CRUD, cash pool & merge logic
│   │   ├── AnalyticsCalculator.swift     # Per-profile aggregation, MoM analytics & split math
│   │   └── LedgerExportImportManager.swift # JSON v2.0 & RFC-4180 CSV serializer/parser
│   ├── Theme/
│   │   ├── BrandColors.swift             # Brand palette & category SF Symbols
│   │   ├── FormatUtils.swift             # Currency, POSIX date, rounding & month helpers
│   │   └── InputModifiers.swift          # High-contrast input, card & button modifiers
│   ├── Views/
│   │   ├── Components/
│   │   │   ├── TopHeaderBar.swift        # Deep Navy header & active profile avatar button
│   │   │   ├── ChipViews.swift           # Filter & selection chips
│   │   │   └── TransactionRowCard.swift  # Transaction list item with attribution pills
│   │   ├── Screens/
│   │   │   ├── DashboardView.swift
│   │   │   ├── LedgerView.swift
│   │   │   ├── AnalyticsView.swift
│   │   │   ├── DebtsView.swift
│   │   │   └── ProfilesView.swift
│   │   └── Sheets/
│   │       ├── AddTransactionSheet.swift
│   │       ├── AddLiabilitySheet.swift
│   │       ├── SplitExpenseSheet.swift
│   │       ├── AddEditProfileSheet.swift
│   │       ├── RefillCashSheet.swift
│   │       └── ExportImportSheet.swift
│   ├── Assets.xcassets
│   └── artifact/                         # App screenshots used in this README
└── familybooksTests/
    └── familybooksTests.swift            # Swift Testing unit test suite
```

---

## Requirements & Running

- **Xcode**: 16.0+
- **Target Platform**: iOS 18.5+ (iPhone & iPad)
- **Frameworks**: SwiftUI, SwiftData, Observation, UniformTypeIdentifiers, Testing

### Build & Run
1. Open `familybooks.xcodeproj` in Xcode.
2. Select the **`familybooks`** scheme and an iOS 18.5+ Simulator (e.g., iPhone 17) or physical device.
3. Press **Cmd + R** to build and run.

### Run Unit Tests
Switch to the **`familybooksTests`** scheme (or run tests via **Cmd + U**) to execute the Swift Testing suite in `familybooksTests/familybooksTests.swift`:

| Test Case | Coverage |
| :--- | :--- |
| `cleanFirstLaunchStartsCompletelyEmpty` | Verifies empty initial profiles, transactions, liabilities, and `$0.00` cash balance |
| `cashInHandBalanceCalculationAndBaselineAdjustment` | Cash refills, cash vs. card spend effects, and baseline calibration |
| `perProfileFinancialAggregationAndNetLiabilityStanding` | Income, paid out, spent for, shares contributed/received, IOUs, 3rd-party debts & net standing |
| `partialLiabilityPaymentsTransitionUnpaidToPartialToSettled` | State transitions `UNPAID` -> `PARTIAL` -> `SETTLED` and settlement timestamps |
| `splitExpenseCalculationRoundingAndIOUCreation` | Equal share rounding (`$33.33`), optional ledger logging, and `FAMILY_IOU` generation |
| `jsonV2ExportImportRoundTripWithScopeFiltering` | Full Year, Particular Month, and Multi-Month JSON v2.0 round-trip & duplicate profile skipping |
| `csvExportImportRoundTripWithRFC4180QuotingAndScopeFilter` | 13-column CSV export, RFC-4180 quotes/newlines, and month-scoped import merge |
