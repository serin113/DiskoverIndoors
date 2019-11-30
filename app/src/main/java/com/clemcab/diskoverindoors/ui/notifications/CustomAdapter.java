package com.clemcab.diskoverindoors.ui.notifications;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.clemcab.diskoverindoors.IndoorLocation;
import com.clemcab.diskoverindoors.MainActivity;
import com.clemcab.diskoverindoors.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.lang.reflect.Field;
import java.util.List;

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
    NavigationData navdata;

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
//                viewModel.setNavData(navData);
                ( (MainActivity)(fragment.getActivity()) ).navData = navData;
                displayAlert(navData);
            }
        }
    };

    public  void displayAlert(NavigationData navigationData) {
        ImageView image = new ImageView(fragment.getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());

        //  processes building name and room to generate file name
        //  small caps, no spaces, separated by underscore
        String building = navigationData.building.toLowerCase().replaceAll("\\s+","_");
        String level = Integer.toString(navigationData.dest_floor);
        String room = navigationData.dest_room.toLowerCase().replaceAll("\\s+","_");
        String destinationImageName = building + "_" + level + "_" + room;

        builder.setMessage("This is your destination.");
        builder.setPositiveButton(
                "Continue",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Navigation.findNavController(fragment.getView()).navigate(R.id.action_list_select);
                    }
                });
        builder.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        try {
            Class res = R.drawable.class;
            Field field = res.getField(destinationImageName);
            int drawableId = field.getInt(null);
            image.setImageResource(drawableId);
        }
        catch (Exception e) {
            image.setImageResource(R.drawable.image_not_found);
        }
        builder.setView(image);

        builder.create();
        builder.show();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.location_list, null);
        view.setClickable(true);
        view.setOnClickListener(onClickListener);
        TextView room = view.findViewById(R.id.textView);
        room.setHeight(80);

        String building = indoorLocationList.get(i).building;
        float dest_x = indoorLocationList.get(i).x_coord;
        float dest_y = indoorLocationList.get(i).y_coord;
        int dest_level = indoorLocationList.get(i).level;
        String dest_room = indoorLocationList.get(i).title;

        NavigationData navData = new NavigationData(building, start_x, start_y, start_level, dest_x, dest_y, dest_level, dest_room);
        room.setText(indoorLocationList.get(i).title + " " + indoorLocationList.get(i).subtitle);
        view.setTag(navData);

        return view;
    }
}
