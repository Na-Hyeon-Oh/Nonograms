package edu.skku.map.pa2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    int finishcnt=0;
    int blackcnt=0;

    int maxRow=0;
    int maxCol=0;

    TextView search_for;
    Button search_btn, gallery_btn;
    GridView grid, rowgrid, colgrid;
    GridViewAdapter gameGridAdapter;
    //GridViewAdapter gridAdapter;//^
    intGridViewAdapter rowAdapter, colAdapter;

    ImageView image;
    Bitmap originalImg=null;
    Bitmap whiteBlock, blackBlock;
    ArrayList<Bitmap> gameBlocks, blocks; //blocks is for right answer, gameBlocks is actual playground
    int[][] rowNum, colNum;
    ArrayList<String> row2grid, col2grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //image=findViewById(R.id.eximage);//^

        search_for= findViewById(R.id.textView);
        search_btn = (Button) findViewById(R.id.search);
        gallery_btn = (Button) findViewById(R.id.gallery);

        Bitmap white=BitmapFactory.decodeResource(this.getResources(), R.drawable.white);
        Bitmap black=BitmapFactory.decodeResource(this.getResources(), R.drawable.black);
        whiteBlock = Bitmap.createScaledBitmap(white, 13, 13, true );
        blackBlock = Bitmap.createScaledBitmap(black, 13, 13, true );

        grid=(GridView) findViewById(R.id.gridView);
        gameBlocks=new ArrayList<Bitmap>(400);
        for(int i=0;i<400;i++){
            gameBlocks.add(whiteBlock);
        }
        gameGridAdapter=new GridViewAdapter(gameBlocks, getApplicationContext(), R.layout.activity_main);
        grid.setAdapter(gameGridAdapter);

        rowgrid=(GridView) findViewById(R.id.gridrow);
        colgrid=(GridView) findViewById(R.id.gridcol);

        rowNum=new int[20][10];
        colNum=new int[20][10];
        row2grid=new ArrayList<String>();
        col2grid=new ArrayList<String>();


        //get image from Search NAVER API
        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String image_search=search_for.getText().toString();
                String index="1";
                String clientId = "wnbOVpLLVOPqhCKzqLrJ";
                String clientSecret = "vJq9NhHQIq";

                OkHttpClient client = new OkHttpClient();

                HttpUrl.Builder urlBuilder = HttpUrl.parse("https://openapi.naver.com/v1/search/image").newBuilder();
                urlBuilder.addQueryParameter("query", image_search);
                urlBuilder.addQueryParameter("display", index);

                String url = urlBuilder.build().toString();
                Request req = new Request.Builder().header("X-Naver-Client-Id", clientId).addHeader("X-Naver-Client-Secret", clientSecret).url(url).build();

                //not in main activity
                client.newCall(req).enqueue(new Callback() {
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        final String myResponse = response.body().string();

                        Gson gson = new GsonBuilder().create();
                        DataModel dataModel = gson.fromJson(myResponse, DataModel.class);
                        List<DataModel.Data> data=dataModel.getItems();

                        String url=data.get(0).getLink();
                        System.out.println(url);//^

                        if(url!=null){
                            //load image from url
                            ImageLoadTask task = new ImageLoadTask(url);
                            task.execute();
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "No URL Info", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        //get image from Gallery
        gallery_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open gallery and select image
                pickFromGallery();
            }
        });


        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (originalImg != null) {
                    //Toast.makeText(getApplicationContext(), position + "번째 선택", Toast.LENGTH_SHORT).show();

                    //If right(black), change block to black
                    if (checkRight(position) == 1) {
                        finishcnt++;
                        gameBlocks.set(position, blackBlock);
                    }

                    //Else(white), refresh
                    else {
                        Toast.makeText(getApplicationContext(), "WRONG!", Toast.LENGTH_SHORT).show();

                        for (int i = 0; i < 400; i++) {
                            gameBlocks.set(i, whiteBlock);
                        }

                        finishcnt = 0;
                    }

                    gameGridAdapter = new GridViewAdapter(gameBlocks, getApplicationContext(), R.layout.activity_main);
                    grid.setAdapter(gameGridAdapter);

                    //game ends
                    if (finishcnt == blackcnt)
                        Toast.makeText(getApplicationContext(), "FINISH!", Toast.LENGTH_LONG).show();
                }

                else{
                    Toast.makeText(getApplicationContext(), "NO Selected Image", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private int checkRight(int index){
        int ret=0;//0: wrong  1: right

        if(blocks.get(index)==blackBlock) ret=1;

        return ret;
    }

    private void pickFromGallery(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 0);
    }

    private void imageSetting(){
        finishcnt=0;
        blackcnt=0;
        for (int i = 0; i < 400; i++) {
            gameBlocks.set(i, whiteBlock);
        }
        gameGridAdapter = new GridViewAdapter(gameBlocks, getApplicationContext(), R.layout.activity_main);
        grid.setAdapter(gameGridAdapter);

        //Image resizing to square(1:1)
        int dstLen=260;
        Bitmap scaledImg = resizeBitmap(dstLen, originalImg);

        //convert to black and white using grayscale values
        scaledImg=toGrayscale(scaledImg);

        //split to 20*20 images
        blocks=new ArrayList<Bitmap>(400);

        int splittedLen=dstLen/20;
        int xCoor, yCoor=0;
        for(int i=0; i <20; i++){
            xCoor=0;
            for(int j=0; j<20; j++){
                blocks.add(scaledImg.createBitmap(scaledImg, xCoor, yCoor, splittedLen, splittedLen));
                xCoor+=splittedLen;
            }
            yCoor+=splittedLen;
        }

        //convert to white and black blocks
        long red = 0;
        long green = 0;
        long blue = 0;
        long pixelCount = 0;
        int color;
        long avgGrayscale;
        Bitmap tmp;
        for(int i=0; i < 400; i++){
            tmp=blocks.get(i);
            pixelCount=0;
            red=0;
            green=0;
            blue=0;

            for (int y=0; y < splittedLen; y++) {
                for (int x=0; x < splittedLen; x++) {
                    color = tmp.getPixel(x, y);
                    pixelCount++;
                    red += Color.red(color);
                    green += Color.green(color);
                    blue += Color.blue(color);
                }
            }

            //get average grayscale value
            avgGrayscale = (red+green+blue)/pixelCount;
            if(avgGrayscale > 128){
                //blocks to white block
                blocks.set(i, whiteBlock);
            }
            else{
                //blocks to black block
                blocks.set(i, blackBlock);

                blackcnt++;
            }
        }

        //image.setImageBitmap(scaledImg);
        //gridAdapter=new GridViewAdapter(blocks, getApplicationContext(), R.layout.activity_main);
        //grid.setAdapter(gridAdapter);//^

        //If white gameBlock
        if(finishcnt==blackcnt) Toast.makeText(getApplicationContext(), "FINISH!", Toast.LENGTH_LONG).show();
        setRowCol();
    }

    private void setRowCol(){
        maxCol=0; maxRow=0;
        rowNum=null; colNum=null;
        rowNum=new int[20][10]; colNum=new int[20][10];
        row2grid.clear(); col2grid.clear();

        int cnt=0;
        int rowcnt=0;
        int colcnt=0;

        //row
        for(int i=0;i<20;i++){
            cnt=0;
            rowcnt=0;

            for(int j=0;j<20;j++){
                if(blocks.get(20*i+j)==blackBlock){
                    cnt++;
                }
                else {
                    if (cnt != 0) {
                        rowNum[i][rowcnt]=cnt;
                        rowcnt++;
                        cnt = 0;
                        if (rowcnt > maxRow) maxRow = rowcnt;
                    }
                }
            }
            if(cnt!=0){
                rowNum[i][rowcnt]=cnt;
                rowcnt++;
                cnt=0;
                if(rowcnt>maxRow) maxRow=rowcnt;
            }
        }

        //col
        for(int i=0;i<20;i++){
            cnt=0;
            colcnt=0;

            for(int j=0;j<20;j++){
                if(blocks.get(i+20*j)==blackBlock){
                    cnt++;
                }
                else {
                    if(cnt!=0){
                        colNum[i][colcnt]=cnt;
                        colcnt++;
                        cnt=0;
                        if(colcnt>maxCol) maxCol=colcnt;
                    }
                }
            }
            if(cnt!=0){
                colNum[i][colcnt]=cnt;
                colcnt++;
                cnt=0;
                if(colcnt>maxCol) maxCol=colcnt;
            }
        }

        /* set */
        //set(merge) to arraylist
        int num;
        //row
        for(int i=0;i<20; i++){
            for(int j=0;j<maxRow;j++){
                if((num=rowNum[i][j])!=0) {
                    row2grid.add(String.valueOf(num));
                }
                else {
                    row2grid.add("0");
                }
            }
        }
        //col
        for(int j=0;j<maxCol;j++){
            for(int i=0;i<20;i++) {
                if ((num=colNum[i][j]) != 0) {
                    col2grid.add(String.valueOf(num));
                }
                else col2grid.add("0");
            }
        }

        if(maxRow==0){
            for(int i=0;i<20;i++){
                row2grid.add("0");
            }

            if(maxCol==0){
                for(int i=0;i<20;i++) col2grid.add("0");
            }
        }
        else if(maxCol==0){
            for(int i=0;i<20;i++) col2grid.add("0");
        }


        //set grid
        rowgrid.setNumColumns(maxRow);
        colgrid.setNumColumns(20);

        rowAdapter=new intGridViewAdapter(row2grid, getApplicationContext(), R.layout.activity_main);
        colAdapter=new intGridViewAdapter(col2grid, getApplicationContext(), R.layout.activity_main);
        rowgrid.setAdapter(rowAdapter);
        colgrid.setAdapter(colAdapter);
    }

    public Bitmap resizeBitmap(int targetWidth, Bitmap source){
        //double ratio = (double)targetWidth / (double)source.getWidth();
        int targetHeight = targetWidth;

        Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);

        if(result != source){
            source.recycle();
        }
        return result;
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width = bmpOriginal.getWidth();
        int height = bmpOriginal.getHeight();
        Bitmap ret = Bitmap.createBitmap(width, height, bmpOriginal.getConfig());

        //color information
        int A, R, G, B;
        int pixel;
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get pixel color
                pixel = bmpOriginal.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);

                // use 128 as grayscale value
                if (gray > 128) {
                    gray = 255;
                }
                else{
                    gray = 0;
                }

                // set new pixel color to output bitmap
                ret.setPixel(x, y, Color.argb(A, gray, gray, gray));
            }
        }
        return ret;

        /* just gray image
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);

        return bmpGrayscale;
        */
    }

    //Gallery Intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                try {
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                    in.close();

                    //set grid
                    if(bitmap!=null) {
                        originalImg = bitmap;
                        imageSetting();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Image Load Error. This image is invalid.\nPlease select another Image from Gallery", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Cancel Selecting Image from Gallery", Toast.LENGTH_LONG).show();
            }
        }
    }

    //GridAdapter for image grid(game)
    class GridViewAdapter extends BaseAdapter {
        ArrayList<Bitmap> items;
        Context context;
        LayoutInflater inflater;

        public GridViewAdapter(ArrayList<Bitmap> objects, Context ctx, int textViewResourceId){
            this.items=objects;
            this.context=ctx;
            inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView=null;

            if(convertView==null){
                convertView=inflater.inflate(R.layout.gridview, parent, false);
            }

            imageView=(ImageView)convertView.findViewById(R.id.item);
            imageView.setImageBitmap(items.get(position));

            return imageView;
        }
    }

    //GridAdapter for RowS and Cols
    class intGridViewAdapter extends BaseAdapter {
        ArrayList<String> items;
        Context context;
        LayoutInflater inflater;

        public intGridViewAdapter(ArrayList<String> objects, Context ctx, int textViewResourceId){
            this.items=objects;
            this.context=ctx;
            inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView=null;

            if(convertView==null){
                convertView=inflater.inflate(R.layout.rowcol, parent, false);
            }

            textView=(TextView)convertView.findViewById(R.id.num);
            textView.setText(items.get(position));

            return textView;
        }
    }

    //SyncTask for ImageLoader from url(link)
    public class ImageLoadTask extends AsyncTask<Void,Void, Bitmap> {
        private String urlStr;
        //private ImageView imageView;
        private HashMap<String, Bitmap> bitmapHash = new HashMap<String, Bitmap>();

        public ImageLoadTask(String urlStr) {
            this.urlStr = urlStr;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bitmap = null;
            try {
                if (bitmapHash.containsKey(urlStr)) {
                    Bitmap oldbitmap = bitmapHash.remove(urlStr);
                    if(oldbitmap != null) {
                        oldbitmap.recycle();
                        oldbitmap = null;
                    }
                }

                URL url = new URL(urlStr);
                bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                bitmapHash.put(urlStr,bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            //set grid
            if(bitmap!=null) {
                //image.setImageBitmap(bitmap);
                originalImg = bitmap;
                imageSetting();
            }
            else {
                Toast.makeText(getApplicationContext(), "Image Load Error. This url is invalid.\nPlease search for another Image", Toast.LENGTH_LONG).show();
            }
        }
    }

    /*
    private void getBitmapFromURL(String src) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        BufferedInputStream bis = null;

        try
        {
            URL url = new URL(src);
            connection = (HttpURLConnection)url.openConnection();
            connection.connect();

            int nSize = connection.getContentLength();
            bis = new BufferedInputStream(connection.getInputStream(), nSize);
            bitmap = BitmapFactory.decodeStream(bis);
        } catch (Exception e){
            e.printStackTrace();
        }
        finally{
            if(bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(connection != null ) {
                connection.disconnect();
            }
        }

        if(bitmap!=null) {
            originalImg = bitmap;
            imageSetting();
        }
    }
     */
}