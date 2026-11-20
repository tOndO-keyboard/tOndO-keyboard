import SwiftUI
import StoreKit

/// Container app: hosts the keyboard extension, its settings and the Pro purchase
/// (the iOS counterpart of MainActivity/SettingsActivity on Android).
@main
struct TondoApp: App {
    @StateObject private var store = ProStore()

    var body: some Scene {
        WindowGroup {
            NavigationView {
                SettingsView()
            }
            .navigationViewStyle(.stack)
            .environmentObject(store)
            .task { await store.refresh() }
            .onOpenURL { _ in /* tondokeyboard://settings opens the settings screen (the root). */ }
        }
    }
}

/// StoreKit 2 purchase of the same product sold on Google Play ("pro_version_01").
@MainActor
final class ProStore: ObservableObject {
    static let productId = "pro_version_01"

    @Published private(set) var isPro: Bool = AppGroup.defaults.bool(forKey: AppGroup.proKey)
    @Published private(set) var product: Product?

    func refresh() async {
        product = try? await Product.products(for: [Self.productId]).first
        for await result in Transaction.currentEntitlements {
            if case .verified(let t) = result, t.productID == Self.productId, t.revocationDate == nil {
                setPro(true)
            }
        }
    }

    func purchase() async {
        guard let product else { return }
        if case .success(.verified(let t)) = try? await product.purchase() {
            setPro(true)
            await t.finish()
        }
    }

    func restore() async {
        try? await AppStore.sync()
        await refresh()
    }

    private func setPro(_ value: Bool) {
        isPro = value
        AppGroup.defaults.set(value, forKey: AppGroup.proKey)
    }
}
