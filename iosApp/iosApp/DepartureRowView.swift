import Shared
import SwiftUI

struct DepartureRowView: View {
    let departure: Departure
    let zoneId: String

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text(departure.direction)
                Text(departure.lineCode)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text(timeText(departure.scheduled, zoneId: zoneId))
                .font(.headline)
        }
        .accessibilityElement(children: .combine)
    }
}
