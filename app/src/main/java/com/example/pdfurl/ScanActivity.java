package com.example.pdfurl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Toast;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

/**
 * PDF掃描程序
 * Created by Kingfeng on 2018/4/16.
 */

public class ScanActivity extends Activity implements QRCodeView.Delegate {

    private QRCodeView mQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        mQR = (ZXingView) findViewById(R.id.zx_view);

        mQR.setResultHandler(this);

        mQR.startSpot();
    }

    @Override
    public void onScanQRCodeSuccess(String result) {

        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString("result", result);
        resultIntent.putExtras(bundle);
        setResult(RESULT_OK, resultIntent);
        finish();

        vibrate();

        startAlarm(this);

        mQR.stopCamera();

    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Toast.makeText(ScanActivity.this, "打開相機出錯，請檢查是否開啟權限！", Toast.LENGTH_SHORT).show();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    private static void startAlarm(Context context) {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (notification == null) return;
        Ringtone ringtone = RingtoneManager.getRingtone(context, notification);
        ringtone.play();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mQR.startCamera();
    }

    @Override
    protected void onStop() {
        mQR.stopCamera();
        super.onStop();
    }
}
