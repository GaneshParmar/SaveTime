package com.cs565project.smart.db.entities;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Database entity to hold details of an app.
 */
@SuppressWarnings("unused")
@Entity
public class AppDetails {
    @PrimaryKey
    @NonNull
    private String packageName;

    private String appName;
    private String category;

    private int thresholdTime;

    public AppDetails(@NonNull String packageName, String appName, String category, int thresholdTime) {
        this.packageName = packageName;
        this.appName = appName;
        this.category = category;
        this.thresholdTime = thresholdTime;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public String getCategory() {
        return category;
    }

    public int getThresholdTime() {
        return thresholdTime;
    }
}
