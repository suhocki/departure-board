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
    /// The start of the window, cut to the minute like on Android.
    @Published private(set) var updatedAt = Date()

    /// Set when a refresh fails and the old list stays on screen.
    @Published private(set) var refreshError: LoadError?

    private let loader: IosTimetableLoader
    private var isLoading = false

    init(apiKey: String) {
        loader = IosTimetableLoader(apiKey: apiKey)
    }

    /// First load, Retry and pull to refresh. A call while a request runs is skipped.
    func load() async {
        if isLoading { return }
        isLoading = true
        defer { isLoading = false }
        let previous = loadedTimetable
        refreshError = nil
        if previous == nil { state = .loading }
        // The shared loader returns a result, it does not throw for network errors.
        // Anything else is shown as an unexpected error.
        let started = Date()
        do {
            let result = try await loader.load()
            if let success = result as? LoadResultSuccess<Timetable>, let timetable = success.value {
                updatedAt = Date(timeIntervalSince1970: floor(started.timeIntervalSince1970 / 60) * 60)
                state = .loaded(timetable)
            } else if let failure = result as? LoadResultFailure {
                fail(failure.error, keeping: previous)
            } else {
                fail(.unexpected, keeping: previous)
            }
        } catch {
            fail(.unexpected, keeping: previous)
        }
    }

    private var loadedTimetable: Timetable? {
        if case .loaded(let timetable) = state { return timetable }
        return nil
    }

    private func fail(_ error: LoadError, keeping previous: Timetable?) {
        if previous == nil {
            state = .failed(error)
        } else {
            refreshError = error
        }
    }
}
