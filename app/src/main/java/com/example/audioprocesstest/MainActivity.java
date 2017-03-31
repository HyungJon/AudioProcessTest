package com.example.audioprocesstest;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioRecord;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    Thread t; // thread obj for holding audio proc thread
    int Fs = 44100; // sampling rate
    boolean isRunning = true; // switch audio on/off
    SeekBar freqSlider;
    float freqSliderVal;
    SeekBar volSlider;
    float volSliderVal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // create output audio obj of class AudioTrack
        final int buffsize = AudioTrack.getMinBufferSize(Fs, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        final short samples[] = new short[buffsize];
        final int amp = 10000;
        final double twopi = 8.*Math.atan(1.);
        final double fr = 440.f;
        final double ph = 0.0;
        final Context context = getApplicationContext();


        Button playButton = (Button) findViewById(R.id.play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                t = new Thread() {
                    public void run() {
                        setPriority(Thread.MAX_PRIORITY);


                        byte[] sound = null;
                        int i = 0;
                        InputStream is = context.getResources().openRawResource(R.raw.mingus);

                        byte[] header = new byte[44];
                        try {
                            is.read(header);
                            ByteBuffer wrapped = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN);
                            Fs = wrapped.getInt();
                        } catch (IOException e) {

                        }

                        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, Fs,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                buffsize,
                                AudioTrack.MODE_STREAM);

                        try {
                            sound = new byte[buffsize*2];
                            audioTrack.play();
                            while ((i = is.read(sound)) != -1) {
                                short[] sample = new short[buffsize];
                                ByteBuffer bb = ByteBuffer.wrap(sound);
                                bb.order( ByteOrder.LITTLE_ENDIAN);
                                int j = 0;
                                while( bb.hasRemaining()) {
                                    short v = bb.getShort();
                                    sample[j++] = v;
                                }
                                audioTrack.setStereoVolume(volSliderVal, volSliderVal);
                                // setStereoVolume is deprecated
                                audioTrack.write(sample, 0, buffsize);
                            }
                        } catch (IOException e) {

                        }

                        double fr = 440.f;
                        double ph = 0.0;

                        audioTrack.stop();
                        audioTrack.release();
                    }
                };
                t.start();
            }
        });

        volSlider = (SeekBar) findViewById(R.id.volume);
        SeekBar.OnSeekBarChangeListener volListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    volSliderVal = (progress / (float) seekBar.getMax()) * 2.f;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        volSlider.setOnSeekBarChangeListener(volListener);

    }

    public void onDestroy(){
        super.onDestroy();
        isRunning = false;
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t = null;
    }
}
