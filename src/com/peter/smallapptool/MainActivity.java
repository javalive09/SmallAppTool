package com.peter.smallapptool;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import com.peter.smallapptool.R;
import com.peter.smallapptool.AppAdapter.AppInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnItemClickListener, OnItemLongClickListener {

    private AlertDialog mDialog = null;
    private FrameLayout loading = null;
    private String forecStopPackageName = null;
    private AppAdapter<AppInfo> appAdapter = null;
    private static final String TOP_APP = "top_app";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOverflowMenu();
        relodData();
    }

    BroadcastReceiver forceStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            if (data != null) {
                String str = data.getSchemeSpecificPart();
                if (!TextUtils.isEmpty(forecStopPackageName) && !TextUtils.isEmpty(str)
                        && str.equals(forecStopPackageName)) {
                    finishSetting();
                    unregisterReceiver(forceStopReceiver);
                }
            }
        }
    };
    
    /**
     * 获取所有的应用信息
     * 
     * @return
     */
    private List<AppInfo> getAllAppInfos() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> appList = pm.getInstalledApplications(0);
        List<AppInfo> runningApps = new ArrayList<AppInfo>(appList.size());
        SharedPreferences sp = getSharedPreferences(TOP_APP, MODE_PRIVATE);
        String topApp = sp.getString(TOP_APP, "com.qihoo.cleandroid_cn");
        for (ApplicationInfo info : appList) {
            if (info != null && !isSystemApp(info) && !info.packageName.equals(getPackageName())) {
                AppInfo inf = new AppInfo();
                inf.packageName = info.packageName;
                inf.appName = info.loadLabel(pm).toString();
                if (inf.packageName.equals(topApp)) {
                    runningApps.add(0, inf);
                } else {
                    runningApps.add(inf);
                }
            }
        }

        return runningApps;
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {//system apps
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void relodData() {
        setContentView(R.layout.main);
        appAdapter = new AppAdapter<AppInfo>(MainActivity.this, getAllAppInfos());
        ListView appListView = (ListView) findViewById(R.id.app_list);
        appListView.setOnItemClickListener(this);
        appListView.setOnItemLongClickListener(this);
        appListView.setAdapter(appAdapter);
        loading = (FrameLayout) findViewById(R.id.pbview);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDialog == null) {
            mDialog = new AlertDialog.Builder(MainActivity.this).create();
        }
        switch (item.getItemId()) {
        case R.id.action_refresh:
            relodData();
            break;
        case R.id.action_help:
            mDialog.setTitle(getResources().getString(R.string.action_help));
            mDialog.setMessage(getResources().getString(R.string.action_help_msg));
            mDialog.show();
            break;
        case R.id.action_about:
            mDialog.setTitle(getResources().getString(R.string.action_about));
            mDialog.setMessage(getResources().getString(R.string.action_about_msg));
            mDialog.show();
            break;
        case R.id.action_feedback:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getOverflowMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uninstall(final String packageName) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                loading.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Void result) {
                loading.setVisibility(View.INVISIBLE);
                relodData();
            }

            @Override
            protected Void doInBackground(Void... params) {
                List<String> commands = new ArrayList<String>();
                commands.add("su");
                commands.add("|");
                commands.add("pm");
                commands.add("uninstall");
                commands.add(packageName);
                ProcessBuilder pb = new ProcessBuilder(commands);
                try {
                    pb.start();
                } catch (IOException e) {
                    showForceStopView(packageName);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.execute();
    }

    private void clearCache(final String packageName) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                loading.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Void result) {
                loading.setVisibility(View.INVISIBLE);
                relodData();
            }

            @Override
            protected Void doInBackground(Void... params) {
                List<String> commands = new ArrayList<String>();
                commands.add("su");
                commands.add("|");
                commands.add("pm");
                commands.add("clear");
                Log.i("peter", packageName);
                commands.add(packageName);
                ProcessBuilder pb = new ProcessBuilder(commands);
                try {
                    pb.start();
                } catch (IOException e) {
                    showForceStopView(packageName);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.execute();
    }

    private void showForceStopView(String packageName) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        filter.addDataScheme("package");
        registerReceiver(forceStopReceiver, filter);
        forecStopPackageName = packageName;
        int version = Build.VERSION.SDK_INT;
        Intent intent = new Intent();
        if (version >= 9) {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            Uri uri = Uri.fromParts("package", packageName, null);
            intent.setData(uri);
        } else {
            final String appPkgName = "pkg";
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra(appPkgName, packageName);
        }
        startActivity(intent);
    }

    private void finishSetting() {
        Intent in = new Intent();
        in.setClass(MainActivity.this, MainActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//确保finish掉setting
        in.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);//确保MainActivity不被finish掉
        startActivity(in);
    }

    @Override
    public void onClick(View v) {
        View parent = (View) v.getParent().getParent();
        AppInfo info = (AppInfo) parent.getTag(R.id.appinfo);
        String packageName = info.packageName;
        switch (v.getId()) {
        case R.id.uninstall:
            uninstall(packageName);
            break;
        case R.id.detail:
            showForceStopView(packageName);
            break;
        case R.id.clearcache:
            clearCache(packageName);
            break;
        default:
            break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppInfo info = appAdapter.getItem(position);
        info.mShowOperation = !info.mShowOperation;
        appAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        AppInfo info = appAdapter.getItem(position);
        SharedPreferences sp = getSharedPreferences(TOP_APP, MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString(TOP_APP, info.packageName);
        editor.commit();
        Toast.makeText(this, "top app : " + info.packageName, Toast.LENGTH_LONG).show();
        relodData();
        return true;
    }

}
