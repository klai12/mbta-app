package com.example.wreckanicebeach;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;

public class ChooseBusLine extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_bus_line);
		// Show the Up button in the action bar.
		setupActionBar();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.choose_bus_line, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void toShowSchedule(View view) {
		String routeId = getRouteId(view);
    	Intent intent = new Intent(this, ShowSchedule.class);
    	intent.putExtra("ROUTE_ID", routeId);
    	startActivity(intent);
    }
	
	private String getRouteId(View view) {
		String routeId = "";
		switch (view.getId()) {
			case (R.id.one_button):
				routeId = "01";
				break;
			case (R.id.forty_seven_button):
				routeId = "47";
				break;
			case (R.id.sixty_six_button):
				routeId = "66";
				break;
			case (R.id.seventy_button):
				routeId = "70";
				break;
			case (R.id.seventy_a_button):
				routeId = "70A";
				break;
			case (R.id.seventy_three_button):
				routeId = "73";
				break;
			case (R.id.seventy_four_button):
				routeId = "74";
				break;
		}
		return routeId;
	}

}