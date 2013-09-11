package it.polimi.andmaps;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class OverlayItemCollection extends ItemizedOverlay<OverlayItem> {

	private Context context;
	private boolean inDb;

	private List<OverlayItem> items = new ArrayList<OverlayItem>();

	public OverlayItemCollection(Context context, Drawable marker, boolean inDb) {
		super(boundCenterBottom(marker));
		this.context = context;
		this.inDb = inDb;
	}

	@Override
	protected OverlayItem createItem(int i) {
		return items.get(i);
	}

	@Override
	public int size() {
		return items.size();
	}

	public void clear() {
		items.clear();
	}

	public void addOverlay(OverlayItem item) {
		items.add(item);
		populate();
	}

	@Override
	protected boolean onTap(int i) {

		Integer id;

		try {
			id = new Integer(items.get(i).getSnippet());
		} catch (NumberFormatException e) {
			id = null;
		}

		new PopUpInfo(context, id, items.get(i).getTitle(),inDb).show();

		return true;

	}

}