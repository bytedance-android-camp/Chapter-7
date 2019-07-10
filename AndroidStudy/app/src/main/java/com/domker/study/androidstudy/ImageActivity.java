package com.domker.study.androidstudy;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ImageActivity extends AppCompatActivity implements View.OnTouchListener {
    ViewFlipper flipper = null;
    LayoutInflater layoutInflater = null;
    GestureDetector detector = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        flipper = (ViewFlipper) findViewById(R.id.flipper);
        flipper.setOnTouchListener(this);
        detector = new GestureDetector(new MyGestureListener());
        layoutInflater = getLayoutInflater();
        flipper.post(new Runnable() {
            @Override
            public void run() {
                addImage(decodeBitmapFromResource(getResources(),
                        R.drawable.drawableimage,
                        flipper.getWidth(),
                        flipper.getHeight()));
                addImage(decodeBitmapFromVectorResource(R.drawable.ic_markunread));
                new ReadFileTask(flipper.getWidth(),flipper.getHeight()).execute("/sdcard/fileimage.jpg");
                new ReadAssetsTask(flipper.getWidth(),flipper.getHeight()).execute("assetsimage.jpg");
                new ReadRawTask(flipper.getWidth(),flipper.getHeight()).execute(R.raw.rawimage);
                loadNetImage(flipper.getWidth(), flipper.getHeight());
            }
        });

    }

    private class ReadRawTask extends AsyncTask<Integer, Void, Bitmap> {
        int mWidth = 0;
        int mHeight = 0;
        ReadRawTask(int width, int height) {
            mWidth = width;
            mHeight = height;
        }
        @Override
        protected Bitmap doInBackground(Integer... integers) {
            return decodeBitmapFromRaw(ImageActivity.this.getResources(), integers[0], mWidth, mHeight);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            addImage(bitmap);
        }
    }

    private class ReadFileTask extends AsyncTask<String, Void, Bitmap> {
        int mWidth = 0;
        int mHeight = 0;
        ReadFileTask(int width, int height) {
            mWidth = width;
            mHeight = height;
        }
        @Override
        protected Bitmap doInBackground(String... strings) {
            return decodeBitmapFromFile(strings[0],
                    mWidth,
                    mHeight);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            addImage(bitmap);
        }
    }

    private class ReadAssetsTask extends AsyncTask<String, Void, Bitmap> {
        int mWidth = 0;
        int mHeight = 0;
        ReadAssetsTask(int width, int height) {
            mWidth = width;
            mHeight = height;
        }
        @Override
        protected Bitmap doInBackground(String... strings) {
            return decodeBitmapFromAssets(ImageActivity.this,
                    strings[0],
                    mWidth,
                    mHeight);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            addImage(bitmap);
        }
    }

    private void loadNetImage(final int width, final int height) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = decodeBitmapFromNet("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1562328963756&di=9c0c6c839381c8314a3ce8e7db61deb2&imgtype=0&src=http%3A%2F%2Fpic13.nipic.com%2F20110316%2F5961966_124313527122_2.jpg",
                        width,
                        height);
                ImageActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addImage(bitmap);
                    }
                });
            }
        }).start();
    }

    private void addImage(Bitmap bitmap) {
        ImageView imageView = (ImageView) layoutInflater.inflate(R.layout.activity_image_item, null);
        imageView.setImageBitmap(bitmap);
        flipper.addView(imageView);
    }

    private Bitmap decodeBitmapFromNet(String url, int reqWidth, int reqHeight) {
        InputStream is = null;
        byte[] data = null;
        try {
            URL imgUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) imgUrl
                    .openConnection();
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();
            data = inputStreamToByteArray(is);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (data != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            return bitmap;
        } else {
            return null;
        }
    }

    public static byte[] inputStreamToByteArray(InputStream in) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return outputStream.toByteArray();
    }

    private Bitmap decodeBitmapFromVectorResource(int resId) {
        Drawable drawable = getDrawable(resId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private Bitmap decodeBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    private Bitmap decodeBitmapFromFile(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        int degree = readRotationDegree(path);
        if (degree != 0) {
            bitmap = rotateBitmap(bitmap, (float) degree); //三星机型适配
        }
        return bitmap;
    }

    private Bitmap decodeBitmapFromRaw(Resources res, int resId, int reqWidth, int reqHeight) {
        InputStream is = res .openRawResource(resId);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        try {
            is.reset();
        } catch (IOException e) {
            return null;
        }
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private Bitmap decodeBitmapFromAssets(Context context, String fileName, int reqWidth, int reqHeight) {
        AssetManager asset = context.getAssets();
        InputStream is;
        try {
            is = asset.open(fileName);
        } catch (IOException e) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        try {
            is.reset();
        } catch (IOException e) {
            return null;
        }
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private int readRotationDegree(String path) {
        int rotation = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    rotation = 270;
                    break;
                default:
                    rotation = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotation;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, Float degree) {
        Matrix matrix = new Matrix();
        matrix.setRotate(degree, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
        try {
            Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (bitmap != null) {
                bitmap.recycle();
            }
            return bmp;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        final int FLING_MIN_DISTANCE = 100, FLING_MIN_VELOCITY = 200;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            // Fling left
            if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                flipper.showNext();
            } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                // Fling right
                flipper.showPrevious();
            }
            return true;
        }
    }
}
