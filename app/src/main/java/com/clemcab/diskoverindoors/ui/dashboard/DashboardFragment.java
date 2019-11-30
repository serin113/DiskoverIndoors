package com.clemcab.diskoverindoors.ui.dashboard;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.clemcab.diskoverindoors.BuildingData;
import com.clemcab.diskoverindoors.DBHelper;
import com.clemcab.diskoverindoors.MainActivity;
import com.clemcab.diskoverindoors.R;
import com.clemcab.diskoverindoors.ui.home.HomeViewModel;
import com.clemcab.diskoverindoors.ui.notifications.NavigationData;
import com.clemcab.diskoverindoors.ui.notifications.NotificationsViewModel;

import java.lang.reflect.Field;

public class DashboardFragment extends Fragment{

    private Accelerometer accelerometer;
    private DBHelper db;
    private ImageView mainImageView;
    private ImageView userMarkerImageView;
    private Bitmap layout;
    private Bitmap mutableMap;;
    private Bitmap mutableUserMarker;
    private Canvas mapCanvas;
    private Canvas layoutCanvas;
    private Canvas userMarkerCanvas;
    private NavigationData navigationData = null;
    private BuildingData buildingData;
    double currentX;
    double currentY;

    // settings for resizing image to bitmaps
    private final int MAP_REQ_WIDTH = 500;
    private final int MAP_REQ_HEIGHT = 500;
    private final int USER_MARKER_REQ_WIDTH = 30;
    private final int USER_MARKER_REQ_HEIGHT = 30;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        mainImageView = root.findViewById(R.id.imageViewMain);
        userMarkerImageView = root.findViewById(R.id.imageViewUserMarker);

        db = ((MainActivity)getActivity()).DBHelper;
        navigationData = ((MainActivity)getActivity()).navData;

        // initiate the 2D scene
        if (navigationData != null && db != null) {
            buildingData = db.getBuildingfromName(navigationData.building);
            int drawableId = getDrawableId(navigationData.building, navigationData.start_floor);
            if (drawableId > 0) {
                Bitmap map = decodeSampledBitmapFromResource(getActivity().getResources(), drawableId, MAP_REQ_WIDTH, MAP_REQ_HEIGHT);
                Bitmap userMarker = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.map_pointer, USER_MARKER_REQ_WIDTH, USER_MARKER_REQ_HEIGHT);

                mutableMap = map.copy(Bitmap.Config.ARGB_8888, true);
                int mapWidth = mutableMap.getWidth();
                int mapHeight = mutableMap.getHeight();

                mutableUserMarker = userMarker.copy(Bitmap.Config.ARGB_8888, true);
                int userMarkerWidth = mutableUserMarker.getWidth();
                int userMarkerHeight = mutableUserMarker.getHeight();

                mapCanvas = new Canvas(mutableMap);

                double x_coord = getRelativeCoords((double) navigationData.start_x, (double) buildingData.xscale, mapWidth);
                double y_coord = getRelativeCoords((double) navigationData.start_y * -1, (double) buildingData.yscale, mapHeight);

                currentX = x_coord - (userMarkerWidth / 2f); // adjusts to the center of the image
                currentY = y_coord - (userMarkerHeight / 2f);
                mapCanvas.drawBitmap(mutableUserMarker, (float) currentX, (float) currentY, null);

                mainImageView.setImageDrawable(new BitmapDrawable(getActivity().getResources(), mutableMap));
            }
        }

        // accelerometer handles all sensor computations and management
        accelerometer = new Accelerometer(getActivity());
        // handles the user marker's movement
        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(double x_vel, double y_vel, double z_vel, double timeDiff, float azimuth) {
                double multiplier = 500d;

                double deltaX = (x_vel*multiplier)/timeDiff;
                double deltaY = (y_vel*multiplier)/timeDiff;

                double newX = currentX + deltaX;
                double newY = currentY + deltaY;

                Matrix matrix = new Matrix();
                matrix.setRotate(azimuth);
                matrix.setTranslate((float) newX, (float) newY);

                currentX = newX;
                currentY = newY;

                mapCanvas.drawBitmap(mutableUserMarker, matrix, null);
                mainImageView.setImageDrawable(new BitmapDrawable(getActivity().getResources(), mutableMap));
            }
            @Override
            public void onRotation(float azimuth) {
//                map_pointer.setRotation(azimuth);
            }
        });

        return root;
    }

    // return resource id
    public int getDrawableId(String building, int level) {
        int drawableId;
        String imageName = building.toLowerCase().replaceAll("\\s", "_") + "_" + Integer.toString(level);
        try {
            Class res = R.drawable.class;
            Field field = res.getField(imageName);
            drawableId = field.getInt(null);
        }
        catch (Exception e) {
            drawableId = -1;
            Log.e("DebugTag", "getDrawableId: exception encountered, " + imageName + " not found.");
        }
        return drawableId;
    }

    // return coordinates relative to the map's size
    public double getRelativeCoords(double coord, double scale, double actual) {
        return (coord + scale/2) * (actual/scale);
    }

    // return the minimum inSampleSize given required height and width
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }

            Log.e("DebugTag", "calculateInSampleSize: resized width, height: " + width/inSampleSize + ", " + height/inSampleSize );
            Log.e("DebugTag", "calculateInSampleSize: inSamplingSize: " + inSampleSize);

        }
        return inSampleSize;
    }

    // returns Bitmap object
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        accelerometer.unregister();
        mainImageView.setImageDrawable(null);
        userMarkerImageView.setImageDrawable(null);
    }

}

// generic class and interface to handle callback
 class NavigationDataObserver {
//    public OnCustomEventListener mListener; //listener field
//    public interface OnCustomEventListener{
//       void onEvent(NavigationData navData);
//    }
//    public void setCustomEventListener(OnCustomEventListener eventListener) {
//        this.mListener = eventListener;
//    }
    private Listener listener;
    public void setListener(Listener l) {
        listener = l;
    }
    public interface Listener {
        void onDataReceived(NavigationData navdata);
    }

    NavigationDataObserver (FragmentActivity activity, LifecycleOwner owner) {
        NotificationsViewModel notificationsViewModel = ViewModelProviders.of(activity).get(NotificationsViewModel.class);
        if (notificationsViewModel.navData != null) {
            notificationsViewModel.navData.observe(owner, new Observer<NavigationData>() {
                @Override
                public void onChanged(NavigationData navData) {
                    if (listener !=null) {
                        listener.onDataReceived(navData);
                        Log.e("DebugTag", "ObserverClass onChanged: " + navData.building);
                    }
                }
            });
        }
    }
}