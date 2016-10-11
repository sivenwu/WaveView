# WaveView

### Thank you for your support!

Imitate the Android progress of  method, At present support functions:

1. waveview to support the dynamic change of progress  

2. waveview to support the progress callback Activity or fragments, use mask effect and the property animation 

3. waveview to support custom change wave properties, including color wave, wave speed, wave shape of container (currently support circle, rectangular, and mask drawable) 

   ​

Details you can run the Demo and study the source code.



### My :

E-mail : sy.wu@foxmail.com

Blog : http://www.jianshu.com/users/d388bcf9c4d3/  

 **KeyWord**  rolling wave ,normal wave.waveview shapes,speed mode,shado...

### Demo ：

 ![waveview.gif](http://upload-images.jianshu.io/upload_images/2516602-d582b82fe16e1189.gif?imageMogr2/auto-orient/strip)

### How to use it?

#### First

Add a WaveView into your XML

```java
   <com.yuan.waveview.WaveView
            android:id="@+id/waveview"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerHorizontal="true"
            android:layout_marginTop="20dp"
            android:layout_margin="20dp"
            android:layout_below="@id/radio_view_speed"
            app:backgroundColor="@color/white"
            app:progressColor="@color/black"
            app:max="20"
            />
```

"backgroundColor" is the view color

"progressColor" is the wave color

"max" is the max progress



#### Second

You can be a series of operations in the Activity or Fragment.such as :

(1) set WaveView Shape

```
  waveView.setMode(WaveView.MODE_RECT);
```

(2) set WaveView Color

```
waveView.setWaveColor(Color.RED);
waveView.setbgColor(Color.WHITE);
```

(3) set WaveView flowing speed

```
waveView.setSpeed(WaveView.SPEED_FAST);
```

(4) set WaveView progress or max

```
   waveView.setProgress(progress);
   waveView.setMax(100);
```



#### Last

How to listen progress of waveView in Activity or Fragmenet?

```
   waveView.setProgressListener(new WaveView.waveProgressListener() {
            @Override
            public void onPorgress(boolean isDone, long progress, long max) { 
                if (isDone){
                    Toast.makeText(MainActivity.this,"Loading completed!!!",Toast.LENGTH_SHORT).show();
                }
            }
        });
```



### Thank you for your support!

### Enjoy ~



