package com.pawan.parkedphotos;

import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.media.projection.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Phone-only consent UI, kept separate from the projected viewer to avoid recursion. */
public class PhoneSetupActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView state;
    private boolean requesting;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xffedf4f7);t.setPadding(0,dp(10),0,dp(10));return t;}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);if(saved!=null)requesting=saved.getBoolean("requesting");
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(24),dp(24),dp(24),dp(24));body.setBackgroundColor(0xff101820);
        body.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(24)+b.left,dp(24)+b.top,dp(24)+b.right,dp(24)+b.bottom);return i;});
        body.addView(text(getString(R.string.app_name),30));
        body.addView(text("While parked, open AndroidMirror on Android Auto. Then tap Share phone screen here and approve Android’s screen-sharing prompt.",18));
        body.addView(text("The car shows a live, view-only copy. Navigate using your phone. Screen contents, including notifications, may be visible to everyone in the car. Protected apps may show a blank screen. No audio is captured and no recording is saved.",16));
        state=text("",16);body.addView(state);
        Button start=new Button(this);start.setText(R.string.start_sharing);start.setOnClickListener(v->requestSharing());body.addView(start);
        Button stop=new Button(this);stop.setText(R.string.stop_sharing);stop.setOnClickListener(v->startService(new Intent(this,MirrorService.class).setAction("STOP")));body.addView(stop);
        body.addView(text("Sharing stops when the car viewer closes or Android Auto restricts it. Restarting requires your approval again. Keep the phone unlocked.",16));
        setContentView(body);
    }
    private void requestSharing(){
        if(requesting)return;
        if(getDisplay()==null||getDisplay().getDisplayId()!=Display.DEFAULT_DISPLAY){state.setText(R.string.phone_only);return;}
        MediaProjectionManager manager=getSystemService(MediaProjectionManager.class);
        requesting=true;
        Intent request=Build.VERSION.SDK_INT>=34?manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()):manager.createScreenCaptureIntent();
        startActivityForResult(request,201);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=201)return;requesting=false;
        if(result!=RESULT_OK||data==null){MirrorService.status="Screen sharing was not approved.";return;}
        Rect bounds=getSystemService(WindowManager.class).getMaximumWindowMetrics().getBounds();
        try{startForegroundService(new Intent(this,MirrorService.class).putExtra("consent",data).putExtra("width",bounds.width()).putExtra("height",bounds.height()).putExtra("density",getResources().getConfiguration().densityDpi));}
        catch(RuntimeException e){MirrorService.status="Could not start sharing: "+e.getClass().getSimpleName();}
    }
    private final Runnable refresh=new Runnable(){public void run(){state.setText(MirrorService.status);handler.postDelayed(this,500);}};
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("requesting",requesting);super.onSaveInstanceState(state);}
}
