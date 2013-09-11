package it.polimi.andmaps;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class Search extends Activity implements OnClickListener {

	private EditText searchTerm;
	
	private RadioGroup searchRadios;
	private RadioGroup categoryRadios;
	
	private ArrayList<Integer> activeCategories;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		activeCategories = getIntent().getIntegerArrayListExtra("activecategories");

		findViewById(R.id.byaddress_radio).setOnClickListener(this);
		findViewById(R.id.byname_radio).setOnClickListener(this);
		
		findViewById(R.id.selectcategories_radio).setOnClickListener(this);
		
		findViewById(R.id.search_button).setOnClickListener(this);

		searchTerm = (EditText) findViewById(R.id.searchterm_edittext);

		searchRadios = (RadioGroup) findViewById(R.id.search_radiogroup);
		categoryRadios = (RadioGroup) findViewById(R.id.category_radiogroup);

	}

	public void onClick(View clickedView) {

		switch (clickedView.getId()) {
		case R.id.byaddress_radio:
			categoryRadios.setVisibility(View.GONE);
			break;
		case R.id.byname_radio:
			categoryRadios.setVisibility(View.VISIBLE);
			break;
		case R.id.selectcategories_radio:
			Intent categories = new Intent(this, CategorySelector.class);
			categories.putExtra("allownoselection", false);
			categories.putExtra("activecategories", activeCategories);
			startActivityForResult(categories, Util.REQUEST_DEFAULT);
			break;
		case R.id.search_button:
			doSearch();
			break;
		}

	}

	private void doSearch() {

		if (searchTerm.getText().length() < 2) {
			Toast.makeText(this,
					getResources().getString(R.string.invalidsearchterm_text), 3).show();
			return;
		}

		switch (searchRadios.getCheckedRadioButtonId()) {

		case R.id.byaddress_radio:

			try {

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = factory.newDocumentBuilder();
				URL url = new URL(Util.geoLocalizationURL
						+ searchTerm.getText().toString().replace(" ", "+"));
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setDoOutput(true);
				conn.connect();
				InputStream in = conn.getInputStream();

				Document doc = docBuilder.parse(in);
				doc.getDocumentElement().normalize();

				Integer statusCode = new Integer(doc.getElementsByTagName("code")
						.item(0).getChildNodes().item(0).getNodeValue());

				if (statusCode == 200) {

					String address = doc.getElementsByTagName("address")
							.item(0).getChildNodes().item(0).getNodeValue();
					String coords = doc.getElementsByTagName("coordinates")
							.item(0).getChildNodes().item(0).getNodeValue();

					Intent result = new Intent();
					result.putExtra("address", address);
					result.putExtra("lat", new Double(coords.split(",")[1]));
					result.putExtra("lon", new Double(coords.split(",")[0]));
					setResult(Util.RESULT_SEARCHBYADDRESS, result);
					finish();

				} else {

					Toast.makeText(this,
							getResources().getString(R.string.noresults_text), 3).show();

				}

			} catch (Exception e) {

				Toast.makeText(this,
						getResources().getString(R.string.noconnectivity_text), 3).show();

			}

			break;
			

		case R.id.byname_radio:

			DbAdapter db = new DbAdapter(this);

			db.open();

			Cursor c = db.getItemsLike(
					searchTerm.getText().toString(),
					categoryRadios.getCheckedRadioButtonId() == R.id.allcategories_radio ?
						null : activeCategories);

			ArrayList<SearchResultItem> itemsFound = new ArrayList<SearchResultItem>();
			if (c.moveToFirst())
				do
					itemsFound.add(new SearchResultItem(
							c.getString(1),
							c.getInt(2),
							c.getString(3),
							c.getDouble(4),
							c.getDouble(5)));
				while (c.moveToNext());

			c.close();
			db.close();

			switch (itemsFound.size()) {
			case 0:
				Toast.makeText(this,
						getResources().getString(R.string.noresults_text), 3).show();
				break;
			case 1:
				Intent result = new Intent();
				result.putExtra("category", itemsFound.get(0).getCategory());
				result.putExtra("lat", itemsFound.get(0).getLat());
				result.putExtra("lon", itemsFound.get(0).getLon());
				setResult(Util.RESULT_SEARCHBYNAME, result);
				finish();
				break;
			default:
				Bundle bundle = new Bundle();
				bundle.putBoolean("pref", false);
				bundle.putParcelableArrayList("searchresults", itemsFound);
				Intent resultList = new Intent(this, SearchResultList.class);
				resultList.putExtras(bundle);
				startActivityForResult(resultList, Util.REQUEST_DEFAULT);
			}

			break;

		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		super.onActivityResult(requestCode, resultCode, intent);

		switch (resultCode) {

		case Util.RESULT_SEARCHBYNAME:

			Intent result = new Intent();
			result.putExtras(intent);
			setResult(resultCode, result);
			finish();

			break;

		case Util.RESULT_ACTIVECATEGORIES:

			activeCategories = intent
					.getIntegerArrayListExtra("activecategories");

			break;

		case RESULT_CANCELED:

			if (activeCategories.size() == 0)
				((RadioButton) findViewById(R.id.allcategories_radio)).setChecked(true);

		}

	}

}