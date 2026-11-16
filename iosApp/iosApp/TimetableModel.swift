import Shared
import SwiftUI

@MainActor
final class TimetableModel: ObservableObject {
    enum State {
        case loading
        case loaded(Timetable)
        case failed(LoadError)
    }

    @Published private(set) var state: State = .loading

    private let loader: IosTimetableLoader

    init(apiKey: String) {
        loader = IosTimetableLoader(apiKey: apiKey)
    }

    func load() async {
        // The shared loader returns a result, it does not throw for network errors.
        // Anything else is shown as an unexpected error.
        do {
            let result = try await loader.load()
            if let success = result as? LoadResultSuccess<Timetable>, let timetable = success.value {
                state = .loaded(timetable)
            } else if let failure = result as? LoadResultFailure {
                state = .failed(failure.error)
            } else {
                state = .failed(.unexpected)
            }
        } catch {
            state = .failed(.unexpected)
        }
    }
}
