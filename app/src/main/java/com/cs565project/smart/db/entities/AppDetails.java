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

    //added 08/01/2022
    //this is the category if the app is entertaining
    public boolean isEntertainmentApp;

    public AppDetails(@NonNull String packageName, String appName, String category, int thresholdTime,boolean isEntertainmentApp) {
        this.packageName = packageName;
        this.appName = appName;
        this.category = category;
        this.thresholdTime = thresholdTime;
        this.isEntertainmentApp=isEntertainmentApp;
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

    //added 08/01/2022
    //this is the category if the app is entertaining
    public boolean getIsEntertainmentApp(){
        return isEntertainmentApp;
    }
}
