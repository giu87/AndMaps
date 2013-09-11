package it.polimi.andmaps;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public abstract class Util {

	/* base url for standard http requests, 10.0.2.2 means localhost */
	public static final String baseURL = "http://andmaps.altervista.org/";

	public static final String geoLocalizationURL = "http://www.maps.google.com/maps/geo?output=xml&key=&0lMet3gclcNmQPYcPtMPEEU3DFe3GjX5jbowm4w&q=";

	/* request codes for intent data propagation */
	public static final int REQUEST_DEFAULT = 0;

	/* result codes for intent data propagation */
	public static final int RESULT_ACTIVECATEGORIES = 1;
	public static final int RESULT_SEARCHBYNAME = 2;
	public static final int RESULT_SEARCHBYADDRESS = 3;


	public static String getURL(String urlString) {

		try {

			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoOutput(true);
			conn.connect();
			InputStream in = conn.getInputStream();

			ByteArrayBuffer buffer = new ByteArrayBuffer(50);
			int current = 0;

			while ((current = in.read()) != -1) {
				buffer.append((byte) current);
			}

			String string = new String(buffer.toByteArray());

			return string;

		} catch (Exception e) {

			return "#ERROR# " + e.getMessage();

		}

	}

	public static boolean downloadAndStoreImage(Context context, String urlString, String fileName) {

		int tries = 15;

		while (tries > 0) {

			try {

				URL url = new URL(urlString);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoInput(true);
				conn.connect();

				int streamLength = conn.getContentLength();
				byte[] stream = new byte[streamLength];

				InputStream in = conn.getInputStream();

				if (in.read(stream) != streamLength) {
					tries--;
					Util.l("Retrying image download (readed bytes are not all bytes)");
					continue;
				}
				in.close();

				FileOutputStream file = context.openFileOutput(fileName, 0);
				file.write(stream);
				file.close();

				return true;

			} catch (Exception e) {

				tries--;
				Util.l("Retrying image download (exception raised)");
				continue;

			}

		}

		return false;

	}

	public static BitmapDrawable loadImage(Context context, String fileName) {

		try {

			FileInputStream file = context.openFileInput(fileName);
			Bitmap bitmap = BitmapFactory.decodeStream(file);

			return new BitmapDrawable(bitmap);

		} catch (Exception e) {

			return null;

		}

	}
	
	public static String capFirst(String string) {
		
		string = string.substring(0, 1).toUpperCase() + string.substring(1);

		return string;
		
	}
	
	
	/* Log debug shortcuts */
	
	public static String l(String s) {
		Log.d("AndMapsDebug", s);
		return s;
	}
	public static boolean l(boolean b) {
		Log.d("AndMapsDebug", new Boolean(b).toString());
		return b;
	}
	public static int l(int i) {
		Log.d("AndMapsDebug", new Integer(i).toString());
		return i;
	}
	public static long l(long l) {
		Log.d("AndMapsDebug", new Long(l).toString());
		return l;
	}
	public static double l(double d) {
		Log.d("AndMapsDebug", new Double(d).toString());
		return d;
	}
	public static Exception l(Exception e) {
		Log.d("AndMapsDebug", e.toString());
		return e;
	}

}
