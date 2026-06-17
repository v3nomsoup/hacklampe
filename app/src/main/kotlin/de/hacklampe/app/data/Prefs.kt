package de.hacklampe.app.data

import android.content.Context

/** Zentraler Zugriff auf gespeicherte Einstellungen. */
object Prefs {
    private const val FILE = "hacklampe_prefs"
    private const val KEY_SENSITIVITY = "sensitivity"
    private const val KEY_AUTOSTART = "autostart"

    const val DEFAULT_SENSITIVITY = 5

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getSensitivity(context: Context): Int =
        prefs(context).getInt(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)

    fun setSensitivity(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_SENSITIVITY, value).apply()

    fun isAutoStart(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTOSTART, false)

    fun setAutoStart(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_AUTOSTART, value).apply()
}
