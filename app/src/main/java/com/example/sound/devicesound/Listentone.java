package com.example.sound.devicesound;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.*;

import java.util.ArrayList;
import java.util.List;

public class Listentone {

    int HANDSHAKE_START_HZ = 4096;
    int HANDSHAKE_END_HZ = 5120 + 1024;

    int START_HZ = 1024;
    int STEP_HZ = 256;
    int BITS = 4;

    int FEC_BYTES = 4;

    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mSampleRate = 44100;
    private int mChannelCount = AudioFormat.CHANNEL_IN_MONO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private float interval = 0.1f;

    private int mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);

    public AudioRecord mAudioRecord = null;
    int audioEncodig;
    boolean startFlag;
    FastFourierTransformer transform;

    public Listentone(){
        transform = new FastFourierTransformer(DftNormalization.STANDARD);
        startFlag = false;
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
        mAudioRecord.startRecording();
    }
    public void PreRequest() {
        List<Integer> list = new ArrayList<>();
        int blocksize = findPowerSize((int) (long) Math.round(interval / 2 * mSampleRate));
        //Recorder로부터 음성을 입력받는다.
        short[] buffer = new short[blocksize];
        int flag = 0;
        while(true)
        {
            int bufferedReadResult = mAudioRecord.read(buffer, 0, blocksize);
            //받아온 소리에서 주파수를 찾아낸다.
            double[] newArray = new double[buffer.length];
            for (int i = 0; i < buffer.length; i++) {
                newArray[i] = (double) (buffer[i]);
            }//데이터 수집 시작
            double x = findFrequency(newArray);
            if(match(x,HANDSHAKE_START_HZ)) flag = 1;
            if(flag == 1) list.add((int)(long)(Math.round((x-START_HZ)/STEP_HZ)));
            if(match(x,HANDSHAKE_END_HZ)) {
                flag = 0;
                output(list);
                list.removeAll(list);
              //데이터 수집 종료
            }
        }

    }
    public void output(List<Integer> list)
    {
        //주파수 수집완료 출력과정
        Log.d("Listentone",list.toString());
        ArrayList<Character> Result = new ArrayList<>();
        int count = 2; //시작주파수를 버리기 위한 정수
        while (count+1 < list.size()){
            Integer a = list.get(count);
            int b = list.get(count+1);
//        Log.d("Listentone",Integer.toString(a));
//        Log.d("Listentone",Integer.toString(b));
            int x = (a * 16) + b;
//        Log.d("Listentone",Integer.toString(x));
            Result.add((char)x);
            count = count + 2;
        }
        String s="";//문자열 조합을 위한 과정
        for (int i = 0;i < Result.size();i++){
            s += Character.toString(Result.get(i));
        }
        Log.d("Listentone",s);
    }
    public boolean match(double freq1, double freq2) { //match
        return Math.abs(freq1 - freq2) < 50;
    }
    private int findPowerSize(int round) {
        int x = 1;
        while(true) {
            x = x*2;
            if(x >= round){
                return x;
            }
        }
    }
    private double findFrequency(double[] toTransform){
        int len = toTransform.length;
        double[] real = new double[len];
        double[] img = new double[len];
        double realNum;
        double imgNum;
        double[] mag = new double[len];
        double peak_coeff = 0;
        int indexNumber =0;
        Double peak_freq =0.0;
        Complex[] complex = transform.transform(toTransform,TransformType.FORWARD);
        Double[] freq = this.fftFreq(complex.length,1);
        for(int i =0;i<complex.length;i++)
        {
            realNum = complex[i].getReal();
            imgNum = complex[i].getImaginary();
            mag[i] = Math.sqrt((realNum * realNum) + (imgNum * imgNum));
        }
        for(int i =0; i<complex.length; i++){
            if(peak_coeff < mag[i]) {
                peak_coeff = mag[i];
                indexNumber = i;
                System.out.println(indexNumber);
            }
        }
        peak_freq = freq[indexNumber];
        return peak_freq * mSampleRate;
    }
    private Double[] fftFreq(int length, int x){
        if(length % 2 != 0)length = length-1;
        Double[] freq = new Double[length];
        int[] val = new int[length];
        double a = 1.0 / length * x;
        for(int i =0; i<length;i++)
        {
            val[i] = i;
        }
        for (int i =0;i<length;i++){
            freq[i] = val[i] * a;
            if(freq[i] == 0.5){
                a = -a;
            }
        }
        return freq;
    }
}
