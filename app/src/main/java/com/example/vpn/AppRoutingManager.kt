package com.example.vpn

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.example.logs.LogRepository

data class AppItem(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean
)

object AppRoutingManager {
    fun getInstalledApps(context: Context): List<AppItem> {
        val appsList = mutableListOf<AppItem>()
        val pm = context.packageManager
        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in packages) {
                // Return only user launchable or typical apps to clean up lists
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appName = appInfo.loadLabel(pm).toString()
                val packageName = appInfo.packageName
                val icon = try {
                    appInfo.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                appsList.add(AppItem(appName, packageName, icon, isSystem))
            }
        } catch (e: Exception) {
            LogRepository.e("AppRoutingManager", "Error querying packages: ${e.message}")
        }
        return appsList.sortedBy { it.appName }
    }
}
