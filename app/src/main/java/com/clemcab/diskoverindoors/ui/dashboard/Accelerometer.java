package com.clemcab.diskoverindoors.ui.dashboard;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.AbstractList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;

public class Accelerometer {
    public interface Listener {
        void onTranslation(float x_vel,float y_vel, float z_vel, float x, float timeDiff);
    }
    private Listener listener;
    public void setListener(Listener l) {
        listener = l;
    }
    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor gravAccelSensor;
    private Sensor magSensor;
    private SensorEventListener sensorEventListener;

    private float[] gravAccelReading;
    private float[] magReading;
    private float[] rotationMatrix;
    private List<Float> sensorValsX;
    private List<Float> sensorValsY;
    private List<Float> sensorValsZ;
    private long sensorVals_timeStart;

    /* SETTINGS */
    private final int sensorBufferMax   = 4;
    private final float accelThreshMax  = 3f; // not sure
    private final float velThreshMin    = 0f;
    private final float velThreshMax    = 2f; // probably?
    private final boolean zAxisEnabled  = true;
    private final boolean groundLock    = true;
    private int usSamplingDelayAccel    = 16667;

    private float Simp(List<Float> vals, float timeDiff) {
        float velocity =
            (
                (
                        (float)sensorBufferMax / (1000000f/timeDiff)
                )/8f
            ) * (
                vals.get(0) + 3f*(vals.get(1)+vals.get(2)) + vals.get(3)
            );
        return velocity;
    }

    private void updateVals(List<Float> vals, float val) {
        if (vals.size() >= sensorBufferMax)
            vals.clear();
        vals.add(val);
    }

    Accelerometer (Context context) {
        gravAccelReading = new float[3];
        magReading = new float[3];
        rotationMatrix = new float[9];

        sensorValsX = new ArrayList<>(sensorBufferMax);
        sensorValsY = new ArrayList<>(sensorBufferMax);
        sensorValsZ = new ArrayList<>(sensorBufferMax);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    if (groundLock) {
                        System.arraycopy(sensorEvent.values, 0, gravAccelReading,
                                0, gravAccelReading.length);
                    }

                } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    if (groundLock) {
                        System.arraycopy(sensorEvent.values, 0, magReading,
                                0, magReading.length);
                    }

                } else if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    if (sensorValsX.size() == sensorBufferMax)
                        sensorVals_timeStart = System.nanoTime();

                    float x_accel = sensorEvent.values[0];
                    float y_accel = sensorEvent.values[1];
                    float z_accel = sensorEvent.values[2];

                    x_accel = x_accel<=accelThreshMax ? x_accel : accelThreshMax;
                    y_accel = y_accel<=accelThreshMax ? y_accel : accelThreshMax;
                    z_accel = z_accel<=accelThreshMax ? z_accel : accelThreshMax;

                    updateVals(sensorValsX, sensorEvent.values[0]);
                    updateVals(sensorValsY, sensorEvent.values[1]);
                    updateVals(sensorValsZ, sensorEvent.values[2]);

                    if (sensorValsX.size() >= sensorBufferMax) {
                        float timeDiff = ((float)System.nanoTime() - sensorVals_timeStart) / 1000f;
                        SensorManager.getRotationMatrix(
                                rotationMatrix,
                                null,
                                gravAccelReading,
                                magReading);

                        float x_vel = Simp(sensorValsX, timeDiff);
                        float y_vel = Simp(sensorValsY, timeDiff);
                        float z_vel = Simp(sensorValsZ, timeDiff);

                        boolean inThreshVelMin =
                            Math.abs(x_vel) >= velThreshMin &&
                            Math.abs(y_vel) >= velThreshMin &&
                            Math.abs(z_vel) >= velThreshMin;

                        if (inThreshVelMin) {
                            boolean inThreshVelMax =
                                Math.abs(x_vel) <= velThreshMax &&
                                Math.abs(y_vel) <= velThreshMax &&
                                Math.abs(z_vel) <= velThreshMax;
                            if (inThreshVelMax) {
                                if (groundLock) {
                                    float x_vel_t = x_vel * rotationMatrix[0] + y_vel * rotationMatrix[1] + z_vel * rotationMatrix[2];
                                    float y_vel_t = x_vel * rotationMatrix[3] + y_vel * rotationMatrix[4] + z_vel * rotationMatrix[5];
                                    float z_vel_t = x_vel * rotationMatrix[6] + y_vel * rotationMatrix[7] + z_vel * rotationMatrix[8];
                                    x_vel = x_vel_t;
                                    y_vel = y_vel_t;
                                    z_vel = z_vel_t;
                                }
                            } else {
                                x_vel = velThreshMax;
                                y_vel = velThreshMax;
                                z_vel = velThreshMax;
                            }
                        } else {
                            x_vel = 0f;
                            y_vel = 0f;
                            z_vel = 0f;
                        }

                        if (listener != null) {
                            if (zAxisEnabled)
                                listener.onTranslation(x_vel, y_vel, z_vel, x_accel, (1000000f/(float)timeDiff));
                            else
                                listener.onTranslation(x_vel, y_vel, 0, x_accel, (1000000f/(float)timeDiff));
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

    }
    public void register() {
        if (accelSensor.getMinDelay() > usSamplingDelayAccel)
            usSamplingDelayAccel = accelSensor.getMinDelay();
        else if (usSamplingDelayAccel > accelSensor.getMaxDelay() && accelSensor.getMaxDelay() > 0)
            usSamplingDelayAccel = accelSensor.getMaxDelay();

        if (gravAccelSensor.getMinDelay() > usSamplingDelayAccel)
            usSamplingDelayAccel = gravAccelSensor.getMinDelay();
        else if (usSamplingDelayAccel > gravAccelSensor.getMaxDelay() && gravAccelSensor.getMaxDelay() > 0)
            usSamplingDelayAccel = gravAccelSensor.getMaxDelay();

        if (magSensor.getMinDelay() > usSamplingDelayAccel)
            usSamplingDelayAccel = magSensor.getMinDelay();
        else if (usSamplingDelayAccel > magSensor.getMaxDelay() && magSensor.getMaxDelay() > 0)
            usSamplingDelayAccel = magSensor.getMaxDelay();

        sensorManager.registerListener(sensorEventListener, accelSensor, usSamplingDelayAccel);
        sensorManager.registerListener(sensorEventListener, gravAccelSensor, usSamplingDelayAccel);
        sensorManager.registerListener(sensorEventListener, magSensor, usSamplingDelayAccel);
    }
    public void unregister() {
        sensorManager.unregisterListener(sensorEventListener);
    }
}
