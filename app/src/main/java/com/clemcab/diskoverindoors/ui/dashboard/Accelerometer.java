package com.clemcab.diskoverindoors.ui.dashboard;

import android.content.Context;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Timer;

public class Accelerometer {
    /* SETTINGS */
    private final double accelThreshMin  = 0.03d;    // accelerometer min threshold
    private final double accelThreshMax  = 100d;     // accelerometer max threshold
    //    private final float velThreshMin    = 0.02f;
    private final double velThreshMin    = 0.2d;     // approx. velocity min threshold
    private final double velThreshMax    = 3d;       // approx. velocity max threshold

    private final double abaqConstVel    = 1d;       // AbAq algo: const. mov't rate

    private final boolean abaqConst     = true;     // enable AbAq algo
    private final boolean groundLock    = false;    // enable orientation locking
    private final boolean gyroFusion    = false;    // enable gyro sensor fusion
    private int usSamplingDelayAccel    = 16667;    // suggested sensor update rate (microseconds)

    public interface Listener {
        void onTranslation(double x_vel, double y_vel, double z_vel, double timeDiff, float azimuth);
        void onRotation(float azimuth);
        void onPressureChange(float altitude);
    }
    private Listener listener;
    public void setListener(Listener l) {
        listener = l;
    }
    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor barometerSensor;
    private Sensor gravAccelSensor;
    private Sensor magSensor;
    private Sensor gyroSensor;
    private SensorEventListener sensorEventListener;

    private float[] gravAccelReading;
    private float[] magReading;
    private float[] rotationMatrix;

    private float azimuth;
    private boolean hasBarometer;
    private List<Double> sensorValsX;
    private List<Double> sensorValsY;
    private List<Double> sensorValsZ;
    private long sensorVals_timeStart;

    private float azimuth_offset = 0f;
    private float[] azimuth_offset_matrix;

    /* USED ONLY WHEN gyroFusion IS ENABLED */
    private float[] gyroReading;
    private float[] accMagOrientation;
    private float[] gyroOrientation;
    private float[] gyroMatrix;
    private float[] fusedOrientation;

    public void setAzimuthOffset(float offset) {
        Matrix m = new Matrix();
        m.setRotate(offset);
        this.azimuth_offset = offset;
        m.getValues(this.azimuth_offset_matrix);
    }
    public void printAzimuth() {
        Log.e("AZIMUTH", Float.toString(this.azimuth));
        Log.e("AZIMUTH_OFFSET", Float.toString(this.azimuth_offset));
    }

    public boolean hasBarometer() {
        return this.hasBarometer;
    }

    /* GYRO FUSION STUFF */
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    /* GYRO FUSION STUFF */
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    /* GYRO FUSION STUFF */
    public final float EPSILON = 0.000000001f;
    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    /* GYRO FUSION STUFF */
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyroReading, 0, 3);
            getRotationVectorFromGyro(gyroReading, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    /* GYRO FUSION STUFF */
    private static final int TIME_CONSTANT = 30;
    private static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer = new Timer();
    class calculateFusedOrientationTask extends TimerTask {

        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            fusedOrientation[0] =
                    FILTER_COEFFICIENT * gyroOrientation[0]
                            + oneMinusCoeff * accMagOrientation[0];

            fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1]
                            + oneMinusCoeff * accMagOrientation[1];

            fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2]
                            + oneMinusCoeff * accMagOrientation[2];

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        }
    }

    /* SIMPSON'S 3/8 RULE ALGO */
    private double Simp(List<Double> vals, double timeDiff) {
        double velocity =
            (
                timeDiff * (
                    vals.get(0) + 3d*(vals.get(1)+vals.get(2)) + vals.get(3)
                )
            ) / 2000000000d;
        return velocity;
    }

    /* ACCELEROMETER CLASS INITIATION */
    public Accelerometer (Context context) {
        azimuth_offset_matrix = new float[9];
        new Matrix().getValues(azimuth_offset_matrix);

        gravAccelReading = new float[3];
        magReading = new float[3];
        rotationMatrix = new float[9];

        if (gyroFusion) {
            fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),1000, TIME_CONSTANT);
            gyroReading = new float[3];
            accMagOrientation = new float[3];
            gyroOrientation = new float[3];
            gyroMatrix = new float[9];
            fusedOrientation = new float[3];
            gyroOrientation[0] = 0f;
            gyroOrientation[1] = 0f;
            gyroOrientation[2] = 0f;
            gyroMatrix[0] = 1f;
            gyroMatrix[1] = 0f;
            gyroMatrix[2] = 0f;
            gyroMatrix[3] = 0f;
            gyroMatrix[4] = 1f;
            gyroMatrix[5] = 0f;
            gyroMatrix[6] = 0f;
            gyroMatrix[7] = 0f;
            gyroMatrix[8] = 1f;
        }

        sensorValsX = new ArrayList<>(4);
        sensorValsY = new ArrayList<>(4);
        sensorValsZ = new ArrayList<>(4);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        barometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        gravAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                switch (sensorEvent.sensor.getType()) {
                    case Sensor.TYPE_PRESSURE:
                        float pressure = sensorEvent.values[0];
                        float altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure);

                        if (listener != null)
                            listener.onPressureChange(altitude);
                    case Sensor.TYPE_ACCELEROMETER:
                        if (groundLock || abaqConst) {
                            System.arraycopy(sensorEvent.values, 0, gravAccelReading,
                                    0, gravAccelReading.length);
                            if (groundLock && gyroFusion && !abaqConst) {
                                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravAccelReading, magReading))
                                    SensorManager.getOrientation(rotationMatrix, accMagOrientation);
                            }
                        }
                        break;

                    case Sensor.TYPE_MAGNETIC_FIELD:
                        if (groundLock || abaqConst) {
                            System.arraycopy(sensorEvent.values, 0, magReading,
                                    0, magReading.length);
                            if (abaqConst) {
                                float[] orientationVals = new float[3];
                                SensorManager.getOrientation(rotationMatrix, orientationVals);
                                azimuth = (float)(((double)orientationVals[0]*180d)/Math.PI) + azimuth_offset;
                                azimuth = azimuth < 0f ? azimuth + 360f : (azimuth >= 360f ? azimuth - 360f : azimuth);
                                if (listener != null)
                                    listener.onRotation(azimuth);
                            }
                        }
                        break;

                    case Sensor.TYPE_GYROSCOPE:
                        if (gyroFusion)
                            gyroFunction(sensorEvent);
                        break;

                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        if (sensorValsX.size() == 0)
                            sensorVals_timeStart = sensorEvent.timestamp;

                        if (groundLock || abaqConst) {
                            SensorManager.getRotationMatrix(
                                    rotationMatrix,
                                    null,
                                    gravAccelReading,
                                    magReading);
                        }

                        double x_accel_t = sensorEvent.values[0];
                        double y_accel_t = sensorEvent.values[1];
                        double z_accel_t = sensorEvent.values[2];
                        float[] rot;
                        if (gyroFusion)
                            rot = gyroMatrix;
                        else
                            rot = rotationMatrix;

                        double x_accel = groundLock && !abaqConst ?
                                x_accel_t * (double)rot[0] + y_accel_t * (double)rot[1] + z_accel_t * (double)rot[2]
                                : x_accel_t;
                        double y_accel = groundLock && !abaqConst ?
                                x_accel_t * (double)rot[3] + y_accel_t * (double)rot[4] + z_accel_t * (double)rot[5]
                                : y_accel_t;
                        double z_accel = groundLock && !abaqConst ?
                                x_accel_t * (double)rot[6] + y_accel_t * (double)rot[7] + z_accel_t * (double)rot[8]
                                : z_accel_t;

                        x_accel_t = Math.abs(x_accel);
                        y_accel_t = Math.abs(y_accel);
                        z_accel_t = Math.abs(z_accel);
                        x_accel = x_accel_t <= accelThreshMax ? (x_accel_t >= accelThreshMin ? x_accel : 0d) : accelThreshMax;
                        y_accel = y_accel_t <= accelThreshMax ? (y_accel_t >= accelThreshMin ? y_accel : 0d) : accelThreshMax;
                        z_accel = z_accel_t <= accelThreshMax ? (z_accel_t >= accelThreshMin ? z_accel : 0d) : accelThreshMax;

                        if (sensorValsX.size() < 4) {
                            sensorValsX.add(x_accel);
                            sensorValsY.add(y_accel);
                            sensorValsZ.add(z_accel);
                        } else {
                            double timeDiff = (double)(sensorEvent.timestamp - sensorVals_timeStart);

                            double x_vel = Simp(sensorValsX, timeDiff);
                            double y_vel = Simp(sensorValsY, timeDiff);
                            double z_vel = Simp(sensorValsZ, timeDiff);
                            double x_vel_t = Math.abs(x_vel);
                            double y_vel_t = Math.abs(y_vel);
                            double z_vel_t = Math.abs(z_vel);
                            x_vel = (x_vel_t <= velThreshMax) ?
                                    (x_vel_t >= velThreshMin ? x_vel : 0d) :
                                    (x_vel >= 0d ? velThreshMax : -velThreshMax);
                            y_vel = (y_vel_t <= velThreshMax) ?
                                    (y_vel_t >= velThreshMin ? y_vel : 0d) :
                                    (x_vel >= 0d ? velThreshMax : -velThreshMax);
                            z_vel = (z_vel_t <= velThreshMax) ?
                                    (z_vel_t >= velThreshMin ? z_vel : 0d) :
                                    (x_vel >= 0d ? velThreshMax : -velThreshMax);

                            if (abaqConst) {
                                double velMag = Math.sqrt(x_vel*x_vel + y_vel*y_vel + z_vel*z_vel);
                                if (velMag>0d) {
                                    x_vel_t = 0d;
                                    y_vel_t = -1d * velMag;
                                    z_vel_t = 0d;
                                    x_vel = x_vel_t * (double) rot[0] + y_vel_t * (double) rot[3] + z_vel_t * (double) rot[6];
                                    y_vel = x_vel_t * (double) rot[1] + y_vel_t * (double) rot[4] + z_vel_t * (double) rot[7];
                                    z_vel = x_vel_t * (double) rot[2] + y_vel_t * (double) rot[5] + z_vel_t * (double) rot[8];

                                    if (azimuth_offset != 0) {
                                        rot = azimuth_offset_matrix;
                                        x_vel_t = x_vel;
                                        y_vel_t = y_vel;
                                        z_vel_t = z_vel;
                                        x_vel = x_vel_t * (double) rot[0] + y_vel_t * (double) rot[1] + z_vel_t * (double) rot[2];
                                        y_vel = x_vel_t * (double) rot[3] + y_vel_t * (double) rot[4] + z_vel_t * (double) rot[5];
                                        z_vel = x_vel_t * (double) rot[6] + y_vel_t * (double) rot[7] + z_vel_t * (double) rot[8];
                                    }

                                    x_vel = (Math.abs(x_vel) <= abaqConstVel) ?
                                            x_vel :
                                            (x_vel >= 0d ? abaqConstVel : -abaqConstVel);
                                    y_vel = (Math.abs(y_vel) <= abaqConstVel) ?
                                            y_vel :
                                            (y_vel >= 0d ? abaqConstVel : -abaqConstVel);
                                    z_vel = (Math.abs(z_vel) <= abaqConstVel) ?
                                            z_vel :
                                            (z_vel >= 0d ? abaqConstVel : -abaqConstVel);
                                }
                            }

    //                        float[] orientationVals = new float[3];
    //                        SensorManager.getOrientation(rot, orientationVals);
    //                        azimuth = (float)(((double)orientationVals[0]*180d)/Math.PI);
    //                        azimuth = azimuth < 0f ? azimuth + 360f : azimuth;
                            if (listener != null)
                                listener.onTranslation(x_vel, y_vel, z_vel, 1000000000d/timeDiff, azimuth);

                            sensorValsX.clear();
                            sensorValsY.clear();
                            sensorValsZ.clear();
                        }
                        break;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    public void register() {
        sensorManager.registerListener(sensorEventListener, accelSensor, usSamplingDelayAccel);
        hasBarometer = sensorManager.registerListener(sensorEventListener, barometerSensor, usSamplingDelayAccel);
        if (groundLock || gyroFusion || abaqConst) {
            sensorManager.registerListener(sensorEventListener, gravAccelSensor, usSamplingDelayAccel);
            sensorManager.registerListener(sensorEventListener, magSensor, usSamplingDelayAccel);
        }
        if (gyroFusion)
            sensorManager.registerListener(sensorEventListener, gyroSensor, usSamplingDelayAccel);
    }
    public void unregister() {
        sensorManager.unregisterListener(sensorEventListener);
    }
}
