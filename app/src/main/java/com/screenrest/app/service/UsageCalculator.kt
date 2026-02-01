package com.screenrest.app.service

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageCalculator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager: UsageStatsManager? = 
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    
    private val powerManager: PowerManager? = 
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    
    fun getContinuousUsageSinceTimestamp(timestamp: Long): Long {
        if (usageStatsManager == null) return 0L
        
        val currentTime = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            timestamp,
            currentTime
        )
        
        return stats?.sumOf { it.totalTimeInForeground } ?: 0L
    }
    
    fun isScreenOn(): Boolean {
        return powerManager?.isInteractive ?: true
    }
    
    fun getTodayUsage(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        return getContinuousUsageSinceTimestamp(calendar.timeInMillis)
    }
}
