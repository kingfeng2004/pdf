package com.example.pdfurl;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.joanzapata.pdfview.PDFView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * PDF主程序
 * Created by Kingfeng on 2018/4/16.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static int SCANNIN_GREQUEST_CODE = 1;
    private static final int UPDATE_RESULTS = 1;
    private static String urlFactOrder = "http://192.168.2.7/eip/EnterInfo/sgsh/uapdfapp.jsp?factOrder=";
    private static String urlCfmseeOK = "http://192.168.2.7/eip/EnterInfo/sgsh/uapdfcfmsee.jsp";
    private static String urlpath = "http://192.168.2.7/eip/UAOrder/upfile/AAA.pdf";
    private static String urlpath2 = "http://192.168.2.7/eip/UAOrder/upfile/";
    private TextView mbutton_scanpdf;
    private EditText inputSearch;
    private PDFView mPDFview;
    private String inputSearchs = "";
    private String DISPLAY_RESULTS = "";
    private String pdfNameAll="a.pdf";
    //private String pdfName;
    private ProgressDialog dialog;
    private String outfilepath;
    private File outfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mbutton_scanpdf = (TextView) findViewById(R.id.button_search);
        inputSearch = (EditText) findViewById(R.id.input_search);
        mPDFview = (PDFView) findViewById(R.id.pdfView);
        findViewById(R.id.button_qrcode).setOnClickListener(this);

        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            outfilepath=Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        //pdfNameAll = urlpath.substring(urlpath.lastIndexOf("/"));
        //pdfName = pdfNameAll.substring(pdfNameAll.indexOf("."));

        mbutton_scanpdf.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                inputSearchs = inputSearch.getText().toString().trim().toUpperCase();
                sendRequestWithOkHttp(inputSearchs);
            }
        });

        inputSearch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(inputSearchs == null || inputSearchs.length() < 1) {
                    showResponse("請輸入單號後再長按住確認。");
                }
                else {
                    showDialog();
                }
                return true;
            }
        });
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("確認");
        builder.setMessage(Html.fromHtml("您確認嗎？"))
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        goCfmseePDF(inputSearchs);
                    }
                })
         .setNegativeButton("忽略", new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
         }
         });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void goCfmseePDF(final String factOrder) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendOkHttpRequest(urlCfmseeOK
                                    +"?factOrder="+factOrder,
                            new okhttp3.Callback() {
                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    String responseData = response.body().string().trim();
                                    showResponse(factOrder + " 成型時間確認成功！");
                                    //finish();
                                }

                                @Override
                                public void onFailure(Call call, IOException e) {
                                    showResponse("成型時間確認失敗！");
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void showResponse(final String responseData) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), responseData, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void sendOkHttpRequest(final String address, final okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(address)
                .build();
        client.newCall(request).enqueue(callback);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_qrcode:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ScanActivity.class);
                startActivityForResult(intent, SCANNIN_GREQUEST_CODE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String QRCodeValues;
        switch (requestCode) {
            case SCANNIN_GREQUEST_CODE:
                if(resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    QRCodeValues = bundle.getString("result").trim().toUpperCase();
                    sendRequestWithOkHttp(QRCodeValues);
                }

                break;
        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_RESULTS:
                    try {
                        if(String.valueOf((String) msg.obj).equalsIgnoreCase("N")) {
                            inputSearch.setText("");
                            urlpath = urlpath2 + "error.pdf";
                        }
                        else {
                            inputSearch.setText(inputSearchs);
                            urlpath = urlpath2 + String.valueOf((String) msg.obj) + ".pdf";
                        }

                        dialog = ProgressDialog.show(MainActivity.this, "", "正在加載……");
                        DownloadPDF downloadpdf = new DownloadPDF();
                        downloadpdf.execute();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        showResponse("沒有對應的文檔！");
                    }

                    break;
                default:
                    break;
            }
        }
    };

    private void sendRequestWithOkHttp(final String QRCodeValues) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(urlFactOrder+QRCodeValues);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    //String responseData = response.toString().trim().toUpperCase();
                    String responseData = response.toString().trim().toLowerCase();
                    if(responseData.equalsIgnoreCase("N")) {
                        showResponse("沒有資料！");
                        DISPLAY_RESULTS = "N";
                    } else {
                        DISPLAY_RESULTS = responseData;
                    }

                    Message message = new Message();
                    message.what = UPDATE_RESULTS;
                    message.obj = DISPLAY_RESULTS;
                    handler.sendMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        Timer timer=new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                if(outfile.exists()){
                    outfile.delete();
                    //Log.e("刪除文件", ""+outfile.exists());
                }
            }
        }, 6000);
        timer.cancel();
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    class DownloadPDF extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            URL url;
            try {
                url = new URL(urlpath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("Charset", "UTF-8");
                conn.setDoInput(true);
                conn.setConnectTimeout(3000);
                conn.connect();
                if(HttpURLConnection.HTTP_OK == conn.getResponseCode()){
                    byte[] bytes = new byte[1024];
                    InputStream is = conn.getInputStream();
                    outfile = new File(outfilepath+"/",pdfNameAll);

                    if(!outfile.exists()){
                        outfile.createNewFile();
                    }
                    FileOutputStream fos = new FileOutputStream(outfile);
                    int len = -1;
                    while((len = is.read(bytes))>0){
                        fos.write(bytes,0,len);
                    }
                    fos.flush();
                    fos.close();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally{

            }
            return "下載完成";
        }
        @Override
        protected void onPostExecute(String result) {
            //dialog.show();
            //Log.e("result值", result);
            dialog.dismiss();
            mPDFview.fromFile(outfile)
                    .defaultPage(1)
                    .showMinimap(false)
                    .enableSwipe(true)
                    .load();

            super.onPostExecute(result);
        }
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

    }
}