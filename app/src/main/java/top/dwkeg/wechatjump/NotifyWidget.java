package top.dwkeg.wechatjump;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import top.dwkeg.wechatjump.accessibility.Shotter;

/**
 *
 * Created by dwkeg on 2018/1/2.
 */

public class NotifyWidget extends AppWidgetProvider {
	public static final String CUSTOM_ACTION_NOTIFY_CLICK = "notify_click";
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if(CUSTOM_ACTION_NOTIFY_CLICK.equals(intent.getAction())){
			Intent serviceIntent = new Intent(context, Shotter.class);
			serviceIntent.putExtra("stop_info",true);
			context.startService(serviceIntent);
			Log.d(Constants.LOGTAG_COMMONIFO, "received notify click");
		}
	}
}
