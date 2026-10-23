package com.pawan.parkedphotos;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.hardware.display.*;
import android.media.ImageReader;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.atomic.AtomicReference;

/** Idle-layout test on an owned secondary display; never requests phone capture. */
public class ZoomLayoutTest extends Instrumentation {
    private MainActivity activity;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void geometryChecks(){
        ZoomGeometry.Frame fit=ZoomGeometry.calculate(1808,680,1080,2400,1,999,999);
        require(fit.height==680&&fit.width==306&&fit.panX==0&&fit.panY==0,"Portrait fit and reset");
        ZoomGeometry.Frame portrait=ZoomGeometry.calculate(1808,680,1080,2400,4,100000,-100000);
        require(portrait.left>=0&&portrait.left+portrait.width<=1808,"Narrow portrait remains centered horizontally");
        require(portrait.top<=0&&portrait.top+portrait.height>=680,"Portrait pan cannot expose blank vertical edges");
        ZoomGeometry.Frame landscape=ZoomGeometry.calculate(1808,680,2400,1080,2,-100000,100000);
        require(landscape.left<=0&&landscape.left+landscape.width>=1808,"Landscape pan horizontal bounds");
        require(landscape.top<=0&&landscape.top+landscape.height>=680,"Landscape pan vertical bounds");
        ZoomGeometry.Frame min=ZoomGeometry.calculate(1808,680,1080,2400,-5,0,0);
        ZoomGeometry.Frame max=ZoomGeometry.calculate(1808,680,1080,2400,50,0,0);
        require(min.width==fit.width&&min.height==fit.height,"Minimum zoom clamp");
        require(max.width==portrait.width&&max.height==portrait.height,"Maximum zoom clamp");
    }
    @Override public void onStart(){
        Bundle results=new Bundle();int code=-1;
        ImageReader reader=null;VirtualDisplay display=null;
        try{
            require(!MirrorService.sharing,"Run layout checks while screen sharing is stopped");
            geometryChecks();
            getUiAutomation().adoptShellPermissionIdentity("android.permission.INTERNAL_SYSTEM_WINDOW","android.permission.ADD_TRUSTED_DISPLAY");
            reader=ImageReader.newInstance(1920,720,PixelFormat.RGBA_8888,2);
            reader.setOnImageAvailableListener(r->{try(android.media.Image image=r.acquireLatestImage()){}},new Handler(Looper.getMainLooper()));
            DisplayManager manager=getTargetContext().getSystemService(DisplayManager.class);
            display=manager.createVirtualDisplay("AndroidMirror layout test 1920x720",1920,720,160,reader.getSurface(),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY|DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION|DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC|(1<<10)); // TRUSTED, test display only
            require(display!=null,"Could not create owned test display");
            ActivityOptions options=ActivityOptions.makeBasic().setLaunchDisplayId(display.getDisplay().getDisplayId());
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK),options.toBundle());
            long deadline=SystemClock.elapsedRealtime()+5000;
            AtomicReference<Boolean> ready=new AtomicReference<>(false);
            do{
                runOnMainSync(()->ready.set(activity.findViewById(R.id.mirror_viewport).getWidth()>0));
                if(ready.get())break;
                Thread.sleep(50);
            }while(SystemClock.elapsedRealtime()<deadline);
            require(ready.get(),"Viewer did not lay out");
            runOnMainSync(()->{
                View viewport=activity.findViewById(R.id.mirror_viewport);
                View controls=activity.findViewById(R.id.zoom_controls);
                SurfaceView surface=activity.findViewById(R.id.mirror_surface);
                TextView label=activity.findViewById(R.id.zoom_label);
                Button plus=activity.findViewById(R.id.zoom_in),minus=activity.findViewById(R.id.zoom_out),fit=activity.findViewById(R.id.zoom_fit);
                int[] a=new int[2],b=new int[2];viewport.getLocationInWindow(a);controls.getLocationInWindow(b);
                require(b[0]>=a[0]+viewport.getWidth(),"Zoom controls must be beside the viewport");
                require(b[1]==a[1]&&controls.getHeight()==viewport.getHeight(),"Controls must occupy the side rather than the bottom");
                require("100%".contentEquals(label.getText()),"Initial zoom label");
                require(!minus.isEnabled(),"Zoom out disabled at fit");
                int initialWidth=surface.getLayoutParams().width;
                plus.performClick();plus.performClick();
                require("150%".contentEquals(label.getText()),"Zoom buttons increase zoom");
                require(surface.getLayoutParams().width>initialWidth,"Zoom enlarges mirrored surface");
                require(surface.getClipBounds()!=null,"Zoomed SurfaceView must be clipped to viewport");
                for(int i=0;i<30;i++)plus.performClick();
                require("400%".contentEquals(label.getText())&&!plus.isEnabled(),"Zoom maximum and disabled button");
                long now=SystemClock.uptimeMillis();
                MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,10,10,0);
                MotionEvent move=MotionEvent.obtain(now,now+20,MotionEvent.ACTION_MOVE,100000,100000,0);
                MotionEvent up=MotionEvent.obtain(now,now+40,MotionEvent.ACTION_UP,100000,100000,0);
                surface.dispatchTouchEvent(down);surface.dispatchTouchEvent(move);surface.dispatchTouchEvent(up);
                down.recycle();move.recycle();up.recycle();
                fit.performClick();
                require("100%".contentEquals(label.getText()),"Fit resets zoom");
                require(surface.getLayoutParams().width==initialWidth,"Fit restores surface size after panning");
                require(controls instanceof LinearLayout&&((LinearLayout)controls).getChildCount()==5,"Sidebar contains only title, zoom buttons, percentage and Fit");
            });
            results.putString("result","PASS: 1920x720 secondary-display layout, right sidebar, zoom/fit buttons, pan bounds, clipping, and limits");
        }catch(Throwable error){results.putString("error",error.toString());code=0;}
        finally{
            if(activity!=null){runOnMainSync(()->activity.finish());waitForIdleSync();}
            if(display!=null)display.release();
            if(reader!=null){reader.setOnImageAvailableListener(null,null);reader.close();}
            getUiAutomation().dropShellPermissionIdentity();
        }
        finish(code,results);
    }
}
