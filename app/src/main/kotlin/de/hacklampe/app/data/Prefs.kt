package de.hacklampe.app.data

import android.content.Context

/** Zentraler Zugriff auf gespeicherte Einstellungen. */
object Prefs {
    private const val FILE = "hacklampe_prefs"
    private const val KEY_SENSITIVITY = "sensitivity"
    private const val KEY_AUTOSTART = "autostart"
    private const val KEY_CALIBRATED = "calibrated"
    private const val KEY_CAL_PEAK = "cal_peak"
    private const val KEY_CAL_VALLEY = "cal_valley"

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

    fun isCalibrated(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CALIBRATED, false)

    fun getCalibratedPeak(context: Context): Float =
        prefs(context).getFloat(KEY_CAL_PEAK, 0f)

    fun getCalibratedValley(context: Context): Float =
        prefs(context).getFloat(KEY_CAL_VALLEY, 0f)

    fun setCalibration(context: Context, peak: Float, valley: Float) =
        prefs(context).edit()
            .putBoolean(KEY_CALIBRATED, true)
            .putFloat(KEY_CAL_PEAK, peak)
            .putFloat(KEY_CAL_VALLEY, valley)
            .apply()

    fun clearCalibration(context: Context) =
        prefs(context).edit().putBoolean(KEY_CALIBRATED, false).apply()
}
