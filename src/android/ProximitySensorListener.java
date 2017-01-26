/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

// reference: https://github.com/gdmec/lightsensor  

package org.awokenwell.proximity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;

import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import android.view.WindowManager.LayoutParams;

/**
 * This class listens to the proximity sensor and stores the latest value. 
 */
public class ProximitySensorListener extends CordovaPlugin implements SensorEventListener {

    public static int STOPPED = 0;
    public static int STARTING = 1;
    public static int RUNNING = 2;
    public static int ERROR_FAILED_TO_START = 3;
    
    // sensor result 
    public static int NEAR = 1;
    public static int FAR = 0;

    int status;                         // status of listener
    int proximity;                        // most recent proximity value
    long timeStamp;                     // time of most recent value
    long lastAccessTime;                // time the value was last retrieved

    private SensorManager sensorManager;// Sensor manager
    Sensor mSensor;                     // Compass sensor returned by sensor manager

    private PowerManager.WakeLock pmWakeLock;
    private PowerManager.WakeLock partialWakeLock;

    private LayoutParams defaultLayoutParams = null;

    private CallbackContext callbackContext;

    /**
     * Constructor.
     */
    public ProximitySensorListener() {
        this.proximity = 0;
        this.timeStamp = 0;
        this.setStatus(ProximitySensorListener.STOPPED);
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @SuppressWarnings("deprecation")
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);

        try {
            PowerManager pm = (PowerManager) cordova.getActivity().getSystemService(Context.POWER_SERVICE);
            boolean supportProximity = false;
            Field f = PowerManager.class.getDeclaredField("PROXIMITY_SCREEN_OFF_WAKE_LOCK");
            int proximityScreenOffWakeLock = (Integer) f.get(null);

            if (android.os.Build.VERSION.SDK_INT >= 17) {
                Method method = pm.getClass().getDeclaredMethod("isWakeLockLevelSupported", int.class);
                supportProximity = (Boolean) method.invoke(pm, proximityScreenOffWakeLock);
            } else {
                Method method = pm.getClass().getDeclaredMethod("getSupportedWakeLockFlags");
                int supportedFlags = (Integer) method.invoke(pm);
                supportProximity = ((supportedFlags & proximityScreenOffWakeLock) != 0x0);
            }

            if (supportProximity) {
                this.pmWakeLock = pm.newWakeLock(proximityScreenOffWakeLock, "org.awokenwell.proximity");
                this.pmWakeLock.setReferenceCounted(false);
            }

            this.partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "org.awokenwell.proximity");
        } catch (Exception e) {
            System.out.println("Impossible to get power manager supported wake lock flags");
        }
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action                The action to execute.
     * @param args                  JSONArry of arguments for the plugin.
     * @param callbackS=Context     The callback id used when calling back into JavaScript.
     * @return                      True if the action was valid.
     * @throws JSONException 
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("start")) {
            this.start();
        }
        else if (action.equals("stop")) {
            this.stop();
        }
        else if (action.equals("getProximityState")) {
            int responseValue = this.proximity;
            callbackContext.success(responseValue);
            return true;
        }
        else {
            // Unsupported action
            return false;
        }
        return true;
    }

    /**
     * Called when listener is to be shut down and object is being destroyed.
     */
    public void onDestroy() {
        this.stop();
    }

    /**
     * Called when app has navigated and JS listeners have been destroyed.
     */
    public void onReset() {
        this.stop();
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * Start listening for compass sensor.
     *
     * @return          status of listener
     */
    public int start() {

        // If already starting or running, then just return
        if ((this.status == ProximitySensorListener.RUNNING) || (this.status == ProximitySensorListener.STARTING)) {
            return this.status;
        }

        // Get proximity sensor from sensor manager
        @SuppressWarnings("deprecation")
        List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_PROXIMITY);

        if (this.pmWakeLock != null && !this.pmWakeLock.isHeld()) {
            this.pmWakeLock.acquire();
        }

        // If found, then register as listener
        if (list != null && list.size() > 0) {
            this.mSensor = list.get(0);
            this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            this.lastAccessTime = System.currentTimeMillis();
            this.setStatus(ProximitySensorListener.STARTING);
        }

        // If error, then set status to error
        else {
            this.setStatus(ProximitySensorListener.ERROR_FAILED_TO_START);
        }

        return this.status;
    }

    /**
     * Stop listening to compass sensor.
     */
    public void stop() {
        if (this.status != ProximitySensorListener.STOPPED) {
            this.sensorManager.unregisterListener(this);
        }
        if (this.pmWakeLock != null && this.pmWakeLock.isHeld()) {
            this.pmWakeLock.release();
        }
        this.setStatus(ProximitySensorListener.STOPPED);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    /**
     * Sensor listener event.
     *
     * @param SensorEvent event
     */
    public void onSensorChanged(SensorEvent event) {

        int proximity; 
        
        if (event.values[0] == 0) {
            proximity = ProximitySensorListener.NEAR;
            
            if (this.partialWakeLock != null && !this.partialWakeLock.isHeld()) {
                this.partialWakeLock.acquire();
            }
        } else {
            proximity = ProximitySensorListener.FAR;

            if (this.partialWakeLock != null && this.partialWakeLock.isHeld()) {
                this.partialWakeLock.release();
            }
        }

        // Save proximity
        this.timeStamp = System.currentTimeMillis();
        this.proximity = proximity;
        this.setStatus(ProximitySensorListener.RUNNING);

    }

    /**
     * Get status of sensor.
     *
     * @return          status
     */
    public int getProximityState() {
        return this.status;
    }

    /**
     * Get the most recent distance. 
     *
     * @return          distance 
     */
    public int getProximity() {
        this.lastAccessTime = System.currentTimeMillis();
        return this.proximity;
    }

    /**
     * Set the status and send it to JavaScript.
     * @param status
     */
    private void setStatus(int status) {
        this.status = status;
    }
    
}

