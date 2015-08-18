package com.example.pc.coolweather.util;

/**
 * Created by PC on 2015/8/16.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
