package com.example.pc.coolweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pc.coolweather.R;
import com.example.pc.coolweather.model.City;
import com.example.pc.coolweather.model.CoolWeatherDB;
import com.example.pc.coolweather.model.County;
import com.example.pc.coolweather.model.Province;
import com.example.pc.coolweather.util.HttpCallbackListener;
import com.example.pc.coolweather.util.HttpUtil;
import com.example.pc.coolweather.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by PC on 2015/8/16.
 */
public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;

    private List<String> dataList = new ArrayList<String>();

    /*
    * list of Province
    * */
    private List<Province> provinceList;

    /*
    * list of City
    * */
    private List<City> cityList;

    /*
    * list of County
    * */
    private List<County> countyList;

    /*
    * selected Province
    * */
    private Province selectedProvince;

    /*
    * selected City
    * */
    private City selectedCity;

    /*
    * current selected Level
    * */
    private int currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.v("ChooseAreaActivity","its begin");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
/*        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.clear();
        Log.v("ChooseAreaActivity","here we wish SP is cleared");*/
        if (pref.getBoolean("city_selected",false)){
            Log.v("ChooseAreaActivity","city is selected at beginnging");
            Intent intent = new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.choose_area);
        listView = (ListView)findViewById(R.id.list_view);
        titleText  = (TextView) findViewById(R.id.title_text);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstence(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY){
                    String countyCode = countyList.get(position).getCountyCode();
                    Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();
    }

    /*
    * query province data from DB if it is exists, otherwise, query it on the server
    * */
    private void queryProvinces(){
        provinceList = coolWeatherDB.loadProvince();
        if (provinceList.size()>0){
            dataList.clear();
            for (Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("China");
            currentLevel = LEVEL_PROVINCE;
        }else {
            queryFromServer(null,"province");
        }
    }
    /*
    * query city data from DB if it is exists, otherwise, query it on the server
    * */
    private void queryCities(){
        cityList = coolWeatherDB.loadCity(selectedProvince.getId());
        if (cityList.size()>0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        }else {
            queryFromServer(selectedProvince.getProvinceCode(), "city");
        }
    }

    /*
    * query county data from DB if it is exists, otherwise, query it on the server
    * */
    private void queryCounties(){
        countyList = coolWeatherDB.loadCounty(selectedCity.getId());
        if (countyList.size()>0){
            dataList.clear();
            for (County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        }else {
            queryFromServer(selectedCity.getCityCode(),"county");
        }
    }
    /*
    * query data from server according to the incoming code and type
    * */
    private void queryFromServer(final String code, final String type){
        String address;
        if (!TextUtils.isEmpty(code)){
            address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
        }else {
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }

        showProgressDialog();

        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvincesResponse(coolWeatherDB,response);
                }else if ("city".equals(type)){
                    result = Utility.handleCitysResponse(coolWeatherDB,response,selectedProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCountiesResponse(coolWeatherDB,response,selectedCity.getId());
                }
                if(result){
                    //通过runOnUiThread()方法回到主线程处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,"加载失败！",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /*
    * show progress dialog
    * */
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /*
    * close progress dialog
    * */
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
    /*
    * 捕获back按键，根据当前级别判断是应该返回市列表、省列表、还是直接退出。
    * */
    @Override
    public void onBackPressed(){
        if (currentLevel == LEVEL_COUNTY){
            queryCities();
        }else if (currentLevel == LEVEL_CITY){
            queryProvinces();
        }else
            finish();
    }


}
