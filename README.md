# Libzy
An app to narrow down a large Spotify library into suggested albums, based on the genre/familiarity preferences of your current mood.

## Planned Features
- ability to hide recently played genres or top genres, to try something new
- integration with spotify remote SDK to play/queue results directly from Libzy
- ability to remove saved albums from library directly from Libzy
- ability to remove album's association with a certain genre so it not longer appears in results for that genre
- ability to visit the Spotify page for an album without playing it
- separate page for individual albums, showing genre data (with options to browse those genres or add them to your selected categories?) & other info available from API like audio analysis of tracks
- support for searching by individual tracks instead of only albums
- display 1-3 sample albums for each genre in the list on SelectGenreActivity
- list currently selected genres at top of SelectGenreActivity
- "discover more from this genre" button to the right of the genre header in BrowseAlbumsActivity (brings them to an activity similar to BrowseAlbums but with one vertical scrolling grid list of albums, and maybe a description of the genre pulled from google/wikipedia below the genre header)