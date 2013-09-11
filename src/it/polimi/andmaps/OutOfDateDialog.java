package it.polimi.andmaps;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

public class OutOfDateDialog extends Dialog {

	private Activity context;

	public OutOfDateDialog(Context context) {

		super(context);
		this.context = (Activity) context;

	}

	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.outofdate);
		setTitle(R.string.updater_title);

		findViewById(R.id.update_button).setOnClickListener(
			new View.OnClickListener() {
				public void onClick(View clickedView) {
					dismiss();
					/* disables start and exit buttons to avoid them pressed before intent starts */
					((Button) context.findViewById(R.id.start_button)).setClickable(false);
					((Button) context.findViewById(R.id.exit_button)).setClickable(false);
					Intent updater = new Intent(context, Updater.class);
					context.startActivity(updater);
				}
			});

		findViewById(R.id.cancel_button).setOnClickListener(
			new View.OnClickListener() {
				public void onClick(View clickedView) {
					dismiss();
					View.inflate(context, R.layout.updatereminder,
								 (RelativeLayout) context.findViewById(R.id.main_layout));
					context.findViewById(R.id.updatereminder_button).setOnClickListener(
						new View.OnClickListener() {
							public void onClick(View clickedView) {
								/* disables buttons to avoid them pressed before intent starts */
								((Button) context.findViewById(R.id.start_button)).setClickable(false);
								((Button) context.findViewById(R.id.exit_button)).setClickable(false);
								((Button) context.findViewById(R.id.updatereminder_button)).setClickable(false);
								Intent updater = new Intent(context, Updater.class);
								context.startActivity(updater);
							}
						});
				}
			});

	}

}