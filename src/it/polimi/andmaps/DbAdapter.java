package it.polimi.andmaps;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {

	public static final String KEY_ID = "id";
	public static final String KEY_NAME = "nome";
	public static final String KEY_CATEGORY = "tipo";
	public static final String KEY_ADDRESS = "indirizzo";
	public static final String KEY_LAT = "lat";
	public static final String KEY_LON = "lon";

	private static final String KEY_VERSION = "ver";

	private static final String DATABASE_NAME = "app_db";
	private static final String DATABASE_POI_TABLE = "poi";
	private static final String DATABASE_VER_TABLE = "ver";
	private static final String DATABASE_CAT_TABLE = "cat";
	private static final String DATABASE_FAV_TABLE = "fav";

	private static final String DATABASE_CREATE_POI_TABLE = "create table "
			+ DATABASE_POI_TABLE + "(" + KEY_ID + " integer primary key, "
			+ KEY_NAME + " text not null, " + KEY_CATEGORY + " integer, "
			+ KEY_ADDRESS + " text not null, " + KEY_LAT + " double, "
			+ KEY_LON + " double);";

	private static final String DATABASE_CREATE_VER_TABLE = "create table "
			+ DATABASE_VER_TABLE + "(" + KEY_ID + " integer primary key, "
			+ KEY_VERSION + " integer);";

	private static final String DATABASE_CREATE_CAT_TABLE = "create table "
			+ DATABASE_CAT_TABLE + "(" + KEY_ID + " integer primary key, "
			+ KEY_NAME + " text not null);";

	private static final String DATABASE_CREATE_FAV_TABLE = "create table "
			+ DATABASE_FAV_TABLE + "(" + KEY_ID + " integer primary key);";

	private static final String DATABASE_DROP_POI_TABLE =
			"drop table " + DATABASE_POI_TABLE + ";";
	
	private static final String DATABASE_DROP_VER_TABLE =
			"drop table " + DATABASE_VER_TABLE + ";";
	
	private static final String DATABASE_DROP_CAT_TABLE =
			"drop table " + DATABASE_CAT_TABLE + ";";
	
	//private static final String DATABASE_DROP_FAV_TABLE =
		//	"drop table " + DATABASE_FAV_TABLE + ";";


	private DatabaseHelper dbHelper;
	public SQLiteDatabase db;


	public DbAdapter(Context context) {
		dbHelper = new DatabaseHelper(context);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE_POI_TABLE);
			db.execSQL(DATABASE_CREATE_VER_TABLE);
			db.execSQL(DATABASE_CREATE_CAT_TABLE);
			db.execSQL(DATABASE_CREATE_FAV_TABLE);
			// Sets current db version to zero
			ContentValues zeroUpdate = new ContentValues();
			zeroUpdate.put(KEY_ID, 0);
			zeroUpdate.put(KEY_VERSION, 0);
			db.insert(DATABASE_VER_TABLE, null, zeroUpdate);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			return;
		}
	}

	public void reset() {
		
		Util.l("Resetting db");

		// destroy
		db.execSQL(DATABASE_DROP_POI_TABLE);
		db.execSQL(DATABASE_DROP_VER_TABLE);
		db.execSQL(DATABASE_DROP_CAT_TABLE);
		//db.execSQL(DATABASE_DROP_FAV_TABLE);

		// create
		db.execSQL(DATABASE_CREATE_POI_TABLE);
		db.execSQL(DATABASE_CREATE_VER_TABLE);
		db.execSQL(DATABASE_CREATE_CAT_TABLE);
		//db.execSQL(DATABASE_CREATE_FAV_TABLE);

		// sets current db version to zero
		ContentValues zeroUpdate = new ContentValues();
		zeroUpdate.put(KEY_ID, 0);
		zeroUpdate.put(KEY_VERSION, 0);
		db.insert(DATABASE_VER_TABLE, null, zeroUpdate);

	}

	// open/close db functions

	public DbAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbHelper.close();
	}
	
	
	// items management functions

	public long insertItem(int id, String name, int category, String address,
			double lat, double lon) {
		ContentValues values = new ContentValues();
		values.put(KEY_ID, id);
		values.put(KEY_NAME, name);
		values.put(KEY_CATEGORY, category);
		values.put(KEY_ADDRESS, address);
		values.put(KEY_LAT, lat);
		values.put(KEY_LON, lon);
		return db.insert(DATABASE_POI_TABLE, null, values);
	}

	public int deleteItem(int id) {
		return db.delete(DATABASE_POI_TABLE, KEY_ID + "=" + id, null);
	}
	
	public int updateItem(int id, String name, int category, String address,
			double lat, double lon) {
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, name);
		values.put(KEY_CATEGORY, category);
		values.put(KEY_ADDRESS, address);
		values.put(KEY_LAT, lat);
		values.put(KEY_LON, lon);
		return db.update(DATABASE_POI_TABLE, values, KEY_ID + " = " + id, null);
	}

	public Cursor getAllItems() {
		return db.query(
				DATABASE_POI_TABLE,
				new String[] { KEY_ID, KEY_NAME, KEY_CATEGORY, KEY_ADDRESS, KEY_LAT, KEY_LON },
				null, null, null, null, null);
	}

	public Cursor getItemsInArea(double lat, double lon, double side) {
		return db.query(
				DATABASE_POI_TABLE,
				new String[] { KEY_ID, KEY_NAME, KEY_CATEGORY, KEY_ADDRESS, KEY_LAT, KEY_LON },
				KEY_LAT + " > " + lat + " AND " + KEY_LAT + " < " + (lat + side) + " AND " +
				KEY_LON + " > " + lon + " AND " + KEY_LON + " < " + (lon + side),
				null, null, null, null);
	}

	public Cursor getItemsLike(String searchTerm, ArrayList<Integer> categoriesArray) {
		String categories = "";
		if (categoriesArray != null) {
			for (Integer catIndex : categoriesArray)
				if (categories == "")
					categories += catIndex;
				else
					categories += ", " + catIndex;
			categories = " AND " + KEY_CATEGORY + " IN (" + categories + ")";
		}
		return db.query(
				DATABASE_POI_TABLE,
				new String[] { KEY_ID, KEY_NAME, KEY_CATEGORY, KEY_ADDRESS, KEY_LAT, KEY_LON },
				KEY_NAME + " like '%" + searchTerm + "%'" + categories,
				null, null, null, null);
	}

	// categories management functions
	
	public long insertCategory(int id, String name) {
		ContentValues values = new ContentValues();
		values.put(KEY_ID, id);
		values.put(KEY_NAME, name);
		return db.insert(DATABASE_CAT_TABLE, null, values);
	}

	public int deleteCategory(int id) {
		return db.delete(DATABASE_CAT_TABLE, KEY_ID + "=" + id, null);
	}
	
	public int updateCategory(int id, String nome) {
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, nome);
		return db.update(DATABASE_CAT_TABLE, values, KEY_ID + " = " + id, null);
	}

	public Cursor getAllCategories() {
		return db.query(
			DATABASE_CAT_TABLE,
			new String[] {KEY_ID, KEY_NAME},
			null, null, null, null, null);
	}
	
	
	// db version management functions

	public int getLastUpdateVersion() {
		Cursor lastUpdate = db.query(
			DATABASE_VER_TABLE,
			new String[] {KEY_VERSION},
			KEY_ID + " = 0",
			null, null, null, null);
		lastUpdate.moveToFirst();
		int version = lastUpdate.getInt(0);
		lastUpdate.close();
		return version;
	}

	public int setLastUpdateVersion(int lastUpdateVersion) {
		ContentValues lastUpdateValues = new ContentValues();
		lastUpdateValues.put(KEY_VERSION, lastUpdateVersion);
		return db.update(DATABASE_VER_TABLE, lastUpdateValues, KEY_ID + " = 0", null);
	}


	// favorites management functions

	public long addFav(int id) {
		ContentValues values = new ContentValues();
		values.put(KEY_ID, id);
		return db.insert(DATABASE_FAV_TABLE, null, values);
	}

	public int delFav(Integer id) {
		return db.delete(DATABASE_FAV_TABLE, KEY_ID + "=" + id, null);
	}

	public Cursor getAllFavorites() {
		return db.query(DATABASE_FAV_TABLE, null, null, null, null, null, null);
	}
	
	public boolean isFav(int id) {
		Cursor c = db.query(
				DATABASE_FAV_TABLE,
				new String[] { KEY_ID },
				KEY_ID + " = " + id,
				null, null, null, null);
		boolean fav = c.moveToFirst();
		c.close();
		return fav;
	}

	public Cursor getFavInfo(ArrayList<Integer> idsArray) {
		String ids = "";
		if (idsArray != null) {
			for (Integer idIndex : idsArray)
				if (ids == "")
					ids += idIndex;
				else
					ids += ", " + idIndex;
			ids = KEY_ID + " IN (" + ids + ")";
		}
		return db.query(
				DATABASE_POI_TABLE,
				null,
				ids,
				null, null, null, null);
	}

	public double[] getLatLon(Integer id) {

		Cursor c = db.query(DATABASE_POI_TABLE, 
				new String[] {KEY_LAT, KEY_LON},
				"id = "+id, null, null, null, null);
		if(c.moveToFirst()){
			double [] temp = new double[2];
			temp[0] = Double.parseDouble(c.getString(0));
			temp[1] = Double.parseDouble(c.getString(1));
			return temp;
		}
		return null;
	}

}