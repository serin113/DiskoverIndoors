package com.clemcab.diskoverindoors.ui.dashboard;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.clemcab.diskoverindoors.BuildingData;
import com.clemcab.diskoverindoors.DBHelper;
import com.clemcab.diskoverindoors.MainActivity;
import com.clemcab.diskoverindoors.R;
import com.clemcab.diskoverindoors.ui.home.HomeViewModel;
import com.clemcab.diskoverindoors.ui.notifications.NavigationData;
import com.clemcab.diskoverindoors.ui.notifications.NotificationsViewModel;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.IOException;

import java.lang.reflect.Field;
import java.util.List;

public class DashboardFragment extends Fragment {

    private Accelerometer accelerometer;
    private DBHelper db;

    private ImageView mainImageView;
    private ImageView userMarkerImageView;
    private int mapWidth;
    private int mapHeight;
    private Bitmap mutableMap;
    private Bitmap mutableUserMarker;
    private Bitmap mutableUpstairsMarker;
    private Bitmap mutableDownstairsMarker;
    private Bitmap mutableDestinationMarker;
    private Canvas mapCanvas;
    List<float[]> staircaseList;

    private NavigationData navigationData = null;
    private BuildingData buildingData;

    double currentX;
    double currentY;

    private float startingAltitude;
    private float currentAltitude;
    float altitudeDifference;

    // settings for resizing image to bitmaps
    private final int MAP_REQ_WIDTH = 500;
    private final int MAP_REQ_HEIGHT = 500;
    private final int MARKER_REQ_WIDTH = 30;
    private final int MARKER_REQ_HEIGHT = 30;
    // canvas units per meter
    private double METER_PER_DB = 10d;
    private double DB_PER_METER = 1d/METER_PER_DB;
    private double CANVAS_PER_DB;

    private BarcodeDetector barcodeDetector;
    private Detector.Processor<Barcode> barcodeProcessor;
    private SurfaceView surfaceView;
    private FrameLayout dashboardFrame;
    private CameraSource cameraSource;
    private int surfaceViewHeight;
    private int surfaceViewWidth;
    private boolean cameraVisible;
    private boolean alertActive;
    private ExtendedFloatingActionButton qrfab;
    private SurfaceHolder surfaceHolder;
    private DashboardViewModel dashboardViewModel;
    private TextView dashboardBuildingTitle;
    private TextView dashboardStart;
    private TextView dashboardDest;
    private Toast toast = null;

    String intsCon(int a, int b) {
        return Integer.toString(a) + " " + Integer.toString(b);
    }
    String intsCon(double a, double b) {
        return Double.toString(a) + " " + Double.toString(b);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        db = ((MainActivity) this.getActivity()).DBHelper;

        mutableUpstairsMarker = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.upstairs_marker, MARKER_REQ_WIDTH, MARKER_REQ_HEIGHT);
        mutableDownstairsMarker = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.downstairs_marker, MARKER_REQ_WIDTH, MARKER_REQ_HEIGHT);
        mutableDestinationMarker = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.destination_marker, MARKER_REQ_WIDTH, MARKER_REQ_HEIGHT);
        mutableUserMarker = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.map_pointer, MARKER_REQ_WIDTH, MARKER_REQ_HEIGHT);

        // accelerometer handles all sensor computations and management
        accelerometer = ((MainActivity) getActivity()).Accelerometer;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        navigationData = ((MainActivity) this.getActivity()).navData;

        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        mainImageView = root.findViewById(R.id.imageViewMain);
        userMarkerImageView = root.findViewById(R.id.imageViewUserMarker);

        dashboardBuildingTitle = root.findViewById(R.id.dashboardBuilding);
        dashboardStart = root.findViewById(R.id.dashboardStart);
        dashboardDest = root.findViewById(R.id.dashboardDest);

        cameraVisible = false;

        surfaceView = root.findViewById(R.id.camerapreview_lock);
        surfaceHolder = surfaceView.getHolder();
        dashboardFrame = root.findViewById(R.id.dashboardCameraFrame);
        qrfab = root.findViewById(R.id.qrfab);

        dashboardViewModel = ViewModelProviders.of(this).get(DashboardViewModel.class);

//        observer.qrCOde {
//            // redraw everything
//        }

        // initiate the 2D scene
        if (navigationData != null && db != null) {
            buildingData = db.getBuildingfromName(navigationData.building);
            accelerometer.setAzimuthOffset(buildingData.compassDegreeOffset);

            dashboardBuildingTitle.setText(buildingData.name);
            // print current level name in dashboardStart
            dashboardStart.setText(buildingData.floorNameFromLevel(navigationData.start_floor));
            dashboardDest.setText(navigationData.dest_room+", "+buildingData.floorNameFromLevel(navigationData.dest_floor));
            dashboardBuildingTitle.setText(buildingData.name);

            int drawableId = getDrawableId(navigationData.building, navigationData.start_floor);
            if (drawableId > 0) {
                Glide.with(this)
                        .load(drawableId)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(mainImageView);

                int userMarkerWidth = mutableUserMarker.getWidth();
                int userMarkerHeight = mutableUserMarker.getHeight();

                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(getResources(), drawableId, opt);
                if (mutableMap != null)
                    mutableMap.recycle();
                mutableMap = Bitmap.createBitmap(opt.outWidth, opt.outHeight, Bitmap.Config.ARGB_8888);
                mapWidth = mutableMap.getWidth();
                mapHeight = mutableMap.getHeight();

                CANVAS_PER_DB = ((double)mapWidth / (double)buildingData.xscale) * 7d;

                staircaseList = db.getStaircaseCoordsFromBuildingAndLevel(navigationData.building,Integer.toString(navigationData.start_floor));

                mapCanvas = new Canvas(mutableMap);

                setMarkers(navigationData.start_floor, navigationData.dest_floor);

                double x_coord = getRelativeCoords((double) navigationData.start_x, (double) buildingData.xscale, mapWidth);
                double y_coord = getRelativeCoords((double) navigationData.start_y * -1, (double) buildingData.yscale, mapHeight);

                currentX = x_coord - (userMarkerWidth / 2f); // adjusts to the center of the image
                currentY = y_coord - (userMarkerHeight / 2f);
                mapCanvas.drawBitmap(mutableUserMarker, (float) currentX, (float) currentY, null);
                userMarkerImageView.setImageDrawable(new BitmapDrawable(getActivity().getResources(), mutableMap));
            }
        }

        // handles the user marker's movement
        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(double x_vel, double y_vel, double z_vel, double timeDiff, float azimuth) {
                double deltaX = (x_vel*DB_PER_METER*CANVAS_PER_DB)/timeDiff;
                double deltaY = (y_vel*DB_PER_METER*CANVAS_PER_DB)/timeDiff;

                currentX += deltaX;
                currentY += + deltaY;
            }
            @Override
            public void onRotation(float azimuth) {
//                accelerometer.printAzimuth();
                mapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                final float markerHalfX = ((float)mutableUserMarker.getWidth()/2f);
                final float markerHalfY = ((float)mutableUserMarker.getHeight()/2f);
                Matrix matrix = new Matrix();
//                Log.e("MovementTest", "onRotation: (x,y,0) = ((" + currentY + ", " + currentY + ", " + azimuth + ")" );
                matrix.setTranslate((float)currentX-markerHalfX, (float)currentY-markerHalfY);
                matrix.postRotate(azimuth, (float)currentX, (float)currentY);
                setMarkers(navigationData.start_floor, navigationData.dest_floor);
                mapCanvas.drawBitmap(mutableUserMarker, matrix, null);
                userMarkerImageView.setImageDrawable(new BitmapDrawable(getActivity().getResources(), mutableMap));

                if (isNearDestination()) {
                    if (!alertActive){
                        displayAlert();
                    }
                }
            }

            @Override
            public void onPressureChange(float altitude) {
                // set threshold to 60% of vertical distance between floors
                float threshold = 0.6f * buildingData.delta;

                if (accelerometer.hasBarometer()) {
                    if (startingAltitude == 0.0f)
                        startingAltitude = altitude;
                     else
                         currentAltitude = altitude;

                     if (startingAltitude * currentAltitude != 0.0f)
                         altitudeDifference = startingAltitude - currentAltitude;

                     if (Math.abs(altitudeDifference) >= threshold) {
//                         Log.e("SensorTest", "THRESHOLD BREACH  THRESHOLD BREACH  THRESHOLD BREACH  THRESHOLD BREACH");
                         if (altitudeDifference > 0) {
                             // went down, check if at lowest floor
                             if (navigationData.start_floor > 1) {
                                 navigationData.start_floor -= 1;
                                 startingAltitude -= buildingData.delta;
                             }
//                             Log.e("SensorTest", "DOWN DOWN DOWN DOWN DOWN DOWN");
                         } else {
                             // went up, check if at highest floor
                             if (navigationData.start_floor < buildingData.totalFloors) {
                                 navigationData.start_floor += 1;
                                 startingAltitude += buildingData.delta;
                             }
//                             Log.e("SensorTest", "UP UP UP UP UP UP UP UP");
                         }
                         changeLevel(navigationData.building, navigationData.start_floor);


//                         Log.e("SensorTest", "RESET starting altitude > " + startingAltitude);
                     }
//                    Log.e("SensorTest", "onPressureChange: start > " + startingAltitude + " current > " + currentAltitude + " difference > " + altitudeDifference);
                }
            }
        });

        return root;
    }

    // updates map image and markers
    public void changeLevel(String building, int newLevel) {
        Glide.with(this)
                .load(getDrawableId(building, newLevel))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(mainImageView);
        dashboardStart.setText(buildingData.floorNameFromLevel(newLevel));
        setMarkers(newLevel,navigationData.dest_floor);
        userMarkerImageView.setImageDrawable(new BitmapDrawable(getActivity().getResources(), mutableMap));
        // generate list of staircases for the level
        staircaseList = db.getStaircaseCoordsFromBuildingAndLevel(navigationData.building,Integer.toString(navigationData.start_floor));
    }

    // update markers: going up/down stairs, destination marker
    public void setMarkers(int currentLevel, int destinationLevel) {
        Bitmap marker;
        float marker_x;
        float marker_y;

        if (currentLevel == destinationLevel) {
            marker = mutableDestinationMarker;
            marker_x = navigationData.dest_x;
            marker_y = navigationData.dest_y;
        } else {
            marker = ((currentLevel > destinationLevel) ? mutableDownstairsMarker : mutableUpstairsMarker);
            float[] stairCoords = getNearestStairs();
            marker_x = stairCoords[0];
            marker_y = stairCoords[1];
        }

        double x_coord = getRelativeCoords((double) marker_x, (double) buildingData.xscale, mapWidth);
        double y_coord = getRelativeCoords((double) marker_y * -1, (double) buildingData.yscale, mapHeight);
        double adjustedX = x_coord - (marker.getWidth() / 2f); // adjusts to the center of the image
        double adjustedY = y_coord - (marker.getHeight() / 2f);

        mapCanvas.drawBitmap(marker, (float) adjustedX, (float) adjustedY, null);
    }

    public float[] getNearestStairs() {
        float[] nearestStairsCoords = new float[2];
        // gets list of staircase coordinates on the current floor level
//        Log.e("StairMarkerDebug", "getNearestStairs: # of stairs = " + staircaseList.size());

        if (staircaseList.size() > 1) {
            double leastDistance = -1;
            int nearestStairIndex = -1;

            for (int i = 0; i < staircaseList.size(); i++) {
                double stairX = staircaseList.get(i)[0];
                double stairY = staircaseList.get(i)[1];

                double relativeStairX = getRelativeCoords( stairX, (double) buildingData.xscale, mapWidth);
                double relativeStairY = getRelativeCoords(stairY * -1, (double) buildingData.yscale, mapHeight);

                double distance = euclidianDistance(relativeStairX, relativeStairY, currentX, currentY);

                if (leastDistance == -1 || distance < leastDistance) {
                    leastDistance = distance;
                    nearestStairIndex = i;
                }
//                Log.e("StairMarkerDebug", "getNearestStairs: current distance = " + distance + " least distance = " + leastDistance);
            }
            nearestStairsCoords = staircaseList.get(nearestStairIndex);
        } else if (staircaseList.size() == 1) {
            // only one staircase on that level
            nearestStairsCoords = staircaseList.get(0);
        } else {
            // no db result
//            Log.e("DatabaseError", "getNearestStairs: no list fetched.");
        }
        return nearestStairsCoords;
    }

    public double euclidianDistance(double x1, double y1, double x2, double y2){
        return Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

    public boolean isNearDestination() {
        if (navigationData.dest_floor == navigationData.start_floor) {
            if (euclidianDistance(
                    currentX, currentY,
                    getRelativeCoords((double)navigationData.dest_x, (double)buildingData.xscale, mapWidth),
                    getRelativeCoords((double)navigationData.dest_y * -1, (double)buildingData.yscale, mapHeight)
            ) <= (5d * (DB_PER_METER * CANVAS_PER_DB))) {
                return true;
            }
        }
        return false;
    }

    public  void displayAlert() {
        ImageView image = new ImageView(getActivity());
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());

        alertActive = true;

        //  processes building name and room to generate file name
        String building = (navigationData.building.toLowerCase()).replaceAll("[^a-z0-9_]","_");
        String room = (navigationData.dest_room.toLowerCase()).replaceAll("[^a-z0-9_]","_");
        String level = Integer.toString(navigationData.dest_floor);
        String destinationImageName = building + "_" + level + "_" + room;
        Log.e("destinationImageName", destinationImageName );

        builder.setTitle(navigationData.dest_room);
        builder.setMessage("You've arrived at your destination!");
        builder.setPositiveButton(
                "Start new navigation",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((MainActivity)getActivity()).navView.setVisibility(View.GONE);
                        dialog.dismiss();
                        Navigation.findNavController(getView()).navigate(R.id.go_to_home);
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

    public boolean localize(String qrcode) {
        String[] code_fields = qrcode.split("::", 0);
        if (buildingData.alias.equals(code_fields[0])) {
            accelerometer.setAzimuthOffset(buildingData.compassDegreeOffset);

            Float[] rel_coords = db.getCoordsFromCode(qrcode);

            double x_coord = getRelativeCoords((double) rel_coords[0], (double) buildingData.xscale, mapWidth);
            double y_coord = getRelativeCoords((double) rel_coords[1] * -1, (double) buildingData.yscale, mapHeight);

            navigationData.start_floor = Integer.parseInt(code_fields[1]);
            navigationData.start_x = rel_coords[0];
            navigationData.start_y = rel_coords[1];

            double adjustedX = x_coord - (mutableUserMarker.getWidth() / 2f); // adjusts to the center of the image
            double adjustedY = y_coord - (mutableUserMarker.getHeight() / 2f);

            currentX = adjustedX;
            currentY = adjustedY;

//            mapCanvas.drawBitmap(mutableUserMarker, (float) adjustedX, (float) adjustedY, null);

            changeLevel(navigationData.building, navigationData.start_floor);
            return true;
        }
        return false;
    }

    public void disableCamera(boolean onUIThread) {
        if (onUIThread) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (cameraVisible) {
                        barcodeDetector.release();
                        dashboardFrame.setVisibility(View.INVISIBLE);
                        cameraSource.stop();
                        cameraVisible = false;
                        qrfab.setIcon(getResources().getDrawable(R.drawable.ic_qrcode_solid));
                        qrfab.extend();
                    }
                }
            });
        } else {
            if (cameraVisible) {
                barcodeDetector.release();
                dashboardFrame.setVisibility(View.INVISIBLE);
                cameraSource.stop();
                cameraVisible = false;
                qrfab.setIcon(getResources().getDrawable(R.drawable.ic_qrcode_solid));
                qrfab.extend();
            }
        }
    }
    public void disableCamera() {
        disableCamera(true);
    }

    public void enableCamera(boolean onUIThread) {
        if (onUIThread) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!cameraVisible) {
                        if (surfaceHolder != null) {
                            try {
                                dashboardFrame.setVisibility(View.VISIBLE);
                                cameraSource.start(surfaceHolder);
                                barcodeDetector.setProcessor(barcodeProcessor);
                                cameraVisible = true;
                                qrfab.setIcon(getResources().getDrawable(R.drawable.ic_close));
                                qrfab.shrink();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        } else {
            if (!cameraVisible) {
                if (surfaceHolder != null) {
                    try {
                        dashboardFrame.setVisibility(View.VISIBLE);
                        cameraSource.start(surfaceHolder);
                        barcodeDetector.setProcessor(barcodeProcessor);
                        cameraVisible = true;
                        qrfab.setIcon(getResources().getDrawable(R.drawable.ic_close));
                        qrfab.shrink();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public void enableCamera() {
        enableCamera(true);
    }

    public void toggleCamera() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dashboardFrame.getVisibility() == View.VISIBLE)
                    disableCamera(false);
                else
                    enableCamera(false);
                dashboardFrame.postInvalidate();
            }
        });
    }

    @Override
    public void onViewCreated (View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        qrfab.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleCamera();
                }
            }
        );

        barcodeDetector = new BarcodeDetector.Builder(getActivity())
                .setBarcodeFormats(Barcode.QR_CODE).build();

        final SurfaceHolder.Callback initCameraPreviewCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, 100);
                    return;
                }
                try {
                    holder.setKeepScreenOn(true);
                    int viewHeight = dashboardFrame.getMeasuredHeight();
                    int viewWidth = dashboardFrame.getMeasuredWidth();

                    CameraSource.Builder cameraSourceBuilder = new CameraSource.Builder(getActivity(), barcodeDetector)
                            .setFacing(CameraSource.CAMERA_FACING_BACK)
                            .setRequestedFps(15)
                            .setAutoFocusEnabled(true);

                    if ((double) viewHeight / (double) viewWidth >= 1) {
                        cameraSourceBuilder = cameraSourceBuilder.setRequestedPreviewSize(720, 1000);
                    } else {
                        cameraSourceBuilder = cameraSourceBuilder.setRequestedPreviewSize(1000, 720);
                    }

                    cameraSource = cameraSourceBuilder.build();
                    cameraSource.start();
                    int camHeight = cameraSource.getPreviewSize().getHeight();
                    int camWidth = cameraSource.getPreviewSize().getWidth();
                    cameraSource.stop();

                    double viewRatio = (double) viewHeight / (double) viewWidth;
                    double camRatio = (double) camHeight / (double) camWidth;
                    double scale = 1d;
                    if (viewRatio < camRatio)
                        scale = (double) viewWidth / (double) camWidth;
                    else
                        scale = (double) viewHeight / (double) camHeight;
                    int newHeight = (int) Math.floor((double) camHeight * scale);
                    int newWidth = (int) Math.floor((double) camWidth * scale);
                    surfaceViewHeight = newHeight;
                    surfaceViewWidth = newWidth;
//                    Log.e("DIM_FRAME", intsCon(viewWidth,viewHeight));
//                    Log.e("DIM_SUGG", intsCon(camWidth,camHeight));
//                    Log.e("DIM_RATIO_FS", intsCon(viewRatio, camRatio));
//                    Log.e("DIM_SCALE", Double.toString(scale));
//                    Log.e("DIM_SURFACE", intsCon(surfaceView.getMeasuredWidth(),surfaceView.getMeasuredHeight()));
//                    Log.e("DIM_NEW", intsCon(newWidth,newHeight));
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            Log.e("NOPE_SURFACE_OLD", intsCon(surfaceView.getMeasuredWidth(),surfaceView.getMeasuredHeight()));
//                            Log.e("NOPE_FRAME_OLD", intsCon(cameraFrame.getMeasuredWidth(),cameraFrame.getMeasuredHeight()));
                            surfaceHolder.setFixedSize(surfaceViewWidth, surfaceViewHeight);
//                            cameraFrame.getLayoutParams().width = surfaceViewWidth;
//                            cameraFrame.getLayoutParams().height = surfaceViewHeight;
//                            cameraFrame.invalidate();
//                            cameraFrame.requestLayout();
                            surfaceView.getLayoutParams().width = surfaceViewWidth;
                            surfaceView.getLayoutParams().height = surfaceViewHeight;
                            surfaceView.invalidate();
                            surfaceView.requestLayout();
//                            Log.e("NOPE_SURFACE_NEW", intsCon(surfaceView.getMeasuredWidth(),surfaceView.getMeasuredHeight()));
//                            Log.e("NOPE_FRAME_NEW", intsCon(cameraFrame.getMeasuredWidth(),cameraFrame.getMeasuredHeight()));
                        }
                    });
//                    Log.e("DIM_SURFACE_F", intsCon(surfaceView.getMeasuredWidth(),surfaceView.getMeasuredHeight()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        };

        // initialize surface view for the camera
        surfaceHolder.addCallback(initCameraPreviewCallback);

        // handle processing of detected QR codes
        barcodeProcessor = new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if (surfaceView == null)
                    return;
                if (qrCodes.size()!=0) {
                    Barcode centerQR = null;

                    final int buffer_div = ((MainActivity)getActivity()).CameraRegionThreshold;

                    int camHeight = cameraSource.getPreviewSize().getHeight();
                    int camWidth = cameraSource.getPreviewSize().getWidth();
                    int viewHeight = surfaceView.getMeasuredHeight();
                    int viewWidth = surfaceView.getMeasuredWidth();
                    int visibleHeight = dashboardFrame.getMeasuredHeight();
                    int visibleWidth = dashboardFrame.getMeasuredWidth();

                    double camRatio = (double)camHeight/(double)camWidth;
                    double viewRatio = (double)viewHeight/(double)viewWidth;
                    double scale = 1d;
                    int visibleHeight_scaled = 0;
                    int visibleWidth_scaled = 0;
                    if (viewRatio < camRatio) {
                        scale = (double) camWidth / (double) visibleWidth;
                        visibleWidth_scaled = camWidth;
                        visibleHeight_scaled = (int)Math.floor((double)visibleHeight * scale);
                    } else {
                        scale = (double) camHeight / (double) visibleHeight;
                        visibleWidth_scaled = (int)Math.floor((double)visibleWidth * scale);
                        visibleHeight_scaled = camHeight;
                    }

                    int thresh_buffer, left_thresh, right_thresh, top_thresh, bottom_thresh;
                    int[] left_range = new int[2];
                    int[] right_range = new int[2];
                    int[] top_range = new int[2];
                    int[] bottom_range = new int[2];

                    if (viewRatio < camRatio) {
                        int t = Math.floorDiv((camHeight - visibleHeight_scaled), 2);
                        int b = camHeight - t;
                        int third = Math.floorDiv(b - t, 3);
                        top_thresh = t + third;
                        bottom_thresh = b - third;
                        left_thresh = Math.floorDiv(camWidth, 2) - Math.floorDiv(third, 2);
                        right_thresh = left_thresh + third;
                        thresh_buffer = Math.floorDiv(third, buffer_div);
                    } else {
                        int l = Math.floorDiv((camWidth - visibleWidth_scaled), 2);
                        int r = camWidth - l;
                        int third = Math.floorDiv(r - l,3);
                        left_thresh = l + third;
                        right_thresh = r - third;
                        top_thresh = Math.floorDiv(camHeight, 2) - Math.floorDiv(third, 2);
                        bottom_thresh = top_thresh + third;
                        thresh_buffer = Math.floorDiv(third, buffer_div);
                    }

                    left_range[0] = left_thresh;
                    left_range[1] = left_thresh + thresh_buffer;
                    right_range[0] = right_thresh - thresh_buffer;
                    right_range[1] = right_thresh;
                    top_range[0] = top_thresh;
                    top_range[1] = top_thresh + thresh_buffer;
                    bottom_range[0] = bottom_thresh - thresh_buffer;
                    bottom_range[1] = bottom_thresh;

                    for (int i=0; i<qrCodes.size(); i++) {
                        Rect bound = qrCodes.valueAt(i).getBoundingBox();
                        boolean inRange = (bound.left >= left_range[0]) && (bound.left <= left_range[1]);
                        inRange = inRange && (bound.top >= top_range[0]) && (bound.top <= top_range[1]);
                        inRange = inRange && (bound.right >= right_range[0]) && (bound.right <= right_range[1]);
                        inRange = inRange && (bound.bottom >= bottom_range[0]) && (bound.bottom <= bottom_range[1]);

                        if (inRange) {
                            centerQR = qrCodes.valueAt(i);
                            break;
                        }
                    }
                    if (centerQR != null) {
                        final String scannedQRCode = centerQR.displayValue;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (db.codeExists(scannedQRCode)) {
                                    localize(scannedQRCode);
                                    disableCamera();
                                } else {
                                    if (toast != null) {
                                        toast.cancel();
                                    }
                                    toast = Toast.makeText(getActivity(), "Invalid QR code: " + scannedQRCode, Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            }
                        });
                    }
                }
            }
        };
        barcodeDetector.setProcessor(barcodeProcessor);
    }

    // return resource id of floor plan image
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
        options.inMutable = true;
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
        disableCamera();
        accelerometer.unregister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disableCamera();
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