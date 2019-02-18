package com.example.xyzreader.remote;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
    public static final URL BASE_URL;
    private static String TAG = Config.class.toString();

    //public static final String HTTPS_GO_UDACITY_COM_XYZ_READER_JSON = "https://go.udacity.com/xyz-reader-json";
    public static final String HTTPS_GO_UDACITY_COM_XYZ_READER_JSON = "https://nspf.github.io/XYZReader/data.json";

    static {
        URL url = null;
        try {
            url = new URL(HTTPS_GO_UDACITY_COM_XYZ_READER_JSON);
        } catch (MalformedURLException ignored) {
            // TODO: throw a real error
            Log.e(TAG, "Please check your internet connection.");
        }

        BASE_URL = url;
    }
}
