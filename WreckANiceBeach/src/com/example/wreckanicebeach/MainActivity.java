package com.example.wreckanicebeach;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.content.Intent;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void toCheckSchedule(View view) {
    	Intent intent = new Intent(this, CheckScheduleActivity.class);
    	startActivity(intent);
    }
    
    public void toFindStop(View view) {
		Intent intent = new Intent(this, FindStopActivity.class);
		startActivity(intent);
    }
    
    public void toGetDirections(View view) {
    	Intent intent = new Intent(this, GetOriginDestinationActivity.class);
    	startActivity(intent);
    }
    
    public void toSpeak(View view) {
    	Intent intent = new Intent(this, SpeakActivity.class);
    	startActivity(intent);
    }
    
}
