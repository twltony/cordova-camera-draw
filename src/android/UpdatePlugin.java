package com.nimble.qualityinspection.uat;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;

public class UpdatePlugin extends CordovaPlugin {
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if ("update".equals(action)) {
            try {
                PackageInfo pi = cordova.getActivity().getPackageManager().getPackageInfo(cordova.getActivity().getPackageName(), 0);
                UpdateUtils.update(pi.versionCode, cordova.getActivity());
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}
