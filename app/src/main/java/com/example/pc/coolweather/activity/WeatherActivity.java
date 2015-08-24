package com.example.pc.coolweather.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pc.coolweather.R;
import com.example.pc.coolweather.util.HttpCallbackListener;
import com.example.pc.coolweather.util.HttpUtil;
import com.example.pc.coolweather.util.Utility;

/**
 * Created by PC on 2015/8/21.
 */
public class WeatherActivity extends Activity {
    private LinearLayout weatherInfoLayout;
    /*
    * show city name
    * */
    private TextView cityNameText;
    /*
    * shwo publish time
    * */
    private TextView publishText;
    /*
    * show weather desp
    * */
    private TextView weatherDespText;
    /*
    * show t 1
    * */
    private TextView temp1Text;
    /*
    * show t 2
    * */
    private TextView temp2Twxt;
    /*
    * show current time
    * */
    private TextView currentDateText;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weather_layout);
        //初始化各控件
        Log.v("WeatherActivity","Here we init the WeatherActivity");
        weatherInfoLayout = (LinearLayout)findViewById(R.id.weather_info_layout);
        cityNameText = (TextView)findViewById(R.id.city_name);
        publishText = (TextView)findViewById(R.id.publish_test);
        weatherDespText = (TextView)findViewById(R.id.weather_desp);
        temp1Text = (TextView)findViewById(R.id.temp1);
        temp2Twxt = (TextView)findViewById(R.id.temp2);
        currentDateText = (TextView)findViewById(R.id.current_date);
        String countyCode = getIntent().getStringExtra("county_code");
 //       Log.v("打印countyCode：",countyCode);
        if(!TextUtils.isEmpty(countyCode)){
            //有县级代号的时候就去查询天气
            Log.v("Weather","here is not except");
            publishText.setText("同步中。。。");
            weatherInfoLayout.setVisibility(View.INVISIBLE);
            cityNameText.setVisibility(View.INVISIBLE);
            queryWeatherCode(countyCode);
        }else {
            //没有县级代号时直接显示本地天气
            showWeather();
        }
    }

    /*
    * 查询县级代号所对应的天气
    * */
    private void queryWeatherCode(String countyCode){
        String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
        queryFromServer(address,"countyCode");
    }

    /*
    * 查询天气代号所对应的天气
    * */
    private void queryWeatherinfo(String weatherCode){
        String address = "http://www.weather.com.cn/data/cityinfo/"+weatherCode+".html";
        queryFromServer(address,"weatherCode");
    }
    /*
    * 根据地址和类型查询天气代号或者天气信息
    * */
    private void queryFromServer(final String address,final String type){
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                if ("countyCode".equals(type)) {
                    if (!TextUtils.isEmpty(response)) {
                        //解析天气代号
                        String[] array = response.split("\\|");
                        if (array != null && array.length == 2) {
                            String weatherCode = array[1];
                            queryWeatherinfo(weatherCode);
                        }
                    }
                } else if ("weatherCode".equals(type)) {
                    //处理服务器返回的天气信息
                    Utility.handleWeatherResponse(WeatherActivity.this, response);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        publishText.setText("同步失败！");
                    }
                });
            }
        });
    }
    /*
    * 从SharePreference文件中读取存储的天气信息，并显示到界面上
    * */
    private void showWeather() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        cityNameText.setText(preferences.getString("city_name", "cf cityName"));
        temp1Text.setText(preferences.getString("temp1", "cf temp1Text"));
        temp2Twxt.setText(preferences.getString("temp2", "cf temp2Text"));
        weatherDespText.setText(preferences.getString("weather_desp", "cf weatherDesp"));
        publishText.setText("今天" + preferences.getString("publish_time", "cf publishTime") + "发布");
        currentDateText.setText(preferences.getString("current_time", "cf currentTime"));
        Log.v("WeatherActivity", "showWeather is load ");
        weatherInfoLayout.setVisibility(View.VISIBLE);
    }
}
