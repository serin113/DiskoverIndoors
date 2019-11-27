package com.clemcab.diskoverindoors.ui.notifications;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.clemcab.diskoverindoors.IndoorLocation;
import com.clemcab.diskoverindoors.R;
import com.clemcab.diskoverindoors.ui.home.HomeViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Observer;

public class CustomAdapter extends BaseAdapter {
    Context context;
    List<IndoorLocation> indoorLocationList;
    int start_level;
    float start_x;
    float start_y;
    LayoutInflater inflater;
    NotificationsViewModel viewModel;
    Fragment fragment;
    BottomNavigationView bottomNavigationView;

    public CustomAdapter(Fragment fragment, Context applicationContext, List<IndoorLocation> indoorLocationList, int startingLevel, Float[] startingCoords) {
        this.context = applicationContext;
        this.indoorLocationList = indoorLocationList;
        this.start_level = startingLevel;
        this.start_x = startingCoords[0];
        this.start_y = startingCoords[1];
        inflater = (LayoutInflater.from(applicationContext));
        this.fragment = fragment;

        bottomNavigationView = fragment.getActivity().findViewById(R.id.nav_view);

        this.viewModel = ViewModelProviders.of(fragment.getActivity()).get(NotificationsViewModel.class);
    }

    @Override
    public int getCount() {
        return indoorLocationList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public View.OnClickListener onClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            TextView room = v.findViewById(R.id.textView);
//            String data = room.getText().toString();
            NavigationData navData = (NavigationData) v.getTag();
            if (navData != null) {
                viewModel.setNavData(navData);
                bottomNavigationView.getMenu().getItem(0).setEnabled(false);
                Navigation.findNavController(fragment.getView()).navigate(R.id.action_list_select);
            }
        }
    };

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.location_list, null);
        view.setClickable(true);
        view.setOnClickListener(onClickListener);
        TextView room = view.findViewById(R.id.textView);

        String building = indoorLocationList.get(i).building;
        float dest_x = indoorLocationList.get(i).x_coord;
        float dest_y = indoorLocationList.get(i).y_coord;
        int dest_level = indoorLocationList.get(i).level;

        NavigationData navData = new NavigationData(building, start_x, start_y, start_level, dest_x, dest_y, dest_level);
//        if (this.start_x != dest_x || this.start_y != dest_y || this.start_level != dest_level) { // not completely fool proof
        room.setText(indoorLocationList.get(i).title + " " + indoorLocationList.get(i).subtitle);
        view.setTag(navData);
//        } else {
//            room.setVisibility(View.GONE);
//        }
        return view;
    }
}
