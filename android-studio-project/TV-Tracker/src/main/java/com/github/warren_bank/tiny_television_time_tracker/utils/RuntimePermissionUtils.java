package com.github.warren_bank.tiny_television_time_tracker.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class RuntimePermissionUtils {

  public interface RuntimePermissionListener {
    public void onRequestPermissionsGranted (int requestCode, Object passthrough);
    public void onRequestPermissionsDenied  (int requestCode, Object passthrough, String[] missingPermissions);
  }

  private static HashMap<Integer,Object> passthroughCache = new HashMap<Integer,Object>();

  public static void onRequestPermissionsResult(RuntimePermissionListener listener, int requestCode, String[] permissions, int[] grantResults) {
    Object passthrough          = passthroughCache.remove(requestCode);
    String[] missingPermissions = RuntimePermissionUtils.getMissingPermissions(permissions, grantResults);

    if (missingPermissions == null) {
      listener.onRequestPermissionsGranted(requestCode, passthrough);
    }
    else {
      listener.onRequestPermissionsDenied(requestCode, passthrough, missingPermissions);
    }
  }

  public static void onActivityResult(RuntimePermissionListener listener, int requestCode, int resultCode, Intent data) {
    Object passthrough = passthroughCache.remove(requestCode);

    if (resultCode == Activity.RESULT_OK) {
      listener.onRequestPermissionsGranted(requestCode, passthrough);
    }
    else {
      listener.onRequestPermissionsDenied(requestCode, passthrough, /* missingPermissions= */ null);
    }
  }

  public static boolean hasAllPermissions(Activity activity, String[] allRequestedPermissions) {
    String[] missingPermissions = RuntimePermissionUtils.getMissingPermissions(activity, allRequestedPermissions);

    return (missingPermissions == null);
  }

  public static void requestPermissions(Activity activity, RuntimePermissionListener listener, String[] allRequestedPermissions, int requestCode) {
    Object passthrough = null;
    RuntimePermissionUtils.requestPermissions(activity, listener, allRequestedPermissions, requestCode, passthrough);
  }

  public static void requestPermissions(Activity activity, RuntimePermissionListener listener, String[] allRequestedPermissions, int requestCode, Object passthrough) {
    String[] missingPermissions = RuntimePermissionUtils.getMissingPermissions(activity, allRequestedPermissions);

    if (missingPermissions == null) {
      listener.onRequestPermissionsGranted(requestCode, passthrough);
    }
    else {
      passthroughCache.put(requestCode, passthrough);

      activity.requestPermissions(missingPermissions, requestCode);
    }
  }

  private static String[] getMissingPermissions(Activity activity, String[] allRequestedPermissions) {
    if (Build.VERSION.SDK_INT < 23)
      return null;

    if ((allRequestedPermissions == null) || (allRequestedPermissions.length == 0))
      return null;

    List<String> missingPermissions = new ArrayList<String>();

    for (String permission : allRequestedPermissions) {
      if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(permission);
      }
    }

    if (missingPermissions.isEmpty())
      return null;

    return missingPermissions.toArray(new String[missingPermissions.size()]);
  }

  private static String[] getMissingPermissions(String[] allRequestedPermissions, int[] allGrantResults) {
    if ((allRequestedPermissions == null) || (allRequestedPermissions.length == 0))
      return null;

    if ((allGrantResults == null) || (allGrantResults.length == 0))
      return allRequestedPermissions;

    List<String> missingPermissions = new ArrayList<String>();
    int index;

    for (index = 0; (index < allGrantResults.length) && (index < allRequestedPermissions.length); index++) {
      if (allGrantResults[index] != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(allRequestedPermissions[index]);
      }
    }

    while (index < allRequestedPermissions.length) {
      missingPermissions.add(allRequestedPermissions[index]);
      index++;
    }

    if (missingPermissions.isEmpty())
      return null;

    return missingPermissions.toArray(new String[missingPermissions.size()]);
  }

  // ---------------------------------------------------------------------------
  // special case: "android.permission.MANAGE_EXTERNAL_STORAGE"

  public static void showFilePermissions(Activity activity, int requestCode) {
    Object passthrough = null;
    RuntimePermissionUtils.showFilePermissions(activity, requestCode, passthrough);
  }

  public static void showFilePermissions(Activity activity, int requestCode, Object passthrough) {
    Uri uri       = Uri.parse("package:" + activity.getPackageName());
    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);

    passthroughCache.put(requestCode, passthrough);

    activity.startActivityForResult(intent, requestCode);
  }

  public static boolean hasFilePermissions() {
    return RuntimePermissionUtils.canAccessAllFiles();
  }

  private static boolean canAccessAllFiles() {
    return (Build.VERSION.SDK_INT < 30)
      ? true
      : Environment.isExternalStorageManager();
  }

  // ---------------------------------------------------------------------------
}
