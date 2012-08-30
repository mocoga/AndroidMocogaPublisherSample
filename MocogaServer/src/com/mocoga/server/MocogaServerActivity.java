package com.mocoga.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.TabActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import com.mocoga.sdk.Mocoga;
import com.mocoga.sdk.Mocoga.MocogaListener;
import com.mocoga.sdk.datatype.Reward;

public class MocogaServerActivity extends TabActivity {
	private static final String TAG = MocogaServerActivity.class.getSimpleName();
    

	private int offerConSize = 60;
	
	private AsyncTask<Void, Integer, Integer> updatePointTask;
	
	
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
        Mocoga.getInstance().initAppID(getApplicationContext(), "3bbb5aa4-658a-4b79-9534-6578820f9a2e", "PYdfmx97PdzLwQ==");

        /*
    	 * << User ID 설정 >>
    	 *
    	 * - Mocoga에서 보상을 지급할 때, 어느 사용자에게 보상을 지급해야 하는지를 전달하기 위해서는
    	 *   퍼블리셔에서 관리하는 사용자 ID, 즉 보상지급의 대상이 되는 User ID 설정을 해야 합니다.
    	 * - OfferCon을 노출하기 전, 즉 showOfferCon 메소드를 호출하기 이전에 setUserID 메소드를 통해 User ID를 설정해야 합니다.
    	 * - 설정한 User ID 는 보상지급 서버 URL 호출시 user_id 로 전달됩니다.
    	 * - User ID가 설정이 되어 있지 않으면, 보상을 지급할 사용자를 알 수 없으므로 OfferCon이 표시되지 않습니다.
    	 * - 주의! 테스트 앱에서는 편의를 위하여 UUID를 사용하였습니다. 실제 사용시에는 실제 User ID를 입력해주시기 바랍니다.
    	 */
        Mocoga.getInstance().setUserID(generateUserID());
        
        /*
    	 * << 가상화폐 관리 방식 >>
    	 *
    	 * - 앱 내 가상화폐의 관리방식(서버 관리 or 클라이언트 관리)에 따라 Mocoga에서 보상을 지급하는 방식에 차이가 있습니다.
    	 * - 서버에서 관리하신다면
    	 *   : Mocoga는 사용자에게 보상지급이 필요할 경우, 운영 중이신 서버로 보상을 요청하게 됩니다.
    	 *   : 퍼블리셔 캠페인의 가상화폐 정보에서 "서버"를 선택하시고, 구현하신 보상 지급 서버 URL 을 입력합니다.
    	 *   : 주의! 하단 구현방식은 샘플앱을 위한 서버 보상지급 구현입니다. 실제 구현시에는 해당 서버에 맞는 구현을 하시길 바랍니다.
    	 */
        
        /*
         * << Mocoga Listener 등록 >>
         * 
         * - 앱 내에서 유저 보상이 지급되거나 서버쪽으로 유저 보상이 지급되어 UI를 업데이트 해야 하는 경우 SDK에서 listener를 통해 알려주게 됩니다.
         * - 앱 종료시에는 반드시 Mocoga.getInstance().setListener(null)를 호출해서 등록한 listener를 해지해 주시기 바랍니다.
         */
        Mocoga.getInstance().setListener(mocogaListener);
        
    }
    
    
    @Override
    protected void onResume() {
    	
    	// 화면에 OfferCon을 보여줍니다.
    	showCurrentTapOfferCon();
    	
    	updateCurrency();
    	
    	super.onResume();
    }
    
    
    
    @Override
    protected void onPause() {
    	
    	// OfferCon을 숨깁니다.
    	Mocoga.getInstance().hideOfferIcon();
    	
    	super.onPause();
    }
    
    
    @Override
    protected void onDestroy() {
    	
    	// 앱 종료시에 Mocoga에 등록한 Listener를 해지합니다.
    	Mocoga.getInstance().setListener(null);
    	
    	super.onDestroy();
    }
    
    
    /*
     * 현재 Tab에 맞는 OfferCon을 보여줍니다.
     */
    private void showCurrentTapOfferCon() {
    	TabHost tabHost = getTabHost();
    	
    	String tag = tabHost.getCurrentTabTag();
    	
    	/*
		 * 예제에서는 좌표 및 OfferCon 사이즈 설정시 DIP 단위를 사용했습니다.
		 * 실제 앱 개발에서는 필요에서 따라 TypedValue.COMPLEX_UNIT_PX를 사용해서 device screen의 pixel 단위를 사용하셔도 됩니다.
		 */
    	
    	if(tag.equals("tab1")) {
			Mocoga.getInstance().hideOfferIcon();
			Mocoga.getInstance().showOfferCon(MocogaServerActivity.this, 70, 80, Mocoga.OFFER_ICON_LARGE, TypedValue.COMPLEX_UNIT_DIP, Mocoga.ICON_ALIGN_LEFT_BOTTOM);
		}
		else if(tag.equals("tab2")) {
			Mocoga.getInstance().hideOfferIcon();
			Mocoga.getInstance().showOfferCon(MocogaServerActivity.this, 70, 80, Mocoga.OFFER_ICON_NORMAL, TypedValue.COMPLEX_UNIT_DIP, Mocoga.ICON_ALIGN_RIGHT_BOTTOM);
		}
		else if(tag.equals("tab3")) {
			Mocoga.getInstance().hideOfferIcon();
			Mocoga.getInstance().showOfferCon(MocogaServerActivity.this, 160, 160, Mocoga.OFFER_ICON_SMALL, TypedValue.COMPLEX_UNIT_DIP, Mocoga.ICON_ALIGN_LEFT_BOTTOM);
		}
		else if(tag.equals("tab4")) {
			Mocoga.getInstance().hideOfferIcon();
			Mocoga.getInstance().showOfferCon(MocogaServerActivity.this, 160, 160, offerConSize, TypedValue.COMPLEX_UNIT_DIP, Mocoga.ICON_ALIGN_LEFT_BOTTOM);
		}
    	
    }
    
    
    private OnTabChangeListener onTabChangeListener = new OnTabChangeListener() {
		
    	// Tab 전환시 OfferCon의 위치 및 크기를 변경합니다.
		@Override
		public void onTabChanged(String tabId) {
			showCurrentTapOfferCon();
		}
	};
    
	
	public static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        

        return sb.toString();
    }
	
	
	private String generateUserID() {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		String id = p.getString("UserID", null);
		
		if(id == null) {
			id = UUID.randomUUID().toString();
		}
		
		Editor e = p.edit();
		
		e.putString("UserID", id);
		e.commit();
		
		return id;
		
	}
	
	
	private void updateCurrency() {
		if(updatePointTask == null) {
			findViewById(R.id.Indicator).setVisibility(View.VISIBLE);
			updatePointTask = new AsyncTask<Void, Integer, Integer>() {

				@Override
				protected Integer doInBackground(Void... params) {

					int point = 0;
					try {
					
						HttpClient httpClient = new DefaultHttpClient();
						String url = "http://sample-reward.mocoga.com/get_currency?user_id=" + generateUserID();
						HttpGet get = new HttpGet(url);
						HttpResponse response = httpClient.execute(get);
						InputStream is = response.getEntity().getContent();
						
						String str = convertStreamToString(is);
						
						Log.i(TAG, "Result : " + str);
						
						JSONObject r = new JSONObject(str);
						
						point = r.getInt("point");
					}
					catch(Exception e) {
						e.printStackTrace();
					}
					
					return point;
						
				}
				
				protected void onPostExecute(Integer result) {
					updatePointTask = null;
					
					((TextView)findViewById(R.id.Currency)).setText(String.valueOf(result));
					findViewById(R.id.Indicator).setVisibility(View.GONE);
				};
			};
			
			updatePointTask.execute();
		}
		
	}
	
    
    
    /**
     * Mocoga Listener
     */
    private MocogaListener mocogaListener = new MocogaListener() {
		
    	
    	/*
    	 * << 가상화폐 UI 업데이트 >>
    	 *
    	 * - 앱 설치/비디오 시청이 완료된 경우, Mocoga는 보상 지급을 위해 서버 URL 을 호출한 후에 MocogaListener의 
    	 *   mocogaUpdateCurrency method를 호출하여 사용자에게 보여지는 Currency를 업데이트해야 하는 시점을 알려줍니다.
    	 */
		@Override
		public void mocogaUpdateCurrency() {
			updateCurrency();
		}
		
		@Override
		public void mocogaRequestsToGiveReward(String rewardTransId, Reward rewardInfo) {
			
		}
	};
	
}