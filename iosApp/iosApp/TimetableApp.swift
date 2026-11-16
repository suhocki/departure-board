import SwiftUI

@main
struct TimetableApp: App {
    // Set by Info.plist from TIMETABLE_API_KEY in Local.xcconfig. Empty when the file is missing.
    private let apiKey = Bundle.main.object(forInfoDictionaryKey: "TimetableApiKey") as? String ?? ""

    var body: some Scene {
        WindowGroup {
            TimetableScreen(apiKey: apiKey)
        }
    }
}
