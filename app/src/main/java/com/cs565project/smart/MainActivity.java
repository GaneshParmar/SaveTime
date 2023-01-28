package com.cs565project.smart;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

//import com.cs565project.smart.fragments.LogMoodFragment;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.fragments.ReportsFragment;
import com.cs565project.smart.fragments.RestrictionsFragment;
import com.cs565project.smart.service.AppMonitorService;
import com.cs565project.smart.util.PreferencesHelper;
import com.google.android.material.tabs.TabLayout;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * The main entry point into the app. We have 2 tabs, managed by a TabLayout. Corresponding views
 * are displayed by a ViewPager, to whom appropriate fragments are supplied by
 * {@link MainTabsAdapter}.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String KEY_FIRST_START = "firstStart";
    private final long defaultEntertainmentTime=5400000;

    private androidx.drawerlayout.widget.DrawerLayout myDrawer;
    private TextView myToggleButton;
    private boolean isServiceRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start our background service. Without this nothing will work!
        startService(new Intent(this, AppMonitorService.class).setAction(AppMonitorService.ACTION_START_SERVICE));

        //  Declare a new thread to do a preference check and start intro activity if required.
        Thread t = new Thread(() -> {

            //  Create a new boolean and preference and set it to true
            boolean isFirstStart = PreferencesHelper.getBoolPreference(MainActivity.this, KEY_FIRST_START, true);

            //  If the activity has never started before...
            if (isFirstStart) {

                //  Launch app intro
                final Intent i = new Intent(MainActivity.this, IntroActivity.class);

                runOnUiThread(() -> {
                    startActivity(i);
                    finish();
                });
            }
        });

        // Start the thread
        t.start();

        setUpNavigation();

        // Setup the tabs.
        TabLayout tabs = findViewById(R.id.tabs);
        ViewPager pager = findViewById(R.id.main_viewpager);

        MainTabsAdapter adapter = new MainTabsAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        tabs.setupWithViewPager(pager);


        //Test

        //testing if i select apps as entertainment their app details change or not

//        new Thread() {
//            @Override
//            public void run() {
//                AppDao appDao= AppDatabase.getAppDatabase(getApplicationContext()).appDao();
//
//                List<AppDetails> appDetails=appDao.getAppDetails();
//
//                for(AppDetails appDetails1:appDetails){
//                    Log.d("Test Entertainment ", appDetails1.getAppName()+" "+appDetails1.getIsEntertainmentApp());
//                }
//
//            }
//        }.start();

//        Executors.newSingleThreadExecutor().execute(showData);
    }


    /**
     * Setup our toolbar and navigation drawer.
     */
    private void setUpNavigation() {

        // First we set our toolbar and add toggle button for nav drawer.
        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        myDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, myDrawer, toolbar, R.string.app_name, R.string.app_name);
        myDrawer.addDrawerListener(toggle);
        toggle.syncState();

        // Setup the menu items inside the nav drawer. Going with individual elements instead of a
        // menu since this will allow easier dynamic changes.
        isServiceRunning = PreferencesHelper.getBoolPreference(this, AppMonitorService.KEY_SERVICE_RUNNING, true);
        TextView logout = findViewById(R.id.logout),
                settings = findViewById(R.id.settings),
                viewIntro = findViewById(R.id.view_intro);
        myToggleButton = findViewById(R.id.toggle_service);
        logout.setOnClickListener(this);
        settings.setOnClickListener(this);
        viewIntro.setOnClickListener(this);
        myToggleButton.setOnClickListener(this);
        setToggleDrawable();
    }

    private void setToggleDrawable() {
        Drawable toggleDrawable;
        if (isServiceRunning) {
            toggleDrawable = getDrawable(R.drawable.ic_pause);
            myToggleButton.setText(R.string.pause_service);
        } else {
            toggleDrawable = getDrawable(R.drawable.ic_play_arrow);
            myToggleButton.setText(R.string.resume_service);
        }
        myToggleButton.setCompoundDrawablesWithIntrinsicBounds(toggleDrawable, null, null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isServiceRunning = PreferencesHelper.getBoolPreference(this, AppMonitorService.KEY_SERVICE_RUNNING, true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.logout:
                Toast.makeText(this, "Logging out", Toast.LENGTH_SHORT).show();
                myDrawer.closeDrawer(androidx.core.view.GravityCompat.START);
                break;
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                myDrawer.closeDrawer(androidx.core.view.GravityCompat.START);
                break;
            case R.id.toggle_service:
                startService(new Intent(this, AppMonitorService.class)
                        .setAction(AppMonitorService.ACTION_TOGGLE_SERVICE));
                myDrawer.closeDrawer(androidx.core.view.GravityCompat.START);
                isServiceRunning = !isServiceRunning;
                setToggleDrawable();
                break;
            case R.id.view_intro:

                IntroActivity introActivity=new IntroActivity();
                introActivity.isonlyEntertainementSetting=false;
                Intent introIntent = new Intent(this, introActivity.getClass());
                startActivity(introIntent);
                myDrawer.closeDrawer(androidx.core.view.GravityCompat.START);
                break;
        }
    }

    /**
     * Adapter to populate tabs on the main activity.
     * We have 2 tabs: one to show user's activity reports, and one where the user can set blocking
     * parameters. See FRAGMENT_CLASSES to see the fragments that are returned.
     */
    public static class MainTabsAdapter extends FragmentPagerAdapter {

        private static List<Pair<Class<? extends Fragment>, String>> FRAGMENT_CLASSES = Arrays.asList(
                new Pair<Class<? extends Fragment>, String>(ReportsFragment.class, "YOUR ACTIVITY"),
                new Pair<Class<? extends Fragment>, String>(RestrictionsFragment.class, "RESTRICTIONS")
        );

        MainTabsAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            try {
                return FRAGMENT_CLASSES.get(position).first.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Error instantiating fragment", e);
            }
        }

        @Override
        public int getCount() {
            return FRAGMENT_CLASSES.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return FRAGMENT_CLASSES.get(position).second;
        }
    }

    @Override
    public void onBackPressed() {
        if (myDrawer.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            myDrawer.closeDrawer(androidx.core.view.GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void switchToTab(int index) {
        TabLayout tabLayout = findViewById(R.id.tabs);
        TabLayout.Tab tab = tabLayout.getTabAt(index);
        if (tab != null) {
            try {
                tab.select();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }
}
