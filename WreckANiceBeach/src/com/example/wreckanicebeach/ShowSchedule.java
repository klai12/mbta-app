package com.example.wreckanicebeach;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class ShowSchedule extends Activity {
	
	String timeURL = "http://realtime.mbta.com/developer/api/v1/schedulebyroute?api_key=wX9NwuHnZU2ToO7GmGR9uw&route=931_";
	String scheduleString = "initial";

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_schedule);
		callWebService();
		
		// Create the text view
	    TextView textView = new TextView(this);
	    textView.setTextSize(40);
	    textView.setText(scheduleString);
	    setContentView(textView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.show_schedule, menu);
		return true;
	}
	
	public void callWebService() {
		HttpClient httpclient = new DefaultHttpClient();  
        HttpGet request = new HttpGet(timeURL);
        request.addHeader("accept", "application/xml");
        ResponseHandler<String> handler = new BasicResponseHandler();  
        try {  
            scheduleString = httpclient.execute(request, handler);  
        } catch (ClientProtocolException e) {  
            scheduleString = "in first catch block" + e.getMessage();
        	e.printStackTrace();  
        } catch (IOException e) {
        	scheduleString = "in second catch block" + e.getMessage();
            e.printStackTrace();  
        }  
        httpclient.getConnectionManager().shutdown();    
    }

}
