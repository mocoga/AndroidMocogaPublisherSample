package com.mocoga.client;

import android.app.TabActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import com.mocoga.sdk.Mocoga;
import com.mocoga.sdk.Mocoga.MocogaListener;
import com.mocoga.sdk.datatype.Reward;

public class MocogaClientActivity extends TabActivity {
    private static final String TAG = MocogaClientActivity.class.getSimpleName();
	
	private int offerConSize = 60;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        // Tab 초기화
        TabHost tabHost = getTabHost();
        LayoutInflater.from(this).inflate(R.layout.main, tabHost.getTabContentView(), true);
        
        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator("Large")
                .setContent(R.id.View1));
        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator("Normal")
                .setContent(R.id.View2));
        tabHost.addTab(tabHost.newTabSpec("tab3")
                .setIndicator("Small")
                .setContent(R.id.View3));
        tabHost.addTab(tabHost.newTabSpec("tab4")
                .setIndicator("Custom")
                .setContent(R.id.View4));
        
        for (int i = 0; i < tabHost.getTabWidget().getTabCount(); i++) {
            tabHost.getTabWidget().getChildAt(i).getLayoutParams().height = (int)(40 * getResources().getDisplayMetrics().density + 0.5f);;
        }  
        
        tabHost.setOnTabChangedListener(onTabChangeListener);
        
        // Client에 저장된 virtual currency를 세팅
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int point = preferences.getInt("Point", 0);
        ((TextView)findViewById(R.id.Currency)).setText(String.valueOf(point));
        
        
        ((SeekBar)findViewById(R.id.SizeBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				showCurrentTapOfferCon();
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				offerConSize = progress + 20;
			}
		});
        
        
        /*
         * << Mocoga SDK 초기화 이전에 >>
         * 
         * 아래 Permission 및 Activity를 AnroidManifest.xml에 추가해 주세요.
         * 
         * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    	 * <uses-permission android:name="android.permission.INTERNET"/>
    	 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    	 * <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    	 * 
    	 * <activity android:name="com.mocoga.sdk.activities.OfferViewActivity"
         *         android:configChanges="keyboardHidden|orientation|locale|uiMode"
         *         android:theme="@android:style/Theme.Translucent"/>
         *
         * <activity android:name="com.mocoga.sdk.activities.VideoViewActivity"
         *         android:configChanges="keyboardHidden|orientation|locale|uiMode"/>
    	 *
    	 * 
         */
        
        /* << Mocoga SDK 초기화 >>
	   	 *
	   	 * - 최상위 Activity에서 Mocoga SDK를 초기화 합니다.
	   	 * - Mocoga.getInstance().initAppID(getApplicationContext(), "Your App ID", "Your Secret Key");
	   	 * - 주의! 현재 샘플 앱의 AppID와 SecretKey는 테스트용이므로, 실제 사용하려는 AppID와 SecretKey를 추가해주시기 바랍니다.
	   	 */
        Mocoga.getInstance().initAppID(getApplicationContext(), "b94780b0-1212-49ad-be7b-35ea4d989a63", "4A8QhZZuFi0ezA==");
        //Mocoga.getInstance().initAppID(getApplicationContext(), "ba873ce1-19bb-489a-a56e-8fb8bb1715de", "6kcuetqiPmJVag==");
        
        
        Log.i(TAG, "Device ID : " + Mocoga.getInstance().getDeviceId());
        
        /*
		 * << 가상화폐 관리 방식 >>
		 *
		 * - 앱 내 가상화폐의 관리방식(서버 관리 or 클라이언트 관리)에 따라 Mocoga에서 보상을 지급하는 방식에 차이가 있습니다.
		 * - 클라이언트에서 관리하신다면
		 *   : Mocoga는 사용자에게 보상지급이 필요할 경우, 클라이언트로 보상을 요청하게 됩니다.
		 *   : 퍼블리셔 캠페인의 가상화폐 정보에서 "클라이언트"를 선택합니다.
		 *   : 클라이언트에서 Mocoga SDK가 호출해주는 MocogaListener의  mocogaRequestsToGiveReward 메소드를 구현합니다.
		 *   : 주의! 하단 구현방식은 샘플앱을 위한 클라이언트 보상지급 구현입니다. 실제 구현시에는 해당 앱에 맞는 구현을 하시길 바랍니다.
		 */
        
        /*
         * << Mocoga Listener 등록 >>
         * 
         * - 앱 내에서 유저 보상이 지급되거나 서버쪽으로 유저 보상이 지급되어 UI를 업데이트 해야 하는 경우 SDK에서 listener를 통해 알려주게 됩니다.
         * - 앱 종료시에는 반드시 Mocoga.getInstance().setListener(null)를 호출해서 등록한 listener를 해지해 주시기 바랍니다.
         */
        Mocoga.getInstance().setListener(mocogaListener);
        /******************************************************************/
    }
    
    
    @Override
    protected void onResume() {
    	
    	// 화면에 OfferCon을 보여줍니다.
    	showCurrentTapOfferCon();
    	
    	super.onResume();
    }
    
    
    
    @Override
    protected void onPause() {
    	
    	// OfferCon을 숨깁니다.
    	Mocoga.getInstance().hideOfferCon();
    	
    	super.onPause();
    }
    
    
    @Override
    protected void onDestroy() {
    	
    	// 앱 종료시에 Mocoga에 등록한 Listener를 해지합니다.
    	Mocoga.getInstance().setListener(null);
    	
    	super.onDestroy();
    }
    
    
    /**
     * 현재 tab에 맞는 OfferCon을 보여줍니다.
     */
    private void showCurrentTapOfferCon() {
    	TabHost tabHost = getTabHost();
    	
    	String tag = tabHost.getCurrentTabTag();
    	
    	/*
		 * 예제에서는 좌표 및 OfferCon 사이즈 설정시 DIP 단위를 사용했습니다.
		 * 실제 앱 개발에서는 필요에서 따라 TypedValue.COMPLEX_UNIT_PX를 사용해서 device screen의 pixel 단위를 사용하셔도 됩니다.
		 */
    	
    	
    	if(tag.equals("tab1")) {
			Mocoga.getInstance().hideOfferCon();
			Mocoga.getInstance().showOfferCon(MocogaClientActivity.this, 70, 80, Mocoga.OFFER_ICON_LARGE, TypedValue.COMPLEX_UNIT_DIP, Mocoga.ICON_ALIGN_LEFT_BOTTOM);
		}
		else if(tag.equals("tab2")) {
			Mocoga.getInstance().hideOfferCon();
			Mocoga.getInstance().showOfferCon(MocogaClientActivity.this, 70, 80, Mocoga.OFFER_ICON_NORMAL, TypedValue.COMPLEX_UNIT_DIP, Mocoga.ICON_ALIGN_RIGHT_BOTTOM);
		}
		else if(tag.equals("tab3")) {
			Mocoga.getInstance().hideOfferCon();
			Mocoga.getInstance().showOfferCon(MocogaClientActivity.this, 160, 160, Mocoga.OFFER_ICON_SMALL, TypedValue.COMPLEX_UNIT_DIP, Mocoga.ICON_ALIGN_LEFT_BOTTOM);
		}
		else if(tag.equals("tab4")) {
			Mocoga.getInstance().hideOfferCon();
			Mocoga.getInstance().showOfferCon(MocogaClientActivity.this, 160, 160, offerConSize, TypedValue.COMPLEX_UNIT_DIP, Mocoga.ICON_ALIGN_LEFT_BOTTOM);
		}
    	
    }
    
    
    private OnTabChangeListener onTabChangeListener = new OnTabChangeListener() {
		
    	// Tab 전환시 OfferCon의 위치 및 크기를 변경합니다.
		@Override
		public void onTabChanged(String tabId) {
			showCurrentTapOfferCon();
		}
	};
    
    
    
    /*
     * Mocoga Listener
     */
    private MocogaListener mocogaListener = new MocogaListener() {
		
		@Override
		public void mocogaUpdateCurrency() {
			
		}
		
		
		/*
		 * << 클라이언트 보상 지급 구현 >>
		 *
		 * - 앱 설치/비디오 시청이 완료된 경우, Mocoga는 보상 지급을 위해 MocogaListener의 mocogaRequestsToGiveReward()를 호출합니다.
		 * - 퍼블리셔는 method 안에서 사용자에게 보상을 지급하고 didGiveReward 메소드를 호출하여 Mocoga에게 보상을 지급했음을 알려야 합니다.
		 * - 또한 사용자가 업데이트된 가상화폐를 볼수 있도록 UI를 업데이트 하는 것을 권장합니다.
		 * - 퍼블리셔 캠페인의 가상화폐 정보에서 "클라이언트" 를 선택합니다.
		 */
		@Override
		public void mocogaRequestsToGiveReward(String rewardTransId, Reward rewardInfo) {
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	        int point = preferences.getInt("Point", 0);
	        
	        // 기존 point에 새로 지급되는 point를 더합니다.
	        point += rewardInfo.getRewardAmount();
	        
	        Editor e = preferences.edit();
	        e.putInt("Point", point);
	        e.commit();
	        
	        ((TextView)findViewById(R.id.Currency)).setText(String.valueOf(point));
	        
	        // 지급되었음을 Mocoga에 알립니다.
	        Mocoga.getInstance().didGiveReward(rewardTransId);
		}
	};
}