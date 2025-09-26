package com.example.mad_assignment.data.repository

import com.example.mad_assignment.data.datasource.ActivityDataSource
import com.example.mad_assignment.data.model.Activity
import javax.inject.Inject

class ActivityRepository @Inject constructor(
    private val activityDataSource: ActivityDataSource
) {
    suspend fun createActivity(activity: Activity): Boolean {
        return activityDataSource.createActivity(activity).isSuccess
    }

    suspend fun getRecentActivities(): List<Activity> {
        return activityDataSource.getRecentActivities().getOrElse { emptyList() }
    }
}