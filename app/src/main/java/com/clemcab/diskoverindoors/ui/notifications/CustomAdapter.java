package com.clemcab.diskoverindoors.ui.notifications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.clemcab.diskoverindoors.IndoorLocation;
import com.clemcab.diskoverindoors.R;

import java.util.List;

public class CustomAdapter extends BaseAdapter {
    Context context;
    List<IndoorLocation> indoorLocationList;
    String startingRoom;
    LayoutInflater inflater;

    public CustomAdapter(Context applicationContext, List<IndoorLocation> indoorLocationList) {
        this.context = applicationContext;
        this.indoorLocationList = indoorLocationList;
        inflater = (LayoutInflater.from(applicationContext));
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

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.location_list, null);
        TextView room = view.findViewById(R.id.textView);

        room.setText(indoorLocationList.get(i).title + " " + indoorLocationList.get(i).subtitle);

        return view;
    }
}
