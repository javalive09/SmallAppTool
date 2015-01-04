package com.peter.smallapptool;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.peter.smallapptool.MyMenu.ItemViewCreater;
import com.peter.smallapptool.MyMenu.ItemViewOnClickListener;
import com.peter.smallapptool.R;
import com.peter.smallapptool.AppAdapter.AppInfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue.IdleHandler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnItemClickListener, OnItemLongClickListener,
        OnLongClickListener {

    private AlertDialog mDialog;
    private String forecStopPackageName;
    private AppAdapter<AppInfo> appAdapter;
    private static final String TOP_APP = "top_app";
    private static final String LOCKED_APP = "locked_app";
    private BroadcastReceiver forceStopReceiver;
    private boolean isPerformLongClick;
    private ListView appListView;
    private FrameLayout mContainer;
    private MyMenu mMenu;
    private View mCover;
    private int[] menuTitleRes = { 
            R.string.action_refresh, 
            R.string.action_help, 
            R.string.action_about,
            R.string.action_feedback };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getOverflowMenu();
        initMenu();
        relodData();
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private void initMenu() {
        mContainer = (FrameLayout) findViewById(R.id.container);
        mCover = findViewById(R.id.cover);
        mCover.setOnTouchListener(new OnTouchListener() {
            
            @SuppressLint("ClickableViewAccessibility") @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mMenu = new MyMenu(MainActivity.this);
        View anchor = findViewById(R.id.menu);
        mMenu.setAnchor(anchor);
        for (int i = 0; i < menuTitleRes.length; i++) {
            mMenu.addMenuItem(i, menuTitleRes[i], menuTitleRes[i]);
        }
        mMenu.setMenuItemCreater(new ItemViewCreater() {

            @Override
            public View createView(int position, ViewGroup parent) {
                LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                View menu = factory.inflate(R.layout.menu_item, parent, false);
                TextView tv = (TextView) menu.findViewById(R.id.text);
                tv.setText(menuTitleRes[position]);
                return menu;
            }
        });
        mMenu.setMenuItemOnClickListener(new ItemViewOnClickListener() {

            @Override
            public void OnItemClick(int order) {

                if (mDialog == null) {
                    mDialog = new AlertDialog.Builder(MainActivity.this).create();
                }

                switch (order) {
                case R.string.action_refresh:
                    relodData();
                    break;
                case R.string.action_help:
                    mDialog.setTitle(getResources().getString(R.string.action_help));
                    mDialog.setMessage(getResources().getString(R.string.action_help_msg));
                    mDialog.show();
                    break;
                case R.string.action_about:
                    mDialog.setTitle(getResources().getString(R.string.action_about));
                    mDialog.setMessage(getResources().getString(R.string.action_about_msg));
                    mDialog.show();
                    break;
                case R.string.action_feedback:
                    break;
                }
                mMenu.dismiss();
            }
        });
    }
    
    private void showLoading() {
        mCover.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.data_loading_rotate);
        mCover.findViewById(R.id.loading).startAnimation(anim);
    }
    
    private void hideLoading() {
        mCover.findViewById(R.id.loading).clearAnimation();
        mCover.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mMenu.show();
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 获取所有的应用信息
     * 
     * @return
     */
    private List<AppInfo> getAllAppInfos() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> appList = pm.getInstalledApplications(0);
        List<AppInfo> allNoSystemApps = new ArrayList<AppInfo>(appList.size());
        
        SharedPreferences spLock = getSharedPreferences(LOCKED_APP, MODE_PRIVATE);
        
        for (ApplicationInfo info : appList) {// 非系统APP
            if (info != null && !isSystemApp(info) && !info.packageName.equals(getPackageName())) {
                AppInfo inf = new AppInfo();
                inf.packageName = info.packageName;
                inf.appName = info.loadLabel(pm).toString();
                inf.state = AppInfo.NO_RUNNING;//默认
                
                if (isRunndingApp(inf, runningApps)) {//运行的APP
                    inf.state = AppInfo.RUNNING;
                    int state = spLock.getInt(info.packageName, AppInfo.NO_RUNNING);
                    if (state == AppInfo.LOCKED) {//lock的APP
                        inf.state = AppInfo.LOCKED;
                    }
                }
                
                allNoSystemApps.add(inf);
            }
        }

        Collections.sort(allNoSystemApps, AppComparator);

        return allNoSystemApps;
    }

    Comparator<AppInfo> AppComparator = new Comparator<AppInfo>() {

        @Override
        public int compare(AppInfo lhs, AppInfo rhs) {
            if (lhs.state < rhs.state) {
                return -1;
            } else if (lhs.state > rhs.state) {
                return 1;
            }
            return 0;
        }
    };

    private boolean isRunndingApp(AppInfo info, List<RunningAppProcessInfo> runningApps) {
        for (RunningAppProcessInfo runInfo : runningApps) {
            String[] pkgs = runInfo.pkgList;
            for (String pagName : pkgs) {
                if (!TextUtils.isEmpty(pagName) && pagName.equals(info.packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {// system apps
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (forceStopReceiver != null) {
            unregisterReceiver(forceStopReceiver);
            forceStopReceiver = null;
            appAdapter.notifyDataSetChanged();
        }
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

    private void relodData() {
        mContainer.removeAllViews();
        View.inflate(getApplicationContext(), R.layout.splash, mContainer);
        Looper.myQueue().addIdleHandler(new IdleHandler() {

            @Override
            public boolean queueIdle() {
                long time = System.currentTimeMillis();
                final List<AppInfo> info = getAllAppInfos();
                long delta = System.currentTimeMillis() - time;
                Log.i("peter", "delta time = " + delta);
                if (appAdapter == null) {
                    appAdapter = new AppAdapter<AppInfo>(MainActivity.this);
                }
                mContainer.removeAllViews();
                View.inflate(getApplicationContext(), R.layout.list, mContainer);
                appListView = (ListView) findViewById(R.id.app_list);
                appListView.setOnItemClickListener(MainActivity.this);
                appListView.setOnItemLongClickListener(MainActivity.this);
                appAdapter.setData(info);
                appListView.setAdapter(appAdapter);
                return false;
            }
        });

    }

    private void uninstall(final AppInfo info) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                showLoading();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                hideLoading();
                if (result) {
                    info.state = AppInfo.UNINSTALLED;
                    appAdapter.notifyDataSetChanged();
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = true;
                String cmd = "pm uninstall " + info.packageName + " \n";
                try {
                    ProcessUtils.executeCommand(cmd, 2000);
                } catch (IOException e) {
                    result = false;
                    registReceiver(info);
                    Uri uri = Uri.parse("package:" + info.packageName);
                    Intent intent = new Intent(Intent.ACTION_DELETE, uri);
                    startActivity(intent);
                } catch (InterruptedException e) {
                } catch (TimeoutException e) {
                }
                return result;
            }

        }.execute();
    }

    private void clearCache(final AppInfo info) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                showLoading();
            }

            @Override
            protected void onPostExecute(Void result) {
                hideLoading();
            }

            @Override
            protected Void doInBackground(Void... params) {
                String cmd = "pm clear " + info.packageName + " \n";
                try {
                    ProcessUtils.executeCommand(cmd, 2000);
                } catch (IOException e) {
                    showForceStopView(info);
                } catch (InterruptedException e) {
                } catch (TimeoutException e) {
                }
                return null;
            }

        }.execute();
    }

    private void forceStop(final AppInfo info) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                showLoading();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                hideLoading();
                if (result) {
                    info.state = AppInfo.KILLING;
                    appAdapter.notifyDataSetChanged();
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = true;
                String cmd = "am force-stop " + info.packageName + " \n";
                try {
                    ProcessUtils.executeCommand(cmd, 2000);
                } catch (IOException e) {
                    result = false;
                    showForceStopView(info);
                } catch (InterruptedException e) {
                } catch (TimeoutException e) {
                }

                return result;
            }

        }.execute();
    }

    private void showForceStopView(AppInfo info) {
        int version = Build.VERSION.SDK_INT;
        Intent intent = new Intent();
        if (version >= 9) {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            Uri uri = Uri.fromParts("package", info.packageName, null);
            intent.setData(uri);
        } else {
            final String appPkgName = "pkg";
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra(appPkgName, info.packageName);
        }
        registReceiver(info);
        startActivity(intent);
    }

    private void registReceiver(AppInfo info) {
        forecStopPackageName = info.packageName;
        forceStopReceiver = new MyReceiver(info);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(forceStopReceiver, filter);
    }

    private void finishSetting() {
        Intent in = new Intent();
        in.setClass(MainActivity.this, MainActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);// 确保finish掉setting
        in.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);// 确保MainActivity不被finish掉
        startActivity(in);
    }

    @Override
    public void onClick(View v) {
        View parent = (View) v.getParent().getParent();
        AppInfo info = (AppInfo) parent.getTag(R.id.appinfo);
        switch (v.getId()) {
        case R.id.uninstall:
            uninstall(info);
            break;
        case R.id.detail:
            showForceStopView(info);
            break;
        case R.id.clearcache:
            clearCache(info);
            break;
        case R.id.kill_lock:
            if (info.state == AppInfo.RUNNING) {
                forceStop(info);
            }
            break;
        case R.id.menu:
            mMenu.show();
            break;
        default:
            break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (isPerformLongClick) {
            isPerformLongClick = false;
        } else {
            AppInfo info = appAdapter.getItem(position);
            if (info.state != AppInfo.LOCKED) {
                info.mShowOperation = !info.mShowOperation;
                appAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (isPerformLongClick) {
            isPerformLongClick = false;
        } 
//        else {
//            AppInfo info = appAdapter.getItem(position);
//            SharedPreferences sp = getSharedPreferences(TOP_APP, MODE_PRIVATE);
//            sp.edit().putString(TOP_APP, info.packageName).commit();
//            Toast.makeText(getApplicationContext(), "Top App : " + info.packageName, Toast.LENGTH_LONG).show();
//            relodData();
//        }
        return true;
    }

    private class MyReceiver extends BroadcastReceiver {

        AppInfo mInfo;

        public MyReceiver(AppInfo info) {
            mInfo = info;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            if (data != null) {
                String str = data.getSchemeSpecificPart();
                if (!TextUtils.isEmpty(forecStopPackageName) && !TextUtils.isEmpty(str)
                        && str.equals(forecStopPackageName)) {

                    String action = intent.getAction();
                    if (Intent.ACTION_PACKAGE_RESTARTED.equals(action)) {
                        finishSetting();
                        mInfo.state = AppInfo.KILLING;
                    } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                        mInfo.state = AppInfo.UNINSTALLED;
                    }
                }
            }
        }

    }

    @Override
    public boolean onLongClick(View v) {
        View parent = (View) v.getParent().getParent();
        AppInfo info = (AppInfo) parent.getTag(R.id.appinfo);
        switch (v.getId()) {
        case R.id.kill_lock:
            isPerformLongClick = true;
            if (info.state == AppInfo.RUNNING) {
                info.state = AppInfo.LOCKED;
            } else if (info.state == AppInfo.LOCKED) {
                info.state = AppInfo.RUNNING;
            }

            SharedPreferences sp = getSharedPreferences(LOCKED_APP, MODE_PRIVATE);
            sp.edit().putInt(info.packageName, info.state).commit();
            appAdapter.notifyDataSetInvalidated();
            Vibrator mVibrator01 = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
            mVibrator01.vibrate(100);
            return true;
        }
        return false;
    }

}
