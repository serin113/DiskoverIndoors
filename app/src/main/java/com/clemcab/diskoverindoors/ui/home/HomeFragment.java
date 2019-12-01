package com.clemcab.diskoverindoors.ui.home;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.clemcab.diskoverindoors.BuildConfig;
import com.clemcab.diskoverindoors.BuildingData;
import com.clemcab.diskoverindoors.DBHelper;
import com.clemcab.diskoverindoors.MainActivity;
import com.clemcab.diskoverindoors.R;
import com.clemcab.diskoverindoors.ui.notifications.NavigationData;
import com.clemcab.diskoverindoors.ui.notifications.NotificationsViewModel;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private TextView textView;
    private Toast toast = null;
    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private HomeViewModel homeViewModel;
    private NotificationsViewModel notificationsViewModel;
    private BarcodeDetector barcodeDetector;
    private FrameLayout cameraFrame;
    private boolean isAlertActive = false;
    private DBHelper db;
    private View navigationButton;
    private View locationButton;
    private int surfaceViewWidth;
    private int surfaceViewHeight;

    String intsCon(int a, int b) {
        return Integer.toString(a) + " " + Integer.toString(b);
    }
    String intsCon(double a, double b) {
        return Double.toString(a) + " " + Double.toString(b);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        db = ((MainActivity) this.getActivity()).DBHelper;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        surfaceView = root.findViewById(R.id.camerapreview);
        textView = root.findViewById(R.id.text_home);
        navigationButton = getActivity().findViewById(R.id.navigation_navigation);
        locationButton = getActivity().findViewById(R.id.navigation_locations);
        notificationsViewModel = ViewModelProviders.of(this).get(NotificationsViewModel.class);
        homeViewModel = ViewModelProviders.of(this.getActivity()).get(HomeViewModel.class);
        return root;
    }
    @Override
    public void onViewCreated (View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize barcode reader and camera
        barcodeDetector = new BarcodeDetector.Builder(getActivity()).setBarcodeFormats(Barcode.QR_CODE).build();

        final SurfaceHolder.Callback initCameraPreviewCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.CAMERA}, 100);
                    textView.setText(getActivity().getString(R.string.camera_required));
                    return;
                }
                textView.setText(getActivity().getString(R.string.scan_to_start));
                try {
                    cameraFrame = getActivity().findViewById(R.id.previewframe);
                    holder.setKeepScreenOn(true);
                    int viewHeight = cameraFrame.getMeasuredHeight();
                    int viewWidth = cameraFrame.getMeasuredWidth();

                    CameraSource.Builder cameraSourceBuilder = new CameraSource.Builder(getActivity(),barcodeDetector)
                            .setFacing(CameraSource.CAMERA_FACING_BACK)
                            .setAutoFocusEnabled(true);

                    if ((double)viewHeight/(double)viewWidth >= 1) {
                        cameraSourceBuilder = cameraSourceBuilder.setRequestedPreviewSize(720,1000);
                    } else {
                        cameraSourceBuilder = cameraSourceBuilder.setRequestedPreviewSize(1000,720);
                    }

                    cameraSource = cameraSourceBuilder.build();
                    cameraSource.start(holder);
                    int camHeight = cameraSource.getPreviewSize().getHeight();
                    int camWidth = cameraSource.getPreviewSize().getWidth();

                    double viewRatio = (double)viewHeight/(double)viewWidth;
                    double camRatio = (double)camHeight/(double)camWidth;
                    double scale = 1d;
                    if (viewRatio < camRatio)
                        scale = (double) viewWidth / (double) camWidth;
                    else
                        scale = (double) viewHeight / (double) camHeight;
                    int newHeight = (int)Math.floor((double)camHeight * scale);
                    int newWidth = (int)Math.floor((double)camWidth * scale);
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
                            surfaceView.getHolder().setFixedSize(surfaceViewWidth,surfaceViewHeight);
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
                } catch (IOException e) {
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
        surfaceView.getHolder().addCallback(initCameraPreviewCallback);

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
                    int visibleHeight = cameraFrame.getMeasuredHeight();
                    int visibleWidth = cameraFrame.getMeasuredWidth();

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


//                    Log.e("RANGE_","============================");
//                    Log.e("RANGE_SURFACE", intsCon(surfaceView.getMeasuredWidth(), surfaceView.getMeasuredHeight()));
//                    Log.e("RANGE_CAMERASOURCE", intsCon(cameraSource.getPreviewSize().getWidth(), cameraSource.getPreviewSize().getHeight()));
//                    Log.e("RANGE_VISIBLEFRAME", intsCon(visibleWidth, visibleHeight));
//                    Log.e("RANGE_VR_CR", intsCon(viewRatio,camRatio));
//                    Log.e("RANGE_VR<CR", Boolean.toString(viewRatio < camRatio));
//                    Log.e("RANGE_SCALE", Double.toString(scale));

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
//                        Log.e("RANGE_LT", intsCon(bound.left, bound.top));
//                        Log.e("RANGE_RB", intsCon(bound.right, bound.bottom));
//                        Log.e("RANGE_THRESH_BUF", Integer.toString(thresh_buffer));
//                        Log.e("RANGE_THRESH_LT", intsCon(left_range[0],top_range[0]));
//                        Log.e("RANGE_THRESH_RB", intsCon(right_range[1],bottom_range[1]));
//                        Log.e("RANGE_IN_L", Boolean.toString((bound.left >= left_range[0]) && (bound.left <= left_range[1])));
//                        Log.e("RANGE_IN_T", Boolean.toString((bound.top >= top_range[0]) && (bound.top <= top_range[1])));
//                        Log.e("RANGE_IN_R", Boolean.toString((bound.right >= right_range[0]) && (bound.right <= right_range[1])));
//                        Log.e("RANGE_IN_B", Boolean.toString((bound.bottom >= bottom_range[0]) && (bound.bottom <= bottom_range[1])));

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
                                    if (!isAlertActive) {
                                        displayAlert(scannedQRCode);
                                        isAlertActive = true;
                                        cameraSource.stop();
                                    }
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
        });
    }

    public void displayAlert(final String qrCode) {
        final MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(getActivity());

        String[] args = qrCode.split("::",0);
        String building = args[0];
        Log.e("StringTest", "from QR code " + building + " " + building.length() );
        int level = Integer.parseInt(args[1]);

        BuildingData buildingData = db.getBuildingfromName(building);

        String floorName = buildingData.floorNameFromLevel(level);
        String buildingAlias = buildingData.alias;
        String buildingName = buildingData.name;
        Log.e("StringTest", "from database " + buildingAlias + " " + buildingAlias.length() );

        builder1.setTitle("Navigate");

        String message = "You are at " + floorName + " of " + buildingName + " (" + buildingAlias + ")";
        builder1.setMessage(message);

        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Choose Destination",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        View currView = getView();
                        if (currView != null && isAlertActive) {
                            homeViewModel.setQrCode(qrCode);
                            ((MainActivity) getActivity()).navView.setVisibility(View.VISIBLE);
                            navigationButton.setEnabled(true);
                            locationButton.setEnabled(true);
                            Navigation.findNavController(currView).navigate(R.id.action_select_destination);
                            isAlertActive = false;
                        }
                    }
                });

        builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    isAlertActive = false;
                    try {
                        cameraSource.start(surfaceView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        });

        builder1.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isAlertActive = false;
                try {
                    cameraSource.start(surfaceView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        AlertDialog navigateDialog = builder1.create();
        navigateDialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraSource != null)
            cameraSource.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            cameraSource.start(surfaceView.getHolder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (toast != null) {
            toast.cancel();
        }
    }
}