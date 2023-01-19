package com.cs565project.smart.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.Category;
import com.cs565project.smart.db.entities.DailyAppUsage;
//import com.cs565project.smart.db.entities.MoodLog;
import com.cs565project.smart.db.entities.RecommendationActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Data access object which provides queries, inserts, updates and deletes in the app database.
 */
@Dao
public interface AppDao {
    @Query("SELECT * FROM DailyAppUsage WHERE packageName = (:packageName) AND date = (:date) LIMIT 1")
    DailyAppUsage getAppUsage(String packageName, Date date);

    @Query("SELECT * FROM DailyAppUsage WHERE date = (:date)")
    List<DailyAppUsage> getAppUsage(Date date);

    @Query("SELECT * FROM DailyAppUsage WHERE date >= (:startDate) AND date <= (:endDate)")
    List<DailyAppUsage> getAppUsage(Date startDate, Date endDate);

    @Query("SELECT * FROM DailyAppUsage WHERE packageName = (:packageName)")
    List<DailyAppUsage> getAppUsage(String packageName);

    @Query("SELECT sum(dailyUseTime) FROM DailyAppUsage WHERE date = (:date) And packageName != 'com.android.settings'")
    long getTotalUsageTime(Date date);

    @Query("SELECT MIN(date) FROM DailyAppUsage")
    Date getUsageDataStartDate();

    @Query("SELECT * FROM AppDetails")
    List<AppDetails> getAppDetails();

    @Query("SELECT packageName FROM AppDetails")
    List<String> getAppPackageNames();

    @Query("SELECT * FROM AppDetails WHERE packageName IN (:packageNames)")
    List<AppDetails> getAppDetails(List<String> packageNames);

    @Query("SELECT * FROM AppDetails WHERE packageName = (:packageName) LIMIT 1")
    AppDetails getAppDetails(String packageName);

    @Query("SELECT * FROM AppDetails WHERE thresholdTime > -1")
    List<AppDetails> getRestrictedAppDetails();

//    @Query("SELECT * FROM MoodLog WHERE dateTime = (:date)")
//    MoodLog getMoodLog(Date date);
//
//    @Query("SELECT * FROM MoodLog WHERE dateTime >= (:startDate) AND dateTime <= (:endDate)")
//    List<MoodLog> getMoodLog(Date startDate, Date endDate);
//
//    @Query("SELECT * FROM MoodLog")
//    List<MoodLog> getAllMoodLog();

    @Query("SELECT * FROM RecommendationActivity")
    List<RecommendationActivity> getRecommendationActivities();

    @Query("SELECT * FROM RecommendationActivity WHERE activityType = (:activityType)")
    List<RecommendationActivity> getRecommendationActivities(String activityType);

    //added 08/01/2022
    //Query for returning
    @Query("SELECT packageName FROM APPDETAILS WHERE isEntertainmentApp = 1")
    List<String> getEntertainmentApps();

    @Query("SELECT * FROM Category")
    List<Category> getCategories();

    @Query("SELECT name FROM Category WHERE shouldRestrict = (:shouldRestrict)")
    List<String> getCategories(boolean shouldRestrict);

    @Query("UPDATE AppDetails Set isEntertainmentApp = 0 WHERE isEntertainmentApp = 1")
    void removeAllEntertainMentApps();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAppUsage(DailyAppUsage... appUsages);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAppUsage(List<DailyAppUsage> appUsages);
    @Insert
    void insertAppDetails(AppDetails... apps);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAppDetails(List<AppDetails> apps);
//    @Insert
//    void insertMood(MoodLog... mood);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecommendationActivity(RecommendationActivity activity);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategory(Category category);

    @Update
    void updateAppDetails(AppDetails... apps);
    @Update
    void updateAppUsage(DailyAppUsage... appUsages);
    @Update
    void updateRecommendationActivity(RecommendationActivity activity);



    
    @Delete
    void deleteAppUsage(DailyAppUsage... appUsages);
    @Delete
    void deleteAppDetails(AppDetails... apps);
    @Delete
    void deleteRecommendationActivities(List<RecommendationActivity> activities);
}
