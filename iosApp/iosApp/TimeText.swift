import Shared
import SwiftUI

/// "+13 min" up to 30 minutes late, "Expected 17:24" after that. The rule is in shared code.
func delayText(_ departure: Departure, status: DepartureStatusDelayed, zoneId: String) -> String {
    let label = DelayLabelKt.delayLabel(scheduled: departure.scheduled, expected: status.expected)
    if let expected = label as? DelayLabelExpectedAt {
        return "Expected \(timeText(expected.expected, zoneId: zoneId))"
    }
    if let minutes = label as? DelayLabelMinutes {
        return "+\(minutes.minutes) min"
    }
    return ""
}

/// Clock time in the time zone of the station, in the 12 or 24 hour style of the device.
func timeText(_ instant: KotlinInstant, zoneId: String) -> String {
    let formatter = DateFormatter()
    formatter.locale = Locale.current
    formatter.timeZone = TimeZone(identifier: zoneId) ?? TimeZone(identifier: "UTC")
    formatter.dateStyle = .none
    formatter.timeStyle = .short
    return formatter.string(from: Date(timeIntervalSince1970: TimeInterval(instant.epochSeconds)))
}
