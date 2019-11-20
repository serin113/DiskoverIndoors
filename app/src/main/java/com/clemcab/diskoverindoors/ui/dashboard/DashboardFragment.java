package com.clemcab.diskoverindoors.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.clemcab.diskoverindoors.R;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private Accelerometer accelerometer;

    private ImageView map_pointer;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel = ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        final TextView textView = root.findViewById(R.id.text_dashboard);

        map_pointer = root.findViewById(R.id.map_pointer);

        accelerometer = new Accelerometer(getActivity());
        final TextView textViewAccel = root.findViewById(R.id.textViewAccel);

        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(float x_vel, float y_vel, float z_vel, float x_accel, float timeDiff) {
                float x_coord = map_pointer.getX();
                float y_coord = map_pointer.getY();
                String text = "Accelerometer Readings:\n" +
                              "x_vel = " + x_vel + " m/s\n " +
                              "y_vel = " + y_vel + " m/s\n " +
                              "z_vel = " + z_vel + " m/s\n " +
                              "timeDiff = " + timeDiff + " Hz\n " +
                              "x - " + x_coord + "\n" +
                              "y - " + y_coord + "\n" ;
                map_pointer.setX(x_coord - (x_vel/timeDiff)*1000);
                map_pointer.setY(y_coord + (y_vel/timeDiff)*1000);
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