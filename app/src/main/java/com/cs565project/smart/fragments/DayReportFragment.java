package com.cs565project.smart.fragments;


import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cs565project.smart.IntroActivity;
import com.cs565project.smart.MainActivity;
import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;
//import com.cs565project.smart.db.entities.MoodLog;
import com.cs565project.smart.fragments.adapter.ChartLegendAdapter;
import com.cs565project.smart.recommender.RestrictionRecommender;
import com.cs565project.smart.service.AppMonitorService;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.DbUtils;
//import com.cs565project.smart.util.EmotionUtil;
import com.cs565project.smart.util.GraphUtil;
import com.cs565project.smart.util.PreferencesHelper;
import com.cs565project.smart.util.UsageStatsUtil;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.github.mikephil.charting.utils.ColorTemplate.rgb;

/**
 * Fragment to show per-day reports to users.
 */
public class DayReportFragment extends Fragment implements ChartLegendAdapter.OnItemClickListener, View.OnKeyListener, SetRestrictionFragment.OnDurationSelectedListener {

    private static final String EXTRA_DATE = "extra_date";
    private static final String EXTRA_CATEGORY = "category";

    private static final float PIE_HOLE_RADIUS = 90f;
    private static final float PIE_SCALE_FACTOR = 0f;

    public static final int[] PIE_COLORS = {
            rgb("#be1e2e"), rgb("#f04136"), rgb("#f15a2b"), rgb("#e2b37c"),
            rgb("#2b3890"), rgb("#37474f"), rgb("#4a148c"), rgb("#ad1457"),
            rgb("#006064"), rgb("#0d47a1"), rgb("#fdd835"), rgb("#ff1744"),
            rgb("#000000")
    };
    private static final int MAX_ENTRIES = 7;

    // References to views.
    private View myRootView;
    private PieChart myPieChart, myPieChartSecondary;
    private SwipeRefreshLayout myRefreshLayout;
    private RecyclerView myLegend;
    private Button viewMoreBtn,showEntertainmentSettingBtn;
    private TextView entertainmentTimeLeft,entertainmentTime;
    private LinearLayout entertaimentDiv;

    // Our state.
    private PieData myPieData, mySecondaryPieData;
    private List<ChartLegendAdapter.LegendInfo> myLegendInfos,myLegendInfosAtStart;
    private long myTotalUsageTime;
    private Date myDate;
    private String myCurrentCategory;
    private int myPieX, myMinimizedPieX;
    private boolean myAnimatePie;
//    private String myMood;
    //added 1/8/2023 G@nesh
    private boolean viewMore;

    //added 09/01/2023
    private long totalEntertainmentTime,entertainmentTimeLeftToday,entertainmentTimeUsed;


    // For background execution.
    private Executor myExecutor = Executors.newSingleThreadExecutor();
    private Handler myHandler = new Handler();
    private TimeInterpolator myInterpolator = new AccelerateDecelerateInterpolator();
//    private EmotionUtil myEmotionUtil;

    // Runnable to collect app usage information and update our state. Don't run in UI thread.
    private Runnable loadData = new Runnable() {
        @Override
        public void run() {
            Context c = getActivity();



            if (c == null) return;

            // Read usage info from DB.
            AppDao dao = AppDatabase.getAppDatabase(c).appDao();
            List<DailyAppUsage> appUsages = dao.getAppUsage(new Date(UsageStatsUtil.getStartOfDayMillis(myDate)));
            List<String> entertainmentApps=dao.getEntertainmentApps();

            // This map will hold the key value pairs to be inserted in the chart.
            Map<String, Long> usageMap = new HashMap<>();
            Map<String, Long> secondaryUsageMap = new HashMap<>();

            // Initialize state with defaults.
            Map<String, List<String>> subtitleInfo = new HashMap<>();
            myLegendInfos = new ArrayList<>();
            myLegendInfosAtStart=new ArrayList<>();
            myTotalUsageTime = 0;
            entertainmentTimeUsed=0;

            // Populate the usageMap.
            for (DailyAppUsage appUsage : appUsages) {
                if (c.getPackageName().equals(appUsage.getPackageName())) { continue; }
                else if(appUsage.getPackageName().equals("com.android.settings")){continue;}
                AppDetails appDetails = dao.getAppDetails(appUsage.getPackageName());
//                String category = appDetails.getCategory();

//                if (AppInfo.NO_CATEGORY.equals(category)) { continue; } // For testing; remove


//                if (usageMap.containsKey(category)) {
//                    usageMap.put(category, usageMap.get(category) + appUsage.getDailyUseTime());
//                    subtitleInfo.get(category).add(appDetails.getAppName());
//                } else {
//                    usageMap.put(category, appUsage.getDailyUseTime());
//                    subtitleInfo.put(category, new ArrayList<>(Collections.singleton(appDetails.getAppName())));
//                }

                // If apps within a category are visible, we need to adjust the data accordingly.
                if (isInSecondaryView()) {
//                    if (myCurrentCategory.equals(category)) {
                    Log.d("Test", appDetails.getPackageName()+" "+appUsage.getDailyUseTime());



                    myTotalUsageTime += appUsage.getDailyUseTime();
                        secondaryUsageMap.put(appDetails.getPackageName(), appUsage.getDailyUseTime());
//                    }
                } else {
                    Log.d("Test", appDetails.getPackageName()+" "+appUsage.getDailyUseTime());

                    myTotalUsageTime += appUsage.getDailyUseTime();
                    secondaryUsageMap.put(appDetails.getPackageName(), appUsage.getDailyUseTime());
                }

                if(entertainmentApps.contains(appUsage.getPackageName())){

                    entertainmentTimeUsed+= appUsage.getDailyUseTime();

                }

            }


            List<PieEntry> entries = processUsageMap(secondaryUsageMap, subtitleInfo, isInSecondaryView(), !isInSecondaryView());
            PieDataSet dataSet = new PieDataSet(entries, "App usage");
            dataSet.setColors(PIE_COLORS);
            dataSet.setDrawIcons(true);
            dataSet.setIconsOffset(new MPPointF(0,15));
            dataSet.setSliceSpace(5);


            myPieData = new PieData(dataSet);

            if (isInSecondaryView()) {
                List<PieEntry> secondaryEntries = processUsageMap(secondaryUsageMap,
                        subtitleInfo, true, true);
//                PieDataSet secondaryDataSet = new PieDataSet(secondaryEntries, "App usage");
//                secondaryDataSet.setColors(PIE_COLORS);
//                secondaryDataSet.setDrawValues(false);
//                mySecondaryPieData = new PieData(secondaryDataSet);
            }

            // Also load the mood.
//            MoodLog latestMoodLog = myEmotionUtil.getLatestMoodLog(myDate);
//            if (latestMoodLog != null) {
//                myMood = myEmotionUtil.getEmoji((int) Math.round(latestMoodLog.happy_value * 4));
//            } else {
//                myMood = getString(R.string.unknown);
//            }

            myHandler.post(postLoadData);
        }

        private List<PieEntry> processUsageMap(
                Map<String, Long> usageMap, Map<String, List<String>> subtitleInfo,
                boolean isSecondaryData, boolean addToLegend) {

            // Output list.
            List<PieEntry> entries = new ArrayList<>();
            Context context = getActivity();
            if (context == null) { return entries; }

            // Add to output in the descending order of keys in the usageMap.
            List<String> keys = new ArrayList<>(usageMap.keySet());
            Collections.sort(keys, (b, a) -> Long.compare(usageMap.get(a), usageMap.get(b)));
            int i = 0;
            for (String key : keys) {
                long usage = usageMap.get(key);
                Drawable icon = null;
                Drawable iconD=null;
                String title, subTitle;

                if (isSecondaryData) {
                    // In PER_CATEGORY state, usageMap is keyed using package names, but we want to
                    // show app name as the title in chart. package name will be the subtitle.
                    AppInfo appInfo = new AppInfo(key, context);
                    title = appInfo.getAppName();
                    subTitle = key;
                    iconD= appInfo.getAppIcon();

                    Bitmap iconBitmap = iconD==null?null:Bitmap.createScaledBitmap(getBitmapFromDrawable(iconD), 70, 70, false);

                    icon=new BitmapDrawable(getResources(),iconBitmap);
                    assert icon != null;
                } else {
                    // In TOTAL state, the categories are the titles, and apps in them are the subtitles.
                    title = key;
                    subTitle = GraphUtil.buildSubtitle(context, subtitleInfo.get(key));
                }

                // We want to limit the number of entries in the chart.
                if (i >= MAX_ENTRIES) {
                    PieEntry lastEntry = entries.get(MAX_ENTRIES - 1);
                    PieEntry entry = new PieEntry(usage + lastEntry.getValue(), getString(R.string.others));
                    entries.set(MAX_ENTRIES-1, entry);

                } else {
                    PieEntry entry = new PieEntry(usage, icon);
                    entries.add(entry);

                }

                if (addToLegend) {
                    if(i<MAX_ENTRIES) {
                        myLegendInfosAtStart.add(new ChartLegendAdapter.LegendInfo(title, subTitle, iconD,
                                usage, PIE_COLORS[Math.min(i, MAX_ENTRIES - 1)]));
                    }

                    myLegendInfos.add(new ChartLegendAdapter.LegendInfo(title, subTitle, iconD,
                            usage, PIE_COLORS[Math.min(i, MAX_ENTRIES - 1)]));
                }
                i++;
            }

            return entries;
        }
    };
    @NonNull
    private Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {

        if (drawable==null)return null;

        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }
    // Runnable to be run in the UI thread after our state has been updated.
    private Runnable postLoadData = new Runnable() {
        @Override
        public void run() {
            Context context = getActivity();
            if (context == null) {
                return;
            }

            //added 09/01/2023
            updateEnterTextFields();

            // Update the main pie chart data.
            if (myAnimatePie) {

                myPieChart.animateY(600, Easing.EaseInOutQuad);
                myAnimatePie = false;
            }

            myPieChart.setData(myPieData);

            if (!isInSecondaryView()) {
                myPieChartSecondary.animateY(600, Easing.EaseInOutQuad);
                myPieChartSecondary.setData(mySecondaryPieData);
            }


            // The center text shows duration, the current category being viewed, and the recorded mood.
            SpannableString centerTextDuration =
                    new SpannableString(formateDateToHandMin(UsageStatsUtil.formatDuration(myTotalUsageTime, context)));
            centerTextDuration.setSpan(new RelativeSizeSpan(1.4f), 0, centerTextDuration.length(), 0);
            centerTextDuration.setSpan(new StyleSpan(Typeface.BOLD), 0, centerTextDuration.length(), 0);

//            String centerTextCategory = getString(R.string.total);
            CharSequence centerText = TextUtils.concat(centerTextDuration);

            if (!isInSecondaryView()) {
                myPieChartSecondary.setCenterText(centerText);
            } else {
                myPieChart.setCenterText(centerText);
            }
            myPieChart.invalidate();
            myPieChartSecondary.invalidate();

            setAdapter(context);

            myLegend.getAdapter().notifyDataSetChanged();

//            test> changeAdapterRecycleView(context);
            // Hide any loading spinners.
            myRefreshLayout.setRefreshing(false);

            entertaimentDiv.setVisibility(View.VISIBLE);
            viewMoreBtn.setVisibility(View.VISIBLE);


        }



        private String formateDateToHandMin(String formatDuration) {

            String[] duration=formatDuration.split(":");
            if(duration.length==1) return duration[0];
            String hour=duration[0];
            String[] min=duration[1].split("hr");
            return hour+" hr "+min[0]+"min";

        }
    };

        private void setAdapter(Context context) {
            // Update the chart legend.
            ChartLegendAdapter adapter = (ChartLegendAdapter) myLegend.getAdapter();
            if (adapter == null) {
                if(viewMore){
                    adapter = new ChartLegendAdapter(myLegendInfos, myTotalUsageTime, context, DayReportFragment.this);

                }
                else{
                    adapter = new ChartLegendAdapter(myLegendInfosAtStart, myTotalUsageTime, context, DayReportFragment.this);

                }
                myLegend.setAdapter(adapter);
            } else {
                if(viewMore){
                    Log.d("Inside","Hello");
                    adapter.setData(myLegendInfos,myTotalUsageTime);
                }
                else {
                    adapter.setData(myLegendInfosAtStart, myTotalUsageTime);

                }
            }
        }

        public void changeAdapterRecycleView(Context context){

            Log.d("Test Inside","inside");
            viewMore=true;
            this.setAdapter(context);
            myLegend.getAdapter().notifyDataSetChanged();
        }
    public static DayReportFragment getInstance(long dateInMillis, String category) {
        Bundle args = new Bundle();
        args.putLong(EXTRA_DATE, dateInMillis);
        args.putString(EXTRA_CATEGORY, category);
        DayReportFragment fragment = new DayReportFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public DayReportFragment() {
        // Required empty public constructor
    }

    public String getCurrentCategory() {
        return myCurrentCategory;
    }

    private void updateEnterTextFields(){
        //Preferences of 09/01/2022
        //Entertainment Time get here

        totalEntertainmentTime=PreferencesHelper.getLongPreference(getContext(),AppMonitorService.ThreshHoldEntertainmentTime,AppMonitorService.defaultThresholdEnterTime);
        entertainmentTimeLeftToday= (totalEntertainmentTime - entertainmentTimeUsed)<0?0:(totalEntertainmentTime - entertainmentTimeUsed);

        PreferencesHelper.setPreference(getContext(),AppMonitorService.leftEntertainmentTime,entertainmentTimeLeftToday);



        //added 09/01/2023
        //setting the entertainment time
        entertainmentTimeLeft.setText(""+UsageStatsUtil.formatDuration(entertainmentTimeLeftToday,getContext()));
        entertainmentTime.setText(""+UsageStatsUtil.formatDuration(totalEntertainmentTime,getContext()));

        if(entertainmentTimeLeftToday==0){
            entertainmentTimeLeft.setTextColor(getResources().getColor(R.color.red));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {



        // Inflate the layout for this fragment
        myRootView = inflater.inflate(R.layout.fragment_day_report, container, false);

        // Get references to the required views.
        myRefreshLayout = myRootView.findViewById(R.id.swipe_refresh);
        myPieChart = myRootView.findViewById(R.id.pie_chart);
        myPieChartSecondary = myRootView.findViewById(R.id.pie_chart_secondary);
        myLegend = myRootView.findViewById(R.id.pie_categories_list);
        viewMoreBtn=myRootView.findViewById(R.id.viewmore);
        entertainmentTime=myRootView.findViewById(R.id.entertainment_time);
        entertainmentTimeLeft=myRootView.findViewById(R.id.time_left);
        showEntertainmentSettingBtn=myRootView.findViewById(R.id.entertainment_options);
        entertaimentDiv=myRootView.findViewById(R.id.entertaimentDiv);


        entertaimentDiv.setVisibility(View.GONE);
        viewMoreBtn.setVisibility(View.GONE);

        myAnimatePie=true;

        viewMoreBtn.setOnClickListener(
                v -> {
                        Log.d("Test","btn Clicked");
                        changeAdapterRecycleView(getContext());
                        v.setVisibility(View.GONE);
                }
        );


        showEntertainmentSettingBtn.setOnClickListener(
                v->{
                    IntroActivity newIntroActivity=new IntroActivity();
                    newIntroActivity.isonlyEntertainementSetting=true;
                    Intent callOnBoardEntertainment=new Intent(getContext(), newIntroActivity.getClass());

                    startActivity(callOnBoardEntertainment);

                });

        // Listen for layout completion, so that we can start animations.
        myPieChart.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                init();
                myPieChart.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return myRootView;
    }

    private void init() {
        // Get position and size of our pie-chart and derive animation translation and scaling.
        myMinimizedPieX = (int) (-PIE_SCALE_FACTOR * myPieChart.getWidth());
        myPieX = (int) myPieChart.getX();

        // Init our views.
        myRefreshLayout.setOnRefreshListener(this::onRefresh);
        setupPieAndLegendView();
        // Populate some state. This calls setPieData internally.
        String category = null;
        if (getArguments() != null) {
            category = getArguments().getString(EXTRA_CATEGORY);
            myAnimatePie = false;
        } else {
            myAnimatePie = true;
        }
        if (category == null || category.isEmpty()) {
            myCurrentCategory = "invalid"; // Ugly hack, ugh.
            switchToTotalView();
        } else {
            switchToPerCategoryView(category);
        }

        myRootView.setFocusableInTouchMode(true);
        myRootView.requestFocus();
        myRootView.setOnKeyListener(this);
    }

    private void onRefresh() {
        myAnimatePie = true;
        viewMore=false;
        viewMoreBtn.setVisibility(View.VISIBLE);
        setPieData();
    }

    private boolean isInSecondaryView() {
        return true;
//                || myCurrentCategory != null && !myCurrentCategory.isEmpty();
    }

    @SuppressLint({"ClickableViewAccessibility", "ResourceAsColor"})
    private void setupPieAndLegendView() {
        Context context = getActivity();
        if (context == null) return;
        for (PieChart pieChart : Arrays.asList(myPieChart, myPieChartSecondary)) {
            pieChart.getDescription().setEnabled(false);
            pieChart.getLegend().setEnabled(false);
            pieChart.setUsePercentValues(true);
            pieChart.setEntryLabelColor(Color.BLACK);
            pieChart.setHoleRadius(PIE_HOLE_RADIUS);
            pieChart.setDrawCenterText(true);
            pieChart.setCenterTextSize(20);
            pieChart.setDrawEntryLabels(false);
           pieChart.animateX(1000);
            pieChart.setExtraOffsets(15,15,15,15);
//            pieChart.setDrawRoundedSlices(true);
//            pieChart.setTransparentCircleColor(Color.WHITE);
//            pieChart.setTransparentCircleAlpha(255);

        }
        View.OnTouchListener existingPieListener = myPieChart.getOnTouchListener();
        myPieChart.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                switchToTotalView();
            }
            return existingPieListener.onTouch(v, event);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        myLegend.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(context, layoutManager.getOrientation());
        myLegend.addItemDecoration(dividerItemDecoration);
        myLegend.setItemAnimator(new DefaultItemAnimator());
        assert getArguments() != null;
        myDate = new Date(getArguments().getLong(EXTRA_DATE, System.currentTimeMillis()));
    }

    private void setPieData() {
        myRefreshLayout.setRefreshing(true);
        myExecutor.execute(loadData);
    }

    @Override
    public void onItemClick(int position) {
        switchToPerCategoryView(myLegendInfos.get(position).getTitle());
    }

    @Override
    public boolean onItemLongClick(int position) {
        if (isInSecondaryView()) {
            ChartLegendAdapter.LegendInfo legendInfo = myLegendInfos.get(position);
            myExecutor.execute(()->{
                if (getActivity() == null) return;
                AppDao dao = AppDatabase.getAppDatabase(getActivity()).appDao();
                int thresholdTime = RestrictionRecommender.recommendRestriction(
                        dao.getAppDetails(legendInfo.getSubTitle()),
                        dao.getAppUsage(legendInfo.getSubTitle()),
//                        dao.getAllMoodLog(),
                        new HashSet<>(dao.getCategories(true))
                );
                myHandler.post(() -> {
                    SetRestrictionFragment sf=SetRestrictionFragment
                            .newInstance(legendInfo.getTitle(), legendInfo.getSubTitle(), thresholdTime)
                            .setListener(DayReportFragment.this);
                    sf.show(getActivity().getFragmentManager(), "SET_RESTRICTION");
                });
            });

            return true;
        }
        return false;
    }

    private void switchToPerCategoryView(String category) {
        // If we are already in details view, nothing to do.
        if (isInSecondaryView()) {
            return;
        }

        myCurrentCategory = category;
        myPieChart.animate().x(myMinimizedPieX).scaleX(PIE_SCALE_FACTOR).scaleY(PIE_SCALE_FACTOR)
                .setInterpolator(myInterpolator).start();
        myPieChartSecondary.animate().alpha(1f).scaleX(1f).scaleY(1f).setInterpolator(myInterpolator).start();
        setPieData();
    }

    private void switchToTotalView() {
        goBack();
    }

    /**
     * Handle back button press. Return whether the back press was consumed by the fragment.
     */
    public boolean goBack() {
        if (!isInSecondaryView()) {
            return false;
        }

        // We are in details view. Go back to total view and consume the button press so that
        // our parent does not go back.
        myCurrentCategory = "";

        // Animations!
        myPieChart.animate().x(myPieX).scaleX(1f).scaleY(1f).setInterpolator(myInterpolator).start();
        myPieChartSecondary.animate().alpha(0f).scaleX(0f).scaleY(0f).setInterpolator(myInterpolator).start();
        setPieData();
        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && goBack();
    }

    /*private String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }*/

    @Override
    public void onDurationConfirmed(String packageName, long duration) {
        if (getActivity() == null) return;
        Runnable postSaveRunnable = () -> myHandler.post(() -> {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Restriction saved", Toast.LENGTH_SHORT).show();
            }
        });
        myExecutor.execute(new DbUtils.SaveRestrictionToDb(getActivity(), packageName, (int) duration,
                postSaveRunnable));
    }

    @Override
    public void onCancel() {

    }




}
