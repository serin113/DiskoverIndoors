package com.clemcab.diskoverindoors.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.clemcab.diskoverindoors.DBHelper;
import com.clemcab.diskoverindoors.IndoorLocation;
import com.clemcab.diskoverindoors.MainActivity;
import com.clemcab.diskoverindoors.R;
import com.clemcab.diskoverindoors.ui.home.HomeViewModel;

import java.util.List;

public class NotificationsFragment extends Fragment {

    private NotificationsViewModel notificationsViewModel;
    private HomeViewModel homeViewModel;
    private String scannedQrCode;
    private DBHelper db;
    private List<IndoorLocation> indoorLocationList;
    private Float[] startingCoords;
    private ListView listView;
    private Fragment fragment = this;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel = ViewModelProviders.of(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        listView = root.findViewById(R.id.listView);

        db = ((MainActivity)getActivity()).DBHelper;

        homeViewModel = ViewModelProviders.of(this.getActivity()).get(HomeViewModel.class);
        homeViewModel.qrCode.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String qrCode) {
                scannedQrCode = qrCode;
                String[] args = scannedQrCode.split("::");
                int startingLevel = Integer.parseInt(args[1]);
                startingCoords = db.getCoordsFromCode(scannedQrCode);
                indoorLocationList = db.getRoomList(scannedQrCode);


                CustomAdapter customAdapter = new CustomAdapter(fragment, getContext(), indoorLocationList, startingLevel, startingCoords);
                listView.setAdapter(customAdapter);
            }
        });

        return root;
    }
}