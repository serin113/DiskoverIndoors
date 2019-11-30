package com.clemcab.diskoverindoors.ui.dashboard;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Matrix;
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

public class DashboardFragment extends Fragment {

    private Accelerometer accelerometer;
    private NotificationsViewModel notificationsViewModel;
    private DBHelper db;
    private ImageView imageView;
    private Bitmap mutableUserMarker;
    private Canvas mapCanvas;
    private NavigationData navigationData = null;
    private BuildingData buildingData;

    // settings for resizing image to bitmaps
    private final int MAP_REQ_WIDTH = 500;
    private final int MAP_REQ_HEIGHT = 500;
    private final int USER_MARKER_REQ_WIDTH = 30;
    private final int USER_MARKER_REQ_HEIGHT = 30;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        imageView = root.findViewById(R.id.imageView);

        db = ((MainActivity)getActivity()).DBHelper;
        navigationData = ( (MainActivity) (getActivity()) ).navData;

        cameraVisible = false;
        surfaceView = root.findViewById(R.id.camerapreview_lock);
        surfaceHolder = surfaceView.getHolder();
        dashboardFrame = root.findViewById(R.id.dashboardCameraFrame);
        qrfab = root.findViewById(R.id.qrfab);
        dashboardViewModel = ViewModelProviders.of(this).get(DashboardViewModel.class);

        // initiate the 2D scene
        if (navigationData != null && db != null) {
            buildingData = db.getBuildingfromName(navigationData.building);
            int drawableId = getDrawableId(navigationData.building, navigationData.start_floor);
            if (drawableId > 0) {
                // draw the map canvas
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(getActivity().getResources(), drawableId, options);
                Bitmap map = decodeSampledBitmapFromResource(getActivity().getResources(), drawableId, MAP_REQ_WIDTH, MAP_REQ_HEIGHT);
                Bitmap mutableMap = map.copy(Bitmap.Config.ARGB_8888, true);
                mapCanvas = new Canvas(mutableMap);

                // draw user marker on canvas
                Bitmap userMarker = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.map_pointer, USER_MARKER_REQ_WIDTH, USER_MARKER_REQ_HEIGHT);
                mutableUserMarker = userMarker.copy(Bitmap.Config.ARGB_8888, true);

                int mapWidth = mutableMap.getWidth();
                int mapHeight = mutableMap.getHeight();
                int userMarkerWidth = mutableUserMarker.getWidth();
                int userMarkerHeight = mutableUserMarker.getHeight();
                double x_coord = getRelativeCoords((double) navigationData.start_x, (double) buildingData.xscale, mapWidth);
                double y_coord = getRelativeCoords((double) navigationData.start_y * -1, (double) buildingData.yscale, mapHeight);

                mapCanvas.drawBitmap(mutableUserMarker, (float) x_coord - (userMarkerWidth / 2), (float) y_coord - (userMarkerHeight / 2), null);

                imageView.setImageDrawable(new BitmapDrawable(getActivity().getResources(), mutableMap));

            }
        }

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

                Matrix matrix = new Matrix();
                matrix.setTranslate((float)deltaX, (float)deltaY);

//                map_pointer.setX(x_coord - (float)deltaX);
//                map_pointer.setY(y_coord + (float)deltaY);
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

    @Override
    public void onViewCreated (View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        qrfab.setOnClickListener(
            new View.OnClickListener() {
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
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
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

                    final int buffer_div = 7;

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
//                        getActivity().runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (db.codeExists(scannedQRCode)) {
//                                    if (!isAlertActive) {
//                                        displayAlert(scannedQRCode);
//                                        isAlertActive = true;
//                                        cameraSource.stop();
//                                    }
//                                } else {
//                                    if (toast != null) {
//                                        toast.cancel();
//                                    }
//                                    toast = Toast.makeText(getActivity(), "Invalid QR code: " + scannedQRCode, Toast.LENGTH_SHORT);
//                                    toast.show();
//                                }
//                            }
//                        });
                    }
                }
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