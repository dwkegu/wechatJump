package top.dwkeg.wechatjump;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import top.dwkeg.wechatjump.accessibility.GameDetectService;
import top.dwkeg.wechatjump.accessibility.Shotter;

/**
 *
 * Created by dwkeg on 2018/1/2.
 */

public class MainActivity extends AppCompatActivity {
	public static final int REQUEST_MEDIA_PROJECTION = 0x2893;
	@BindView(R.id.tv_content)
	TextView tvContent;
	@BindView(R.id.ev_parameter)
	EditText editTextParameter;
	@BindView(R.id.btn_start)
	Button startBtn;
	@BindView(R.id.btn_stop)
	Button stopBtn;
	@OnClick(R.id.btn_stop)
	public void stopClick(View view){
		Intent intent = new Intent(this, Shotter.class);
		intent.putExtra("stop_info", true);
		startService(intent);
		startBtn.setEnabled(true);
		stopBtn.setEnabled(false);
	}
	@OnClick(R.id.btn_start)
	public void startClick(View view){
		String input = editTextParameter.getText().toString().trim();
		if(TextUtils.isEmpty(input)){
			requestScreenShot();
			return;
		}
		try{
			float parameter = Float.valueOf(input);
			GameDetectService.confficience = parameter;
		}catch (NumberFormatException e){
			e.printStackTrace();
		}
		requestScreenShot();
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

	}

	@Override
	protected void onResume() {
		super.onResume();
		if(Shotter.status){
			startBtn.setEnabled(false);
			stopBtn.setEnabled(true);
		}else{
			startBtn.setEnabled(true);
			stopBtn.setEnabled(false);
		}

	}
//	ServiceConnection connection = new ServiceConnection() {
//		@Override
//		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
//			Shotter service = ((Shotter.LocalBinder)iBinder).getServie();
//
//		}
//
//		@Override
//		public void onServiceDisconnected(ComponentName componentName) {
//			startBtn.setEnabled(true);
//			stopBtn.setEnabled(false);
//		}
//	};

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
				if (resultCode == RESULT_OK && data != null) {
					startBtn.setEnabled(false);
					stopBtn.setEnabled(true);
					getWindowManager().getDefaultDisplay().getMetrics(GameDetectService.metrics);
					Intent intent = new Intent(this, Shotter.class);
					intent.putExtra("intent_data", data);
					startService(intent);
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public native String stringFromJNI();

	static {
		System.loadLibrary("native-lib");
	}


}
