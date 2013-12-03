package com.example.wreckanicebeach;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;

public class SpeakActivity extends Activity {

	public static final int SPEECH_RECOGNITION_REQUEST = 9001;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_speak);
		// Show the Up button in the action bar.
		setupActionBar();
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

	public void parse(View view, String result) {
		String FIND_STOP = ".*(find|stop|where|station).*";
		if (result.matches(FIND_STOP)) {
			Intent intent = new Intent(this, FindStopActivity.class);
			startActivity(intent);
		} else {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText(result);
		}
	}

}