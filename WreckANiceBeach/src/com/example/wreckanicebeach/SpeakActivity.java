package com.example.wreckanicebeach;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;

public class SpeakActivity extends ActionBarActivity {

	public static final int SPEECH_RECOGNITION_REQUEST = 9001;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_speak);
		// Show the Up button in the action bar.
		setupActionBar();
		speak(this.getCurrentFocus());
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
		getMenuInflater().inflate(R.menu.speak, menu);
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SPEECH_RECOGNITION_REQUEST
				&& resultCode == RESULT_OK) {
			ArrayList<String> raw_results = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			String[] results = new String[raw_results.size()];
			results = raw_results.toArray(results);
			parse(this.getCurrentFocus(), results[0]);
		}
	}

	// Begin speech recognition
	public void speak(View view) {
		if (SpeechRecognizer.isRecognitionAvailable(this)) {
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
					RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			startActivityForResult(intent, SPEECH_RECOGNITION_REQUEST);
		} else {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText("Sorry, recognition not available.");
		}
	}

	public void parse(View view, String result) {
		Log.d("SpeakActivity", result);
		String findStop = ".*(find|stop|where|station).*";
		Pattern checkSchedule = Pattern
				.compile(".*(next|schedule|schedules|time|times|check).*");
		Matcher scheduleMatcher = checkSchedule.matcher(result);
		Pattern directions = Pattern.compile("to (.*)");
		Matcher directionsMatcher = directions.matcher(result);
		Pattern origin2 = Pattern.compile("from (.*)");
		Matcher origin2Matcher = origin2.matcher(result);
		if (result.matches(findStop)) {
			Intent intent = new Intent(this, FindStopActivity.class);
			Pattern route = Pattern
					.compile("\\d+|ct1|ct2|ct3|34 E|70 A|Mattapan|Fairmount|Fitchburg|South Acton|Framingham|Worcester|Franklin|Greenbush|Haverhill|Kingston|Plymouth|Lowell|Middleborough|Lakeville|Needham|Newburyport|Rockport|Providence|Stoughton|Hingham|Hull|Charlestown");
			Pattern subwayRoute = Pattern.compile(
					"red|orange|green|blue|silver", Pattern.CASE_INSENSITIVE);
			Pattern mode = Pattern
					.compile("bus|subway|rail|commuter rail|boat|ferry");
			Matcher routeMatcher = route.matcher(result);
			Matcher subwayRouteMatcher = subwayRoute.matcher(result);
			Matcher modeMatcher = mode.matcher(result);
			if (routeMatcher.find(0)) {
				intent.putExtra("ROUTE", routeMatcher.group());
			} else if (subwayRouteMatcher.find(0)) {
				intent.putExtra("SUBWAY_ROUTE", subwayRouteMatcher.group());
			} else if (modeMatcher.find(0)) {
				intent.putExtra("MODE", modeMatcher.group());
			}
			startActivity(intent);
		} else if (directionsMatcher.find(0)) {
			Intent intent = new Intent(this, GetDirectionsActivity.class);
			Log.d("Destination", directionsMatcher.group(1));
			intent.putExtra("DESTINATION", directionsMatcher.group(1));
			Pattern origin = Pattern.compile("from (.*)");
			Matcher originMatcher = origin.matcher(result.replace(
					directionsMatcher.group(), ""));
			if (originMatcher.find(0)) {
				Log.d("Origin", originMatcher.group(1));
				intent.putExtra("ORIGIN", originMatcher.group(1));
			}
			startActivity(intent);
		} else if (origin2Matcher.find(0)) {
			Intent intent = new Intent(this, GetDirectionsActivity.class);
			Log.d("Origin", origin2Matcher.group(1));
			intent.putExtra("ORIGIN", origin2Matcher.group(1));
			Pattern destination = Pattern.compile("to (.*)");
			Matcher destinationMatcher = destination.matcher(result.replace(
					origin2Matcher.group(), ""));
			if (destinationMatcher.find(0)) {
				Log.d("Destination", destinationMatcher.group(1));
				intent.putExtra("DESTINATION", destinationMatcher.group(1));
			}
			startActivity(intent);
		} else if (scheduleMatcher.find(0)) {
			Intent intent = new Intent(this, ShowSchedule.class);
			Pattern route = Pattern
					.compile("\\d+|ct1|ct2|ct3|34 E|70 A|Mattapan|Fairmount|Fitchburg|South Acton|Framingham|Worcester|Franklin|Greenbush|Haverhill|Kingston|Plymouth|Lowell|Middleborough|Lakeville|Needham|Newburyport|Rockport|Providence|Stoughton|Hingham|Hull|Charlestown");
			Pattern subwayRoute = Pattern.compile(
					"red|orange|green|blue|silver", Pattern.CASE_INSENSITIVE);
			Matcher routeMatcher = route.matcher(result);
			Matcher subwayRouteMatcher = subwayRoute.matcher(result);
			if (subwayRouteMatcher.find(0)) {
				Log.d("In subway route matcher: ", subwayRouteMatcher.group());
				intent.putExtra("ROUTE_ID",
						getSubwayId(subwayRouteMatcher.group().toLowerCase()));
			} else if (routeMatcher.find(0)) {
				intent.putExtra("ROUTE_ID", getRouteId(routeMatcher.group()).toLowerCase());
			}
			startActivity(intent);
		} else {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText(result);
		}
	}

	private String getSubwayId(String choice) {
		String routeId = "";
		if (choice.equals("red")) {
			routeId = "931_";
		} else if (choice.equals("green")) {
			routeId = "812_";
		} else if (choice.equals("orange")) {
			routeId = "903_";
		} else if (choice.equals("blue")) {
			routeId = "946_";
		} else if (choice.equals("silver")) {
			routeId = "741_";
		}
		return routeId;
	}

	private String getRouteId(String route) {
		if (route.equals("ct1")) {
			return "701";
		} else if (route.equals("ct2")) {
			return "747";
		} else if (route.equals("ct3")) {
			return "708";
		} else if (route.equals("34 E")) {
			return "34E";
		} else if (route.equals("70 A")) {
			return "70A";
		} else if (route.equals("Mattapan")) {
			return "899_";
		} else if (route.equals("Fairmount")) {
			return "CR-Fairmount";
		} else if (route.equals("Fitchburg") || route.equals("Acton")) {
			return "CR-Fitchburg";
		} else if (route.equals("Framingham") || route.equals("Worcester")) {
			return "CR-Worcester";
		} else if (route.equals("Franklin")) {
			return "CR-Franklin";
		} else if (route.equals("Greenbush")) {
			return "CR-Greenbush";
		} else if (route.equals("Haverhill")) {
			return "CR-Haverhill";
		} else if (route.equals("Kingston") || route.equals("Plymouth")) {
			return "CR-Kingston";
		} else if (route.equals("Middleborough") || route.equals("Lakeville")) {
			return "CR-Middleborough";
		} else if (route.equals("Needham")) {
			return "CR-Needham";
		} else if (route.equals("Newburyport") || route.equals("Rockport")) {
			return "CR-Newburyport";
		} else if (route.equals("Providence") || route.equals("Stoughton")) {
			return "CR-Providence";
		} else if (route.equals("Hingham")) {
			return "Boat-F1";
		} else if (route.equals("Hull")) {
			return "Boat-F3";
		} else if (route.equals("Charlestown")) {
			return "Boat-F4";
		} else {
			return route;
		}
	}

}