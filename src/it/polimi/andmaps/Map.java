package it.polimi.andmaps;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class Map extends MapActivity {

	public OptimizedMapView mapView;

	private MapController mapCtrl;

	private List<Overlay> mapOverlays;
	private List<MyOverLay> routeOverlays;
	private OverlayItemCollection currentPos, lastSearch;

	private DbAdapter db;

	private List<Category> allCategories = new ArrayList<Category>();

	private MapLocationListener locationListener;
	private LocationManager locationManager;
	private boolean GPSActive = false;
	private GeoPoint gpsPosition;
	private Integer route_id;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		initMap();
	}

	private void initMap() {

		mapView = (OptimizedMapView) findViewById(R.id.map_view);
		mapCtrl = mapView.getController();

		// Setting up zoom controls
		mapView.setBuiltInZoomControls(false);
		((LinearLayout) findViewById(R.id.zoom_container))
				.addView(new MapZoomControls(this, mapView));

		double[] startPos = { 45.482281, 9.229481 };

		mapCtrl.setZoom(15);
		mapCtrl.animateTo(new GeoPoint((int) (startPos[0] * 1E6),
				(int) (startPos[1] * 1E6)));

		// Creating overlays for current gps position and last search
		BitmapDrawable img = (BitmapDrawable) getResources().getDrawable(
				R.drawable.dot);
		// img.setBounds(0,0,10,10);

		currentPos = new OverlayItemCollection(this, img, false);
		lastSearch = new OverlayItemCollection(this, getResources()
				.getDrawable(R.drawable.bubble), false);
		routeOverlays = new ArrayList<MyOverLay>();
		
		// Creating overlays for each category
		db = new DbAdapter(this);
		db.open();

		Cursor c = db.getAllCategories();
		if (c.moveToFirst()) {
			do
				allCategories.add(new Category(c.getInt(0), c.getString(1),
						true, new OverlayItemCollection(this, Util.loadImage(
								this, c.getInt(0) + ".png"), true)));
			while (c.moveToNext());
		}
		c.close();
		db.close();

		// TODO: painting current area overlay items
		double[] init = { 45.4904470, 9.2130300, 45.4645690, 9.2404940 };
		mapView.computeArea(init);
		addOverlays();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.mylocation:
			showMyGpsPosition();
			break;

		case R.id.search:
			Intent search = new Intent(this, Search.class);
			search.putExtra("activecategories", getActiveCategories());
			startActivityForResult(search, Util.REQUEST_DEFAULT);
			break;

		case R.id.categories:

			Intent categories = new Intent(this, CategorySelector.class);
			categories.putExtra("allownoselection", true);
			categories.putExtra("activecategories", getActiveCategories());
			startActivityForResult(categories, Util.REQUEST_DEFAULT);
			break;

		case R.id.favorites:

			db.open();

			Cursor i = db.getAllFavorites();
			if (i.moveToFirst()) {
				ArrayList<Integer> idsArray = new ArrayList<Integer>();
				do {
					idsArray.add(i.getInt(0));
				} while (i.moveToNext());
				i.close();
				Cursor p = db.getFavInfo(idsArray);
				ArrayList<SearchResultItem> itemsFound = new ArrayList<SearchResultItem>();
				if (p.moveToFirst())
					do
						itemsFound.add(new SearchResultItem(p.getString(1), p
								.getInt(2), p.getString(3), p.getDouble(4), p
								.getDouble(5)));
					while (p.moveToNext());
				p.close();
				db.close();
				Bundle bundle = new Bundle();
				bundle.putParcelableArrayList("searchresults", itemsFound);
				bundle.putBoolean("pref", true);
				Intent resultList = new Intent(this, SearchResultList.class);
				resultList.putExtras(bundle);
				startActivityForResult(resultList, Util.RESULT_SEARCHBYNAME);
			} else {
				i.close();
				db.close();
				Toast.makeText(this, getResources().getString(R.string.nofavs_text),
						Toast.LENGTH_LONG).show();
			}

			break;
		}
		return false;
	}

	// TODO: controllare
	private void showMyGpsPosition() {
		GPSActive = true;
		locationListener = new MapLocationListener(this);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, locationListener);
		Location gpsLocation = locationManager.getLastKnownLocation("gps");

		setCurrentPos(gpsLocation);
	}

	// TODO: is it really needed?
	private ArrayList<Integer> getActiveCategories() {

		ArrayList<Integer> activeCategories = new ArrayList<Integer>();
		for (Category cat : allCategories)
			if (cat.isActive())
				activeCategories.add(cat.getId());

		return activeCategories;

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		super.onActivityResult(requestCode, resultCode, intent);

		switch (resultCode) {

		case Util.RESULT_ACTIVECATEGORIES:

			ArrayList<Integer> result = intent
					.getIntegerArrayListExtra("activecategories");

			for (Category cat : allCategories) {
				if (cat.isActive() != result.contains(cat.getId())) {
					cat.setActive(!cat.isActive());
					if (cat.isActive()) {
						if (cat.getLinkedOverlay().size() > 0) {
							Util.l("ADDING OVERLAY " + cat.getName());
							mapOverlays.add(cat.getLinkedOverlay());
						} else {
							Util.l("OVERLAY " + cat.getName()
									+ " WON'T BE ADDED BECAUSE IT'S ZERO SIZE");
						}
					} else {
						if (mapOverlays.contains(cat.getLinkedOverlay())) {
							Util.l("DROPPING OVERLAY " + cat.getName());
							mapOverlays.remove(cat.getLinkedOverlay());
						} else {
							Util
									.l("OVERLAY "
											+ cat.getName()
											+ " WON'T BE DROPPED BECAUSE IT HAS NEVER BEEN ADDED");
						}
					}
				} else {
					Util.l("NO CHANGE FOR OVERLAY " + cat.getName());
				}
			}
			break;

		case Util.RESULT_SEARCHBYNAME:

		{
			Bundle extras = intent.getExtras();

			int category = extras.getInt("category");

			for (Category cat : allCategories)
				if (cat.getId() == category && !cat.isActive()) {
					cat.setActive(true);
					Util.l("ADDING OVERLAY " + cat.getName());
					mapOverlays.add(cat.getLinkedOverlay());
					break;
				}

			double lat = extras.getDouble("lat");
			double lon = extras.getDouble("lon");

			double diff_x = mapView.getProjection().fromPixels(
					mapView.getRight(), 0).getLongitudeE6()
					- mapView.getProjection().fromPixels(0, 0).getLongitudeE6();
			double diff_y = mapView.getProjection().fromPixels(0, 0)
					.getLatitudeE6()
					- mapView.getProjection()
							.fromPixels(0, mapView.getBottom()).getLatitudeE6();

			GeoPoint itemPosition = new GeoPoint((int) (lat * 1E6),
					(int) (lon * 1E6));
			mapCtrl.animateTo(itemPosition);

			double x = (itemPosition.getLongitudeE6() - diff_x / 2) / 1E6;
			double x2 = (itemPosition.getLongitudeE6() + diff_x / 2) / 1E6;
			double y = (itemPosition.getLatitudeE6() + diff_y / 2) / 1E6;
			double y2 = (itemPosition.getLatitudeE6() - diff_y / 2) / 1E6;
			double[] pos = { y, x, y2, x2 };

			if (!mapView.isInArea(pos)) {
				mapView.computeArea(pos);
				addOverlays();
			}
		}
			break;

		case Util.RESULT_SEARCHBYADDRESS:

		{
			Bundle extras = intent.getExtras();
			lastSearch.clear();
			String address = extras.getString("address");
			double lat = extras.getDouble("lat");
			double lon = extras.getDouble("lon");

			double diff_x = mapView.getProjection().fromPixels(
					mapView.getRight(), 0).getLongitudeE6()
					- mapView.getProjection().fromPixels(0, 0).getLongitudeE6();
			double diff_y = mapView.getProjection().fromPixels(0, 0)
					.getLatitudeE6()
					- mapView.getProjection()
							.fromPixels(0, mapView.getBottom()).getLatitudeE6();

			GeoPoint addrPosition = new GeoPoint((int) (lat * 1E6),
					(int) (lon * 1E6));
			mapCtrl.animateTo(addrPosition);

			OverlayItem addrOverlay = new OverlayItem(addrPosition, address,
					null);
			lastSearch.addOverlay(addrOverlay);
			mapOverlays.add(lastSearch);

			double x = (addrPosition.getLongitudeE6() - diff_x / 2) / 1E6;
			double x2 = (addrPosition.getLongitudeE6() + diff_x / 2) / 1E6;
			double y = (addrPosition.getLatitudeE6() + diff_y / 2) / 1E6;
			double y2 = (addrPosition.getLatitudeE6() - diff_y / 2) / 1E6;
			double[] pos = { y, x, y2, x2 };

			if (!mapView.isInArea(pos)) {
				mapView.computeArea(pos);
				addOverlays();
			}
			Util.l("lastSearch.size() = " + lastSearch.size());

		}
			break;

		case RESULT_CANCELED:

			return;

		}

	}

	// TODO: maybe there's a way to make it private
	public void computeOverlays(double lat, double lon, double side) {

		for (Category cat : allCategories)
			cat.getLinkedOverlay().clear();

		db.open();
		Cursor c = db.getItemsInArea(lat, lon, side);
		if (c.moveToFirst()) {
			do {
				addOverlayItem(c);
			} while (c.moveToNext());
		}
		c.close();
		db.close();
	}

	// TODO: maybe there's a way to make it private
	public void addOverlays() {

		// TODO: why clearing them?
		mapOverlays = mapView.getOverlays();
		mapOverlays.clear();

		// TODO: check!
		if (currentPos.size() != 0)
			mapOverlays.add(currentPos);

		if (lastSearch.size() != 0)
			mapOverlays.add(lastSearch);
		
		if(routeOverlays.size() != 0) {
			Iterator<MyOverLay> it = routeOverlays.iterator();
			while (it.hasNext()){
				mapOverlays.add(it.next());
			}
		}

		for (Category cat : allCategories) {
			if (cat.getLinkedOverlay().size() > 0 && cat.isActive()) {
				Util.l("Adding overlay " + cat.getName()
						+ " to map: ADDED (size is: "
						+ cat.getLinkedOverlay().size() + ")");
				mapOverlays.add(cat.getLinkedOverlay());
			} else {
				Util.l("Adding overlay " + cat.getName()
						+ " to map: NOT ADDED (zero size OR noActive)");
			}
		}
	}

	// TODO: check!
	private void addOverlayItem(Cursor c) {

		Double lat = c.getDouble(4) * 1E6;
		Double lon = c.getDouble(5) * 1E6;

		GeoPoint point = new GeoPoint(lat.intValue(), lon.intValue());
		OverlayItem item = new OverlayItem(point, c.getString(1), new Integer(c
				.getInt(0)).toString());

		int category = c.getInt(2);

		for (Category cat : allCategories)
			if (cat.getId() == category) {
				Util.l("Painting overlay " + c.getString(1) + " (Cat "
						+ cat.getName() + ", Add " + c.getString(3) + ", Lat "
						+ c.getDouble(4) + ", Long " + c.getDouble(5) + ")");
				cat.getLinkedOverlay().addOverlay(item);
				break;
			}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private class MapLocationListener implements LocationListener {

		Context context;

		public MapLocationListener(Context context) {
			this.context = context;
		}

		public void onLocationChanged(Location location) {
			if (GPSActive)
				setCurrentPos(location);
			if (routeOverlays.size()!=0){
				routeFactory(route_id);
			}
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

	}

	private void setCurrentPos(Location gpsLocation) {

		currentPos.clear();

		if (gpsLocation == null) {
			Toast
					.makeText(this, "Impossible to connect GPS",
							Toast.LENGTH_LONG).show();

		} else {

			double diff_x = mapView.getProjection().fromPixels(
					mapView.getRight(), 0).getLongitudeE6()
					- mapView.getProjection().fromPixels(0, 0).getLongitudeE6();
			double diff_y = mapView.getProjection().fromPixels(0, 0)
					.getLatitudeE6()
					- mapView.getProjection()
							.fromPixels(0, mapView.getBottom()).getLatitudeE6();

			gpsPosition = new GeoPoint((int) (gpsLocation
					.getLatitude() * 1E6),
					(int) (gpsLocation.getLongitude() * 1E6));
			
			double x = (gpsPosition.getLongitudeE6() - diff_x / 2) / 1E6;
			double x2 = (gpsPosition.getLongitudeE6() + diff_x / 2) / 1E6;
			double y = (gpsPosition.getLatitudeE6() + diff_y / 2) / 1E6;
			double y2 = (gpsPosition.getLatitudeE6() - diff_y / 2) / 1E6;
			double[] pos = { y, x, y2, x2 };
			if(mapView.inDistrict(pos) == 0){

				mapCtrl.animateTo(gpsPosition);

				OverlayItem gpsOverlay = new OverlayItem(gpsPosition,
						getResources().getString(R.string.currentposition_text),
						null);
				currentPos.addOverlay(gpsOverlay);
				mapOverlays.add(currentPos);
			
				if (!mapView.isInArea(pos)) {
					mapView.computeArea(pos);
					addOverlays();
				}
			}
			else {
				routeOverlays.clear();
				addOverlays();
				mapView.invalidate();
				Toast.makeText(this,
						getResources().getString(R.string.outmilano_text),
						Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public boolean routeFactory(Integer id) {
		
		routeOverlays.clear();
		
		if (currentPos.size() != 0) {

			double [] latlon = new double [2];
	
			DbAdapter db = new DbAdapter(this);
			db.open();
			
			latlon = db.getLatLon(id);
			route_id = id;
					
			routeOverlays = mapView.drawPath(gpsPosition,new GeoPoint((int) (latlon[0] *1E6), (int) (latlon[1]*1E6)),1,mapView);

			addOverlays();
			mapView.invalidate();
			return true;
		}
		
		Toast.makeText(this, "GPS non attivo", Toast.LENGTH_LONG).show();
		return false;
	}
}