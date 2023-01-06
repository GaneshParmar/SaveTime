package com.cs565project.smart.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Database entity to hold details of activities which can be recommended to the user.
 */
@Entity
public class RecommendationActivity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String activityName;
    public boolean isSet;
    public int timeOfDay;
    public String activityType;

    public RecommendationActivity(String activityName, boolean isSet, int timeOfDay, String activityType) {
        this.activityName = activityName;
        this.isSet = isSet;
        this.timeOfDay = timeOfDay;
        this.activityType = activityType;
    }

}
