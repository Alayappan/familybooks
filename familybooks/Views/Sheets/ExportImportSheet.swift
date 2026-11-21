import SwiftUI
import UIKit
import UniformTypeIdentifiers

struct BackupTextDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json, .commaSeparatedText, .plainText] }

    var text: String

    init(text: String = "") {
        self.text = text
    }

    init(configuration: ReadConfiguration) throws {
        if let data = configuration.file.regularFileContents,
           let content = String(data: data, encoding: .utf8) {
            self.text = content
        } else {
            self.text = ""
        }
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        let data = text.data(using: .utf8) ?? Data()
        return FileWrapper(regularFileWithContents: data)
    }
}

struct ExportImportSheet: View {
    let store: LedgerStore

    @Environment(\.dismiss) private var dismiss

    enum SheetTab: String, CaseIterable, Identifiable {
        case exportLedger = "Export Ledger"
        case importLedger = "Import Ledger"

        var id: String { rawValue }
    }

    @State private var activeTab: SheetTab = .exportLedger

    // Export state
    @State private var exportScopeMode: ExportScopeMode = .fullYear
    @State private var exportYear: Int
    @State private var exportSelectedMonths: Set<Int>
    @State private var exportFormat: ExportFormat = .json
    @State private var exportStatusMessage: String?
    @State private var showFileExporter: Bool = false
    @State private var exportDocument: BackupTextDocument = BackupTextDocument()
    @State private var exportFilename: String = "FamilyBooks_Backup"

    // Import state
    @State private var importRawText: String = ""
    @State private var parsedPreview: ParsedImportPreview?
    @State private var parseError: String?
    @State private var filterImportByScope: Bool = false
    @State private var importScopeMode: ExportScopeMode = .fullYear
    @State private var importYear: Int
    @State private var importSelectedMonths: Set<Int>
    @State private var showFileImporter: Bool = false
    @State private var importResultMessage: String?

    init(store: LedgerStore) {
        self.store = store
        let comps = Calendar.current.dateComponents([.year, .month], from: Date())
        let currentYear = comps.year ?? 2026
        let currentZeroMonth = max(0, min(11, (comps.month ?? 1) - 1))
        _exportYear = State(initialValue: currentYear)
        _exportSelectedMonths = State(initialValue: [currentZeroMonth])
        _importYear = State(initialValue: currentYear)
        _importSelectedMonths = State(initialValue: [currentZeroMonth])
    }

    private var availableYears: [Int] {
        let currentYear = Calendar.current.component(.year, from: Date())
        var years = Set<Int>([currentYear - 1, currentYear, currentYear + 1])
        for tx in store.transactions {
            years.insert(FormatUtils.yearAndZeroBasedMonth(fromEpochMs: tx.date).year)
        }
        for liab in store.liabilities {
            years.insert(FormatUtils.yearAndZeroBasedMonth(fromEpochMs: liab.date).year)
        }
        return years.sorted(by: >)
    }

    private var currentExportScope: ExportScopeFilter {
        ExportScopeFilter(
            mode: exportScopeMode,
            year: exportYear,
            selectedZeroBasedMonths: exportSelectedMonths
        )
    }

    private var exportedTextPayload: String {
        switch exportFormat {
        case .json:
            return (try? LedgerExportImportManager.exportJSON(
                profiles: store.profiles,
                transactions: store.transactions,
                liabilities: store.liabilities,
                scope: currentExportScope,
                exportedBy: store.defaultPayerName
            )) ?? "{}"
        case .csv:
            return LedgerExportImportManager.exportCSV(
                transactions: store.transactions,
                scope: currentExportScope
            )
        }
    }

    private var exportMatchCounts: (txCount: Int, liabCount: Int, profileCount: Int) {
        let scope = currentExportScope
        let txCount = store.transactions.filter { !$0.isDeleted && scope.matches(epochMs: $0.date) }.count
        let liabCount = store.liabilities.filter { !$0.isDeleted && scope.matches(epochMs: $0.date) }.count
        return (txCount, liabCount, store.profiles.count)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Tab Switcher
                    HStack(spacing: 10) {
                        ForEach(SheetTab.allCases) { tab in
                            let selected = activeTab == tab
                            Button {
                                activeTab = tab
                            } label: {
                                Text(tab.rawValue)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(selected ? .white : BrandColors.textPrimary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 11)
                                    .background(selected ? BrandColors.navy900 : BrandColors.surfaceLight)
                                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                                            .stroke(selected ? BrandColors.navy900 : BrandColors.inputBorderUnfocused, lineWidth: 1.1)
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    if activeTab == .exportLedger {
                        exportSection
                    } else {
                        importSection
                    }
                }
                .padding(16)
            }
            .navigationTitle("Offline Backup & Restore")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                        .foregroundStyle(BrandColors.navy900)
                }
            }
            .fileExporter(
                isPresented: $showFileExporter,
                document: exportDocument,
                contentType: exportFormat == .json ? .json : .commaSeparatedText,
                defaultFilename: exportFilename
            ) { result in
                switch result {
                case .success:
                    exportStatusMessage = "Saved backup file successfully."
                    store.showToast("Backup file saved")
                case .failure(let error):
                    exportStatusMessage = "Export failed: \(error.localizedDescription)"
                }
            }
            .fileImporter(
                isPresented: $showFileImporter,
                allowedContentTypes: [.json, .commaSeparatedText, .plainText],
                allowsMultipleSelection: false
            ) { result in
                handleImportedFile(result)
            }
        }
        .familyBooksSheetStyle()
    }

    // MARK: - Export Tab UI

    private var exportSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Format Toggle
            VStack(alignment: .leading, spacing: 10) {
                Text("Export Format")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(BrandColors.textPrimary)

                HStack(spacing: 10) {
                    formatCard(.json, subtitle: "Full v2.0 (Profiles, Ledger, Debts)")
                    formatCard(.csv, subtitle: "Spreadsheet Transactions (.csv)")
                }
            }
            .cardSurface()

            // Scope Selector
            VStack(alignment: .leading, spacing: 12) {
                Text("Date Scope")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(BrandColors.textPrimary)

                scopePickerView(
                    mode: $exportScopeMode,
                    year: $exportYear,
                    selectedMonths: $exportSelectedMonths,
                    yearsList: availableYears
                )
            }
            .cardSurface()

            // Export Preview Summary
            let counts = exportMatchCounts
            VStack(alignment: .leading, spacing: 8) {
                Text("EXPORT SUMMARY")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(BrandColors.tealAccent)
                    .tracking(0.8)

                Text(currentExportScope.descriptionText)
                    .font(.headline)
                    .foregroundStyle(.white)

                HStack(spacing: 14) {
                    Label("\(counts.txCount) Entries", systemImage: "list.bullet.rectangle")
                    if exportFormat == .json {
                        Label("\(counts.liabCount) Debts", systemImage: "banknote")
                        Label("\(counts.profileCount) Profiles", systemImage: "person.2")
                    }
                }
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white.opacity(0.88))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(BrandColors.navy900)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            if let exportStatusMessage {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(BrandColors.incomeGreen)
                    Text(exportStatusMessage)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(BrandColors.incomeGreen)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(BrandColors.incomeGreenBg)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }

            // Export Actions
            VStack(spacing: 10) {
                HStack(spacing: 10) {
                    Button {
                        UIPasteboard.general.string = exportedTextPayload
                        exportStatusMessage = "Copied \(exportFormat.rawValue) backup to clipboard."
                        store.showToast("Backup copied to clipboard")
                    } label: {
                        Label("Copy Data", systemImage: "doc.on.doc")
                    }
                    .buttonStyle(SecondaryButtonStyle())

                    Button {
                        exportDocument = BackupTextDocument(text: exportedTextPayload)
                        let safeScope = currentExportScope.descriptionText
                            .replacingOccurrences(of: " ", with: "_")
                            .replacingOccurrences(of: ",", with: "")
                        exportFilename = "FamilyBooks_\(safeScope)"
                        showFileExporter = true
                    } label: {
                        Label("Save File", systemImage: "square.and.arrow.down")
                    }
                    .buttonStyle(PrimaryButtonStyle())
                }

                ShareLink(
                    item: exportedTextPayload,
                    subject: Text("FamilyBooks \(exportFormat.rawValue) Backup (\(currentExportScope.descriptionText))")
                ) {
                    Label("Share \(exportFormat.rawValue) Backup", systemImage: "square.and.arrow.up")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(BrandColors.navy900)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(BrandColors.tealLight)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(BrandColors.tealDark.opacity(0.45), lineWidth: 1.1)
                        )
                }
            }
        }
    }

    private func formatCard(_ format: ExportFormat, subtitle: String) -> some View {
        let selected = exportFormat == format
        return Button {
            exportFormat = format
        } label: {
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(format.rawValue)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(selected ? .white : BrandColors.textPrimary)
                    Spacer()
                    Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(selected ? BrandColors.tealAccent : BrandColors.inputBorderUnfocused)
                }
                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(selected ? .white.opacity(0.85) : BrandColors.textSecondary)
                    .multilineTextAlignment(.leading)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(selected ? BrandColors.navy900 : BrandColors.inputContainerLight)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(selected ? BrandColors.navy900 : BrandColors.inputBorderUnfocused, lineWidth: 1.1)
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Import Tab UI

    private var importSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 12) {
                Text("Load FamilyBooks Backup (JSON v2.0 or CSV)")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(BrandColors.textPrimary)

                HStack(spacing: 10) {
                    Button {
                        if let clip = UIPasteboard.general.string, !clip.isEmpty {
                            importRawText = clip
                            analyzeImportText()
                        } else {
                            parseError = "Clipboard is empty."
                        }
                    } label: {
                        Label("Paste Clipboard", systemImage: "doc.on.clipboard")
                    }
                    .buttonStyle(SecondaryButtonStyle())

                    Button {
                        showFileImporter = true
                    } label: {
                        Label("Import File", systemImage: "folder")
                    }
                    .buttonStyle(PrimaryButtonStyle())
                }

                StyledTextEditor(
                    label: "Or Paste JSON / CSV Backup Text Directly",
                    placeholder: "Paste FamilyBooks JSON v2.0 or CSV contents here...",
                    text: $importRawText,
                    minHeight: 110,
                    monospaced: true
                )

                Button("Preview & Validate Data") {
                    analyzeImportText()
                }
                .buttonStyle(SecondaryButtonStyle())
            }
            .cardSurface()

            if let parseError {
                HStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(BrandColors.expenseRed)
                    Text(parseError)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(BrandColors.expenseRed)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(BrandColors.expenseRedBg)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }

            if let preview = parsedPreview {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        StatusBadgePill(
                            text: "Detected \(preview.format.rawValue)",
                            foreground: BrandColors.tealDark,
                            background: BrandColors.tealLight
                        )
                        Spacer()
                        if !preview.detectedYears.isEmpty {
                            Text("Years: \(preview.detectedYears.map(String.init).joined(separator: ", "))")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BrandColors.textSecondary)
                        }
                    }

                    let activeImportScope: ExportScopeFilter? = filterImportByScope
                        ? ExportScopeFilter(mode: importScopeMode, year: importYear, selectedZeroBasedMonths: importSelectedMonths)
                        : nil
                    let filtered = LedgerExportImportManager.filterPreview(preview, scope: activeImportScope)

                    HStack(spacing: 12) {
                        previewMetricBox(title: "Profiles", value: "\(filtered.profiles.count)")
                        previewMetricBox(title: "Entries", value: "\(filtered.transactions.count)")
                        previewMetricBox(title: "Debts", value: "\(filtered.liabilities.count)")
                    }

                    Toggle(isOn: $filterImportByScope) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Filter Import by Year / Month Scope")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(BrandColors.textPrimary)
                            Text("Turn off to merge all records from this backup.")
                                .font(.caption2)
                                .foregroundStyle(BrandColors.textSecondary)
                        }
                    }
                    .tint(BrandColors.tealDark)

                    if filterImportByScope {
                        scopePickerView(
                            mode: $importScopeMode,
                            year: $importYear,
                            selectedMonths: $importSelectedMonths,
                            yearsList: preview.detectedYears.isEmpty ? availableYears : preview.detectedYears
                        )
                    }

                    Button("Merge Into FamilyBooks Ledger") {
                        performMerge(preview: preview, scope: activeImportScope)
                    }
                    .buttonStyle(PrimaryButtonStyle(backgroundColor: BrandColors.tealDark))
                }
                .cardSurface()
            }

            if let importResultMessage {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.seal.fill")
                        .foregroundStyle(BrandColors.incomeGreen)
                    Text(importResultMessage)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(BrandColors.incomeGreen)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(BrandColors.incomeGreenBg)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
        }
    }

    private func previewMetricBox(title: String, value: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.headline.weight(.bold))
                .foregroundStyle(BrandColors.navy900)
            Text(title)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(BrandColors.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(BrandColors.inputContainerLight)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(BrandColors.cardBorderLight, lineWidth: 1)
        )
    }

    // MARK: - Shared Scope Picker View

    @ViewBuilder
    private func scopePickerView(
        mode: Binding<ExportScopeMode>,
        year: Binding<Int>,
        selectedMonths: Binding<Set<Int>>,
        yearsList: [Int]
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            FlowLayout(spacing: 8) {
                ForEach(ExportScopeMode.allCases) { item in
                    SelectableChip(
                        title: item.rawValue,
                        isSelected: mode.wrappedValue == item
                    ) {
                        mode.wrappedValue = item
                        if item == .particularMonth {
                            let first = selectedMonths.wrappedValue.sorted().first ?? 0
                            selectedMonths.wrappedValue = [first]
                        }
                    }
                }
            }

            // Year Chips
            VStack(alignment: .leading, spacing: 6) {
                Text("Year")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(BrandColors.textSecondary)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(yearsList, id: \.self) { yr in
                            SelectableChip(
                                title: String(yr),
                                isSelected: year.wrappedValue == yr,
                                accentColor: BrandColors.tealDark
                            ) {
                                year.wrappedValue = yr
                            }
                        }
                    }
                }
            }

            if mode.wrappedValue == .particularMonth || mode.wrappedValue == .multipleMonths {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text(mode.wrappedValue == .particularMonth ? "Select Month" : "Select Months (Jan–Dec)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(BrandColors.textSecondary)

                        Spacer()

                        if mode.wrappedValue == .multipleMonths {
                            Button("Select All") {
                                selectedMonths.wrappedValue = Set(0..<12)
                            }
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(BrandColors.tealDark)

                            Button("Clear All") {
                                selectedMonths.wrappedValue = []
                            }
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(BrandColors.expenseRed)
                            .padding(.leading, 8)
                        }
                    }

                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 4), spacing: 6) {
                        ForEach(0..<12, id: \.self) { monthIdx in
                            let isChosen = selectedMonths.wrappedValue.contains(monthIdx)
                            Button {
                                if mode.wrappedValue == .particularMonth {
                                    selectedMonths.wrappedValue = [monthIdx]
                                } else {
                                    if isChosen {
                                        selectedMonths.wrappedValue.remove(monthIdx)
                                    } else {
                                        selectedMonths.wrappedValue.insert(monthIdx)
                                    }
                                }
                            } label: {
                                Text(FormatUtils.shortMonthNames[monthIdx])
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(isChosen ? .white : BrandColors.textPrimary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 8)
                                    .background(isChosen ? BrandColors.navy900 : BrandColors.inputContainerLight)
                                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                                            .stroke(isChosen ? BrandColors.navy900 : BrandColors.inputBorderUnfocused, lineWidth: 1)
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    // MARK: - Helpers

    private func handleImportedFile(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            let accessed = url.startAccessingSecurityScopedResource()
            defer {
                if accessed { url.stopAccessingSecurityScopedResource() }
            }
            do {
                let text = try String(contentsOf: url, encoding: .utf8)
                importRawText = text
                analyzeImportText()
            } catch {
                parseError = "Could not read selected file: \(error.localizedDescription)"
            }
        case .failure(let error):
            parseError = "File import failed: \(error.localizedDescription)"
        }
    }

    private func analyzeImportText() {
        parseError = nil
        importResultMessage = nil
        do {
            let preview = try LedgerExportImportManager.parseImportText(importRawText)
            parsedPreview = preview
            if let firstYear = preview.detectedYears.first {
                importYear = firstYear
                if let months = preview.detectedMonthsByYear[firstYear], !months.isEmpty {
                    importSelectedMonths = months
                }
            }
        } catch {
            parsedPreview = nil
            parseError = error.localizedDescription
        }
    }

    private func performMerge(preview: ParsedImportPreview, scope: ExportScopeFilter?) {
        let result = store.mergeImport(preview: preview, scope: scope)
        importResultMessage = "Merged! Profiles +\(result.profilesAdded) (\(result.profilesSkipped) skipped), Entries +\(result.transactionsAdded) (\(result.transactionsUpdated) updated), Debts +\(result.liabilitiesAdded) (\(result.liabilitiesUpdated) updated)."
        store.showToast("Backup merged successfully")
    }
}
