package com.pawan.parkedphotos;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.hardware.display.*;
import android.media.projection.*;
import android.os.*;
import android.view.Surface;

/** Local display transport only. No frames are stored or sent over a socket. */
public class MirrorService extends Service {
    static volatile String status="Open AndroidMirror on the car while parked, then approve sharing on the phone.";
    static volatile boolean sharing;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final IBinder binder=new LocalBinder();
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private Surface surface;
    private boolean viewerVisible,ending;
    private int width=720,height=1600,density=240;
    interface SizeListener { void size(int width,int height); }
    private SizeListener listener;
    class LocalBinder extends Binder { MirrorService service(){return MirrorService.this;} }
    private final Runnable expiry=()->end("Sharing approval expired without an open car viewer. Approve again on the phone.");
    private final MediaProjection.Callback callback=new MediaProjection.Callback(){
        @Override public void onStop(){end("Screen sharing ended. Approve again on the phone to restart.");}
        @Override public void onCapturedContentResize(int w,int h){
            if(w<=0||h<=0||projection==null)return;
            setSize(w,h);
            if(virtualDisplay!=null)virtualDisplay.resize(width,height,density);
            if(listener!=null)listener.size(width,height);
        }
    };
    private void setSize(int w,int h){float scale=Math.min(1f,1280f/Math.max(w,h));width=Math.max(2,Math.round(w*scale));height=Math.max(2,Math.round(h*scale));}
    void attach(Surface target,SizeListener sizeListener){surface=target;listener=sizeListener;viewerVisible=true;if(listener!=null)listener.size(width,height);connectSurface();}
    void detach(){surface=null;listener=null;viewerVisible=false;end("Car viewer closed. Screen sharing stopped; approve again on the phone.");}
    private void connectSurface(){
        if(projection==null||!viewerVisible||surface==null||!surface.isValid())return;
        try{
            if(virtualDisplay==null)virtualDisplay=projection.createVirtualDisplay("ParkedPhoneMirror",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,surface,null,handler);
            else virtualDisplay.setSurface(surface);
            sharing=true;status="Mirroring live · view only · use the phone to navigate";handler.removeCallbacks(expiry);
            android.util.Log.i("ParkedMirrorState","capture_active "+width+"x"+height);
        }catch(RuntimeException e){end("Screen sharing could not start: "+e.getClass().getSimpleName()+". Approve again on the phone.");}
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        if("STOP".equals(intent.getAction())){end("Screen sharing stopped.");return START_NOT_STICKY;}
        Intent consent=intent.getParcelableExtra("consent",Intent.class);
        if(consent==null)return START_NOT_STICKY;
        if(projection!=null){status="A sharing session is already active.";return START_NOT_STICKY;}
        try{
            NotificationManager nm=getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("mirror","Phone screen sharing",NotificationManager.IMPORTANCE_LOW));
            PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,MirrorService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent open=PendingIntent.getActivity(this,2,new Intent(this,PhoneSetupActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            Notification note=new Notification.Builder(this,"mirror").setSmallIcon(R.drawable.ic_photo).setContentTitle(getString(R.string.app_name)+" · screen sharing")
                .setContentText("Your phone screen is shared with the parked car viewer.").setOngoing(true).setContentIntent(open)
                .addAction(new Notification.Action.Builder(null,"Stop sharing",stop).build()).build();
            startForeground(52,note,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            density=Math.max(1,intent.getIntExtra("density",240));setSize(intent.getIntExtra("width",1080),intent.getIntExtra("height",2400));
            projection=getSystemService(MediaProjectionManager.class).getMediaProjection(Activity.RESULT_OK,consent);
            if(projection==null)throw new IllegalStateException("No projection token");
            projection.registerCallback(callback,handler);
            status="Approved. Open AndroidMirror on Android Auto within 60 seconds.";
            handler.postDelayed(expiry,60000);if(listener!=null)listener.size(width,height);connectSurface();
        }catch(RuntimeException e){end("Screen sharing unavailable: "+e.getClass().getSimpleName()+". Approve again on the phone.");}
        return START_NOT_STICKY;
    }
    void end(String reason){
        if(ending)return;ending=true;handler.removeCallbacks(expiry);sharing=false;
        MediaProjection old=projection;projection=null;
        if(virtualDisplay!=null){virtualDisplay.release();virtualDisplay=null;}
        if(old!=null){old.unregisterCallback(callback);old.stop();}
        status=reason;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();ending=false;
        android.util.Log.i("ParkedMirrorState","capture_stopped "+reason);
    }
    @Override public IBinder onBind(Intent intent){return binder;}
    @Override public void onDestroy(){end("Mirror service ended.");surface=null;listener=null;super.onDestroy();}
}
