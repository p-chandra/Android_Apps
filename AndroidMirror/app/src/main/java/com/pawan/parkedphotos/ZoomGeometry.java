package com.pawan.parkedphotos;

/** Viewport math only: zooming never changes the phone or capture resolution. */
final class ZoomGeometry {
    static final float MIN = 1f, MAX = 4f, STEP = 0.25f;

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class Frame {
        final int width, height, left, top;
        final float panX, panY;
        Frame(int width, int height, int left, int top, float panX, float panY) {
            this.width=width; this.height=height; this.left=left; this.top=top;
            this.panX=panX; this.panY=panY;
        }
    }

    static Frame calculate(int viewportWidth, int viewportHeight, int captureWidth, int captureHeight,
                           float zoom, float panX, float panY) {
        if(viewportWidth<=0||viewportHeight<=0||captureWidth<=0||captureHeight<=0)
            throw new IllegalArgumentException("Viewport and capture sizes must be positive");
        float fit=Math.min((float)viewportWidth/captureWidth,(float)viewportHeight/captureHeight);
        int width=Math.max(1,Math.round(captureWidth*fit*clamp(zoom,MIN,MAX)));
        int height=Math.max(1,Math.round(captureHeight*fit*clamp(zoom,MIN,MAX)));
        float maxX=Math.max(0,(width-viewportWidth)/2f),maxY=Math.max(0,(height-viewportHeight)/2f);
        panX=clamp(panX,-maxX,maxX); panY=clamp(panY,-maxY,maxY);
        return new Frame(width,height,Math.round((viewportWidth-width)/2f+panX),
                         Math.round((viewportHeight-height)/2f+panY),panX,panY);
    }
}
