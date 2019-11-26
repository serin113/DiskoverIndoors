package com.clemcab.diskoverindoors.ui.home;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.Navigation;

import com.clemcab.diskoverindoors.DBHelper;
import com.clemcab.diskoverindoors.MainActivity;
import com.clemcab.diskoverindoors.R;
import com.clemcab.diskoverindoors.ui.notifications.NotificationsFragment;
import com.clemcab.diskoverindoors.ui.notifications.NotificationsViewModel;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

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

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        surfaceView = root.findViewById(R.id.camerapreview);
        textView = root.findViewById(R.id.text_home);
        notificationsViewModel = ViewModelProviders.of(this).get(NotificationsViewModel.class);
        homeViewModel = ViewModelProviders.of(this.getActivity()).get(HomeViewModel.class);
        return root;
    }
    @Override
    public void onViewCreated (final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize barcode reader and camera
        barcodeDetector = new BarcodeDetector.Builder(getActivity())
                .setBarcodeFormats(Barcode.QR_CODE).build();

        // initialize surface view for the camera
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.CAMERA}, 100);
                    textView.setText("Camera access is required to scan QR codes.");
                    return;
                }
                textView.setText("Scan QR code to start navigating.");
                try {
                    cameraFrame = getActivity().findViewById(R.id.previewframe);
                    holder.setKeepScreenOn(true);
                    int viewHeight = surfaceView.getHeight();
                    int viewWidth = surfaceView.getWidth();
                    cameraSource = new CameraSource.Builder(getActivity(),barcodeDetector)
                            .setRequestedPreviewSize(viewWidth,viewHeight)
                            .setAutoFocusEnabled(true)
                            .build();
                    cameraSource.start(holder);
                    int camHeight = cameraSource.getPreviewSize().getHeight();
                    int camWidth = cameraSource.getPreviewSize().getWidth();

                    double viewRatio = (double)viewHeight/(double)viewWidth;
                    double camRatio = (double)camHeight/(double)camWidth;
                    boolean isLandscape = camRatio < 1d;

                    double scale = 1d;
                    if (isLandscape)
                        scale = (double) viewWidth / (double)camWidth;
                    else {
                        if (viewRatio >= camRatio)
                            scale = (double) viewWidth / (double) camWidth;
                        else
                            scale = (double) viewHeight / (double) camHeight;
                    }
                    cameraFrame.getLayoutParams().height = (int)Math.floor((double)camHeight * scale);
                    cameraFrame.getLayoutParams().width = (int)Math.floor((double)camWidth * scale);
                    cameraFrame.requestLayout();
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
        });
        // handle processing of detected QR codes
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                final int camWidth = cameraFrame.getLayoutParams().width;
                final int camHeight = cameraFrame.getLayoutParams().height;
                if (qrCodes.size()!=0) {
                    Barcode centerQR = null;
                    final int thresh_buffer = (int)Math.floorDiv(camWidth,27);
                    final int left_thresh = (int)Math.floorDiv(camWidth,3);
                    final int right_thresh = camWidth - left_thresh;
                    final int top_thresh = Math.floorDiv(camHeight,2) - Math.floorDiv(left_thresh,2);
                    final int bottom_thresh = top_thresh + left_thresh;

                    final int[] left_range = {left_thresh-thresh_buffer, left_thresh+thresh_buffer};
                    final int[] top_range = {top_thresh-thresh_buffer, top_thresh+thresh_buffer};
                    final int[] right_range = {right_thresh-thresh_buffer, right_thresh+thresh_buffer};
                    final int[] bottom_range = {bottom_thresh-thresh_buffer, bottom_thresh+thresh_buffer};

                    for (int i=0; i<qrCodes.size(); i++) {
                        Rect bound = qrCodes.valueAt(i).getBoundingBox();
                        boolean inRange = true;
                        inRange = inRange && (bound.left >= left_range[0]) && (bound.left <= left_range[1]);
                        inRange = inRange && (bound.top >= top_range[0]) && (bound.top <= top_range[1]);
                        inRange = inRange && (bound.right >= right_range[0]) && (bound.right <= right_range[1]);
                        inRange = inRange && (bound.bottom >= bottom_range[0]) && (bound.bottom <= bottom_range[1]);

                        if (inRange) {
                            centerQR = qrCodes.valueAt(i);
                        }
                    }
                    if (centerQR != null) {
                        final String scannedQRCode = centerQR.displayValue;

                        db = ((MainActivity) getActivity()).DBHelper;

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
                                    toast = Toast.makeText(getActivity(), "Invalid QR Code: " + scannedQRCode, Toast.LENGTH_SHORT);
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
        final AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
        builder1.setTitle("Navigate");

        String[] args = qrCode.split("::",0);
        String building = args[0];
        String room = args[2];
        String level;
        switch (args[1]){
            case "1":
                level = "lower ground floor";
                break;
            case "2":
                level = "1st floor";
                break;
            case "3":
                level = "2nd floor";
                break;
            case "4":
                level = "3rd floor";
                break;
            default:
                level = null;
        }


        String message = "You are at " + room + ", " + level + " of " + building;
        builder1.setMessage(message);

        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Choose Destination",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        View currView = getView();
                        if (currView != null && isAlertActive) {
                            Navigation.findNavController(currView).navigate(R.id.action_select_destination);
                            homeViewModel.setQrCode(qrCode);
                            isAlertActive = false;
                        }
                    }
                });

        builder1.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
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
        AlertDialog navigateDialog = builder1.create();
        navigateDialog.show();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (toast != null) {
            toast.cancel();
        }
    }
}