package com.example.wreckanicebeach;

import java.util.ArrayList;
import java.util.List;

import com.example.wreckanicebeach.FindStopActivity.ErrorDialogFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MapActivity extends ActionBarActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (servicesConnected()) {
			setContentView(R.layout.activity_map);
			// Show the Up button in the action bar.
			setupActionBar();
			setVolumeControlStream(AudioManager.STREAM_MUSIC);

			Bundle myBundle = getIntent().getExtras();
			if (myBundle.getString("ACTIVITY").equals("FindStopActivity")) {
				String stopString = myBundle.getString("STOP_STRING");
				TextView textView = (TextView) findViewById(R.id.textview);
				textView.setText(stopString);
			} else if (myBundle.getString("ACTIVITY").equals(
					"GetDirectionsActivity")) {

			}

			// Get a handle to the Map Fragment
			GoogleMap map = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();

			if (myBundle.getString("ACTIVITY").equals("FindStopActivity")) {
				String latitude = myBundle.getString("LATITUDE");
				String longitude = myBundle.getString("LONGITUDE");
				String name = myBundle.getString("NAME");

				LatLng latLng = new LatLng(Double.parseDouble(latitude),
						Double.parseDouble(longitude));

				map.setMyLocationEnabled(true);
				map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));

				map.addMarker(new MarkerOptions().title(name).position(latLng));
			} else if (myBundle.getString("ACTIVITY").equals(
					"GetDirectionsActivity")) {
				String polyline = myBundle.getString("POLYLINE");
				List<LatLng> decodedPolyline = decodePoly(polyline);
				LatLng[] decodedPolylineArray = new LatLng[decodedPolyline
						.size()];
				decodedPolylineArray = decodedPolyline
						.toArray(decodedPolylineArray);
				double minLat = decodedPolylineArray[0].latitude;
				double maxLat = decodedPolylineArray[0].latitude;
				double minLng = decodedPolylineArray[0].longitude;
				double maxLng = decodedPolylineArray[0].longitude;

				for (LatLng latlng : decodedPolyline) {
					if (latlng.latitude < minLat) {
						minLat = latlng.latitude;
					} else if (latlng.latitude > maxLat) {
						maxLat = latlng.latitude;
					}
					if (latlng.longitude < minLng) {
						minLng = latlng.longitude;
					} else if (latlng.longitude > maxLng) {
						maxLng = latlng.longitude;
					}
				}

				LatLng southwest = new LatLng(minLat, minLng);
				LatLng northeast = new LatLng(maxLat, maxLng);
				LatLngBounds bounds = new LatLngBounds(southwest, northeast);

				Display display = getWindowManager().getDefaultDisplay();

				map.setMyLocationEnabled(true);
				map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,
						(display.getWidth() - 20), (display.getHeight() - 20), 10));
				map.addPolyline(new PolylineOptions().geodesic(true).width(5))
						.setPoints(decodedPolyline);
			}
		} else {
			TextView textView = new TextView(this);
			textView.setText("Sorry, location not available.");
			setContentView(textView);
		}
	}
	
	@Override
	public Intent getSupportParentActivityIntent() {
		Bundle myBundle = getIntent().getExtras();
		if (myBundle.getString("ACTIVITY").equals("FindStopActivity")) {
			Intent intent = new Intent(this, FindStopActivity.class);
			String route = myBundle.getString("ROUTE");
			String subwayRoute = myBundle.getString("SUBWAY_ROUTE");
			String mode = myBundle.getString("MODE");
			
			intent.putExtra("ROUTE", route);
			intent.putExtra("SUBWAY_ROUTE", subwayRoute);
			intent.putExtra("MODE", mode);
			intent.putExtra("MAP_FLAG", false);
			return intent;
		} else {
			Intent intent = new Intent(this, GetDirectionsActivity.class);
			String destination = myBundle.getString("DESTINATION");
			String origin = myBundle.getString("ORIGIN");
			
			intent.putExtra("DESTINATION", destination);
			intent.putExtra("ORIGIN", origin);
			intent.putExtra("MAP_FLAG", false);
			return intent;
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
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
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

	/*
	 * decodePoly adapted from
	 * http://jeffreysambells.com/2010/05/27/decoding-polylines
	 * -from-google-maps-direction-api-with-java
	 */
	private List<LatLng> decodePoly(String encoded) {

		List<LatLng> poly = new ArrayList<LatLng>();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;

		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
			poly.add(p);
		}

		return poly;
	}

}