package com.nibm.autocare

import android.content.Context

object SettingsManager {

    private const val PREFS = "autocare_settings"

    // S2 — Currency
    fun getCurrency(ctx: Context): String = prefs(ctx).getString("currency", "MYR") ?: "MYR"
    fun setCurrency(ctx: Context, symbol: String) = prefs(ctx).edit().putString("currency", symbol).apply()

    // S3 — Default vehicle
    fun getDefaultVehicle(ctx: Context): String = prefs(ctx).getString("default_vehicle", "") ?: ""
    fun setDefaultVehicle(ctx: Context, reg: String) = prefs(ctx).edit().putString("default_vehicle", reg).apply()

    // W3 — Service interval (km)
    fun getServiceInterval(ctx: Context): Int = prefs(ctx).getInt("service_interval_km", 5000)
    fun setServiceInterval(ctx: Context, km: Int) = prefs(ctx).edit().putInt("service_interval_km", km).apply()

    // W4 — Notification lead time (days before expiry for the first warning)
    fun getNotifDaysBefore(ctx: Context): Int = prefs(ctx).getInt("notif_days_before", 30)
    fun setNotifDaysBefore(ctx: Context, days: Int) = prefs(ctx).edit().putInt("notif_days_before", days).apply()

    // S1 — Notification toggles
    fun isServiceNotifEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean("notif_service", true)
    fun setServiceNotif(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("notif_service", on).apply()

    fun isDocNotifEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean("notif_docs", true)
    fun setDocNotif(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("notif_docs", on).apply()

    fun isWarrantyNotifEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean("notif_warranty", true)
    fun setWarrantyNotif(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("notif_warranty", on).apply()

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
