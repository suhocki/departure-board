import Shared
import SwiftUI

struct TimetableScreen: View {
    @StateObject private var model: TimetableModel

    init(apiKey: String) {
        _model = StateObject(wrappedValue: TimetableModel(apiKey: apiKey))
    }

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Departures")
                .navigationBarTitleDisplayMode(.inline)
        }
        .task { await model.load() }
    }

    @ViewBuilder
    private var content: some View {
        switch model.state {
        case .loading:
            ProgressView()
        case .loaded(let timetable):
            let zone = timetable.station.zone()
            List {
                header(timetable, zone: zone)
                ForEach(timetable.departures, id: \.id) { departure in
                    DepartureRowView(departure: departure, zoneId: zone.id)
                }
            }
            .listStyle(.plain)
        case .failed:
            Text("Something went wrong")
        }
    }

    private func header(_ timetable: Timetable, zone: Kotlinx_datetimeTimeZone) -> some View {
        let start = KotlinInstant.companion.fromEpochSeconds(
            epochSeconds: Int64(model.updatedAt.timeIntervalSince1970),
            nanosecondAdjustment: 0
        )
        let from = timeText(start, zoneId: zone.id)
        let hours = Int(TimetableDefaults.shared.WINDOW_HOURS)
        let unit = hours == 1 ? "hour" : "hours"
        let zoneText = timetable.station.zoneIsFallback
            ? "Times in UTC (station time zone unknown)"
            : "Station time (\(UtcOffsetKt.formatUtcOffset(zone: zone, at: start)))"
        return VStack(alignment: .leading, spacing: 2) {
            Text(timetable.station.name)
                .font(.title2)
            Text("Next \(hours) \(unit) from \(from) · \(zoneText)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .listRowSeparator(.hidden)
    }
}
