package com.peter.smallapptool;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.peter.smallapptool.R;

public class AppAdapter<AppInfo> extends BaseAdapter {

    private List<AppInfo> mAppInfos;
    private MainActivity mAct;
    private PackageManager mPm;
    private LruCache<String, Bitmap> mMemoryCache;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT + 1;
    private static final int KEEP_ALIVE = 1;
    private Executor thread_pool_executor;
    private BitmapDrawable mDefaultDrawable;
    private BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(10);
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    public AppAdapter(MainActivity act) {
        mAct = act;
        mPm = mAct.getPackageManager();
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory / 5;
        //给LruCache分配 
        mMemoryCache = new LruCache<String, Bitmap>(mCacheSize) {

            //必须重写此方法，来测量Bitmap的大小  
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }

        };
        thread_pool_executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void setData(List<AppInfo> appInfos) {
        this.mAppInfos = appInfos;
    }

    public List<AppInfo> getInfos() {
        return mAppInfos;
    }

    /**
     * 更新列表数据
     * 
     * @param appInfos
     */
    public void updateData(List<AppInfo> appInfos) {
        this.mAppInfos = appInfos;
        notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        return mAppInfos.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return mAppInfos != null ? mAppInfos.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 获取数据
        AppInfo info = (AppInfo) getItem(position);

        // 获取View
        ViewCache cache = null;
        if (convertView == null) {
            convertView = View.inflate(mAct, R.layout.listviewitem, null);

            cache = new ViewCache();

            cache.app_icon = (ImageView) convertView.findViewById(R.id.app_icon);
            cache.app_name = (TextView) convertView.findViewById(R.id.app_name);
            cache.clearCache = (Button) convertView.findViewById(R.id.clearcache);
            cache.uninstall = (Button) convertView.findViewById(R.id.uninstall);
            cache.detail = (Button) convertView.findViewById(R.id.detail);
            cache.operation = (LinearLayout) convertView.findViewById(R.id.operation);
            convertView.setTag(cache);
        } else {
            cache = (ViewCache) convertView.getTag();
        }

        // 绑定数据
        convertView.setTag(R.id.appinfo, info);
        ApplicationInfo inf = null;
        try {
            inf = mPm.getApplicationInfo(info.packageName, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        //取缓存图片
        Bitmap bmIcon = mMemoryCache.get(info.packageName);
        if (bmIcon == null) {
            if (mDefaultDrawable == null) {
                mDefaultDrawable = (BitmapDrawable) mAct.getResources().getDrawable(R.drawable.ic_launcher);
            }
            bmIcon = mDefaultDrawable.getBitmap();
            thread_pool_executor.execute(new ThreadPoolTask(cache.app_icon, inf));
        }
        cache.app_icon.setImageBitmap(bmIcon);
        cache.app_name.setText(info.appName);
        cache.clearCache.setOnClickListener(mAct);
        cache.uninstall.setOnClickListener(mAct);
        cache.detail.setOnClickListener(mAct);
        if (info.mShowOperation) {
            cache.operation.setVisibility(View.VISIBLE);
        } else {
            cache.operation.setVisibility(View.GONE);
        }

        return convertView;
    }

    private class ThreadPoolTask implements Runnable {

        ImageView mAppIcon;
        ApplicationInfo mInfo;

        public ThreadPoolTask(ImageView appIcon, ApplicationInfo info) {
            mAppIcon = appIcon;
            mInfo = info;
        }

        @Override
        public void run() {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) mInfo.loadIcon(mPm);
            final Bitmap bmIcon = getRightSizeIcon(bitmapDrawable).getBitmap();
            mMemoryCache.put(mInfo.packageName, bmIcon);
            mAct.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mAppIcon.setImageBitmap(bmIcon);
                }

            });
        }

    }

    private BitmapDrawable getRightSizeIcon(BitmapDrawable drawable) {
        Drawable rightDrawable = mAct.getResources().getDrawable(R.drawable.ic_launcher);
        int rightSize = rightDrawable.getIntrinsicWidth();
        Bitmap bitmap = drawable.getBitmap();
        int width = bitmap.getWidth();
        float widths = width;
        float scale = rightSize / widths;
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        Bitmap bm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return new BitmapDrawable(mAct.getResources(), bm);
    }

    public static class ViewCache {
        ImageView app_icon;
        TextView app_name;
        Button uninstall;
        Button detail;
        Button clearCache;
        LinearLayout operation;
    }

    public static class AppInfo {
        public String appName;
        public String packageName;
        public boolean mShowOperation;
    }
}