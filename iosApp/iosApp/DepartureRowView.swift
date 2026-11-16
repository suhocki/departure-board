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
            VStack(alignment: .trailing, spacing: 2) {
                Text(timeText(departure.scheduled, zoneId: zoneId))
                    .font(.headline)
                    .strikethrough(departure.status is DepartureStatusCancelled)
                if let delayed = departure.status as? DepartureStatusDelayed {
                    statusText(delayText(departure, status: delayed, zoneId: zoneId))
                } else if departure.status is DepartureStatusCancelled {
                    statusText("Cancelled")
                }
            }
        }
        .accessibilityElement(children: .combine)
    }

    private func statusText(_ text: String) -> some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(.red)
    }
}
