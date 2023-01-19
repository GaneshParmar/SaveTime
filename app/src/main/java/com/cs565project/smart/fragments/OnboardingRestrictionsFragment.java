package com.cs565project.smart.fragments;


import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.Category;
import com.cs565project.smart.service.AppMonitorService;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.DbUtils;
import com.cs565project.smart.util.PreferencesHelper;
import com.cs565project.smart.util.UsageStatsUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.cs565project.smart.service.AppMonitorService.ThreshHoldEntertainmentTime;
import static com.cs565project.smart.service.AppMonitorService.defaultThresholdEnterTime;
import static com.cs565project.smart.util.DbUtils.KEY_APPS_UPDATED_IN_DB;

import io.github.deweyreed.scrollhmspicker.ScrollHmsPicker;

/**
 * Fragment to get user's restriction preferences during onboarding.
 */
public class OnboardingRestrictionsFragment extends Fragment {

    private AppDao dao;
    private ProgressBar loading;
    private GridView appGrid;

    private ScrollHmsPicker scrollHmsPicker;
    private Set<AppDetails> selectedApps;

    private long userPreferenceEnterTime;

    private Handler myHandler = new Handler();

    private Runnable loadData = new Runnable() {
        @Override
        public void run() {
            Context c = getContext();
            if (c == null) return;
            DbUtils.populateAppDetailsInDb(c);
            while(!PreferencesHelper.getBoolPreference(c, KEY_APPS_UPDATED_IN_DB, false)) {
                try {
                    Log.d("Test",""+"Inside thread sleep");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.d("Test",""+"Outside thread Sleep");


            dao = AppDatabase.getAppDatabase(c).appDao();
            // Populate our grid with most used apps from last week.
            List<UsageStatsUtil.ForegroundStats> mostUsedApps = new UsageStatsUtil(c).getMostUsedAppsLastWeek();
            int appListSize = Math.min(30, mostUsedApps.size());
            List<String> packagesToFetch = new ArrayList<>(appListSize);
            for (int i = 0; i < appListSize; i++) {
                packagesToFetch.add(mostUsedApps.get(i).getPackageName());
            }

            List<AppDetails> appDetails = AppDatabase.getAppDatabase(c).appDao().getAppDetails(packagesToFetch);
            List<AppInfo> appInfos = new ArrayList<>(appDetails.size());
            boolean[] selected = new boolean[appDetails.size()];
            for (int i = 0, appDetailsSize = appDetails.size(); i < appDetailsSize; i++) {
                AppDetails details = appDetails.get(i);
                appInfos.add(new AppInfo(details.getPackageName(), c));
                selected[i] = false;
            }
            selectedApps = new HashSet<>();

            List<String> preSelectedApps= dao.getEntertainmentApps();
            List<AppDetails> preSelectedAppDetails = AppDatabase.getAppDatabase(c).appDao().getAppDetails(preSelectedApps);



            Log.d("Test",""+"Now entering myHandler Post");
            myHandler.post(() -> {
                Log.d("Test",""+"Entered myHandler Post");


                loading.setVisibility(View.GONE);
                scrollHmsPicker.setVisibility(View.VISIBLE);
                appGrid.setVisibility(View.VISIBLE);

                int pos=0;



                for(AppDetails appDetails1:appDetails){
                    Log.d("Preselected App Out",appDetails1.getAppName());

                    Log.d("Present packages "," "+preSelectedApps.toString());

                    Log.d("Present Out",""+preSelectedApps.contains(appDetails1.getPackageName()));

                    if(preSelectedApps.contains(appDetails1.getPackageName())){


                        Log.d("Preselected App",appDetails1.getAppName());
                        selected[pos]=true;
                        selectedApps.add(appDetails1);

                    }
                    pos++;

                }

                appGrid.setAdapter(new AppsAdapter(appDetails, appInfos,selected));
                appGrid.setOnItemClickListener((parent, view, position, id) -> {
                    if (view instanceof ImageView) {
                        ImageView imageView = (ImageView) view;
                        if (selected[position]) {
                            selectedApps.remove(appDetails.get(position));
                            selected[position] = false;
                            imageView.setColorFilter(Color.argb(200,237, 231, 225));
                        } else {
                            selectedApps.add(appDetails.get(position));
                            selected[position] = true;
                            imageView.setColorFilter(null);
                        }
                    }
                });
            });
        }
    };

    public OnboardingRestrictionsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_onboarding_restrictions, container, false);
        loading = root.findViewById(R.id.loading_progress_bar);
        appGrid = root.findViewById(R.id.apps_grid);

        //added 10/01/2022
        //scrolling time selector
        scrollHmsPicker=root.findViewById(R.id.scrollHmsPicker);


        loading.setVisibility(View.VISIBLE);
        appGrid.setVisibility(View.GONE);
        scrollHmsPicker.setVisibility(View.GONE);
        Executors.newSingleThreadExecutor().execute(loadData);
        return root;
    }

    public void saveData() {
        new Thread() {
            @Override
            public void run() {

                userPreferenceEnterTime=UsageStatsUtil.getMilliSeconds(scrollHmsPicker.getHours(),scrollHmsPicker.getMinutes(),scrollHmsPicker.getSeconds());
                PreferencesHelper.setPreference(getContext(), AppMonitorService.UserPreferenceEntertainmentTime,userPreferenceEnterTime);

                PreferencesHelper.setPreference(getContext(),ThreshHoldEntertainmentTime,userPreferenceEnterTime);
                Context c = getActivity();
                if (c == null) return;

                // Removing all entertainment apps before adding new entertainment apps
                dao.removeAllEntertainMentApps();
                Set<String> categories = new HashSet<>();
                for (AppDetails appDetails: selectedApps) {
                    categories.add(appDetails.getCategory());

                    dao.updateAppDetails(new AppDetails(appDetails.getPackageName(),appDetails.getAppName(),appDetails.getCategory(),appDetails.getThresholdTime(),true));
                }

                for (String category: categories) {
                    Log.d("Inserting", category);

                    //added 08/01/2022
                    //new parameter added !
                    dao.insertCategory(new Category(category, true));


                }


            }
        }.start();
    }

    private static class AppsAdapter extends BaseAdapter {

        List<AppDetails> appDetails;
        List<AppInfo> appInfos;
        boolean [] selected;


        public AppsAdapter(List<AppDetails> appDetails, List<AppInfo> appInfos,boolean[] selected) {
            this.appDetails = appDetails;
            this.appInfos = appInfos;
            this.selected=selected;
        }

        @Override
        public int getCount() {
            return appDetails.size();
        }

        @Override
        public Object getItem(int position) {
            return appDetails.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null || !(convertView instanceof ImageView)) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item_app, parent, false);
            }

            ((ImageView) convertView).setImageDrawable(appInfos.get(position).getAppIcon());
            ((ImageView) convertView).setColorFilter(Color.argb(200,237, 231, 225));

            if(selected[position]){
                ((ImageView) convertView).setColorFilter(null);
            }


            return convertView;
        }
    }
}
