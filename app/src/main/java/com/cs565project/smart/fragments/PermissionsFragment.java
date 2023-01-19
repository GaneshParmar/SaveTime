package com.cs565project.smart.fragments;


import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.cs565project.smart.R;
import com.cs565project.smart.util.UsageStatsUtil;

import static com.cs565project.smart.util.PreferencesHelper.setPreference;


/**
 * Fragment to request necessary permissions from the user during onboarding.
 */
public class PermissionsFragment extends Fragment {
    private static final String KEY_CHECK_USAGE_ACCESS_ON_RESUME = "check_usage_access_on_resume";
    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    public static final int MY_PERMISSIONS_REQUEST_EXTERNAL_FILE = 101;
    private View v;
    public PermissionsFragment() {
        // Required empty public constructor
    }

    private View.OnClickListener usageAccessListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (getActivity() == null) return;
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                intent.setComponent(new ComponentName("com.android.settings",
                        "com.android.settings.Settings$SecuritySettingsActivity"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            setPreference(getContext(), KEY_CHECK_USAGE_ACCESS_ON_RESUME, true);

//            TextView tv1 = v.findViewById(R.id.appMonitoringEnabledText);
//            tv1.setText(usageAccessEnabled());



        }
    };

    private View.OnClickListener overlayAccessListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (getActivity() == null) return;
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getActivity().getPackageName()));
            startActivityForResult(intent, 10);

//            TextView tv2 = v.findViewById(R.id.overlayEnabledText);
//            tv2.setText(overlayAccessEnabled());
        }
    };

    private View.OnClickListener externalStorageAccessListener = new View.OnClickListener() {

        public void onClick(View v) {
            if (getActivity() == null) return;
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            }
//            TextView tv4 = v.findViewById(R.id.externalStorageEnabledText);
//            tv4.setText(externalStorageEnabled());
        }
    };

    private boolean externalStorageEnabled() {
        if (getActivity() == null) return false;
        return (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) ? false : true;
    }

    private boolean usageAccessEnabled() {
        if (getActivity() == null) return false;
        return !UsageStatsUtil.hasUsageAccess(getActivity()) ? false : true;
    }

    private boolean overlayAccessEnabled() {
        return ((Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(getContext()))) ? false: true;
    }


    @Override
    public void onResume() {
        super.onResume();
        Switch tv1 = v.findViewById(R.id.switch1);
        Switch tv2 = v.findViewById(R.id.switch2);
        Switch tv4 = v.findViewById(R.id.switch3);

        tv1.setChecked(usageAccessEnabled());
        tv2.setChecked(overlayAccessEnabled());
        tv4.setChecked(externalStorageEnabled());




    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_permissions, container, false);
        LinearLayout usageAccessButton = v.findViewById(R.id.usageEnableButton);
        LinearLayout overlayAccessButton = v.findViewById(R.id.overlayEnableButton);
        LinearLayout externalStorageAccessButton = v.findViewById(R.id.externalStorageEnableButton);
        usageAccessButton.setOnClickListener(usageAccessListener);
        overlayAccessButton.setOnClickListener(overlayAccessListener);
        externalStorageAccessButton.setOnClickListener(externalStorageAccessListener);

        return v;
    }
}
