package com.clemcab.diskoverindoors.ui.home;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private TextView textView;
    private Toast toast = null;
    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private HomeViewModel homeViewModel;
    private BarcodeDetector barcodeDetector;
    private boolean isAlertActive = false;
    private DBHelper db;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        surfaceView = root.findViewById(R.id.camerapreview);
        textView = root.findViewById(R.id.text_home);

        homeViewModel = ViewModelProviders.of(this.getActivity()).get(HomeViewModel.class);
        return root;
    }
    @Override
    public void onViewCreated (View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize barcode reader and camera
        barcodeDetector = new BarcodeDetector.Builder(getActivity())
                .setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(getActivity(),barcodeDetector)
                .setRequestedPreviewSize(640,480)
                .setAutoFocusEnabled(true)
                .build();
        // initialize surface view for the camera
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.CAMERA}, 100);
                    textView.setText("Camera access is important to be able to scan QR codes.");
                    return;
                }
                textView.setText("Scan QR code to recalibrate your position.");
                try {
                    cameraSource.start(holder);
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

                if (qrCodes.size()!=0) {
                    final String scannedQRCode = qrCodes.valueAt(0).displayValue;

                    db = ((MainActivity)getActivity()).DBHelper;

                    getActivity().runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            if (db.codeExists(scannedQRCode)) {
                                if (!isAlertActive) {
                                    displayAlert(scannedQRCode);
                                    isAlertActive = true;
                                    cameraSource.stop();
                                }
                            } else {
                                if (toast != null){
                                    toast.cancel();
                                }
                                toast = Toast.makeText(getActivity(), "Invalid QR Code: " + scannedQRCode, Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    });
                }
            }
        });
    }

    public void displayAlert(final String qrCode) {
        final AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
        builder1.setTitle("Navigate");

        String[] args = qrCode.split("::",0);
        String message = "You are currently at " + args[0] + ".";
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