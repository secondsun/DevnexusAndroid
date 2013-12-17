package org.devnexus.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import org.devnexus.DevnexusApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.os.Environment.isExternalStorageRemovable;

/**
 * Created by summers on 12/16/13.
 */
public class CachingImageProvider {

    private static final String IMAGE_DIR = "images";
    private static CachingImageProvider instance;

    private final File cacheDir;


    private CachingImageProvider() {
        cacheDir = getDiskCacheDir(DevnexusApplication.CONTEXT, IMAGE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
    }

    public static CachingImageProvider getInstance() {
        if (instance == null) {
            return instance = new CachingImageProvider();
        }
        return instance;
    }

    public AsyncTask<Void, Void, Bitmap> loadImage(final Uri imageURI, final ImageLoaded callerBacker) {

        return new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {

                String imageFileName = imageURI.getLastPathSegment();

                File imageFile = new File(cacheDir.getPath() + File.pathSeparator + imageFileName);
                if (imageFile.exists()) {
                    try {
                        return BitmapFactory.decodeFile(imageFile.getCanonicalPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {

                    try {
                        InputStream input = null;
                        OutputStream output = null;
                        HttpURLConnection connection = null;
                        try {
                            URL url = new URL(imageURI.toString());
                            connection = (HttpURLConnection) url.openConnection();
                            connection.connect();

                            // expect HTTP 200 OK, so we don't mistakenly save error report
                            // instead of the file
                            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                                throw new RuntimeException("Server returned " + connection.getResponseCode());

                            // this will be useful to display download percentage
                            // might be -1: server did not report the length
                            int fileLength = connection.getContentLength();

                            // download the file
                            input = connection.getInputStream();
                            output = new FileOutputStream(imageFile);

                            byte data[] = new byte[4096];
                            long total = 0;
                            int count;
                            while ((count = input.read(data)) != -1) {
                                // allow canceling with back button
                                if (isCancelled())
                                    return null;
                                total += count;

                                output.write(data, 0, count);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            try {
                                if (output != null)
                                    output.close();
                                if (input != null)
                                    input.close();
                            } catch (IOException ignored) {
                            }

                            if (connection != null)
                                connection.disconnect();
                        }

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }


                    try {
                        return BitmapFactory.decodeFile(imageFile.getCanonicalPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (!isCancelled()) {
                    callerBacker.onImageLoad(bitmap);
                }
            }
        }.execute();

    }


    public interface ImageLoaded {
        void onImageLoad(Bitmap bitmap);
    }


    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
    // but if not mounted, falls back on internal storage.

    public static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

}
