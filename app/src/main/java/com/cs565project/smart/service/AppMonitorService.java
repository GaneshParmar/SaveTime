package com.cs565project.smart.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import com.cs565project.smart.MainActivity;
import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;
import com.cs565project.smart.fragments.GeneralSettingsFragment;
import com.cs565project.smart.recommender.ActivityRecommender;
import com.cs565project.smart.recommender.NewsItem;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.DbUtils;
import com.cs565project.smart.util.PreferencesHelper;
import com.cs565project.smart.util.UsageStatsUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The main service of the app. It updates the database with usage data, monitors the current app
 * (and blocks it if necessary), and updates news items, recommended activities etc.
 */
public class AppMonitorService extends Service {

    private static final String CHANNEL_ID = "persistent";

    //added 08/01/2022
    public static final String ThreshHoldEntertainmentTime="thresoldEnterTime";
    public static final String leftEntertainmentTime="leftEnterTime";
    public static final String TodaysDate="todayDate";
    public static final long defaultThresholdEnterTime=5400000;
    public long diffInTime;

    //10/01/2023
    public static final String UserPreferenceEntertainmentTime="UserPreferenceEntertainmentTime";

    public static final String ACTION_START_SERVICE = "start_service";
    public static final String ACTION_STOP_SERVICE = "stop_service";
    public static final String ACTION_TOGGLE_SERVICE = "toggle_service";
    public static final String ACTION_BYPASS_BLOCK = "bypass_block";
    public static final String KEY_SERVICE_RUNNING = "service_running";


    private static final int CYCLE_DELAY = 200;
    private static final int DATA_UPDATE_DELAY = 20000;
    private static final int NEWS_UPDATE_DELAY = (int) DateUtils.HOUR_IN_MILLIS;


    //All data related to entertainment time
    private String todayDate;
    private String todaysDateString;
    private Long threasholdEnterTainementTime;
    private Long leftEnterTainmentTime;
    long userPreferenceEntertainemntTime;

    private UsageStatsUtil myUsageStatsUtil;
    private BlockOverlay myOverlay;
    private boolean isRunning = false;
    private String notificationTitle = "SMART is starting up";
    private String notificationText = "You day summary will appear here";
    private boolean updateNotification = false;
    private String myCurrentApp;
    private boolean myBlockBypassed = false;
    private boolean entertainmentBlock;

    private Executor myExecutor = Executors.newFixedThreadPool(3);
    private Handler myHandler = new Handler();
    private Runnable myBgJob = new Runnable() {
        @Override
        public void run() {


            //added 08/01/2023
            todayDate=PreferencesHelper.getStringPreference(getApplicationContext(),TodaysDate,"Not Recorded");
            threasholdEnterTainementTime=PreferencesHelper.getLongPreference(getApplicationContext(),ThreshHoldEntertainmentTime,defaultThresholdEnterTime);
            leftEnterTainmentTime=PreferencesHelper.getLongPreference(getApplicationContext(),leftEntertainmentTime,0);
            // added 10/01/2023
            userPreferenceEntertainemntTime=PreferencesHelper.getLongPreference(AppMonitorService.this,UserPreferenceEntertainmentTime,defaultThresholdEnterTime);

//            boolean entered=PreferencesHelper.getBoolPreference(getApplicationContext(),"entered22",false);
//            Log.d("Check date equal ",""+checkIfDateChanged());
//
//            Log.d("Check date equal 1",todaysDateString+"" );
//            Log.d("Check date equal 1",todayDate+"" );

            if(checkIfDateChanged()){



                PreferencesHelper.setPreference(AppMonitorService.this,ThreshHoldEntertainmentTime,defaultThresholdEnterTime);
                threasholdEnterTainementTime=PreferencesHelper.getLongPreference(getApplicationContext(),UserPreferenceEntertainmentTime,defaultThresholdEnterTime);

                PreferencesHelper.setPreference(getApplicationContext(),TodaysDate,todaysDateString);
                PreferencesHelper.setPreference(getApplicationContext(),leftEntertainmentTime,threasholdEnterTainementTime);
//                setThresholdEntertainmentTime();

                diffInTime=calculateDiffInTime(-2,-1);

                if(!(diffInTime==0)){

                    threasholdEnterTainementTime=getThresholdEntertainmentTime(diffInTime);

                }

                PreferencesHelper.setPreference(AppMonitorService.this,ThreshHoldEntertainmentTime,threasholdEnterTainementTime);

            }





//            Log.d("Check Usage",""+UsageStatsUtil.formatDuration(diffInTime,getApplicationContext()));

            String currentApp = myUsageStatsUtil.getForegroundApp();

            // Adding/removing overlay should happen in the main thread.
            if (currentApp != null && shouldBlockApp(currentApp)) {
                if (!myBlockBypassed || !currentApp.equals(myCurrentApp)) {
                    AppDao dao = AppDatabase.getAppDatabase(AppMonitorService.this).appDao();
                    AppDetails details = dao.getAppDetails(currentApp);

                    myOverlay.setApp(details, new AppInfo(currentApp, getApplicationContext()).getAppIcon(),entertainmentBlock,UsageStatsUtil.formatDuration( getThresholdEntertainmentTime(calculateDiffInTime(-1,0)),AppMonitorService.this));
                    if (!myOverlay.isVisible()) {
                        myHandler.post(myShowOverlay);
                    }
                }

            } else if (currentApp != null && !"android".equals(currentApp) && myOverlay.isVisible()) {
                myHandler.post(myHideOverlay);
            }

            myCurrentApp = currentApp;

            if (isRunning) {
                myHandler.postDelayed(myBgJobStarter, CYCLE_DELAY);
            }
        }

        private Long getThresholdEntertainmentTime(long diffInTime) {

            if(diffInTime==0){
                return userPreferenceEntertainemntTime;
            }
            else if(diffInTime>0){

                return threasholdEnterTainementTime+(diffInTime/3);

            }
            else{

                return threasholdEnterTainementTime-(2*(Math.abs(diffInTime)/3));
            }

        }

        private long calculateDiffInTime(int x,int y) {



            AppDao dao = AppDatabase.getAppDatabase(getApplicationContext()).appDao();


            final Calendar cal = Calendar.getInstance();

            cal.add(Calendar.DATE, y);
            long yesterdayDay=cal.getTimeInMillis();

            final Calendar cal1 = Calendar.getInstance();
            cal1.add(Calendar.DATE,  x);
            long daybeforeyesterdayDate=cal1.getTimeInMillis();



            long yesterDayUsage=dao.getTotalUsageTime(new Date(UsageStatsUtil.getStartOfDayMillis(new Date(yesterdayDay))));
            long dayBeforeYesterdayUsage=dao.getTotalUsageTime(new Date(UsageStatsUtil.getStartOfDayMillis(new Date(daybeforeyesterdayDate))));


            if(dayBeforeYesterdayUsage==0){
                dayBeforeYesterdayUsage=yesterDayUsage;
            }

            return dayBeforeYesterdayUsage - yesterDayUsage;

        }
    };

    private boolean checkIfDateChanged() {

        todaysDateString=""+new java.util.Date().getDate();

//        Log.d("Check date equal ",todaysDateString+"" );
//        Log.d("Check date equal ",todayDate+"" );

//
        return !(todaysDateString.equals(todayDate));

    }

    private Runnable myBgJobStarter = new BgStarter(myBgJob);

    private Runnable myUpdateDb = new Runnable() {
        @Override
        public void run() {
            Log.d("SMART", "Updating app usage data");
            List<Pair<AppDetails, UsageStatsUtil.ForegroundStats>> restrictedAppsStatus =
                    DbUtils.updateAndGetRestrictedAppsStatus(AppMonitorService.this);
            long timeInRestrictedApps = 0;
            int exceededApps = 0;
            for(Pair<AppDetails, UsageStatsUtil.ForegroundStats> appStatus : restrictedAppsStatus) {
                timeInRestrictedApps += appStatus.second.getTotalTimeInForeground();
                if (appStatus.second.getTotalTimeInForeground() >= appStatus.first.getThresholdTime()) {
                    exceededApps += 1;
                }
            }

            // Update notification title and text.
            notificationText = String.format(Locale.getDefault(), getString(R.string.time_in_restricted_apps),
                    UsageStatsUtil.formatDuration(timeInRestrictedApps, AppMonitorService.this));
            notificationTitle = (exceededApps <= 0) ?
                    "Good job!" :
                    String.format(Locale.getDefault(), getString(R.string.limit_exceeded), exceededApps);
            updateNotification = true;

            if (isRunning) {
                myHandler.postDelayed(myDbJobStarter, DATA_UPDATE_DELAY);
            }
        }
    };
    private Runnable myDbJobStarter = new BgStarter(myUpdateDb);

    private Runnable myUpdateNews = new Runnable() {
        @Override
        public void run() {
            Log.d("SMART", "Updating news");

            List<NewsItem> recommendedNews = NewsItem.getRecommendedNews(AppMonitorService.this);
            if (recommendedNews != null && !recommendedNews.isEmpty()) {

                Log.d("News","is not empty");
                myOverlay.setNewsItems(recommendedNews);
            }
            else{
                Log.d("News","is empty");

            }
            myOverlay.setActivities(ActivityRecommender.getRecommendedActivities(
                    AppDatabase.getAppDatabase(AppMonitorService.this).appDao().getRecommendationActivities()
            ));
            if (isRunning) {
                myHandler.postDelayed(myNewsJobStarter, NEWS_UPDATE_DELAY);
            }
        }
    };
    private Runnable myNewsJobStarter = new BgStarter(myUpdateNews);

    private Runnable myShowOverlay = new Runnable() {
        @Override
        public void run() {
            myOverlay.scrollToStart();
            myOverlay.execute();
        }
    };
    private Runnable myHideOverlay = new Runnable() {
        @Override
        public void run() {
            myOverlay.remove();
        }
    };

    public AppMonitorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myUsageStatsUtil = new UsageStatsUtil(this);
        myOverlay = new BlockOverlay(this, (WindowManager) getSystemService(WINDOW_SERVICE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();
            if (ACTION_START_SERVICE.equals(action)) {
                start();
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                stop();
            } else if (ACTION_TOGGLE_SERVICE.equals(action)) {
                if (isRunning) stop(); else start();
            } else if (ACTION_BYPASS_BLOCK.equals(action)) {
                myBlockBypassed = true;
                myOverlay.remove();
            }
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private void stop() {
        isRunning = false;
        PreferencesHelper.setPreference(this, KEY_SERVICE_RUNNING, false);
        myHandler.removeCallbacks(myBgJobStarter);
        myHandler.removeCallbacks(myDbJobStarter);
        myHandler.removeCallbacks(myNewsJobStarter);
    }

    private void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        PreferencesHelper.setPreference(this, KEY_SERVICE_RUNNING, true);
        myHandler.post(myBgJobStarter);
        myHandler.post(myDbJobStarter);
        myHandler.post(myNewsJobStarter);
    }

    private void postNotification(String title, String text){
        createNotificationChannel();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = notificationBuilder.setContentTitle(title).
                setSmallIcon(R.mipmap.ic_launcher).setContentText(text).setPriority(NotificationCompat.PRIORITY_MIN).
                setContentIntent(pi).build();
        startForeground(1, n);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_MIN);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    private boolean shouldBlockApp(String packageName) {

        if (!PreferencesHelper.getBoolPreference(this,
                GeneralSettingsFragment.PREF_ALLOW_APP_BLOCK.getKey(), true)) {
            return false;
        }
        AppDao dao = AppDatabase.getAppDatabase(AppMonitorService.this).appDao();
        AppDetails details = dao.getAppDetails(packageName);
        DailyAppUsage appUsage = dao.getAppUsage(packageName, new Date(UsageStatsUtil.getStartOfDayMillis(new Date())));

        entertainmentBlock=false;

        if(details!=null && details.getIsEntertainmentApp()){

            entertainmentTimeCheck(dao.getEntertainmentApps(),dao);
            Log.d("Using Enter..App",details.getAppName()+" Time Left "+UsageStatsUtil.formatDuration(leftEnterTainmentTime,AppMonitorService.this));


            if(leftEnterTainmentTime==0){
                entertainmentBlock=true;
                return true;
            }

        }

        return details != null && appUsage != null &&
                details.getThresholdTime() > 0 && details.getThresholdTime() < appUsage.getDailyUseTime();

    }

    private void entertainmentTimeCheck(List<String> entertainmentApps,AppDao dao) {

        long entertainmentAppTimeUsed=0;
        for(String enterApp:entertainmentApps){





            DailyAppUsage appUsage=dao.getAppUsage(enterApp,new Date(UsageStatsUtil.getStartOfDayMillis(new Date())));
            if(appUsage!=null){

                entertainmentAppTimeUsed+=appUsage.getDailyUseTime();

            }


            Log.d("Using Enter..App ",enterApp);
            Log.d("EntertainmentTimeLeft",""+leftEnterTainmentTime);

        }

        Log.d("Threshold Time is ",""+UsageStatsUtil.formatDuration(threasholdEnterTainementTime,getApplicationContext()));
        Log.d("Entertainers Time",""+UsageStatsUtil.formatDuration(entertainmentAppTimeUsed,getApplicationContext()));

        long tempLeftEnterTime=(threasholdEnterTainementTime-entertainmentAppTimeUsed)<0?0:(threasholdEnterTainementTime-entertainmentAppTimeUsed);

        PreferencesHelper.setPreference(AppMonitorService.this,leftEntertainmentTime,tempLeftEnterTime);

        leftEnterTainmentTime=PreferencesHelper.getLongPreference(AppMonitorService.this,leftEntertainmentTime,0);
        //
//        return threasholdEnterTainementTime<=entertainmentAppTimeUsed;

    }

    private class BgStarter implements Runnable {

        private Runnable bgJob;

        BgStarter(Runnable bgJob) {
            this.bgJob = bgJob;
        }

        @Override
        public void run() {
            myExecutor.execute(bgJob);
            if (updateNotification) {
                postNotification(notificationTitle, notificationText);
                updateNotification = false;
            }
        }
    }
}
