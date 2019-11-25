package com.clemcab.diskoverindoors.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import com.clemcab.diskoverindoors.R;
import com.clemcab.diskoverindoors.ui.home.HomeViewModel;

public class NotificationsFragment extends Fragment {

    private NotificationsViewModel notificationsViewModel;
    private HomeViewModel homeViewModel;
    private String scannedQrCode;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel = ViewModelProviders.of(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
//        final TextView textView = root.findViewById(R.id.text_notifications);
//        notificationsViewModel.getText().observe(this, new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

        homeViewModel = ViewModelProviders.of(this.getActivity()).get(HomeViewModel.class);
        homeViewModel.qrCode.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                scannedQrCode = s;
            }
        });

        return root;
    }
}