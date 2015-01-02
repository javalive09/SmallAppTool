package com.peter.smallapptool;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.peter.smallapptool.R;
import com.peter.smallapptool.AppAdapter.AppInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.os.Looper;
import android.os.MessageQueue.IdleHandler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener,
		OnItemClickListener, OnItemLongClickListener {

	private AlertDialog mDialog;
	private ProgressDialog mLoadingDialog;
	private String forecStopPackageName;
	private AppAdapter<AppInfo> appAdapter;
	private static final String TOP_APP = "top_app";
	private BroadcastReceiver forceStopReceiver;
	private ListView appListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getOverflowMenu();
		relodData();
	}

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
			if (info != null && !isSystemApp(info)
					&& !info.packageName.equals(getPackageName())) {
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

	private ProgressDialog getLoadingDialog() {
		if (mLoadingDialog == null) {
			mLoadingDialog = new ProgressDialog(this);
			mLoadingDialog.setMessage("Please wait...");
			mLoadingDialog.setIndeterminate(true);
			mLoadingDialog.setCancelable(false);
		}
		return mLoadingDialog;
	}
	
	private void getOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void relodData() {
		setContentView(R.layout.splash);
		Looper.myQueue().addIdleHandler(new IdleHandler() {
			
			@Override
			public boolean queueIdle() {
				final List<AppInfo> info = getAllAppInfos();
				if(appAdapter == null) {
					appAdapter = new AppAdapter<AppInfo>(MainActivity.this);
				}
				setContentView(R.layout.main);
				appListView = (ListView) findViewById(R.id.app_list);
				appListView.setOnItemClickListener(MainActivity.this);
				appListView.setOnItemLongClickListener(MainActivity.this);
				appAdapter.setData(info);
				appListView.setAdapter(appAdapter);
				return false;
			}
		});
		
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
			mDialog.setMessage(getResources().getString(
					R.string.action_help_msg));
			mDialog.show();
			break;
		case R.id.action_about:
			mDialog.setTitle(getResources().getString(R.string.action_about));
			mDialog.setMessage(getResources().getString(
					R.string.action_about_msg));
			mDialog.show();
			break;
		case R.id.action_feedback:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void uninstall(final AppInfo info) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				getLoadingDialog().show();
			}

			@Override
			protected void onPostExecute(Void result) {
				getLoadingDialog().hide();
				info.mDeleteAnim = true;
				appAdapter.notifyDataSetChanged();
			}

			@Override
			protected Void doInBackground(Void... params) {
				String cmd = "pm uninstall " + info.packageName + " \n";
				try {
					ProcessUtils.executeCommand(cmd, 2000);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
				return null;
			}

		}.execute();
	}

	private void clearCache(final AppInfo info) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				getLoadingDialog().show();
			}

			@Override
			protected void onPostExecute(Void result) {
				getLoadingDialog().hide();
			}

			@Override
			protected Void doInBackground(Void... params) {
				String cmd = "pm clear " + info.packageName + " \n";
				try {
					ProcessUtils.executeCommand(cmd, 2000);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
				return null;
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
			intent.setClassName("com.android.settings",
					"com.android.settings.InstalledAppDetails");
			intent.putExtra(appPkgName, info.packageName);
		}

		forceStopReceiver = new MyReceiver(info);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");
		registerReceiver(forceStopReceiver, filter);
		forecStopPackageName = info.packageName;
		startActivity(intent);
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
		default:
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		AppInfo info = appAdapter.getItem(position);
		info.mShowOperation = !info.mShowOperation;
		appAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		AppInfo info = appAdapter.getItem(position);
		SharedPreferences sp = getSharedPreferences(TOP_APP, MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString(TOP_APP, info.packageName);
		editor.commit();
		Toast.makeText(this, "top app : " + info.packageName, Toast.LENGTH_LONG).show();
		appAdapter.getInfos().remove(info);
		appAdapter.getInfos().add(0, info);;
		appAdapter.notifyDataSetInvalidated();
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
				if (!TextUtils.isEmpty(forecStopPackageName)
						&& !TextUtils.isEmpty(str)
						&& str.equals(forecStopPackageName)) {

					String action = intent.getAction();
					if (Intent.ACTION_PACKAGE_RESTARTED.equals(action)) {
						finishSetting();
					}else if(Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
						mInfo.mDeleteAnim = true;
					}
				}
			}
		}
		
	}

}
