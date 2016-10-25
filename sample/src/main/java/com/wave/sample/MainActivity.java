package com.wave.sample;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.yuan.waveview.WaveView;

import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    WaveView waveView;
    Button button,speedBtn;
    TextView progressTv;

    RadioGroup radioGroup;
    RadioGroup radioGroupColor;
    RadioGroup radioGroupSpeed;

    int count = 0;
    int max = 20;
    int speed = 5;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100){
                if (max >= count) {
                    waveView.setProgress(count);
                }else {
                    resetTimer();
                }
                count++;
            }
        }
    };

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            handler.sendEmptyMessage(100);
        }
    };

    private Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initListener();
    }

    private void initView(){
        waveView = (WaveView) findViewById(R.id.waveview);
        button = (Button) findViewById(R.id.button);
        radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        radioGroupColor = (RadioGroup) findViewById(R.id.radio_group_color);
        radioGroupSpeed = (RadioGroup) findViewById(R.id.radio_group_speed);
        progressTv = (TextView) findViewById(R.id.pb_show_tv);
        speedBtn = (Button) findViewById(R.id.speed);

        waveView.setMax(50);
        waveView.setProgress(10);

        max = (int) waveView.getMax();
    }

    private void initListener(){
        final NumberFormat numberFormat =  NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        waveView.setProgressListener(new WaveView.waveProgressListener() {
            @Override
            public void onPorgress(boolean isDone, long progress, long max) {
                Log.i("yuan","max " + max + "prgress "+progress);
                progressTv.setText(numberFormat.format(progress/(float)max * 100) + "%");
                if (isDone){
                    Toast.makeText(MainActivity.this,"Loading completed!!!",Toast.LENGTH_SHORT).show();
                }
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //先重置
                resetTimer();
                doing();
            }
        });

        speedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int p = (int) (waveView.getProgress()+3);
                waveView.setProgress(p);
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.mode_circle:
                        waveView.setMode(WaveView.MODE_CIRCLE);
                        break;

                    case R.id.mode_rect:
                        waveView.setMode(WaveView.MODE_RECT);
                        break;

                    case R.id.mode_drawable:
                        waveView.setMode(WaveView.MODE_DRAWABLE);
                        break;
                }
            }

        });

        radioGroupColor.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.color_black:
                        waveView.setWaveColor(Color.BLACK);
                        break;

                    case R.id.color_red:
                        waveView.setWaveColor(Color.RED);
                        break;

                    case R.id.color_blue:
                        waveView.setWaveColor(Color.BLUE);
                        break;
                    case R.id.color_green:
                        waveView.setWaveColor(Color.GREEN);
                        break;
                }
            }
        });

        radioGroupSpeed.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.speed_normal:
                        waveView.setSpeed(WaveView.SPEED_NORMAL);
                        break;

                    case R.id.speed_slow:
                        waveView.setSpeed(WaveView.SPEED_SLOW);
                        break;

                    case R.id.speed_fast:
                        waveView.setSpeed(WaveView.SPEED_FAST);
                        break;
                }
            }
        });
    }

    public void doing(){
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(100);
            }
        };
        timer = new Timer();
        timer.schedule(timerTask,1000,1000);
    }

    private void resetTimer(){
        count = 0;
        if (timer != null)
        timer.cancel();
        timer = null;
        if (timerTask !=null)
        timerTask.cancel();
        timerTask = null;
    }
}
