package io.libzy.persistence.prefs

import android.content.Context
import android.content.SharedPreferences

fun Context.getSharedPrefs(): SharedPreferences = getSharedPreferences("spotify_preferences", Context.MODE_PRIVATE)
