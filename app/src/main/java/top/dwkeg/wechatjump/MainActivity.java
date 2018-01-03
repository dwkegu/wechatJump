package top.dwkeg.wechatjump;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import top.dwkeg.wechatjump.accessibility.Shotter;

/**
 *
 * Created by dwkeg on 2018/1/2.
 */

public class MainActivity extends AppCompatActivity {
	public static final int REQUEST_MEDIA_PROJECTION = 0x2893;
	@BindView(R.id.tv_content)
	TextView tvContent;
	@OnClick(R.id.btn_stop)
	public void stopClick(View view){

	}
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
		if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
		}
		if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 200);
		}
		if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 200);
		}
		requestScreenShot();
//		Intent intent = new Intent(this, ShellService.class);
//		intent.putExtra("cmd", "adb shell swipe 200 0 200 1200 3000");
//		startService(intent);
	}

	public void requestScreenShot() {
		MediaProjectionManager manager = ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE));
		if(null==manager){
			return;
		}
		startActivityForResult(manager.createScreenCaptureIntent(),
				REQUEST_MEDIA_PROJECTION
		);
	}

	private void toast(String str) {
		Toast.makeText(MainActivity.this,str,Toast.LENGTH_LONG).show();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case REQUEST_MEDIA_PROJECTION: {
				if (resultCode == -1 && data != null) {
					Intent intent = new Intent(this, Shotter.class);
					intent.putExtra("intent_data", data);
					startService(intent);
				}
			}
		}
	}

	public native String stringFromJNI();

	static {
		System.loadLibrary("native-lib");
	}


}
