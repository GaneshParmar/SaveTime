package com.cs565project.smart.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.cs565project.smart.R;
import com.savvi.rangedatepicker.CalendarPickerView;

import java.util.Date;
import java.util.List;

import static com.savvi.rangedatepicker.CalendarPickerView.SelectionMode.RANGE;
import static com.savvi.rangedatepicker.CalendarPickerView.SelectionMode.SINGLE;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 * A dialog box for picking a date or a date range. Activities using this Dialog should implement
 * OnDateSelectedListener.
 */

public class DatePickerFragment extends DialogFragment implements DialogInterface.OnClickListener {

    /**
     * Callback interface to pass the selected date values back to the parent activity.
     */
    public interface OnDateSelectedListener {
        void onDateSelected(List<Date> selectedDates);
    }

    private static final String KEY_SELECT_RANGE = "SELECT_RANGE";
    private static final String KEY_START_DATE   = "START_DATE";
    private static final String KEY_END_DATE     = "END_DATE";

    private OnDateSelectedListener myListener;
    private CalendarPickerView myPicker;

    public static DatePickerFragment getInstance(boolean selectRange, long startDate, long endDate) {
        DatePickerFragment fragment = new DatePickerFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_SELECT_RANGE, selectRange);
        bundle.putLong(KEY_START_DATE, startDate);
        bundle.putLong(KEY_END_DATE, endDate);
        fragment.setArguments(bundle);
        return fragment;
    }

    public DatePickerFragment setListener(OnDateSelectedListener listener) {
        myListener = listener;
        return this;
    }

    /*@Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Make sure that the container of this Fragment has implemented the callback interface.
        try {
            myListener = (OnDateSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement NoticeDialogListener");
        }
    }*/

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException("Required arguments missing");
        }
        if (getActivity() == null) {
            throw new IllegalStateException("Parent activity cannot be null");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        Date startDate = new Date(args.getLong(KEY_START_DATE)),
                endDate = new Date(args.getLong(KEY_END_DATE));
        myPicker = (CalendarPickerView)
                getActivity().getLayoutInflater().inflate(R.layout.date_range_picker, null);
        myPicker.init(startDate, endDate)
                .inMode(args.getBoolean(KEY_SELECT_RANGE) ? RANGE : SINGLE);
        myPicker.clearSelectedDates();
        myPicker.scrollToDate(endDate);

        builder.setView(myPicker)
                .setPositiveButton("OK", this)
                .setCancelable(true);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (myListener != null) {
            myListener.onDateSelected(myPicker.getSelectedDates());
        }
    }
}
