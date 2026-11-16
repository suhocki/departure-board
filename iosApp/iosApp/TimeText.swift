import Shared
import SwiftUI

/// Clock time in the time zone of the station, in the 12 or 24 hour style of the device.
func timeText(_ instant: KotlinInstant, zoneId: String) -> String {
    let formatter = DateFormatter()
    formatter.locale = Locale.current
    formatter.timeZone = TimeZone(identifier: zoneId) ?? TimeZone(identifier: "UTC")
    formatter.dateStyle = .none
    formatter.timeStyle = .short
    return formatter.string(from: Date(timeIntervalSince1970: TimeInterval(instant.epochSeconds)))
}
