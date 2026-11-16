import Shared
import SwiftUI

struct TimetableScreen: View {
    var body: some View {
        Text("Station \(TimetableDefaults.shared.STATION_ID)")
            .padding()
    }
}
