package it.polimi.andmaps;

import android.content.Context;
import android.view.View;
import android.widget.ZoomControls;

public class MapZoomControls extends ZoomControls {

	private static final int MAX_ZOOM_LEVEL = 19;
	private static final int MIN_ZOOM_LEVEL = 12;

	public MapZoomControls(final Context context, final OptimizedMapView mapView) {

		super(context);

		setOnZoomInClickListener(new View.OnClickListener() {
			public void onClick(View clickedView) {

				mapView.getController().zoomIn();

				mapView.computeArea(mapView.getBounds());
				((Map) context).addOverlays();

				setIsZoomInEnabled(mapView.getZoomLevel() < MAX_ZOOM_LEVEL);
				setIsZoomOutEnabled(true);

			}
		});

		// sarebbe da mettere il controllo sull'uscita da Milano ma i valori sono sballati
		setOnZoomOutClickListener(new View.OnClickListener() {
			public void onClick(View clickedView) {

				mapView.getController().zoomOut();

				mapView.computeArea(mapView.getBounds());
				((Map) context).addOverlays();

				setIsZoomInEnabled(true);
				setIsZoomOutEnabled(mapView.getZoomLevel() > MIN_ZOOM_LEVEL);

			}

		});

	}

}