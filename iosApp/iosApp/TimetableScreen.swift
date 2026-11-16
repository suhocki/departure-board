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
            List(timetable.departures, id: \.id) { departure in
                DepartureRowView(departure: departure, zoneId: timetable.station.zone().id)
            }
            .listStyle(.plain)
        case .failed:
            Text("Something went wrong")
        }
    }
}
