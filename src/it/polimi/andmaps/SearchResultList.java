package it.polimi.andmaps;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchResultList extends ListActivity {

	private SearchResultItemAdapter adapter;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.searchresultlist);

		Bundle bundle = getIntent().getExtras();
		ArrayList<SearchResultItem> results = bundle.getParcelableArrayList("searchresults");
		
		adapter = new SearchResultItemAdapter(this, R.layout.searchresultitem, results);
		setListAdapter(adapter);

	}

	private class SearchResultItemAdapter extends ArrayAdapter<SearchResultItem> {

		private Context context;
		private int viewResourceId;
		private ArrayList<SearchResultItem> items;

		public SearchResultItemAdapter(Context context, int viewResourceId,
				ArrayList<SearchResultItem> items) {

			super(context, viewResourceId, items);
			this.context = context;
			this.viewResourceId = viewResourceId;
			this.items = items;

		}

		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final SearchResultItem i = items.get(position);

			if (view == null) {

				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(viewResourceId, null);

				view.setOnClickListener(new View.OnClickListener() {
					public void onClick(View clickedView) {
						Intent result = new Intent();
						result.putExtra("category", i.getCategory());
						result.putExtra("lat", i.getLat());
						result.putExtra("lon", i.getLon());
						setResult(Util.RESULT_SEARCHBYNAME, result);
						finish();
					}
				});

			}

			if (i != null) {

				ImageView image = (ImageView) view.findViewById(R.id.itemcategory_image);
				image.setImageDrawable(Util.loadImage(context, i.getCategory() + ".png"));

				TextView name = (TextView) view.findViewById(R.id.itemname_text);
				name.setText(i.getName());

				TextView address = (TextView) view.findViewById(R.id.itemaddress_text);
				address.setText(i.getAddress());

			}

			return view;
			
		}

	}

}