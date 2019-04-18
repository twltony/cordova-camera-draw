package com.nimble.qualityinspection.uat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mylhyl.acp.Acp;
import com.mylhyl.acp.AcpListener;
import com.mylhyl.acp.AcpOptions;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class UpdateUtils {
    public static final int REQ_CODE_INSTALL_APP = 99;
    private static String apItoken = "b2b3ab041a37af7d46e503bccadfa07a";
    private static String appId = "5c3c4e8b959d690300fee5e9";

    public static void update(final int localVersionCode, final Activity activity) {
        Acp.getInstance(activity).request(new AcpOptions.Builder()
                        .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .build(),
                new AcpListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onGranted() {
                        checkVersion(localVersionCode, activity);
                    }

                    @Override
                    public void onDenied(List<String> permissions) {

                    }
                });


    }

    public static void checkVersion(final int localVersionCode, final Activity activity) {
        String url = "http://api.fir.im/apps/latest/" + appId;
        Map<String, String> params = new HashMap<>();
        params.put("api_token", apItoken);
        OkHttpUtils.get().url(url).params(params).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e) {
                Log.e("onError", e.getMessage());
            }

            @Override
            public void onResponse(String response) {
                Log.e("onResponse", response);
                try {
                    JSONObject json = new JSONObject(response);
                    String buildStr = json.getString("build");
                    int build = Integer.parseInt(buildStr);
                    if (localVersionCode < build) {
                        //有新版本
                        String url = json.getString("install_url");
                        url = URLDecoder.decode(url);
                        Log.e("url", url);
                        showUpdateDialog(activity, json.getString("changelog"), build, url);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void showUpdateDialog(final Activity activity, String content, int build, final String downUrl) {
        final String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        final String fileName = "rfSalesApp_" + build + ".apk";
        MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .title("发现新版本")
                .positiveText("立即更新")
                .negativeText("取消")
                .cancelable(true)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        new MaterialDialog.Builder(activity)
                                .title("下载进度")
                                .cancelable(false)
                                .content("")
                                .contentGravity(GravityEnum.CENTER)
                                .progress(false, 150, false)
                                .cancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                    }
                                })
                                .showListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface dialogInterface) {
                                        final MaterialDialog progressDialog = (MaterialDialog) dialogInterface;
                                        progressDialog.setMaxProgress(100);
                                        OkHttpUtils.get().url(downUrl).params(null)
                                                .build()
                                                .execute(new FileCallBack(path, fileName) {
                                                    @Override
                                                    public void inProgress(float progress, long total) {
                                                        Log.e("inProgress", progress + " : " + total);
                                                        if (progressDialog != null && progressDialog.isShowing())
                                                            progressDialog.setProgress(Math.round(progress * 100));
                                                    }

                                                    @Override
                                                    public void onError(Call call, Exception e) {
                                                        Log.e("onError", e.getMessage());
                                                    }

                                                    @Override
                                                    public void onResponse(File response) {
                                                        progressDialog.dismiss();
                                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                            //区别于 FLAG_GRANT_READ_URI_PERMISSION 跟 FLAG_GRANT_WRITE_URI_PERMISSION， URI权限会持久存在即使重启，直到明确的用 revokeUriPermission(Uri, int) 撤销。 这个flag只提供可能持久授权。但是接收的应用必须调用ContentResolver的takePersistableUriPermission(Uri, int)方法实现
                                                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                                                            Uri fileUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", response);
                                                            intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
                                                        } else {
                                                            intent.setDataAndType(Uri.fromFile(response), "application/vnd.android.package-archive");
                                                        }
                                                        activity.startActivityForResult(intent, REQ_CODE_INSTALL_APP);
                                                    }
                                                });
                                    }
                                }).show();
                    }
                })
                .build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContent("更新内容:" + content);
        dialog.show();
    }
}
