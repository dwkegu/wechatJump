package top.dwkeg.wechatjump.accessibility;


import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.SoftReference;

import top.dwkeg.wechatjump.Constants;
import top.dwkeg.wechatjump.R;

import static top.dwkeg.wechatjump.NotifyWidget.CUSTOM_ACTION_NOTIFY_CLICK;

/**
 *
 * Created by wei on 16-12-1.
 */
public class Shotter extends Service{

	private ImageReader mImageReader;

	private MediaProjection mMediaProjection;

	private OnShotListener mOnShotListener;

	private final IBinder mBinder = new LocalBinder();


	public class LocalBinder extends Binder {
		public Shotter getServie(){
			return Shotter.this;
		}
	}

	private void setDataIntent(Intent data) {
		mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK,
				data);
		virtualDisplay();
	}
	VirtualDisplay mVirtualDisplay= null;

	private void virtualDisplay() {
		mImageReader = ImageReader.newInstance(
				getScreenWidth(),
				getScreenHeight(),
				PixelFormat.RGBA_8888,
				2);
		mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
				getScreenWidth(),
				getScreenHeight(),
				Resources.getSystem().getDisplayMetrics().densityDpi,
				DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
				mImageReader.getSurface(), null, null);

	}

	public void setmOnShotListener(OnShotListener listener){
		this.shotListener = listener;
	}

	private OnShotListener shotListener = null;

	public void startScreenShot() {
		if(mMediaProjection==null){
			return;
		}
		Log.d(Constants.LOGTAG_COMMONIFO, "obtain image");
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		Image image = mImageReader.acquireLatestImage();
		shotListener.onFinish(image);

	}


	private MediaProjectionManager getMediaProjectionManager() {

		return (MediaProjectionManager) getApplicationContext().getSystemService(
				Context.MEDIA_PROJECTION_SERVICE);
	}



	private int getScreenWidth() {
		return Resources.getSystem().getDisplayMetrics().widthPixels;
	}

	private int getScreenHeight() {
		return Resources.getSystem().getDisplayMetrics().heightPixels;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	public static boolean status = false;
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent!=null&&intent.getBooleanExtra("stop_info", false)){
			if(status){
				stopForeground(true);
				if(mVirtualDisplay!=null){
					mVirtualDisplay.release();
					mVirtualDisplay = null;
				}
				if (mMediaProjection != null) {
					mMediaProjection.stop();
					mMediaProjection = null;
				}
				status = false;
				return START_NOT_STICKY;
			}
		}
		if(intent!=null&&intent.getParcelableExtra("intent_data")!=null) {
			Intent data = intent.getParcelableExtra("intent_data");
			setDataIntent(data);
			Intent mainActivityIntent = new Intent(CUSTOM_ACTION_NOTIFY_CLICK);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
					124, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			Notification.Builder mBuilder = new Notification.Builder(getApplicationContext())
					.setContentIntent(pendingIntent)
					.setSmallIcon(R.drawable.ic_launcher_background)
					.setContentTitle("微信跳一跳助手")
					.setSubText("点击停止");
			startForeground(123, mBuilder.build());
			status = true;
		}else {
			Log.e(Constants.LOGTAG_COMMONIFO, "data miss");
		}
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mVirtualDisplay!=null){
			mVirtualDisplay.release();
			mVirtualDisplay = null;
		}
		if (mMediaProjection != null) {
			mMediaProjection.stop();
			mMediaProjection = null;
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	// a  call back listener
	public interface OnShotListener {
		void onFinish(Image image);
	}
}
