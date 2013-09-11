package it.polimi.andmaps;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchResultItem implements Parcelable {

	private String name;
	private int category;
	private String address;
	private double lat;
	private double lon;

	public SearchResultItem(String name, int type, String address, double lat, double lon) {
		this.name = name;
		this.category = type;
		this.address = address;
		this.lat = lat;
		this.lon = lon;
	}

	public String getName() {
		return name;
	}

	public int getCategory() {
		return category;
	}

	public String getAddress() {
		return address;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name);
		out.writeInt(category);
		out.writeString(address);
		out.writeDouble(lat);
		out.writeDouble(lon);
	}

	public int describeContents() {
		return 0;
	}

	public static Parcelable.Creator<SearchResultItem> CREATOR = new Parcelable.Creator<SearchResultItem>() {
		
		public SearchResultItem createFromParcel(Parcel in) {
			SearchResultItem result = new SearchResultItem(in.readString(),
														   in.readInt(),
														   in.readString(),
														   in.readDouble(),
														   in.readDouble());
			return result;
		}

		public SearchResultItem[] newArray(int size) {
			return null;
		}
		
	};

}