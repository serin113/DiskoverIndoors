package com.clemcab.diskoverindoors.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.clemcab.diskoverindoors.R;
import com.clemcab.diskoverindoors.ui.home.HomeViewModel;
import com.clemcab.diskoverindoors.ui.notifications.NavigationData;
import com.clemcab.diskoverindoors.ui.notifications.NotificationsViewModel;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private Accelerometer accelerometer;

    private ImageView map_pointer;
    private NotificationsViewModel notificationsViewModel;

    private float start_x;
    private float start_y;
    private float start_floor;
    private float dest_x;
    private float dest_y;
    private float dest_floor;

//    @Override
//    public void onCreate() {
//        accelerometer = ((MainActivity)getActivity()).Accelerometer;
//    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel = ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        final TextView textView = root.findViewById(R.id.text_dashboard);

        map_pointer = root.findViewById(R.id.map_pointer);

//        accelerometer = ((MainActivity)getActivity()).Accelerometer;
        accelerometer = new Accelerometer(getActivity());
        final TextView textViewAccel = root.findViewById(R.id.textViewAccel);

        notificationsViewModel = ViewModelProviders.of(this.getActivity()).get(NotificationsViewModel.class);
        if (notificationsViewModel.navData != null) {
            notificationsViewModel.navData.observe(this, new Observer<NavigationData>() {
                @Override
                public void onChanged(NavigationData navData) {
                    start_x = navData.start_x;
                    start_y = navData.start_y;
                    start_floor = navData.start_floor;
                    dest_x = navData.dest_x;
                    dest_y = navData.dest_y;
                    dest_floor = navData.dest_floor;

                    String test = " Start: (" + start_x + ", " + start_y + ") floor " + start_floor + "\n" +
                            " Destination: (" + dest_x + ", " + dest_y + ") floor " + dest_floor + "\n";
                    textViewAccel.setText(test);
                }
            });
        }

        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(double x_vel, double y_vel, double z_vel, double timeDiff, float azimuth) {
                double multiplier = 500d;
                float x_coord = map_pointer.getX();
                float y_coord = map_pointer.getY();
                double deltaX = (x_vel*multiplier)/timeDiff;
                double deltaY = (y_vel*multiplier)/timeDiff;

                map_pointer.setX(x_coord - (float)deltaX);
                map_pointer.setY(y_coord + (float)deltaY);
//                String text = "Accelerometer Readings:\n" +
//                              "x_vel = " + x_vel + " m/s\n " +
//                              "y_vel = " + y_vel + " m/s\n " +
//                              "z_vel = " + z_vel + " m/s\n " +
//                              "timeDiff = " + timeDiff + " Hz\n " +
//                              "x - " + x_coord + "\n" +
//                              "y - " + y_coord + "\n" +
//                              "azimuth = " + azimuth;
//                textViewAccel.setText(text);
            }
            @Override
            public void onRotation(float azimuth) {
                map_pointer.setRotation(azimuth);
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