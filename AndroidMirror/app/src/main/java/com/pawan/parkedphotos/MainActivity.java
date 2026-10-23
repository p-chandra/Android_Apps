package com.pawan.parkedphotos;

import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.UUID;

/** Android Auto parked viewer. No input injection or head-unit access. */
public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private final String instance=UUID.randomUUID().toString();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private MirrorService service;
    private boolean bound,resumed;
    private SurfaceView surface;
    private FrameLayout canvas;
    private TextView status,curtain;
    private TextView zoomLabel;
    private Button zoomIn,zoomOut;
    private float zoom=1f,panX,panY,dragX,dragY;
    private int bufferWidth,bufferHeight;
    private int captureWidth=720,captureHeight=1600;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void diagnostic(String event){
        sendBroadcast(new Intent("com.pawan.carlinkrecorder.PARKED_APP_EVENT").setPackage("com.pawan.carlinkrecorder")
            .putExtra("event",event).putExtra("elapsed_realtime_ms",SystemClock.elapsedRealtime()).putExtra("utc",java.time.Instant.now().toString())
            .putExtra("display_id",getDisplay()==null?-1:getDisplay().getDisplayId()).putExtra("activity_instance",instance)
            .putExtra("changing_configuration",isChangingConfigurations()).putExtra("finishing",isFinishing()).putExtra("video_was_playing",false),
            "com.pawan.carlinkrecorder.permission.DIAGNOSTICS");
        android.util.Log.i("ParkedPhotosState",event+" mirror instance="+instance);
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);diagnostic("created");
        if(getDisplay()==null||getDisplay().getDisplayId()==Display.DEFAULT_DISPLAY){startActivity(new Intent(this,PhoneSetupActivity.class));finish();return;}
        if(state!=null){zoom=ZoomGeometry.clamp(state.getFloat("zoom",1f),ZoomGeometry.MIN,ZoomGeometry.MAX);panX=state.getFloat("panX");panY=state.getFloat("panY");}
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setBackgroundColor(0xff101820);
        body.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(b.left,b.top,b.right,b.bottom);return i;});
        status=new TextView(this);status.setTextColor(0xff7ae3c3);status.setTextSize(14);status.setMaxLines(2);status.setEllipsize(android.text.TextUtils.TruncateAt.END);status.setPadding(dp(8),dp(4),dp(8),dp(4));body.addView(status);
        LinearLayout viewingRow=new LinearLayout(this);viewingRow.setOrientation(LinearLayout.HORIZONTAL);body.addView(viewingRow,new LinearLayout.LayoutParams(-1,0,1));
        canvas=new FrameLayout(this);canvas.setId(R.id.mirror_viewport);canvas.setBackgroundColor(0xff000000);canvas.setClipChildren(true);canvas.setClipToPadding(true);viewingRow.addView(canvas,new LinearLayout.LayoutParams(0,-1,1));
        surface=new SurfaceView(this);surface.setId(R.id.mirror_surface);surface.setContentDescription(getString(R.string.mirror_description));surface.getHolder().addCallback(this);canvas.addView(surface,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        curtain=new TextView(this);curtain.setGravity(Gravity.CENTER);curtain.setTextColor(0xffedf4f7);curtain.setTextSize(18);curtain.setPadding(dp(20),dp(20),dp(20),dp(20));curtain.setBackgroundColor(0xff000000);curtain.setText(R.string.mirror_wait);canvas.addView(curtain,new FrameLayout.LayoutParams(-1,-1));
        canvas.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fit());
        canvas.setOnTouchListener(this::panMirror);surface.setOnTouchListener(this::panMirror);
        LinearLayout controls=new LinearLayout(this);controls.setId(R.id.zoom_controls);controls.setOrientation(LinearLayout.VERTICAL);controls.setGravity(Gravity.CENTER);controls.setPadding(dp(8),dp(8),dp(8),dp(8));controls.setBackgroundColor(0xff101820);viewingRow.addView(controls,new LinearLayout.LayoutParams(dp(112),-1));
        TextView title=new TextView(this);title.setText(R.string.zoom_title);title.setTextColor(0xffedf4f7);title.setTextSize(16);title.setGravity(Gravity.CENTER);controls.addView(title,new LinearLayout.LayoutParams(-1,dp(32)));
        zoomIn=new Button(this);zoomIn.setId(R.id.zoom_in);zoomIn.setText(R.string.zoom_in);zoomIn.setTextSize(24);zoomIn.setContentDescription(getString(R.string.zoom_in_description));zoomIn.setOnClickListener(v->setZoom(zoom+ZoomGeometry.STEP));controls.addView(zoomIn,new LinearLayout.LayoutParams(-1,dp(56)));
        zoomLabel=new TextView(this);zoomLabel.setId(R.id.zoom_label);zoomLabel.setTextColor(0xff7ae3c3);zoomLabel.setTextSize(18);zoomLabel.setGravity(Gravity.CENTER);controls.addView(zoomLabel,new LinearLayout.LayoutParams(-1,dp(36)));
        zoomOut=new Button(this);zoomOut.setId(R.id.zoom_out);zoomOut.setText(R.string.zoom_out);zoomOut.setTextSize(24);zoomOut.setContentDescription(getString(R.string.zoom_out_description));zoomOut.setOnClickListener(v->setZoom(zoom-ZoomGeometry.STEP));controls.addView(zoomOut,new LinearLayout.LayoutParams(-1,dp(56)));
        Button reset=new Button(this);reset.setId(R.id.zoom_fit);reset.setText(R.string.zoom_fit);reset.setAllCaps(false);reset.setContentDescription(getString(R.string.zoom_fit_description));reset.setOnClickListener(v->{panX=panY=0;setZoom(1f);});controls.addView(reset,new LinearLayout.LayoutParams(-1,dp(56)));
        updateZoomControls();
        setContentView(body);
    }
    private void setZoom(float value){zoom=ZoomGeometry.clamp(value,ZoomGeometry.MIN,ZoomGeometry.MAX);fit();updateZoomControls();}
    private void updateZoomControls(){
        zoomLabel.setText(getString(R.string.zoom_percent,Math.round(zoom*100)));
        zoomIn.setEnabled(zoom<ZoomGeometry.MAX);zoomOut.setEnabled(zoom>ZoomGeometry.MIN);
    }
    private boolean panMirror(View view,MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){dragX=event.getRawX();dragY=event.getRawY();return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&event.getPointerCount()==1){
            panX+=event.getRawX()-dragX;panY+=event.getRawY()-dragY;dragX=event.getRawX();dragY=event.getRawY();fit();return true;
        }
        if(event.getActionMasked()==MotionEvent.ACTION_UP){view.performClick();return true;}
        return true;
    }
    private void fit(){
        if(surface==null||canvas.getWidth()==0||canvas.getHeight()==0)return;
        ZoomGeometry.Frame frame=ZoomGeometry.calculate(canvas.getWidth(),canvas.getHeight(),captureWidth,captureHeight,zoom,panX,panY);
        panX=frame.panX;panY=frame.panY;
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)surface.getLayoutParams();
        if(p.width!=frame.width||p.height!=frame.height||p.leftMargin!=frame.left||p.topMargin!=frame.top||p.gravity!=(Gravity.TOP|Gravity.LEFT)){
            p.width=frame.width;p.height=frame.height;p.leftMargin=frame.left;p.topMargin=frame.top;p.gravity=Gravity.TOP|Gravity.LEFT;surface.setLayoutParams(p);
        }
        surface.setClipBounds(new Rect(Math.max(0,-frame.left),Math.max(0,-frame.top),
            Math.min(frame.width,canvas.getWidth()-frame.left),Math.min(frame.height,canvas.getHeight()-frame.top)));
        if(bufferWidth!=captureWidth||bufferHeight!=captureHeight){bufferWidth=captureWidth;bufferHeight=captureHeight;surface.getHolder().setFixedSize(captureWidth,captureHeight);}
    }
    private void attach(){if(service!=null&&resumed&&surface!=null&&surface.getHolder().getSurface().isValid())service.attach(surface.getHolder().getSurface(),(w,h)->{captureWidth=w;captureHeight=h;fit();});}
    private final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder binder){service=((MirrorService.LocalBinder)binder).service();attach();}
        public void onServiceDisconnected(ComponentName name){service=null;}
    };
    private final Runnable refresh=new Runnable(){public void run(){if(status!=null){status.setText(MirrorService.status);curtain.setVisibility(MirrorService.sharing?View.GONE:View.VISIBLE);curtain.setText(MirrorService.status);}handler.postDelayed(this,500);}};
    public void surfaceCreated(SurfaceHolder holder){attach();}
    public void surfaceChanged(SurfaceHolder holder,int format,int w,int h){attach();}
    public void surfaceDestroyed(SurfaceHolder holder){if(service!=null)service.detach();}
    @Override protected void onResume(){super.onResume();resumed=true;diagnostic("resumed");if(status!=null){bound=bindService(new Intent(this,MirrorService.class),connection,BIND_AUTO_CREATE);handler.post(refresh);}}
    @Override protected void onPause(){diagnostic("paused");resumed=false;handler.removeCallbacks(refresh);if(service!=null)service.detach();if(bound){unbindService(connection);bound=false;}service=null;super.onPause();}
    @Override protected void onStop(){diagnostic("stopped");super.onStop();}
    @Override protected void onDestroy(){diagnostic("destroyed");super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){state.putFloat("zoom",zoom);state.putFloat("panX",panX);state.putFloat("panY",panY);super.onSaveInstanceState(state);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);diagnostic(focus?"focus_gained":"focus_lost");}
}
