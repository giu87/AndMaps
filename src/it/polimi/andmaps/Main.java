package it.polimi.andmaps;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Main extends Activity implements OnClickListener {

	private DbAdapter db = new DbAdapter(this);

	/** Main activity init */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		findViewById(R.id.start_button).setOnClickListener(this);
		findViewById(R.id.exit_button).setOnClickListener(this);

		checkDbStatus();

	}
	
	@Override
	public void onResume() {

		super.onResume();
		/* enables buttons in case they were been disabled by updater */
		((Button) findViewById(R.id.start_button)).setClickable(true);
		((Button) findViewById(R.id.exit_button)).setClickable(true);
		if (findViewById(R.id.updatereminder_button) != null)
			((Button) findViewById(R.id.updatereminder_button)).setClickable(true);


		// TODO: removing updatereminder may need better implementation
		if (findViewById(R.id.updatereminder_button) != null) {
			if (Updater.deviceDbVer == Updater.remoteDbVer) {
				setContentView(R.layout.main);
				findViewById(R.id.start_button).setOnClickListener(this);
				findViewById(R.id.exit_button).setOnClickListener(this);
			}
		}

	}

	public void onClick(View clickedView) {

		switch (clickedView.getId()) {
		case R.id.start_button:
			Intent map = new Intent(this, Map.class);
			startActivity(map);
			break;
		case R.id.exit_button:
			finish();
			break;
		}

	}

	private void checkDbStatus() {

		/* Check if POIs database is up-to-date */

		db.open();

		try {

			Updater.deviceDbVer = db.getLastUpdateVersion();
			Updater.remoteDbVer = new Integer(Util.getURL(Util.baseURL + "lastversion.php"));

			if (Updater.remoteDbVer > Updater.deviceDbVer) {

				/* DB is out-of-date and needs to be updated */
				Dialog outOfDate = new OutOfDateDialog(this);
				outOfDate.show();

			}

			Util.l("Current db version: " + Updater.deviceDbVer);

		} catch (NumberFormatException e) {

			/* Probably cannot access update server */

		}

		db.close();

	}

}