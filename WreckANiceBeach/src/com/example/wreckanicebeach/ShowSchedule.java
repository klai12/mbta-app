package com.example.wreckanicebeach;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

//import com.example.wreckanicebeach.FindStopActivity.MBTAXmlParser;
//import com.example.wreckanicebeach.FindStopActivity.Stop;

import android.media.AudioManager;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.widget.TextView;

public class ShowSchedule extends ActionBarActivity implements TextToSpeech.OnInitListener {

	String scheduleByRouteURL = "http://realtime.mbta.com/developer/api/v1/schedulebyroute?api_key=GF9GdCYGNEmHdd5sZiMzyw&route=";
	String xmlString = "no xml here!";
	private TextToSpeech tts;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_schedule);

		tts = new TextToSpeech(this, this);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// Retrieve an XML string from the MBTA API.
		Bundle myBundle = getIntent().getExtras();
		String routeId = myBundle.getString("ROUTE_ID");
		Log.d("routeId sent to ShowSchedule", routeId);
		xmlString = callWebService(scheduleByRouteURL + routeId);

		// Parse the XML string.
		String displayString = "unaltered display string";
		try {
			displayString = parseXML(xmlString);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create the text view
		TextView textView = new TextView(this);
		textView.setText(displayString);
		setContentView(textView);
		tts.speak(displayString, TextToSpeech.QUEUE_FLUSH, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.show_schedule, menu);
		return true;
	}

	public String callWebService(String query) {
		String xmlString = "nothing";
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet request = new HttpGet(query);
		request.addHeader("accept", "application/xml");
		ResponseHandler<String> handler = new BasicResponseHandler();
		try {
			xmlString = httpclient.execute(request, handler);
		} catch (ClientProtocolException e) {
			xmlString = "in first catch block" + e.getMessage();
			e.printStackTrace();
		} catch (IOException e) {
			xmlString = "in second catch block" + e.getMessage();
			e.printStackTrace();
		}
		httpclient.getConnectionManager().shutdown();
		Log.d("xmlString", xmlString);
		return xmlString;
	}

	@Override
	public void onInit(int status) {
		if (status != TextToSpeech.SUCCESS) {
			TextView textView = (TextView) findViewById(R.id.textview);
			textView.setText("Sorry, text to speech not available.");
		} else {
			Log.d("onInit", "in else block");
		}
	}

	@Override
	protected void onDestroy() {
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
		super.onDestroy();
	}

	private String parseXML(String xmlString) throws XmlPullParserException,
			IOException {
		InputStream stream = new ByteArrayInputStream(
				xmlString.getBytes("UTF-8"));
		MBTAXmlParser parser = new MBTAXmlParser();
		String scheduleInfo = "null name";
		try {
			scheduleInfo = parser.parse(stream);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
		return scheduleInfo;
	}

	public class MBTAXmlParser {

		public String ns = null;

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

		private String readFeed(XmlPullParser parser)
				throws XmlPullParserException, IOException {

			parser.require(XmlPullParser.START_TAG, ns, "schedule");

			String routeName = parser.getAttributeValue(null, "route_name");
			DirectionTrips directionTrips = null;

			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (name.equals("direction")) {
					directionTrips = readDirection(parser);
				} else {
					skip(parser);
				}
			}

			Schedule schedule = new Schedule(routeName, directionTrips);
			return schedule.toString();
		}

		private DirectionTrips readDirection(XmlPullParser parser)
				throws IOException, XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, ns, "direction");
			String directionName = parser.getAttributeValue(null,
					"direction_name");
			ArrayList<String> tripNames = new ArrayList<String>();
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (name.equals("trip")) {
					tripNames.add(readTrip(parser));
				} else {
					skip(parser);
				}
			}
			return new DirectionTrips(directionName, tripNames);
		}

		private String readTrip(XmlPullParser parser) throws IOException,
				XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, ns, "trip");
			String tripName = parser.getAttributeValue(null, "trip_name");
			parser.nextTag();
			return tripName;
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

	public class DirectionTrips {
		public String direction;
		public ArrayList<String> trips;

		public DirectionTrips(String direction, ArrayList<String> trips) {
			this.direction = direction;
			this.trips = trips;
		}
	}

	public class Schedule {

		public String routeName;
		public String direction;
		public ArrayList<String> trips;

		public Schedule(String routeName, String direction,
				ArrayList<String> trips) {
			this.routeName = routeName;
			this.direction = direction;
			this.trips = trips;
		}

		public Schedule(String routeName, DirectionTrips directionTrips) {
			this.routeName = routeName;
			this.direction = directionTrips.direction;
			this.trips = directionTrips.trips;
		}

		public String toString() {
			String s = "";
			s += this.routeName + "\n";
			s += this.direction + "\n\n";
			for (String trip : this.trips) {
				s += trip + "\n\n";
			}
			return s;
		}
	}

}