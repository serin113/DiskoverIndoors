package com.clemcab.diskoverindoors.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.clemcab.diskoverindoors.R;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private Accelerometer accelerometer;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel = ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        final TextView textView = root.findViewById(R.id.text_dashboard);
//        dashboardViewModel.getText().observe(this, new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

        accelerometer = new Accelerometer(getActivity());
        final TextView textViewAccel = root.findViewById(R.id.textViewAccel);

        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(float x, float y, float z) {
                String text = "Accelerometer Readings:\n" +
                              "x = " + x + "m/s\n " +
                              "y = " + y + "m/s\n " +
                              "z = " + z + "m/s\n";
                textViewAccel.setText(text);
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        accelerometer.register();
    }

    @Override
    public void onPause() {
        super.onPause();

        accelerometer.unregister();
    }

}