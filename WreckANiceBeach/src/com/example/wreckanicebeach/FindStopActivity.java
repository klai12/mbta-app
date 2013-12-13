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
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.speech.tts.TextToSpeech;

public class FindStopActivity extends FragmentActivity implements
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
	private final static String STOPS_BY_LOCATION = "http://realtime.mbta.com/developer/api/v1/stopsbylocation?api_key=";
	private final static String STOPS_BY_ROUTE = "http://realtime.mbta.com/developer/api/v1/stopsbyroute?api_key=";
	private final static String ROUTES_BY_STOP = "http://realtime.mbta.com/developer/api/v1/routesbystop?api_key=";
	private final static String API_KEY = "wX9NwuHnZU2ToO7GmGR9uw";
	// We don't use namespaces
	private static final String ns = null;
	private boolean initFlag;
	LocationClient mLocationClient;
	Location mCurrentLocation;
	private TextToSpeech tts;
	private List<Stop> stopsNearLocation;
	private List<Stop> stopsOnRoute;
	private ListRouteMode routesModesServingStop;

	public static class Stop {
		public final String id;
		public final String name;
		public final String latitude;
		public final String longitude;

		private Stop(String id, String name, String latitude, String longitude) {
			this.id = id;
			this.name = name;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		// Use @Override to avoid accidental overloading.
		@Override
		public boolean equals(Object o) {
			// Return true if the objects are identical.
			// (This is just an optimization, not required for correctness.)
			if (this == o) {
				return true;
			}

			// Return false if the other object has the wrong type.
			// This type may be an interface depending on the interface's
			// specification.
			if (!(o instanceof Stop)) {
				return false;
			}

			// Cast to the appropriate type.
			// This will succeed because of the instanceof, and lets us access
			// private fields.
			Stop that = (Stop) o;

			// Check each field. Primitive fields, reference fields, and
			// nullable reference
			// fields are all treated differently.
			return this.id.equals(that.id) && this.name.equals(that.name)
					&& this.latitude.equals(that.latitude)
					&& this.longitude.equals(that.longitude);
		}

		@Override
		public int hashCode() {
			// Start with a non-zero constant.
			int result = 17;

			// Include a hash for each field.
			result = 31 * result + this.id.hashCode();
			result = 31 * result + this.name.hashCode();
			result = 31 * result + this.latitude.hashCode();
			result = 31 * result + this.longitude.hashCode();

			return result;
		}
	}

	public static class Route {
		public final String id;
		public final String name;

		private Route(String id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class Mode {
		public final String type;
		public final String name;

		private Mode(String type, String name) {
			this.type = type;
			this.name = name;
		}
	}

	public static class ListRouteMode {
		public final List<Route> routes;
		public final List<Mode> modes;

		private ListRouteMode(List<Route> routes, List<Mode> modes) {
			this.routes = routes;
			this.modes = modes;
		}
	}

	public class MBTAXmlStopParser {

		public List<Stop> parse(InputStream in) throws XmlPullParserException,
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

		private List<Stop> readFeed(XmlPullParser parser)
				throws XmlPullParserException, IOException {
			List<Stop> stops = new ArrayList<Stop>();

			parser.require(XmlPullParser.START_TAG, ns, "stop_list");
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				// Starts by looking for the stop tag
				if (name.equals("stop")) {
					stops.add(readStop(parser));
				} else if (name.equals("direction")) {
					while (parser.next() != XmlPullParser.END_TAG) {
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						String name2 = parser.getName();
						// Starts by looking for the stop tag
						if (name2.equals("stop")) {
							stops.add(readStop(parser));
						} else {
							skip(parser);
						}
					}
				} else {
					skip(parser);
				}
			}
			return stops;
		}

		// Processes stop tags in the feed.
		private Stop readStop(XmlPullParser parser) throws IOException,
				XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, ns, "stop");
			String id = parser.getAttributeValue(null, "stop_id");
			String name = parser.getAttributeValue(null, "stop_name");
			String latitude = parser.getAttributeValue(null, "stop_lat");
			String longitude = parser.getAttributeValue(null, "stop_lon");
			Stop stop = new Stop(id, name, latitude, longitude);
			parser.nextTag();
			return stop;
		}

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

	public class MBTAXmlRouteParser {

		public ListRouteMode parse(InputStream in)
				throws XmlPullParserException, IOException {
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

		private ListRouteMode readFeed(XmlPullParser parser)
				throws XmlPullParserException, IOException {
			List<Route> routes = new ArrayList<Route>();
			List<Mode> modes = new ArrayList<Mode>();

			parser.require(XmlPullParser.START_TAG, ns, "route_list");
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				// Starts by looking for the mode tag
				if (name.equals("mode")) {
					modes.add(readMode(parser));
					while (parser.next() != XmlPullParser.END_TAG) {
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						String name2 = parser.getName();
						// Starts by looking for the route tag
						if (name2.equals("route")) {
							routes.add(readRoute(parser));
						} else {
							skip(parser);
						}
					}
				} else {
					skip(parser);
				}
			}
			ListRouteMode routesModes = new ListRouteMode(routes, modes);
			return routesModes;
		}

		// Processes route tags in the feed.
		private Route readRoute(XmlPullParser parser) throws IOException,
				XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, ns, "route");
			String id = parser.getAttributeValue(null, "route_id");
			String name = parser.getAttributeValue(null, "route_name");
			Route route = new Route(id, name);
			parser.nextTag();
			return route;
		}

		// Processes mode tags in the feed.
		private Mode readMode(XmlPullParser parser) throws IOException,
				XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, ns, "mode");
			String type = parser.getAttributeValue(null, "route_type");
			String name = parser.getAttributeValue(null, "mode_name");
			Log.d("type + name", type + name);
			Mode mode = new Mode(type, name);
			parser.nextTag();
			return mode;
		}

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

	// Implementation of AsyncTask used to download XML feed from mbta.com.
	private class DownloadStopsByLocationTask extends
			AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			try {
				return loadStopListByLocation(urls[0]);
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

	// Implementation of AsyncTask used to download XML feed from mbta.com.
	private class DownloadStopsByRouteTask extends
			AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			try {
				return loadStopListByRoute(urls[0]);
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

	// Implementation of AsyncTask used to download XML feed from mbta.com.
	private class DownloadRoutesByStopTask extends
			AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			try {
				return loadRouteListByStop(urls[0]);
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

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_find_stop);
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
		// Connect the client.
		if (servicesConnected()) {
			mLocationClient.connect();
		} else {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText("Sorry, location not available.");
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
		getMenuInflater().inflate(R.menu.find_stop, menu);
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
		if (initFlag == false && mCurrentLocation != null) {
			String latitude = Double.toString(mCurrentLocation.getLatitude());
			String longitude = Double.toString(mCurrentLocation.getLongitude());
			String urlString = (STOPS_BY_LOCATION + API_KEY + "&lat="
					+ latitude + "&lon=" + longitude);
			loadPage(urlString);
			initFlag = true;
		}
	}

	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onInit(int status) {
		if (status != TextToSpeech.SUCCESS) {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText("Sorry, text to speech not available.");
		}
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

	// Uses AsyncTask to download the XML feed from mbta.com.
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
			if (urlString.contains("stopsbylocation")) {
				new DownloadStopsByLocationTask().execute(urlString);
			} else if (urlString.contains("stopsbyroute")) {
				new DownloadStopsByRouteTask().execute(urlString);
			} else if (urlString.contains("routesbystop")) {
				new DownloadRoutesByStopTask().execute(urlString);
			}
		} else {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText("Sorry, connection error.");
		}
		
	}

	// Uploads XML from mbta.com, parses it. Returns string.
	private String loadStopListByLocation(String urlString)
			throws XmlPullParserException, IOException {
		InputStream stream = null;
		// Instantiate the parser
		MBTAXmlStopParser mbtaXmlStopParser = new MBTAXmlStopParser();

		try {
			stream = downloadUrl(urlString);
			stopsNearLocation = mbtaXmlStopParser.parse(stream);
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (stream != null) {
				stream.close();
			}
		}

		Bundle myBundle = getIntent().getExtras();
		String route = null;
		String subwayRoute = null;
		String mode = null;
		Stop firstStop = null;
		String stopString;
		if (myBundle != null) {
			route = myBundle.getString("ROUTE");
			subwayRoute = myBundle.getString("SUBWAY_ROUTE");
			mode = myBundle.getString("MODE");
		}
		if (route != null) {
			String urlString2 = STOPS_BY_ROUTE + API_KEY + "&route="
					+ routeToId(route);
			loadPage(urlString2);
			while (stopsOnRoute == null) {
				// Wait!
			}
			stopsNearLocation.retainAll(stopsOnRoute);
			Stop[] stopArray = new Stop[stopsNearLocation.size()];
			stopArray = stopsNearLocation.toArray(stopArray);
			if (stopArray.length != 0) {
				firstStop = stopArray[0];
				stopString = "The closest " + route + " stop is "
						+ firstStop.name;
			} else {
				stopString = "Sorry, there are no " + route + " stops near you";
			}
		} else if (subwayRoute != null) {
			for (Stop stop : stopsNearLocation) {
				String urlString3 = ROUTES_BY_STOP + API_KEY + "&stop="
						+ stop.id;
				routesModesServingStop = null;
				loadPage(urlString3);
				while (routesModesServingStop == null) {
					// Wait!
				}
				for (Route routeServingStop : routesModesServingStop.routes) {
					if (subwayRoute.equals("silver")) {
						if (routeServingStop.id.equals("741")
								|| routeServingStop.id.equals("742")
								|| routeServingStop.id.equals("746")
								|| routeServingStop.id.equals("749")
								|| routeServingStop.id.equals("751")) {
							firstStop = stop;
						}
					} else if (routeServingStop.name
							.equals(routeModeToName(subwayRoute))) {
						firstStop = stop;
					}
					if (firstStop != null) {
						break;
					}
				}
				if (firstStop != null) {
					break;
				}
			}
			if (firstStop != null) {
				stopString = "The closest " + routeModeToName(subwayRoute)
						+ " stop is " + firstStop.name;
			} else {
				stopString = "Sorry, there are no " + subwayRoute
						+ " stops near you";
			}
		} else if (mode != null) {
			for (Stop stop : stopsNearLocation) {
				String urlString4 = ROUTES_BY_STOP + API_KEY + "&stop="
						+ stop.id;
				routesModesServingStop = null;
				loadPage(urlString4);
				while (routesModesServingStop == null) {
					// Wait!
				}
				for (Mode modeServingStop : routesModesServingStop.modes) {
					if (modeServingStop.name.equals(routeModeToName(mode))) {
						firstStop = stop;
					}
					if (firstStop != null) {
						break;
					}
				}
				if (firstStop != null) {
					break;
				}
			}
			if (firstStop != null) {
				stopString = "The closest " + mode + " stop is "
						+ firstStop.name;
			} else {
				stopString = "Sorry, there are no " + mode + " stops near you";
			}
		} else {
			Stop[] stopArray = new Stop[stopsNearLocation.size()];
			stopArray = stopsNearLocation.toArray(stopArray);
			firstStop = stopArray[0];
			stopString = "The closest stop is " + firstStop.name;
		}
		if (firstStop != null) {
			Intent intent = new Intent(this, MapActivity.class);
			intent.putExtra("NAME", firstStop.name);
			intent.putExtra("LATITUDE", firstStop.latitude);
			intent.putExtra("LONGITUDE", firstStop.longitude);
			startActivityForResult(intent, MAP_REQUEST);
		}
		tts.speak(stopString, TextToSpeech.QUEUE_FLUSH, null);
		return stopString;
	}

	private String loadStopListByRoute(String urlString)
			throws XmlPullParserException, IOException {
		InputStream stream = null;
		// Instantiate the parser
		MBTAXmlStopParser mbtaXmlStopParser = new MBTAXmlStopParser();

		try {
			stream = downloadUrl(urlString);
			stopsOnRoute = mbtaXmlStopParser.parse(stream);
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (stream != null) {
				stream.close();
			}
		}

		return "";
	}

	private String loadRouteListByStop(String urlString)
			throws XmlPullParserException, IOException {
		InputStream stream = null;
		// Instantiate the parser
		MBTAXmlRouteParser mbtaXmlRouteParser = new MBTAXmlRouteParser();

		try {
			stream = downloadUrl(urlString);
			routesModesServingStop = mbtaXmlRouteParser.parse(stream);
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (stream != null) {
				stream.close();
			}
		}

		return "";
	}

	// Given a string representation of a URL, sets up a connection and gets
	// an input stream.
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

	private String routeToId(String route) {
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

	private String routeModeToName(String string) {
		if (string.equals("red")) {
			return "Red Line";
		} else if (string.equals("orange")) {
			return "Orange Line";
		} else if (string.equals("green")) {
			return "Green Line";
		} else if (string.equals("blue")) {
			return "Blue Line";
		} else if (string.equals("bus")) {
			return "Bus";
		} else if (string.equals("subway")) {
			return "Subway";
		} else if (string.equals("rail") || string.equals("commuter rail")) {
			return "Commuter Rail";
		} else if (string.equals("boat") || string.equals("ferry")) {
			return "Boat";
		} else {
			return "";
		}
	}

}