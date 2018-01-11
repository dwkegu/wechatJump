package top.dwkeg.wechatjump.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.math.*;

import top.dwkeg.wechatjump.Constants;

/**
 *
 * Created by dwkeg on 2018/1/2.
 */

public class GameDetectService extends AccessibilityService {
	private static boolean start = true;
	private static boolean shotServiceConnected = false;
	private Shotter shotService = null;
	private static final int piece_base_height1_2 =20;
	private static int pieceBodyWidth = 40;
	public static float confficience = 1.0f;
	private ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			shotServiceConnected = true;
			Log.d(Constants.LOGTAG_COMMONIFO, "shotter service connected success");
			shotService = ((Shotter.LocalBinder)service).getServie();
			shotService.setmOnShotListener(new Shotter.OnShotListener() {
				@Override
				public void onFinish(Image image) {
					playGame(image);
				}
			});
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(Constants.LOGTAG_COMMONIFO, "shotter service connected failed");
			shotServiceConnected = false;
			shotService = null;
		}
	};
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		Log.d(Constants.LOGTAG_COMMONIFO,Thread.currentThread().getName());
		Log.d(Constants.LOGTAG_COMMONIFO, event.toString());
		switch (event.getEventType()){
			case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
				if(event.getClassName().equals("com.tencent.mm.plugin.appbrand.ui.AppBrandUI")&&!start){
					start = true;
				}else {
					start = false;
					return;
				}
				//com.tencent.mm.plugin.appbrand.ui.AppBrandUI
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						Looper.prepare();
						Intent shotIntent = new Intent(getApplicationContext(), Shotter.class);
						bindService(shotIntent, connection, BIND_AUTO_CREATE);
						while (!shotServiceConnected){
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
								start = false;
								return;
							}
						}
						while(start){
//							Log.d(Constants.LOGTAG_COMMONIFO, "start to shot");
                            if(shotService==null){
                                bindService(new Intent(getApplicationContext(), Shotter.class), connection, BIND_AUTO_CREATE);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(shotService==null){
                                break;
                            }
							shotService.startScreenShot();
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});
				thread.start();
				break;
		}


	}

	private Point[] getPositions(Image image){
		Point[] points= new Point[2];
		points[0] = new Point();
		points[1] = new Point();
		int w = image.getWidth();
		int h = image.getHeight();
		int pieceXSum = 0;
		int pieceXC = 0;
		int pieceYMax = 0;
		int boardX = 0;
		int boardY = 0;
		//扫描棋子的左右边界
		int scanXBoard = w/8;
		int scanStartY = 0;
		Bitmap bitmap = getBitmapFromImage(image);
		int lastPixel = 0;
		int currentPixel = 0;
		for(int i = h/3; i < h * 2 / 3; i+=50){
			lastPixel = bitmap.getPixel(0, i);
			for(int j = 0; j < w; j++){
				currentPixel = bitmap.getPixel(j, i);
				if(currentPixel!=lastPixel){
					scanStartY = i - 50;
					break;
				}
			}
			if(scanStartY!=0){
				break;
			}
		}
		int minWL = scanXBoard;
		int maxWR = w-scanXBoard;
		int maxWidth = 0;
		int pieceStartY = -1;
		int pieceEndY = -1;
		for(int i = scanStartY; i < h *2 /3; i++){
			minWL = -1;
			maxWR = -1;
			for(int j = scanXBoard; j < w - scanXBoard; j++){
				currentPixel = bitmap.getPixel(j,i);
				int r = (currentPixel>>16)&0x00ff;
				int g = (currentPixel>>8)&0x0000ff;
				int b = currentPixel&0x000000ff;
				if(50 < r && r < 60 && 53 < g && g < 63 && 95 < b && b < 110){
					if(pieceStartY<0){
						pieceStartY = i;
					}
					if( i >= pieceEndY){
						pieceEndY = i;
					}
					if(minWL< 0){
						minWL = j;
						maxWR = j;
					}else{
						maxWR = j;
					}
					pieceXSum += j;
					pieceXC += 1;
//					pieceYMax = Math.max(i, pieceYMax);
				}
			}
			if(maxWR-minWL>maxWidth){
//				Log.d(Constants.LOGTAG_COMMONIFO, "origin max width:"+
//						String.valueOf(maxWidth)+"new width :"+String.valueOf(maxWR-minWL)+
//						"height"+String.valueOf(i));
				maxWidth = maxWR-minWL;
				pieceBodyWidth = maxWidth;
				pieceYMax = i;
			}
		}
		if(pieceXSum==0||pieceXC==0){
			points[0].set(0,0);
			points[1].set(0,0);
			return points;
		}
		int pieceX = pieceXSum/pieceXC;
//		int pieceY = pieceYMax-piece_base_height1_2;
		int pieceY = pieceYMax;
		int boardXStart = 0;
		int boardXEnd = 0;
		maxWidth += 20;
		if(pieceX < w/2){
			boardXStart = pieceX+maxWidth/2;
			boardXEnd = w-scanXBoard;
		}else{
			boardXStart = scanXBoard-maxWidth/2;
			boardXEnd = pieceX;
		}
		int i = 0;
//		int bgColor = bitmap.getPixel(boardXStart, h/3);
		for(i = h/3; i < h*2/3; i++){
			lastPixel = bitmap.getPixel((boardXStart+boardXEnd)/2, i-1);
			int lr = (lastPixel>>16)&0x00ff;
			int lg = (lastPixel>>8)&0x0000ff;
			int lb = lastPixel&0x000000ff;
			if(boardX!=0 || boardY !=0){
				break;
			}
			int boardXSum = 0;
			int boardXC = 0;
			for(int j = boardXStart; j < boardXEnd; j++){
				currentPixel = bitmap.getPixel(j,i);
				if(Math.abs(j - pieceX) < pieceBodyWidth){
					continue;
				}
				int cr = (currentPixel>>16)&0x00ff;
				int cg = (currentPixel>>8)&0x0000ff;
				int cb = currentPixel&0x000000ff;
				if(Math.abs(cr-lr)+Math.abs(cg-lg)+Math.abs(cb-lb)>15){
//					Log.d(Constants.LOGTAG_COMMONIFO, String.valueOf(j));
					boardXSum += j;
					boardXC += 1;
				}
			}
			if(boardXSum!=0){
				boardX = boardXSum/boardXC;
			}
		}
		lastPixel = bitmap.getPixel(boardX, i+2);
		int bgR = (lastPixel>>16)&0x00ff;
		int bgG = (lastPixel>>8)&0x0000ff;
		int bgB = lastPixel&0x000000ff;
		int lr;
		int lg;
		int lb;
		int tmpPixel;
		int k =i + 275;
		while(k > i + 2){
			k -= 1;
			tmpPixel = bitmap.getPixel(boardX, k);
			lr = (tmpPixel>>16)&0x00ff;
			lg = (tmpPixel>>8)&0x0000ff;
			lb = tmpPixel&0x000000ff;
			if (Math.abs(lr-bgR)+Math.abs(lg-bgG)+Math.abs(lb-bgB) < 10) {
				boardY = (i+k)/2;
				break;
			}
		}
		if(0 == boardX || 0 == boardY){
			points[0].set(0,0);
			points[1].set(0,0);
			return points;
		}
		if(pieceY > 40){
			Log.d(Constants.LOGTAG_COMMONIFO, "pieceHeight = "+String.valueOf(pieceEndY-pieceStartY));
			pieceY -= (int)(0.5*(pieceEndY-pieceStartY));
		}

		points[0].set(pieceX, pieceY);
		points[1].set(boardX, boardY);
//		bitmap.setPixel(points[0].x, points[0].y, Color.RED);
//		bitmap.setPixel(points[1].x, points[1].y, Color.RED);
//		File file = new File(Environment.getExternalStorageDirectory().getPath()
//				+"/jumpImage");
//		if(!file.exists()&&!file.mkdirs()){
//			return null;
//		}
//		String imagePath = Environment.getExternalStorageDirectory().getPath()+"/jumpImage/jump"
//				+String.valueOf(System.currentTimeMillis())+".png";
//		String position = Environment.getExternalStorageDirectory().getPath()+"/jumpImage/positions.txt";
//		FileOutputStream fos1 = null;
//		FileOutputStream fos2 = null;
//		try {
//			fos1 = new FileOutputStream(imagePath);
//			fos2 = new FileOutputStream(position,true);
//			bitmap.compress(Bitmap.CompressFormat.PNG, 100,  fos1);
//			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos2));
//			writer.write("position 1:" + points[0].toString());
//			writer.write("position 2:" + points[1].toString());
//			writer.write("\n");
//			writer.flush();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				if(fos1!=null){
//					fos1.close();
//				}
//			}catch (IOException e){
//				e.printStackTrace();
//			}
//			try {
//				if(fos2!=null){
//					fos2.close();
//				}
//			}catch (IOException e){
//				e.printStackTrace();
//			}
//		}
		return points;
	}

	private Bitmap getBitmapFromImage(Image image){
		int w = image.getWidth();
		int h = image.getHeight();
		final Image.Plane[] planes= image.getPlanes();
		final ByteBuffer buffer =planes[0].getBuffer();
		int pixelStride = planes[0].getPixelStride();
		int rowStride = planes[0].getRowStride();
		int rowPadding = rowStride - pixelStride*w;
		Bitmap bitmap = Bitmap.createBitmap(w + rowPadding/pixelStride, h,
				Bitmap.Config.ARGB_8888);
		bitmap.copyPixelsFromBuffer(buffer);
		image.close();
		return bitmap;
	}
	public static DisplayMetrics metrics = new DisplayMetrics();
	private long getTime(Point[] points){
		if(points==null||points.length<2) return 0;
		double distance = Math.sqrt(Math.pow(points[0].x-points[1].x,2)
				+ Math.pow(points[0].y-points[1].y, 2));
		if(metrics==null||metrics.density==0){
			return (long)(distance*confficience*2.5);
		}
		Log.d(Constants.LOGTAG_COMMONIFO, "density" + String.valueOf(metrics.density));
		return (long)(3.83*distance*confficience/metrics.density);
	}
	private void playGame(Image image){
		Log.d(Constants.LOGTAG_COMMONIFO, "height"+  image.getHeight() + "width" + image.getWidth());
		Rect rect = new Rect();
		if(getRootInActiveWindow()==null)
			return;
		getRootInActiveWindow().getBoundsInScreen(rect);
		Point[] points = getPositions(image);
		image.close();
		Log.d(Constants.LOGTAG_COMMONIFO, points[0].toString());
		Log.d(Constants.LOGTAG_COMMONIFO, points[1].toString());
		long time = getTime(points);
		GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
		Path path = new Path();
		path.moveTo(rect.centerX(), rect.height()/3);
		path.lineTo(rect.centerX(), Math.min(rect.height()/3 + time/2, rect.height()*2/3));
		if(time<1){
			return;
		}
		gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, time));
		dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
			@Override
			public void onCompleted(GestureDescription gestureDescription) {
				super.onCompleted(gestureDescription);
			}

			@Override
			public void onCancelled(GestureDescription gestureDescription) {
				super.onCancelled(gestureDescription);
			}
		}, null);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(shotServiceConnected)
			unbindService(connection);
	}

	@Override
	public void onInterrupt() {

	}
}
