package com.example.wreckanicebeach;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.util.Xml;
import android.view.Menu;
import android.widget.TextView;

public class ShowSchedule extends Activity {
	
	String scheduleByRouteURL = "http://realtime.mbta.com/developer/api/v1/schedulebyroute?api_key=wX9NwuHnZU2ToO7GmGR9uw&route=";
	String xmlString = "no xml here!";

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_schedule);
		
		//Retrieve an XML string from the MBTA API.
		Intent intent = getIntent();
		String routeId = intent.getStringExtra("ROUTE_ID");
		xmlString = callWebService(scheduleByRouteURL + routeId);
		
		//Parse the XML string.
		String displayString = "somethin' bad happened";
		try {
			displayString = parseXML(xmlString);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Create the text view
	    TextView textView = new TextView(this);
	    textView.setTextSize(40);
	    textView.setText(displayString);
	    setContentView(textView);
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
        return xmlString;
    }
	
	private String parseXML(String xmlString) throws XmlPullParserException, IOException {
		InputStream stream = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
		MBTAXmlParser parser = new MBTAXmlParser();
		List<Trip> trips = null;
		StringBuilder tripString = new StringBuilder();
		try {
			trips = parser.parse(stream);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
		for (Trip trip : trips) {
            tripString.append(trip.routeName + "\n");
            tripString.append(trip.direction + "\n");
            tripString.append(trip.tripName + "\n\n");
		}
		return tripString.toString();
	}
	
	
	public class MBTAXmlParser {
		
		public String ns = null;

		public List<Trip> parse(InputStream in) throws XmlPullParserException,
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

		private List<Trip> readFeed(XmlPullParser parser)
				throws XmlPullParserException, IOException {
			
			List<Trip> trips = new ArrayList<Trip>();

			parser.require(XmlPullParser.START_TAG, ns, "trip");
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				// Starts by looking for the trip tag
				if (name.equals("trip")) {
					trips.add(readTrip(parser));
				} else {
					skip(parser);
				}
			}
			return trips;
		}

		// Processes trip tags in the feed.
		private Trip readTrip(XmlPullParser parser) throws IOException,
		XmlPullParserException {
			
			Trip trip = new Trip();
			
			parser.require(XmlPullParser.START_TAG, ns, "trip");
			String tripName = parser.getAttributeValue(null, "trip _name");
			trip.tripName = tripName;
			parser.nextTag();
			//parser.require(XmlPullParser.END_TAG, ns, "stop");
			return trip;
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
	
	public class Trip {
		
		public String routeName;
		public String direction;
		public String tripName;
		
		public Trip() {
			routeName = "nothing";
			direction = "nothing";
			tripName = "nothing";	
		}
	}

}
