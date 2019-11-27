package com.clemcab.diskoverindoors;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.clemcab.diskoverindoors.ui.dashboard.Accelerometer;
import com.clemcab.diskoverindoors.DBHelper;
import com.clemcab.diskoverindoors.ui.home.HomeViewModel;
import com.clemcab.diskoverindoors.ui.notifications.NavigationData;
import com.clemcab.diskoverindoors.ui.notifications.NotificationsViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {
    public Accelerometer Accelerometer;
    public DBHelper DBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View navigationButton = findViewById(R.id.navigation_navigation);
        View locationButton = findViewById(R.id.navigation_locations);

        navigationButton.setEnabled(false);
        locationButton.setEnabled(false);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_qr, R.id.navigation_navigation, R.id.navigation_locations)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        Accelerometer = new Accelerometer(this);
        DBHelper = new DBHelper(this);
    }
}
