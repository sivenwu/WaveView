package com.yuan.waveview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;

import com.yuan.waveview.utils.DensityUtil;

import java.util.ArrayList;
import java.util.List;

import static android.view.animation.Animation.INFINITE;

/**
 * Created by Yuan on 2016/10/8.
 * Detail wave for view
 *        <tr>
 *            Thank you for your support!
 *            Imitate the Android progress of  method, At present support functions:
 *
 *            1. waveview to support the dynamic change of progress,
 *            2. waveview to support the progress callback Activity or fragments, use mask effect and the property animation,
 *            3. waveview to support custom change wave properties, including color wave, wave speed, wave shape of container (currently support circle, rectangular, and mask drawable).
 *
 *           Details you can run the Demo and study the source code.
 *
 *           My :
 *           E-mail : sy.wu@foxmail.com
 *           Blog : http://www.jianshu.com/users/d388bcf9c4d3/
 *        </tr>
 *
 * KeyWord  rolling wave ,normal wave.waveview shapes,speed mode,shadow...
 */

public class WaveView extends View {

    private final String TAG = "WaveView";
    private Context mContext;

    private Paint wavePaint;

    /**
     * The wave of the normal grain
     */
    private Path wavePath;

    /**
     * The wave of the rolling grain
     */
    private Path shadPath;

    //mode
    private int WAVE_COLOR = Color.BLUE;// Color for wave
    private int BG_COLOR = Color.WHITE;// Color for view of background

    /**
     * the width and height for view  < width and height is 300 dpi  by default ></>
     */
    private float VIEW_WIDTH = 0f;
    private float VIEW_HEIGHT = 0f;
    private float VIEW_WIDTH_TMP = 0f;
    private float VIEW_HEIGHT_TMP = 0f;


    /**
     * the width and height for wave  < Width is half the width of view , Height is auto ></>
     */
    private float WAVE_WIDTH = 0f;
    private float WAVE_HEIGHT = 0f;

    /**
     * There are three kinds of waveview shapes(mode), including circle、rect and drawable
     * < Drawable shape , you need to have default drawable></>
     */
    public final static String MODE_CIRCLE = "circle";
    public final static String MODE_RECT = "rect";
    public final static String MODE_DRAWABLE = "drawable";
    private String mode = MODE_CIRCLE; //default shape is circle

    /**
     * pointList : normal wave of  original collection point
     * shadpointList : rolling wave of  original collection point
     */
    private List<Point> pointList = new ArrayList<>();
    private List<Point> shadpointList = new ArrayList<>();

    /**
     * < Sign control variables ></>
     */
    private boolean isInitPoint = true; // Init original collection point
    private boolean isStartAnimation = false;// The first time for start the flowingAnimation
    private boolean isDone = false;// whether to end
    private boolean isMeasure = false;// The first time for measure view
    private boolean isCompleteLayout = false;//just action when drawing finish
    boolean isHasWindowFocus = false;// is hasWindowFocus

    /**
     * < value ></>
     */
    private float dy = 0;// height of the rise
    private float old_dy = 0; //height of the rise  ,often change
    private float sum_dy = 0;// defalut height
    private float beforDy = 0;//The last time the height of the rise

    private float dx = 0;// Distance for Horizontal-Moving < normal wave >
    private float shd_dx = 0;// Distance for Horizontal-Moving < rolling wave >
    private float runRatio = 1.5f;

    /**
     * There are three kinds of waveview mode of speed , including slow、normal and fast
     */
    public  static float SPEED_SLOW = 10f; // Slow speed
    public  static float SPEED_NORMAL = 30f;// normal speed
    public  static float SPEED_FAST = 40f;// fast speed
    private int  isSlow = 0x01;
    private int isNormal = 0x02;
    private int isFast = 0x03;
    private float speed = SPEED_NORMAL;// default speed
    private int curSpeedMode = isNormal;

    /**
     * < the progress for waveview ></>
     */
    private long progress = 0;// The current progress
    private long curProgress = 0;// The current progress , in order to deal with some logical work
    private long max = 0;// The max progress
    private float progressRatio = 0f;// ratio < result = progress / max >

    private waveProgressListener progressListener;

    private ValueAnimator reiseAnimator;
    private ObjectAnimator flowingAnimato;

    public WaveView(Context context) {
        super(context);
        init(context,null);
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attrs){
        if (attrs != null){
            //attars
            TypedArray typedArray = context.obtainStyledAttributes(attrs,R.styleable.WaveView);

            int bgColor =  typedArray.getColor(R.styleable.WaveView_backgroudColor,BG_COLOR);
            int pColor = typedArray.getColor(R.styleable.WaveView_progressColor,WAVE_COLOR);
            int aMax = typedArray.getInt(R.styleable.WaveView_max, (int) max);
            int aP = typedArray.getInteger(R.styleable.WaveView_progress, (int) progress);

            BG_COLOR = bgColor;
            WAVE_COLOR = pColor;
            max = aMax;
            progress = aP;

            typedArray.recycle();
        }

        VIEW_WIDTH = DensityUtil.dip2px(context,300);
        VIEW_HEIGHT = DensityUtil.dip2px(context,300);

        wavePath = new Path();
        shadPath = new Path();
        wavePath.setFillType(Path.FillType.EVEN_ODD);
//        shadPath.setFillType(Path.FillType.EVEN_ODD);

        this.mContext = context;

        wavePaint = new Paint();
        wavePaint.setColor(BG_COLOR);
        wavePaint.setStrokeWidth(1);
        wavePaint.setStyle(Paint.Style.FILL);
        wavePaint.setAntiAlias(true);
        wavePaint.setAlpha(50);

        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // TODO Auto-generated method stub
                Log.i(TAG,"绘制完成");
                setSpeed(speed);
                if (isHasWindowFocus && progress > 0){

                    isCompleteLayout = true;

                    long cP = max - progress;
                    if (max >= progress) {
                        progressRatio = progress / (float) max;
                        dy = updateDyData();
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        isMeasure = true;
                    }
                }
                VIEW_HEIGHT_TMP = VIEW_HEIGHT;
                VIEW_WIDTH_TMP = VIEW_WIDTH;
                Log.i(TAG,"tmp of width and tmp of hight is init ! "+VIEW_WIDTH_TMP +" "+VIEW_WIDTH_TMP);
            }
        });

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d(TAG,"onMeasure " +isHasWindowFocus);
        if (!isMeasure)
            setMeasuredDimension(getRealWidthMeasureSpec(widthMeasureSpec),getRealHeightMeasureSpec(heightMeasureSpec));
        initPoint();
    }

    /**
     * Initialize the original wave arts collection point , including normal wave ,rolling wave
     */
    private void initPoint(){
        if (isInitPoint){
            isInitPoint = false;
            pointList.clear();
            shadpointList.clear();

            WAVE_WIDTH = (float) (VIEW_WIDTH / 2.5);
//            WAVE_HEIGHT = (float) (VIEW_HEIGHT / 50);
            WAVE_HEIGHT = VIEW_HEIGHT/getWaveHeight();

            dy = VIEW_HEIGHT;//Started from the bottom, when the height is rise, dy gradually reduce
            //How many points calculated maximum support
            int n = Math.round(VIEW_WIDTH / WAVE_WIDTH);
            //start point for normal wave
            int startX = 0;
            Log.i(TAG,"begin point ("+DensityUtil.px2dip(mContext,startX)+" , "+DensityUtil.px2dip(mContext,dy)+")");
            for (int i = 0; i < 4*n+1; i++) {
                Point point = new Point();
                point.y = (int) dy;
                if (i == 0) {
                    point.x = startX;
                } else {
                    startX += WAVE_WIDTH;
                    point.x = startX;
                }
                pointList.add(point);
            }
            // start point for rolling wave
            startX = (int) VIEW_WIDTH;
            for (int i = 0; i < 4*n+1; i++) {
                Point point = new Point();
                point.y = (int) dy;
                if (i == 0) {
                    point.x = startX;
                } else {
                    startX -= WAVE_WIDTH;
                    point.x = startX;
                }
                shadpointList.add(point);
            }

            //change speed base on view_width
            SPEED_NORMAL =  (DensityUtil.px2dip(mContext,VIEW_WIDTH)/20);
            SPEED_SLOW = SPEED_NORMAL/2;
            SPEED_FAST = SPEED_NORMAL * 2;

            SPEED_NORMAL = SPEED_NORMAL ==0?1:SPEED_NORMAL;
            SPEED_SLOW = SPEED_SLOW ==0?0.5f:SPEED_SLOW;
            SPEED_FAST = SPEED_FAST ==0?2:SPEED_FAST;

            if (curSpeedMode == isSlow){
                speed = SPEED_SLOW;
            }else if (curSpeedMode == isFast){
                speed = SPEED_FAST;
            }else{
                speed = SPEED_NORMAL;
            }

            Log.i(TAG,"init speed ( normal : " + SPEED_NORMAL +" slow : "+SPEED_SLOW +" fast : "+SPEED_FAST +" )");
        }
    }

    /**
     *  set waveProgressListener
     * @param progressListener
     */
    public void setProgressListener(waveProgressListener progressListener) {
        this.progressListener = progressListener;
        isDone = false;
    }

    private int getWaveHeight(){
        if (speed == SPEED_FAST){
            return 30;
        }else if (speed == SPEED_SLOW){
            return 70;
        }else{
            return 50;
        }
    }

    /**
     * set speed
     * @param speed including slow、normal and fast
     */
    public void setSpeed(float speed) {
        if (speed == SPEED_FAST || speed == SPEED_NORMAL || speed == SPEED_SLOW) {

            if (speed == SPEED_FAST){
                curSpeedMode = isFast;
            }else if (speed == SPEED_SLOW){
                curSpeedMode = isSlow;
            }else{
                curSpeedMode = isNormal;
            }
            this.speed = speed;
            dx = 0;
            shd_dx = 0;
//            rerefreshPoints();
        }
    }

    /**
     * set shape (mode)
     * @param mode including circle、rect and drawable
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * set max progress
     * @param max
     */
    public void setMax(long max) {
        this.max = max;
        isDone = false;
    }

    /**
     * set color for  view of background
     * @param color
     */
    public void setbgColor(int color) {
        this.BG_COLOR = color;
    }

    /**
     * set color for  wave of background
     * @param color
     */
    public void setWaveColor(int color) {
        this.WAVE_COLOR = color;
    }

    /**
     * set progress
     * @param progress
     */
    public void setProgress(long progress) {
        if (reiseAnimator!=null && reiseAnimator.isRunning()){
//            reiseAnimator.cancel();//不应该取消，应该让其直接结束
            reiseAnimator.end();
        }

        this.progress = progress;
        if (progress == 0){ resetWave();}
        if (!isCompleteLayout){return ;}

        long cP = max - progress;
        if (max >= progress) {
            progressRatio = cP / (float)max;
            updateProgress();
        }
    }

    public long getProgress() {
        return progress;
    }

    public long getMax() {
        return max;
    }

    /**
     * reset point set
     * < When in onDraw need to measure the initialization point set></>
     */
    private void rerefreshPoints(){
        pointList.clear();
        shadpointList.clear();

        WAVE_HEIGHT = VIEW_HEIGHT/getWaveHeight();

        //计算最多能支持多少点 非控制点
        int n = Math.round(VIEW_WIDTH / WAVE_WIDTH);
        //起始点
        int startX = (int) -dx;
        for (int i = 0; i < 4*n+1; i++) {
            Point point = new Point();
            point.y = (int) dy;
            if (i == 0) {
                point.x = startX;
            } else {
                startX += WAVE_WIDTH;
                point.x = startX;
            }
            pointList.add(point);
        }

        startX = (int) VIEW_WIDTH;
        for (int i = 0; i < 4*n+1; i++) {
            Point point = new Point();
            point.y = (int) dy;
            if (i == 0) {
                point.x = startX;
            } else {
                startX -= WAVE_WIDTH;
                point.x = startX;
            }
            shadpointList.add(point);
        }

//        Log.i("wusy","test v : y " +shadpointList.get(0).y);
    }

    public void resetWave(){
        isDone = false;
        dy = VIEW_HEIGHT;
        beforDy = 0;
    }

    private int updateDyData(){
        old_dy = dy;
        int offsetDy = (int) (sum_dy - sum_dy * progressRatio - beforDy);
        beforDy = sum_dy - sum_dy * progressRatio;
        return  offsetDy;
    }

    /**
     * In a second riseAnimation when set the progress !!
     * <second by second execution of progress  ></>
     */
    private void updateProgress(){
        riseAnimation();
    }

    /**
     * for measure width
     * @param widthMeasureSpec
     * @return
     */
    private int  getRealWidthMeasureSpec(int widthMeasureSpec){
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        if (mode == MeasureSpec.AT_MOST){
            Log.i(TAG,"AT_MOST width :"+ DensityUtil.px2dip(mContext,widthSize));//warp
//            VIEW_WIDTH = widthSize;
        }else if (mode == MeasureSpec.EXACTLY){
            Log.i(TAG,"EXACTLY width :"+ DensityUtil.px2dip(mContext,widthSize));
            VIEW_WIDTH = widthSize;
        }else if (mode == MeasureSpec.UNSPECIFIED){
            Log.i(TAG,"UNSPECIFIED width :"+ DensityUtil.px2dip(mContext,widthSize));
//            VIEW_WIDTH = VIEW_WIDTH_TMP;
        }
        return (int) VIEW_WIDTH;
    }

    /**
     * for measure height
     * @param heightMeasureSpec
     * @return
     */
    private int  getRealHeightMeasureSpec(int heightMeasureSpec){

        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (mode == MeasureSpec.AT_MOST){
            Log.i(TAG,"AT_MOST heitht :"+ DensityUtil.px2dip(mContext,heightSize));
//            VIEW_HEIGHT = heightSize;
        }else if (mode == MeasureSpec.EXACTLY){
            Log.i(TAG,"EXACTLY heitht :"+ DensityUtil.px2dip(mContext,heightSize));
            VIEW_HEIGHT = heightSize;
        }else if (mode == MeasureSpec.UNSPECIFIED){
            Log.i(TAG,"UNSPECIFIED heitht :"+ DensityUtil.px2dip(mContext,heightSize));
//            VIEW_HEIGHT = VIEW_HEIGHT_TMP;
        }
        if (!isHasWindowFocus){updateDyData();}else {
            dy = VIEW_HEIGHT;//为了防止多次测量，必须重新更新初始高度
            old_dy = dy;
            sum_dy = dy;
        }

        return (int) VIEW_HEIGHT;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
//        Log.i(TAG,"onLayout");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //To prevent repeated drawing
        wavePath.reset();
        shadPath.reset();

        wavePaint.setColor(BG_COLOR);
        wavePaint.setAlpha(255);
        float radius = VIEW_WIDTH / 2f;

        int saveFlags = Canvas.MATRIX_SAVE_FLAG | Canvas.CLIP_SAVE_FLAG | Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.FULL_COLOR_LAYER_SAVE_FLAG | Canvas.CLIP_TO_LAYER_SAVE_FLAG;
        canvas.saveLayer(0, 0, VIEW_WIDTH, VIEW_HEIGHT, null, saveFlags);

        // set shape
        if (mode.equals(MODE_DRAWABLE)){
            drawableToBitamp(mContext.getResources().getDrawable(R.drawable.wave_icon,null),canvas);
        } else if (mode.equals(MODE_RECT)){
            canvas.drawRect(0,0,VIEW_WIDTH,VIEW_HEIGHT,wavePaint);
        } else{
            canvas.drawCircle(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f, radius, wavePaint);
        }

        wavePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        // drawing normal wave
        wavePaint.setColor(WAVE_COLOR);
        wavePaint.setAlpha(80);
        float end1 = 0;
        for (int i = 0; i < pointList.size(); i++) {
            int j = i + 1;
            if (pointList.size() > i) {
                float start1 = pointList.get(i).x;
                wavePath.moveTo(start1, dy);//+dy
                if (j % 2 == 0 && j >= 2) {
                    end1 = start1;
                    wavePath.quadTo(start1 + WAVE_WIDTH / 2, dy + WAVE_HEIGHT, start1 + WAVE_WIDTH, dy);//+dy
                } else {
                    end1 = start1;
                    wavePath.quadTo(start1 + WAVE_WIDTH / 2, dy - WAVE_HEIGHT, start1 + WAVE_WIDTH, dy);
                }}
        }

        if (end1 >= VIEW_WIDTH) {
            wavePath.lineTo(VIEW_WIDTH, VIEW_HEIGHT);
            wavePath.lineTo(0, VIEW_HEIGHT);
            wavePath.lineTo(0, dy);
            wavePath.close();
            canvas.drawPath(wavePath, wavePaint);
        }

        // drawing rolling wave
        wavePaint.setAlpha(50);
        for (int i = 0; i < shadpointList.size(); i++) {
            int j = i + 1;
            if (shadpointList.size() > i) {
                float start1 = shadpointList.get(i).x + shd_dx;
                shadPath.moveTo(start1, dy);//+dy
                if (j % 2 == 0 && j >= 2) {
                    end1 = start1;
                    shadPath.quadTo(start1 - WAVE_WIDTH / 2, (float) (dy + WAVE_HEIGHT *runRatio), start1 - WAVE_WIDTH, dy);//+dy
                } else {
                    end1 = start1;
                    shadPath.quadTo(start1 - WAVE_WIDTH / 2, (float) (dy - WAVE_HEIGHT*runRatio), start1 - WAVE_WIDTH, dy);
                }
            }
        }
        if (end1 <= -VIEW_WIDTH) {
            shadPath.lineTo(0, VIEW_HEIGHT);
            shadPath.lineTo(VIEW_WIDTH, VIEW_HEIGHT);
            shadPath.lineTo(VIEW_WIDTH, dy);
            shadPath.close();
            canvas.drawPath(shadPath, wavePaint);
        }

        // xfer
        wavePaint.setXfermode(null);
        canvas.restore();
//        super.onDraw(canvas);

        // display listener for activity or fragment
        if (this.progressListener != null){
            if (!isDone && curProgress != this.progress) {
                this.progressListener.onPorgress(this.progress == this.max, this.progress, this.max);
                curProgress = this.progress;
            }
            if (this.progress == this.max){
                isDone = true;
//                dy = -10;//In order to complete fill finally effect
//                resetWave();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        isHasWindowFocus = hasWindowFocus;
        if (!isStartAnimation){
            isStartAnimation = true;
            flowingAnimation();
        }
        // TODO: 2016/10/26  屏幕重新点亮的时候 一定要重新测量！！！
        if (!hasWindowFocus){
            if (flowingAnimato!=null)
                flowingAnimato.cancel();
            if (reiseAnimator!=null)
                reiseAnimator.end();
            isMeasure = false;
        }else{
            if (flowingAnimato!=null && !flowingAnimato.isRunning()){flowingAnimation();}
            if (reiseAnimator!=null && !reiseAnimator.isRunning()){setProgress(this.progress);}
            invalidate();
        }
    }

    private void flowingAnimation(){
        flowingAnimato = ObjectAnimator.ofFloat(this,"wave",0,100)
                .setDuration(100);
        flowingAnimato.setRepeatCount(INFINITE);
        flowingAnimato.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                dx = dx + speed;
                shd_dx = shd_dx + speed/2;//Half the speed of the normal waves

                if (shd_dx == WAVE_WIDTH *2){
                    shd_dx = 0;
                }

                if (dx == WAVE_WIDTH *2){
                    dx = 0;
                }
                rerefreshPoints();
                postInvalidate();
            }
        })
        ;
        flowingAnimato.start();
    }

    private void riseAnimation(){
        if (!isHasWindowFocus){
            // TODO: 2016/10/26 不可视的时候停止
            return ;
        }
        isMeasure = true;
        if (dy > 0) {
            float offset = updateDyData();
//            Log.i("yuan", "move s " + s + "and sum_dy" + sum_dy);
            reiseAnimator = ValueAnimator.ofFloat(0, offset)
                    .setDuration(500);
            reiseAnimator.setInterpolator(new LinearInterpolator());
            reiseAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {}

                @Override
                public void onAnimationEnd(Animator animator) {
                    // TODO: 2016/10/26 必须重置dy,修正偏移 
                    dy = sum_dy - beforDy;
                }

                @Override
                public void onAnimationCancel(Animator animator) {}

                @Override
                public void onAnimationRepeat(Animator animator) {}
            });
            reiseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float m = (float) valueAnimator.getAnimatedValue();
                    float s = old_dy - m;
                    dy = s;
//                        Log.i("yuan", "move m " + m + "dy " + dy);
                }
            });


            reiseAnimator.start();
        }
    }


    /**
     * drawable to bitmap
     * @param drawable
     * @param canvas
     * @return
     */
    private void drawableToBitamp(Drawable drawable,Canvas canvas)
    {
        int w = (int) VIEW_WIDTH;
        int h = (int) VIEW_HEIGHT;
        Bitmap.Config config =
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(w,h,config);
        canvas.drawBitmap(bitmap,0,0,wavePaint);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
    }

    public interface waveProgressListener{
        void onPorgress(boolean isDone, long progress, long max);
    }

}

