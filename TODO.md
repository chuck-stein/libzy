# Development To-Do

- after incorporating Dagger 2, make SpotifyAuthManager a dependency instead of singleton
- flesh out README
- incorporate unit testing
- clean up gradle.build
- improve button styling, make responsive to presses
- remove legacy/compatibility stuff I don't need with my min SDK version
- determine whether I should continue to use @ExperimentalTime and @ExperimentalStdlibApi
- make "Connect Spotify" button on-brand
- decide on minimum and target SDK
- determine whether SpotifyAuthManager as Fragment and Activity dependency is best practice
- determine whether .arr modules should be added to git or .gitignore
- add a modal popup to install spotify if it's not detected on the device when necessary
- document all code
- use spotify app remote to provide song queuing as alternative feature to album search
- make app icon / logo
- add splash screen to LaunchActivity
- test auth when user doesn't have spotify downloaded (should bring to download page, use AuthenticationClient)
- figure out of I should use something other than AppCompatActivity if I'm not using the action bar
- if spotifyAuth() doesn't find spotify on the device, redirect to download page
- find a better name for FilterActivity
- sort out all errors/warnings in debugger
- if always-empty auth problem, persists just use webview or browser auth
- rename redirect URI host to spotify-auth
- define button callbacks in layout xml instead of Activity code
- define gradle version constants
- remove unused dependencies from build.gradle, such as kotlinx and dagger-android
- suppress or debug nav_graph xml error
- debug "pending vsync event" warning
- determine whether my project actually needs dependency injection (are there any nested dependencies? are there any shared dependencies?)
- create a develop branch and start using feature/bugfix branches
- check my kotlin syntax / practices (e.g. am I writing constructors correctly?)
- figure out what to do if SpotifyClient tries to make a call with an expired access token (spotify auth activity must be universally accessible)
- review the [Android Architecture Samples](https://github.com/android/architecture-samples/tree/master) for guidance
- apply a Google or JetBrains style guide
- maybe use adamint's wrapper for auth if it can do it as elegantly as spotify sdk, because then access tokens can always be in the backend
- add a timer for access token expiration with a callback that uses spotify auth sdk then returns to whatever activity user was in
- figure out how to auto-refresh the spotify token (may need a server for spotify's Authorization Code Flow)
- talk to adamint team to see if there's an alternative to requiring `exclude 'META-INF/*.kotlin_module'`
- change "Hey There!" message in SelectGenresActivity to dynamically-decided "Good Evening!" or "Good morning!" or "Good afternoon!"
- rename colors to something more applicable (and remove unused colors)
- determine proper parent theme
- add version constants to build.gradle
- remove unnecessary logs
- complete all inline TODOs
- remove unused resources
- replaced one-parameter lambdas with "it" when appropriate
- use jetpack navigation component
- update all UI and styling
- use a RecyclerView for the genres ChipGroup?
- better constraints for bottom nav bar and horizontal orientation, especially w/ genre chip group 
- handle UI rotations
- remove placeholder chips from SelectGenreActivity layout, because real loading chips are added programmatically (and delete where I programmatically remove them)
- decide on consistent view ID naming convention (either camel case or snake case, because used as both ID and class instance) ORRR just use view binding
- fix gradient not appearing since I updated to Material Theme
- just go through Android developer docs in order
- center genre option chips
- try Dan's mockup font to see how it compares to varela round
- handle failed API calls
- capitalize genre option chips?
- show number of results on each genre option chip
- make bottom of UI layout relative to nav bar
- sort order selector for BrowseResultsActivity (alphabetical, num albums)
- labeled scroll bar for BrowseResultsActivity
- ability to collapse a genre in BrowseResultsActivity
- understand and tweak each xml attribute of nested RecyclerView concoction
- change genresGroupedByAlbums to a DTO?
- use Kotlin's Flow API for getting Spotify data?
- use Android Coroutines Extension
- instead of using Spotify API wrapper, call it myself using Retrofit, following similar pattern as GithubBrowserSample in android-architecture-components-samples (gives a good model for using Dagger 2 as well)
- use fragments?
- cache user library info using Room (determine when I would want to use SharedPreferences instead)
- think about performance in every line of code
- add more color types to main app theme styling
- use dimens constants for everything in layout
- add splash screen for LaunchActivity
- make a more exciting "Connect Spotify" screen... maybe add welcome images and descriptions of waht the app can do and what we use your Spotify for?
- figure out how to constrain top and bottom to nav/status bars instead of screen edge
- look into using view binding / data binding
- use activity lifecycle callbacks to improve performance and persist state [(guide)](https://classroom.udacity.com/courses/ud9012/lessons/e487c600-ed68-4576-a35a-12f211cf032e/concepts/4c1503f9-2de5-45ea-88ae-2138b0482ecc)
- use Timber? 
- make a getter for MutableLiveData to cast it as LiveData
- follow architecture of GithubBrowserSample
- figure out why background gradient animation takes so long to start
- somehow share background gradient animation across activities, might need to use Application or Window or something?
- destroy background gradient animation resource in onDestroy?
- delete SpotifyAuthActivity.kt if it's unused
- make genre options scrollbar not quite on the very edge of the screen
- make alternate layouts when necessary for landscape mode (e.g. SelectGenresActivity)
- if activity themes don't extend AppCompatTheme, then the Activity classes can't extend AppCompatActivity
- make a ConnectSpotifyViewModel to store non-Activity/View-related functions in ConnectSpotifyActivity
- use data binding for any click handlers or LiveData or other data that should communicate between data and layout directly, skipping UI controller
- use LiveData map transformations when applicable
- typealias for Map<String, Set<String>> and MutableMap<String, MutableSet<String>>?
- dynamically load album results as the user scrolls
- organize packages better, probably based on GithubBrowser sample
- remove unnecessary logs
- search through inline TODOs and delete any that are already done
- follow advice from RecyclerView Udacity lesson more closely (e.g. refactoring ViewHolder, using data binding, etc.)
- check if adamint uses a coroutine internally, if so I only need the Main one, not the IO one
- use adamint API wrapper caching, look into other features
- determine whether adamint's get all albums requests 1 at a time.. or maybe it doesn't and that first album is just tog et the total number to batch requests. if it does, make it better
- rework access token management (do after switching to Navigation libary):
    - app initialized? (or user started Spotify-requiring session/task from eventual home screen?)
        - shared preferences contains unexpired access token?
            - initialize SpotifyClient with that token and expiry (pass them down)
        - shared preferences has no token or expired token?
            - run Spotify auth (should be its own activity, or master activity when using Navigation library)
            - store new token and expiration in SharedPreferences
            - initialize SpotifyClient with that token and expiry (pass them down)
    - SpotifyClient is about to start a request job without sufficient time left before token expiry?
        - first call refresh token callback which was passed down from main Activity
            - callback opens a Spotify auth login activity with a separate request code from app initialization request
            - in onActivityResult, if that request code was used, delegate to current ViewModel (find a mechanism for this), telling them to restart job
    - SpotifyClient gets an expired token error anyway?
        - fail safe callback and restart/resume the request job? or just show error message to user?
- use gradle constants for common version numbers, e.g. `buildscript { ext.kotlin_version = '1.3.72'`
- think about and handle edge cases with shared preferences auth communication / refreshing from Activity to SpotifyClientFactory
- figure out programmatic styling myself instead of using air b&b paris
- ensure every coroutine is associated with a cancellable job
- hunt for bugs
- check all my couroutine scopes (probably finish kotlin udacity course first)
- use retrofit instead of adamint
- test all auth refresh conditions
- fix checked chips not being saved on configuration change (saved state handle? livedata?)
- figure out when to use liveData builder
- ask adamint team why they use GlobalScope for requests instead of a scope with Dispatchers.IO
- using Spotify auth SDK, if user does not have Spotify downloaded, move them to the download page
- figure out why auth is requested on SelectGenresFragment right after requesting it from ConnectSpotifyFragment
- switch automatic backup to be true again in AndroidManifest
- finish navigation udacity course, with stuff like animated transitions
- improve Spotify error handling -- just used cached data and notify them of error with option to retry connection/request, 
    instead of reverting their current session to Connect Spotify screen (but fall back to Connect Spotify screen if no cached data)
- follow <application> IDE suggestion in AndroidManifest.xml (add backup info xml)
- give "internal" modifier to Dagger stuff?
- determine which fields should be dependency injected
- figure out when things should be @Singletons, and when that's bad practice (e.g. memory leaks)
- check Dagger efficiency (going back from results to select screen feels slower than before) -- or maybe that's always been like of tht because of lots of views for genre options, and I need a RecyclerView
- think of a better way to handle Spotify request errors, because redirecting to ConnectSpotifyFragment will only force trying auth SDK again, not trying API request again
- debug skipped frames after running auth on ConnectSpotifyFragment, before transition to SelectGenresFragment (too much work on main thread -- probably instantiating things, maybe Dagger, could be the runBlocking{} call in adamint API initialization)
- generally organize build.gradle files
- use adamint snapshot or newer version which allows for custom token refresh
- remove SpotifyAccessToken class after migrating away from adamint since we don't need the expiry from AuthDispatcher call anymore, just the token string
- notify that updateTokenWith will always fail with auth exception because expiresAt hasn't changed
- remove `freeCompilerArgs = ["-XXLanguage:+NewInference"]` from build.gradle if Kotlin compiler has issues with experimental flag
- fix bug where list_item_genre_result layout height increases while scrolling to something with long album title/artist (enforcing a fixed # of lines for all title/artist text should do the trick)
- ensure all coroutines have timeout handlers, cancel handlers, error handlers, an appropriate hierarchy, context, scope (never empty) etc.
- read (AND APPLY!) app remote Spotify SDK & Android lifecycle doc
- add a method to SpotifyRequestDispatcher to subscribe to new auth tokens (RxKotlin?)
- tackle every single edge case, with unit tests, especially with auth and networking and caching and coroutines and syncing all data, etc
- handle Spotify app communication edge cases (e.g. uninstalled during session, new user logged in, not installed on Connect Spotify fragment)
- have different options for sorting results (e.g. play frequency/familiarity, date added, artist alphabetical, album alphabetical, num results for genres)
- if user has a queue going when they play an album, add an option to clear queue, move it to after album, or cancel album play
- refresh genre options screen by swiping down at the top of the scroll view
- figure out why genre options data isn't refreshing when navigating back from results screen (it should, or at least display old data while showing the cache)
- when many genres/albums are selected for Browse Results screen, start populating data as it comes in instead of all at once to improve loading time
- read through open tabs of Android developer documentation and general ordered walkthroughs on left rail
- move everything from activity/fragments to viewmodels if it can/should be (e.g. spotify app remote subscriptions?)
- test ConnectSpotifyFragment's coroutine scope by stopping fragment (but not destroying) in the middle of auth and see if coroutine still completes (navigation happens? auth cancelled exception?)
- run IntelliJ code analysis
- add "Now playing" bar at bottom of BrowseResultsFragment which updates with PlayerState and redirects to Spotify when tapped

## Priorities

1. Get to MVP (implement responsive remote control in Browse Results screen, as well as skeleton screen and better results UI spacing)
2. Address all in-code TODOs
3. Add Dagger subcomponents and scopes
4. Add Room caching and SpotifyRepository
5. Address all the above TODOs in this file
6. Add thorough unit tests
7. Document everything
8. Improve genre options selection UI/UX
9. Alpha release w/ testers
10. Familiarity filters feature
11. Get feedback and more testers from r/androiddev
12. Work on other features