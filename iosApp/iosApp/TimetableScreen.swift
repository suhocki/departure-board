import Shared
import SwiftUI

struct TimetableScreen: View {
    let apiKey: String

    var body: some View {
        VStack(spacing: 8) {
            Text("Station \(TimetableDefaults.shared.STATION_ID)")
            Text(apiKey.isEmpty ? "No API key" : "API key is set")
        }
        .padding()
    }
}
