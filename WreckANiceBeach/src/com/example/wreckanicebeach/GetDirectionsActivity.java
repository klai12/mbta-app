package com.example.wreckanicebeach;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;

import android.location.Location;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Dialog;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;

public class GetDirectionsActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener,
		TextToSpeech.OnInitListener {

	// Global constants
	/*
	 * Define a request code to send to Google Play services This code is
	 * returned in Activity.onActivityResult
	 */
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private final static int MAP_REQUEST = 9002;
	private final static String SERVER_TIME = "http://realtime.mbta.com/developer/api/v1/servertime?api_key=";
	private final static String API_KEY = "GF9GdCYGNEmHdd5sZiMzyw";
	private final static String GET_DIRECTIONS = "https://maps.googleapis.com/maps/api/directions/xml?sensor=true&mode=transit&departure_time=";
	// We don't use namespaces
	private static final String ns = null;
	private boolean initFlag;
	LocationClient mLocationClient;
	Location mCurrentLocation;
	private TextToSpeech tts;
	private String serverTime = null;
	private String destination = null;
	private String origin = null;

	public static class Step {
		String travelMode;
		String instructions;
		String duration;
		String distance;
		String line;
		String arrivalStop;
		String agencyName;
		String agencyURL;

		private Step(String travelMode, String instructions, String duration,
				String distance, String line, String arrivalStop,
				String agencyName, String agencyURL) {
			this.travelMode = travelMode;
			this.instructions = instructions;
			this.duration = duration;
			this.distance = distance;
			this.line = line;
			this.arrivalStop = arrivalStop;
			this.agencyName = agencyName;
			this.agencyURL = agencyURL;
		}
	}

	public static class Directions {
		String status;
		List<Step> steps;
		String departureTime;
		String arrivalTime;
		String polyline;
		String copyrights;
		String warning;

		private Directions(String status, List<Step> steps,
				String departureTime, String arrivalTime, String polyline,
				String copyrights, String warning) {
			this.status = status;
			this.steps = steps;
			this.departureTime = departureTime;
			this.arrivalTime = arrivalTime;
			this.polyline = polyline;
			this.copyrights = copyrights;
			this.warning = warning;
		}
	}

	// Parses an MBTA XML feed, returning the server time
	public class MBTAXmlServerTimeParser {

		public String parse(InputStream in) throws XmlPullParserException,
				IOException {
			try {
				XmlPullParser parser = Xml.newPullParser();
				parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
						false);
				parser.setInput(in, null);
				parser.nextTag();
				return readFeed(parser);
			} finally {
				in.close();
			}
		}

		// Returns the server time
		private String readFeed(XmlPullParser parser)
				throws XmlPullParserException, IOException {

			parser.require(XmlPullParser.START_TAG, ns, "server_time");
			String serverTime = parser.getAttributeValue(null, "server_dt");
			return serverTime;
		}

	}

	// Parses a Google XML feed, returning a list of directions
	public class GoogleDirectionsParser {

		public Directions parse(InputStream in) throws XmlPullParserException,
				IOException {
			try {
				XmlPullParser parser = Xml.newPullParser();
				parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
						false);
				parser.setInput(in, null);
				parser.nextTag();
				return readFeed(parser);
			} finally {
				in.close();
			}
		}

		// Returns a list of stops in the feed
		private Directions readFeed(XmlPullParser parser)
				throws XmlPullParserException, IOException {
			Directions directions = new Directions(null, null, null, null,
					null, null, null);
			List<Step> steps = new ArrayList<Step>();

			parser.require(XmlPullParser.START_TAG, ns, "DirectionsResponse");
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				// Starts by looking for the status tag
				if (name.equals("status")) {
					directions.status = parser.nextText();
					parser.nextTag();
				} else if (name.equals("route")) {
					while (parser.next() != XmlPullParser.END_TAG) {
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						String name2 = parser.getName();
						if (name2.equals("leg")) {
							while (parser.next() != XmlPullParser.END_TAG) {
								if (parser.getEventType() != XmlPullParser.START_TAG) {
									continue;
								}
								String name3 = parser.getName();
								if (name3.equals("step")) {
									steps.add(readStep(parser));
								} else if (name3.equals("departure_time")) {
									while (parser.next() != XmlPullParser.END_TAG) {
										if (parser.getEventType() != XmlPullParser.START_TAG) {
											continue;
										}
										String name4 = parser.getName();
										if (name4.equals("text")) {
											directions.departureTime = parser
													.nextText();
											parser.nextTag();
										} else {
											skip(parser);
										}
									}
								} else if (name3.equals("arrival_time")) {
									while (parser.next() != XmlPullParser.END_TAG) {
										if (parser.getEventType() != XmlPullParser.START_TAG) {
											continue;
										}
										String name5 = parser.getName();
										if (name5.equals("text")) {
											directions.arrivalTime = parser
													.nextText();
											parser.nextTag();
										} else {
											skip(parser);
										}
									}
								} else {
									skip(parser);
								}
							}
						} else if (name2.equals("overview_polyline")) {
							while (parser.next() != XmlPullParser.END_TAG) {
								if (parser.getEventType() != XmlPullParser.START_TAG) {
									continue;
								}
								String name6 = parser.getName();
								if (name6.equals("points")) {
									directions.polyline = parser.nextText();
									parser.nextTag();
								} else {
									skip(parser);
								}
							}
						} else if (name2.equals("copyrights")) {
							directions.copyrights = parser.nextText();
							parser.nextTag();
						} else if (name2.equals("warning")) {
							directions.warning = parser.nextText();
							parser.nextTag();
						} else {
							skip(parser);
						}
					}
				} else {
					skip(parser);
				}
			}
			directions.steps = steps;
			return directions;
		}

		private Step readStep(XmlPullParser parser) throws IOException,
				XmlPullParserException {
			String travelMode = null;
			String instructions = null;
			String duration = null;
			String distance = null;
			String line = null;
			String arrivalStop = null;
			String agencyName = null;
			String agencyURL = null;
			parser.require(XmlPullParser.START_TAG, ns, "step");
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				// Starts by looking for the travel_mode tag
				if (name.equals("travel_mode")) {
					travelMode = parser.nextText();
					parser.nextTag();
				} else if (name.equals("html_instructions")) {
					instructions = parser.nextText();
					parser.nextTag();
				} else if (name.equals("duration")) {
					while (parser.next() != XmlPullParser.END_TAG) {
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						String name2 = parser.getName();
						if (name2.equals("text")) {
							duration = parser.nextText();
							parser.nextTag();
						} else {
							skip(parser);
						}
					}
				} else if (name.equals("distance")) {
					while (parser.next() != XmlPullParser.END_TAG) {
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						String name3 = parser.getName();
						if (name3.equals("text")) {
							distance = parser.nextText();
							parser.nextTag();
						} else {
							skip(parser);
						}
					}
				} else if (name.equals("transit_details")) {
					while (parser.next() != XmlPullParser.END_TAG) {
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						String name4 = parser.getName();
						if (name4.equals("arrival_stop")) {
							while (parser.next() != XmlPullParser.END_TAG) {
								if (parser.getEventType() != XmlPullParser.START_TAG) {
									continue;
								}
								String name5 = parser.getName();
								if (name5.equals("name")) {
									arrivalStop = parser.nextText();
									parser.nextTag();
								} else {
									skip(parser);
								}
							}
						} else if (name4.equals("line")) {
							while (parser.next() != XmlPullParser.END_TAG) {
								if (parser.getEventType() != XmlPullParser.START_TAG) {
									continue;
								}
								String name6 = parser.getName();
								if (name6.equals("short_name")) {
									line = parser.nextText();
									parser.nextTag();
								} else if (name6.equals("agency")) {
									while (parser.next() != XmlPullParser.END_TAG) {
										if (parser.getEventType() != XmlPullParser.START_TAG) {
											continue;
										}
										String name7 = parser.getName();
										if (name7.equals("name")) {
											agencyName = parser.nextText();
											parser.nextTag();
										} else if (name7.equals("url")) {
											agencyURL = parser.nextText();
											parser.nextTag();
										} else {
											skip(parser);
										}
									}
								} else {
									skip(parser);
								}
							}
						} else {
							skip(parser);
						}
					}
				} else {
					skip(parser);
				}
			}
			Step step = new Step(travelMode, instructions, duration, distance,
					line, arrivalStop, agencyName, agencyURL);
			return step;
		}

		// Skips irrelevant tags
		private void skip(XmlPullParser parser) throws XmlPullParserException,
				IOException {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				throw new IllegalStateException();
			}
			int depth = 1;
			while (depth != 0) {
				switch (parser.next()) {
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
				}
			}
		}

	}

	// Implementation of AsyncTask used to download XML Server Time feed
	// from mbta.com.
	private class DownloadServerTimeTask extends
			AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			try {
				return loadServerTime(urls[0]);
			} catch (IOException e) {
				return getResources().getString(R.string.connection_error);
			} catch (XmlPullParserException e) {
				return getResources().getString(R.string.xml_error);
			}
		}

		@Override
		protected void onPostExecute(String result) {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText(result);
		}
	}

	// Implementation of AsyncTask used to download XML Directions
	// from googleapis.com.
	private class DownloadDirectionsTask extends
			AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			try {
				return loadDirections(urls[0]);
			} catch (IOException e) {
				return getResources().getString(R.string.connection_error);
			} catch (XmlPullParserException e) {
				return getResources().getString(R.string.xml_error);
			}
		}

		@Override
		protected void onPostExecute(String result) {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText(result);
		}
	}

	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_get_directions);
		// Show the Up button in the action bar.
		setupActionBar();
		initFlag = false;
		/*
		 * Create a new location client, using the enclosing class to handle
		 * callback.
		 */
		mLocationClient = new LocationClient(this, this, this);
		tts = new TextToSpeech(this, this);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	/*
	 * Called when the Activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		boolean wifiConnected = networkInfo.isConnected();
		networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		boolean mobileConnected = networkInfo.isConnected();
		Log.d("FindStopActivity", "Wifi connected: " + wifiConnected);
		Log.d("FindStopActivity", "Mobile connected: " + mobileConnected);
		
		if (initFlag == false && (wifiConnected || mobileConnected)) {
			initFlag = true;
			String urlString = (SERVER_TIME + API_KEY);
			loadPage(urlString);
			while (serverTime == null) {
				// Wait!
			}
			Bundle myBundle = getIntent().getExtras();
			if (myBundle != null) {
				destination = myBundle.getString("DESTINATION");
				origin = myBundle.getString("ORIGIN");
			}
			if (destination != null && origin != null) {
				String urlString2 = (GET_DIRECTIONS + serverTime
						+ "&destination=" + destination + "&origin=" + origin);
				Log.d("URL", urlString2);
				loadPage(urlString2);
			} else {
				// Connect the client.
				if (servicesConnected()) {
					mLocationClient.connect();
				} else {
					TextView textView = (TextView) findViewById(R.id.textview);
					textView.setText("Sorry, location not available.");
				}
			}
		} else {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText("Sorry, connection error.");
		}
	}

	/*
	 * Called when the Activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		// Disconnecting the client invalidates it.
		if (servicesConnected()) {
			mLocationClient.disconnect();
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// Don't forget to shutdown tts!
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
		super.onDestroy();
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
		getMenuInflater().inflate(R.menu.get_directions, menu);
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
		// Decide what to do based on the original request code
		switch (requestCode) {
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			/*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
			switch (resultCode) {
			case RESULT_OK:
				/*
				 * Try the request again
				 */
				break;
			}
		}
	}

	@Override
	public void onInit(int status) {
		if (status != TextToSpeech.SUCCESS) {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText("Sorry, text to speech not available.");
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			// Get the error dialog from Google Play services
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					connectionResult.getErrorCode(), this,
					CONNECTION_FAILURE_RESOLUTION_REQUEST);

			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				// Set the dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(getSupportFragmentManager(),
						"Location Updates");
			}
		}
	}

	@Override
	public void onConnected(Bundle arg0) {
		mCurrentLocation = mLocationClient.getLastLocation();
		if (mCurrentLocation != null) {
			String latitude = Double.toString(mCurrentLocation.getLatitude());
			String longitude = Double.toString(mCurrentLocation.getLongitude());
			if (destination != null) {
				String urlString2 = (GET_DIRECTIONS + serverTime
						+ "&destination=" + destination + "&origin=" + latitude
						+ "," + longitude);
				Log.d("URL", urlString2);
				loadPage(urlString2);
			} else if (origin != null) {
				String urlString2 = (GET_DIRECTIONS + serverTime
						+ "&destination=" + latitude + "," + longitude
						+ "&origin=" + origin);
				Log.d("URL", urlString2);
				loadPage(urlString2);
			} else {
				TextView textView = (TextView) findViewById(R.id.textview);
				textView.setText("Sorry, need an origin or a destination.");
			}
		}
	}

	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

	private boolean servicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates", "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			// Get the error code
			int errorCode = resultCode;
			// Get the error dialog from Google Play services
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				// Set the dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(getSupportFragmentManager(),
						"Location Updates");
			}
			return false;
		}
	}

	// Given a string representation of a URL, sets up a connection and gets
	// an input stream.
	// Uses AsyncTask to download the XML feed from mbta.com or googleapis.com.
	public void loadPage(String urlString) {

		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		boolean wifiConnected = networkInfo.isConnected();
		networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		boolean mobileConnected = networkInfo.isConnected();
		Log.d("FindStopActivity", "Wifi connected: " + wifiConnected);
		Log.d("FindStopActivity", "Mobile connected: " + mobileConnected);

		if (wifiConnected || mobileConnected) {
			if (urlString.contains("mbta")) {
				new DownloadServerTimeTask().execute(urlString);
			} else if (urlString.contains("googleapis")) {
				new DownloadDirectionsTask().execute(urlString);
			}
		} else {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText("Sorry, connection error.");
		}

	}

	private InputStream downloadUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.addRequestProperty("Accept", "application/xml");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		return conn.getInputStream();
	}

	// Downloads XML from mbta.com, parses it for server time
	private String loadServerTime(String urlString)
			throws XmlPullParserException, IOException {
		InputStream stream = null;
		// Instantiate the parser
		MBTAXmlServerTimeParser mbtaXmlServerTimeParser = new MBTAXmlServerTimeParser();

		try {
			stream = downloadUrl(urlString);
			serverTime = mbtaXmlServerTimeParser.parse(stream);
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (stream != null) {
				stream.close();
			}
		}

		return "";
	}

	// Downloads XML from googleapis.com, parses it for directions
	private String loadDirections(String urlString)
			throws XmlPullParserException, IOException {
		InputStream stream = null;
		// Instantiate the parser
		GoogleDirectionsParser googleDirectionsParser = new GoogleDirectionsParser();
		Directions directions = null;

		try {
			stream = downloadUrl(urlString);
			directions = googleDirectionsParser.parse(stream);
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (stream != null) {
				stream.close();
			}
		}

		if (directions.status.equals("OK")) {
			String overviewInstructions = "";
			for (Step step : directions.steps) {
				String instructions = (step.instructions + " (" + step.duration
						+ ", " + step.distance + ")\n");
				if (step.travelMode.equals("TRANSIT")) {
					instructions = (instructions + step.line + " to "
							+ step.arrivalStop + "\n" + step.agencyName + ", "
							+ step.agencyURL + "\n");
				}
				overviewInstructions = (overviewInstructions + instructions + "\n");
			}
			String output = (overviewInstructions + "\n"
					+ directions.copyrights + "\n" + directions.warning);
			Intent intent = new Intent(this, MapActivity.class);
			intent.putExtra("ACTIVITY", "GetDirectionsActivity");
			intent.putExtra("POLYLINE", directions.polyline);
			startActivityForResult(intent, MAP_REQUEST);
			return output;
		} else {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText("Sorry, could not find directions.");
			return "";
		}
	}

}