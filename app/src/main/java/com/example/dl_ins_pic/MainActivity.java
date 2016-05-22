package com.example.dl_ins_pic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by FJS0420 on 2016/5/19.
 */
public class MainActivity extends Activity {
    private EditText edit_url = null;
    private Button btn_start = null;
    private String add = null;

    private static final int LOAD_SUCCESS = 1;
    private static final int LOAD_ERROR = -1;
    SimpleDraweeView draweeView = null;
    private DateFormat dateFormat = null;

    private final static String ALBUM_PATH
            = Environment.getExternalStorageDirectory() + "/instrgam_pic/";

    private ProgressDialog mSaveDialog = null;
    private Bitmap mBitmap;
    private String mFileName;
    private String mSaveMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        edit_url = (EditText) findViewById(R.id.edit_url);
        btn_start = (Button) findViewById(R.id.btn_start);
        draweeView = (SimpleDraweeView) findViewById(R.id.my_image_view);

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        String str = edit_url.getText().toString();
                        getHttp(str);
                    }
                }).start();
               /* String html = getHTML(str);
                if(html != null)
                Toast.makeText(getApplicationContext(),html.substring(1,20),Toast.LENGTH_LONG).show();*/
            }
        });
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case LOAD_SUCCESS:
                    add = msg.obj.toString();
                    Uri uri = Uri.parse(add);
                   /* ImageRequest imageRequest= ImageRequest.fromUri(uri);
                    CacheKey cacheKey= DefaultCacheKeyFactory.getInstance()
                            .getEncodedCacheKey(imageRequest);
                    BinaryResource resource = ImagePipelineFactory.getInstance()
                            .getMainDiskStorageCache().getResource(cacheKey);
                    File file=((FileBinaryResource)resource).getFile();*/
                    Toast.makeText(getApplicationContext(),add,Toast.LENGTH_LONG).show();
                    draweeView.setImageURI(uri);

                    new Thread(connectNet).start();

                    break;
                case LOAD_ERROR:
                    Toast.makeText(getApplicationContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    break;
            }

        };
    };


    // 下载源码的主方法
    private void getHttp(String add) {
        URL url = null;
        InputStream is = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            // 构建图片的url地址
            url = new URL(add);
            // 开启连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 设置超时的时间，5000毫秒即5秒
            conn.setConnectTimeout(5000);
            // 设置获取图片的方式为GET
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                is = conn.getInputStream();
                byteArrayOutputStream = new ByteArrayOutputStream();
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = is.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, len);
                }
                byteArrayOutputStream.flush();
                byte[] arr = byteArrayOutputStream.toByteArray();
                Message msg = handler.obtainMessage();
                msg.what = LOAD_SUCCESS;
                String source = new String(arr);
                String imageUrl = getImageUrl(source);
                msg.obj = imageUrl;
                handler.sendMessage(msg);
            }
        } catch (Exception e) {
            handler.sendEmptyMessage(LOAD_ERROR);
            e.printStackTrace();
        } finally {

            try {
                if (is != null) {
                    is.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
            } catch (Exception e) {
                handler.sendEmptyMessage(LOAD_ERROR);
                e.printStackTrace();
            }
        }
    }

    /*
      <meta property="og:image" content="https://scontent-nrt1-1.cdninstagram.com/t51.2885-15/sh0.08/e35/p750x750/13260973_1058605227535201_440037878_n.jpg?ig_cache_key=MTI1MTkxMTc3OTcwODM4Njk2Nw%3D%3D.2" />
     */
    private String getImageUrl(String sourcecode){
        String url = null;
        Pattern pattern = Pattern.compile("<meta property=\"og:image\" content=\"(.*?)\" />");
        Matcher matcher = pattern.matcher(sourcecode);
        if(matcher.find())
            url = matcher.group(1);
        return url;
    }

    private Runnable saveFileRunnable = new Runnable(){
        @Override
        public void run() {
            try {
                saveFile(mBitmap, mFileName);
                mSaveMessage = "图片保存成功！";
            } catch (IOException e) {
                mSaveMessage = "图片保存失败！";
                e.printStackTrace();
            }
            messageHandler.sendMessage(messageHandler.obtainMessage());
        }

    };
    private Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), mSaveMessage, Toast.LENGTH_SHORT).show();
        }
    };
    /**
     * 保存文件
     * @param bm
     * @param fileName
     * @throws IOException
     */
    public void saveFile(Bitmap bm, String fileName) throws IOException {
        File dirFile = new File(ALBUM_PATH);
        if(!dirFile.exists()){
            dirFile.mkdir();
        }
        File myCaptureFile = new File(ALBUM_PATH + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
        bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        bos.flush();
        bos.close();
    }


    /*
    * 连接网络
    * 由于在4.0中不允许在主线程中访问网络，所以需要在子线程中访问
    */
    private Runnable connectNet = new Runnable(){
        @Override
        public void run() {
            try {
                //获取系统时间
                dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                String systemTime = dateFormat.format(new java.util.Date());
                String filePath = add;
                mFileName = systemTime + ".jpg";
                //******** 方法2：取得的是InputStream，直接从InputStream生成bitmap ***********/
                mBitmap = BitmapFactory.decodeStream(getImageStream(filePath));
                //********************************************************************/
                saveFile(mBitmap, mFileName);
                mSaveMessage = "图片保存成功！";
            } catch (IOException e) {
                mSaveMessage = "图片保存失败！";
                e.printStackTrace();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "无法链接网络！", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            messageHandler.sendMessage(messageHandler.obtainMessage());
        }

    };
    /**
     * Get image from newwork
     * @param path The path of image
     * @return InputStream
     * @throws Exception
     */
    public InputStream getImageStream(String path) throws Exception{
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
            return conn.getInputStream();
        }
        return null;
    }

}
