package it.polimi.andmaps;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CategorySelector extends ListActivity implements OnClickListener {

	private DbAdapter db = new DbAdapter(this);
	private TypeAdapter adapter;
	private ArrayList<Category> allCategories;
	private boolean allowNoSelection;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.categoryselector);

		allowNoSelection = getIntent()
				.getExtras().getBoolean("allownoselection");

		ArrayList<Integer> activeCategories = getIntent()
				.getIntegerArrayListExtra("activecategories");

		allCategories = new ArrayList<Category>();
		db.open();
		Cursor c = db.getAllCategories();
		if (c.moveToFirst()) {
			do {
				allCategories.add(new Category(c.getInt(0),
											   c.getString(1),
											   activeCategories.contains(c.getInt(0)),
											   null));
			} while (c.moveToNext());
		}
		c.close();
		db.close();

		adapter = new TypeAdapter(this, R.layout.category);
		setListAdapter(adapter);

		findViewById(R.id.ok_button).setOnClickListener(this);

	}

	public void onClick(View clickedView) {
		if (!allowNoSelection && adapter.getActiveCategories().size() == 0) {
			Toast.makeText(this,
					getResources().getString(R.string.noselectionerror_text), 3).show();
		} else {
			Intent result = new Intent();
			result.putExtra("activecategories", adapter.getActiveCategories());
			setResult(Util.RESULT_ACTIVECATEGORIES, result);
			finish();
		}
	}

	private class TypeAdapter extends ArrayAdapter<Category> {

		private Context context;
		private int viewResourceId;

		public TypeAdapter(Context context, int viewResourceId) {

			super(context, viewResourceId, allCategories);
			this.context = context;
			this.viewResourceId = viewResourceId;

		}

		public ArrayList<Integer> getActiveCategories() {

			ArrayList<Integer> activeCategories = new ArrayList<Integer>();
			for (Category cat : allCategories)
				if (cat.isActive())
					activeCategories.add(cat.getId());

			return activeCategories;

		}

		public View getView(int position, View convertView, ViewGroup parent) {

			View v = convertView;
			final Category i = allCategories.get(position);

			if (v == null) {

				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(viewResourceId, null);
				v.setClickable(false);

			}

			if (i != null) {

				ImageView image = (ImageView) v.findViewById(R.id.category_image);
				image.setImageDrawable(Util.loadImage(context, i.getId() + ".png"));

				TextView name = (TextView) v.findViewById(R.id.categoryname_text);
				name.setText(i.getName());
				
				CheckBox flag = (CheckBox) v.findViewById(R.id.active_flag);
				flag.setChecked(i.isActive());
				
				flag.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						i.setActive(isChecked);
					}
				});

			}

			return v;
		}

	}

}