package it.polimi.andmaps;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class OptimizedMapView extends MapView {

	private Map context;

	public double minLat;
	public double minLon;
	public double side;

	public OptimizedMapView(Context context, AttributeSet attrs) {

		super(context, attrs);
		this.context = (Map) context;

	}

	public boolean onTouchEvent(MotionEvent event) {

		double[] bounds = getBounds();

		int inD = inDistrict(bounds);
		if(inD == 0) {
		
			if (!isInArea(bounds)) {
				computeArea(bounds);
				context.addOverlays();
			}
		}
		else {
			returnOnDistrict(this,inD);
			//Toast.makeText(context, "out of Milano", Toast.LENGTH_LONG).show();
		}
		return super.onTouchEvent(event);

	}

	public int inDistrict(double[] bounds) {

		if(bounds[1] < 8.7)
			return 1;
		if(bounds[0] > 45.63)
			return 2;
		if(bounds[3] > 9.54)
			return 3;
		if(bounds[2] < 45.27)
			return 4;
		return 0;
	}

	public double[] getBounds() {

		GeoPoint topLeft = getProjection().fromPixels(0, 0);
		GeoPoint bottomRight = getProjection().fromPixels(getRight(), getBottom());

		double[] bounds = new double[4];
		bounds[0] = topLeft.getLatitudeE6() / 1E6;
		bounds[1] = topLeft.getLongitudeE6() / 1E6;
		bounds[2] = bottomRight.getLatitudeE6() / 1E6;
		bounds[3] = bottomRight.getLongitudeE6() / 1E6;
		
		// Util.l("x = "+bounds[1]+", y = "+bounds[0]+", x2 = "+bounds[3]+", y2 = "+bounds[2]);

		return bounds;

	}

	public boolean isInArea(double[] bounds) {

		return (bounds[0] < (minLat + side) && bounds[2] > minLat &&
				bounds[1] > minLon && bounds[3] < (minLon + side));

	}

	public void computeArea(double[] bounds) {
		
		minLat = (bounds[2] - 4 * (bounds[0] - bounds[2]));
		minLon = ((bounds[1] + bounds[3]) - 9 * (bounds[0] - bounds[2])) / 2;
		side = (9 * (bounds[0] - bounds[2]));
		
		Util.l("AREA CALCOLATA:  long = "+minLon+", lat = "+minLat+", lato = "+side);

		context.computeOverlays(minLat, minLon, side);

	}
	
	public void returnOnDistrict (OptimizedMapView map, int inD) {
		double diff_x = map.getProjection().fromPixels(
				map.getRight(), 0).getLongitudeE6()
				- map.getProjection().fromPixels(0, 0).getLongitudeE6();
		double diff_y = map.getProjection().fromPixels(0, 0)
				.getLatitudeE6()
				- map.getProjection()
						.fromPixels(0, map.getBottom()).getLatitudeE6();
		
		GeoPoint geo;
		switch (inD) {
		case 1:
			geo = new GeoPoint(map.getProjection().fromPixels(0, (int) (map.getBottom()/2)).getLatitudeE6(),(int) (8.7*1E6 + diff_x/2));
			this.getController().animateTo(geo);
			break;
		case 2:
			geo = new GeoPoint((int) (45.63 * 1E6 - diff_y/2),map.getProjection().fromPixels((int) (map.getRight()/2),0).getLongitudeE6());
			this.getController().animateTo(geo);
			break;
		case 3:
			geo = new GeoPoint(map.getProjection().fromPixels(0, (int) (map.getBottom()/2)).getLatitudeE6(),(int) (9.54 *1E6 - diff_x/2));
			this.getController().animateTo(geo);
			break;
		case 4:
			geo = new GeoPoint((int) (45.27 * 1E6 + diff_y/2),map.getProjection().fromPixels((int) (map.getRight()/2),0).getLongitudeE6());
			this.getController().animateTo(geo);
			break;

		default:
			break;
		}
	}
	
	public ArrayList<MyOverLay> drawPath(GeoPoint src, GeoPoint dest, int color, MapView mMapView01) {
		
		ArrayList<MyOverLay> items = new ArrayList<MyOverLay>();
		
		// connect to map web service
		StringBuilder urlString = new StringBuilder();
		urlString.append("http://maps.google.com/maps?f=d&hl=en");
		urlString.append("&saddr=");// from
		urlString.append(Double.toString((double) src.getLatitudeE6() / 1.0E6));
		urlString.append(",");
		urlString
				.append(Double.toString((double) src.getLongitudeE6() / 1.0E6));
		urlString.append("&daddr=");// to
		urlString
				.append(Double.toString((double) dest.getLatitudeE6() / 1.0E6));
		urlString.append(",");
		urlString.append(Double
				.toString((double) dest.getLongitudeE6() / 1.0E6));
		urlString.append("&ie=UTF8&0&om=0&output=kml");
		Log.d("xxx", "URL=" + urlString.toString());
		// get the kml (XML) doc. And parse it to get the coordinates(direction
		// route).
		Document doc = null;
		HttpURLConnection urlConnection = null;
		URL url = null;
		try {
			url = new URL(urlString.toString());
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.connect();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(urlConnection.getInputStream());

			if (doc.getElementsByTagName("GeometryCollection").getLength() > 0) {
				// String path =
				// doc.getElementsByTagName("GeometryCollection").item(0).getFirstChild().getFirstChild().getNodeName();
				String path = doc.getElementsByTagName("GeometryCollection")
						.item(0).getFirstChild().getFirstChild()
						.getFirstChild().getNodeValue();
				Log.d("xxx", "path=" + path);
				String[] pairs = path.split(" ");
				String[] lngLat = pairs[0].split(","); // lngLat[0]=longitude
				// lngLat[1]=latitude
				// lngLat[2]=height
				// src
				GeoPoint startGP = new GeoPoint((int) (Double
						.parseDouble(lngLat[1]) * 1E6), (int) (Double
						.parseDouble(lngLat[0]) * 1E6));
			
				GeoPoint gp1;
				GeoPoint gp2 = startGP;
				for (int i = 1; i < pairs.length; i++) // the last one would be
				// crash
				{
					lngLat = pairs[i].split(",");
					gp1 = gp2;
					// watch out! For GeoPoint, first:latitude, second:longitude
					gp2 = new GeoPoint(
							(int) (Double.parseDouble(lngLat[1]) * 1E6),
							(int) (Double.parseDouble(lngLat[0]) * 1E6));
					items.add(
							new MyOverLay(gp1, gp2, 2));
					Log.d("xxx", "pair:" + pairs[i]);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		
		return items;
	}
}