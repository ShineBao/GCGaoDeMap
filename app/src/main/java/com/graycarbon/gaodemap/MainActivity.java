package com.graycarbon.gaodemap;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.graycarbon.lib.permission.annotation.PermissionRequest;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private MapView mMapView;
    private AMap mAMap;
    private Bundle mSaveInstanceState;

    private ImageView mLocateIV;
    private ImageView mShadowIV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView(savedInstanceState);
    }

    private void initView(Bundle savedInstanceState) {
        mMapView = findViewById(R.id.mapView);
        mLocateIV = findViewById(R.id.locate);
        mShadowIV = findViewById(R.id.shadow);
        Button mTrafficEnabledBT = findViewById(R.id.bt_traffic_enable);
        mTrafficEnabledBT.setVisibility(View.GONE);
        mTrafficEnabledBT.setOnClickListener(this);
        mSaveInstanceState = savedInstanceState;
        // 执行所需要的权限申请
        requestPermission();
    }

    @PermissionRequest({Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION})
    private void requestPermission() {
        initMap();
    }

    /**
     * 地图初始化
     */
    private void initMap() {
        mMapView.onCreate(mSaveInstanceState);
        mAMap = mMapView.getMap();
        // 设置地图显示方式
        mAMap.setMapType(AMap.MAP_TYPE_NORMAL);
        // 设置地图默认缩放比例 （3~19）
        mAMap.moveCamera(CameraUpdateFactory.zoomTo(19));
        //获取UiSettings实例
        UiSettings uiSettings = mAMap.getUiSettings();
        //设置缩放控件
        uiSettings.setZoomControlsEnabled(false);
        //设置定位按钮
        uiSettings.setMyLocationButtonEnabled(false);
        // 定位
        location();
        // 标记位置
        marker();
    }

    /**
     * 定位
     */
    private void location() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE); // 设置定位模式
        // myLocationStyle.interval(2000); // 连续定位时间间隔
        // 以下两行代码，设置不显示定位精度半径
        myLocationStyle.strokeColor(0x00000000);
        myLocationStyle.radiusFillColor(0x00000000);
        // 设置定位图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                .decodeResource(getResources(), R.mipmap.round)));
        mAMap.setMyLocationStyle(myLocationStyle);
        mAMap.setMyLocationEnabled(true);
    }

    /**
     * 标记地图位置
     */
    private void marker() {

        // 地图视角改变监听
        mAMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                startAnimation();
            }

            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {
                isLoad = true;
                endAnimation();
                poiSearch(cameraPosition.target.latitude, cameraPosition.target.longitude);
            }
        });
    }

    private boolean isLoad = true;

    private void startAnimation() {
        if (isLoad) {
            ObjectAnimator translationAnimation = ObjectAnimator.ofFloat(mLocateIV,
                    "translationY", 0, -50);
            translationAnimation.setDuration(500);
            translationAnimation.start();

            AnimatorSet animatorSet = new AnimatorSet();//组合动画
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(mShadowIV, "scaleX", 1, 1.5f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(mShadowIV, "scaleY", 1, 1.5f);

            animatorSet.setDuration(500);
            animatorSet.play(scaleX).with(scaleY);//两个动画同时开始
            animatorSet.start();
            isLoad = false;
        }
    }

    private void endAnimation() {
        ObjectAnimator translationAnimation = ObjectAnimator.ofFloat(mLocateIV,
                "translationY", -50, 0);
        translationAnimation.setDuration(500);
        translationAnimation.start();
        AnimatorSet animatorSet = new AnimatorSet();//组合动画
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mShadowIV, "scaleX", 1.5f, 1);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mShadowIV, "scaleY", 1.5f, 1);

        animatorSet.setDuration(500);
        animatorSet.play(scaleX).with(scaleY);//两个动画同时开始
        animatorSet.start();
    }

    /**
     * 检索附近点
     *
     * @param latitude  经度
     * @param longitude 维度
     */
    private void poiSearch(double latitude, double longitude) {
        PoiSearch.Query query = new PoiSearch.Query("", "", "");
        PoiSearch mPoiSearch = new PoiSearch(this, query);
        mPoiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(latitude, longitude), 1000));
        mPoiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
            @Override
            public void onPoiSearched(PoiResult poiResult, int i) {
                for (PoiItem poiItem : poiResult.getPois()) {
                    Log.i(TAG, poiItem.getTitle() + " - " + poiItem.getSnippet());
                }
            }

            @Override
            public void onPoiItemSearched(PoiItem poiItem, int i) {

            }
        });
        mPoiSearch.searchPOIAsyn();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_traffic_enable: // 设置是否显示实时交通
                if (mAMap != null) {
                    mAMap.setTrafficEnabled(!mAMap.isTrafficEnabled());
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }
}
