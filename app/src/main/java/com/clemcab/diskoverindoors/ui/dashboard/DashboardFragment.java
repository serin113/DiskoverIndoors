package com.clemcab.diskoverindoors.ui.dashboard;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class DashboardFragment extends Fragment {

    private Accelerometer accelerometer;
    private NotificationsViewModel notificationsViewModel;
    private DBHelper db;
    private ImageView imageView = null;

    // settings for resizing image to bitmaps
    private final int MAP_REQ_WIDTH = 500;
    private final int MAP_REQ_HEIGHT = 500;
    private final int USER_MARKER_REQ_WIDTH = 30;
    private final int USER_MARKER_REQ_HEIGHT = 30;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        db = ((MainActivity) this.getActivity()).DBHelper;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        cameraVisible = false;
        surfaceView = root.findViewById(R.id.camerapreview_lock);
        surfaceHolder = surfaceView.getHolder();
        dashboardFrame = root.findViewById(R.id.dashboardCameraFrame);
        qrfab = root.findViewById(R.id.qrfab);
        dashboardViewModel = ViewModelProviders.of(this).get(DashboardViewModel.class);

        // initiate the 2D scene
        notificationsViewModel = ViewModelProviders.of(this.getActivity()).get(NotificationsViewModel.class);
        if (notificationsViewModel.navData != null) {
            notificationsViewModel.navData.observe(getViewLifecycleOwner(), new Observer<NavigationData>() {
                @Override
                public void onChanged(NavigationData navData) {
                    BuildingData buildingData = db.getBuildingfromName(navData.building);

                    int drawableId = getDrawableId(navData.building,navData.start_floor);
                    if (drawableId > 0) {
                        // draw the map canvas
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeResource(getActivity().getResources(), drawableId, options);
                        Bitmap map = decodeSampledBitmapFromResource(getActivity().getResources(), drawableId, MAP_REQ_WIDTH, MAP_REQ_HEIGHT);
                        Bitmap mutableMap =  map.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas mapCanvas = new Canvas(mutableMap);

                        // draw user marker on canvas
                        Bitmap userMarker =  decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.map_pointer, USER_MARKER_REQ_WIDTH, USER_MARKER_REQ_HEIGHT);
                        Bitmap mutableUserMarker = userMarker.copy(Bitmap.Config.ARGB_8888, true);
                        int mapWidth =  mutableMap.getWidth();
                        int mapHeight = mutableMap.getHeight();
                        int userMarkerWidth =  mutableUserMarker.getWidth();
                        int userMarkerHeight = mutableUserMarker.getHeight();
                        double x_coord = getRelativeCoords((double) navData.start_x,(double) buildingData.xscale,mapWidth);
                        double y_coord = getRelativeCoords((double) navData.start_y*-1,(double) buildingData.yscale,mapHeight);

                        mapCanvas.drawBitmap(mutableUserMarker, (float) x_coord - (userMarkerWidth/2),(float) y_coord - (userMarkerHeight/2), null);

                        imageView.setImageDrawable(new BitmapDrawable(getActivity().getResources(), mutableMap));
                    }
                }
            });
        }

        // initialize map canvas
        imageView = root.findViewById(R.id.imageView);

        // accelerometer handles all sensor computations and management
        accelerometer = new Accelerometer(getActivity());
        // handles the user marker's movement
        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(double x_vel, double y_vel, double z_vel, double timeDiff, float azimuth) {
                double multiplier = 500d;

                double deltaX = (x_vel*multiplier)/timeDiff;
                double deltaY = (y_vel*multiplier)/timeDiff;

//                map_pointer.setX(x_coord - (float)deltaX);
//                map_pointer.setY(y_coord + (float)deltaY);
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
//                map_pointer.setRotation(azimuth);
            }
        });

        return root;
    }

    public void toggleCamera() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dashboardFrame.getVisibility() == View.VISIBLE) {
                    dashboardFrame.setVisibility(View.INVISIBLE);
                    cameraSource.stop();
                } else {
                    if (surfaceHolder != null) {
                        try {
                            dashboardFrame.setVisibility(View.VISIBLE);
                            cameraSource.start(surfaceHolder);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                dashboardFrame.postInvalidate();
            }
        });
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
        cameraSource.stop();
        accelerometer.unregister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraSource.stop();
        accelerometer.unregister();
        imageView.setImageDrawable(null);
    }

}