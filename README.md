# Libzy
An app to narrow down a large Spotify library into suggested albums, based on the genre/familiarity preferences of your current mood.

## To-Do
- flesh out architecture
- incorporate unit testing
- clean up gradle.build
- improve button styling, make responsive to presses
- make "Connect Spotify" button on-brand
- decide on minimum and target SDK
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
- debug "pending vsync event" warning
- determine whether my project actually needs dependency injection (are there any nested dependencies? are there any shared dependencies?)
- create a develop branch and start using feature/bugfix branches