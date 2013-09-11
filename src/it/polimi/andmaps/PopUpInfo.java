package it.polimi.andmaps;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PopUpInfo extends Dialog {
	private Map context;
	private Integer id;
	private String title = "";
	private String tel = "";
	private String orari = "";
	private boolean inDb;
	private String infoLocale = "";
	private boolean isFav = false;

	private Button addFavoriteButton;
	private Button delFavoriteButton;
	
	private Button route;


	public PopUpInfo(Context context, Integer id, String title, boolean inDb) {
		super(context);
		this.id = id;
		this.context = (Map) context;
		this.inDb = inDb;

		if (inDb || title.compareTo(context.getResources()
				.getString(R.string.currentposition_text)) == 0) {
			setTitle(title);
		} else {
			this.title = title;
			setTitle("Indirizzo ricercato");
		}
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!inDb) {
			setContentView(R.layout.popupgps);
		} else
			setContentView(R.layout.popup);

		findViewById(R.id.close_button).setOnClickListener(
			new View.OnClickListener() {
				public void onClick(View v) {
					dismiss();
				}
			});
		
		if (inDb) {
			View call = findViewById(R.id.call_button);
			call.setOnClickListener(new View.OnClickListener(){
				public void onClick(View v) {
					if(!tel.equals("")){
						Uri uri = Uri.fromParts("tel", tel, null);
						Intent callIntent = new Intent(Intent.ACTION_CALL, uri);
						callIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
						context.startActivity(callIntent);
					}
					else {
						infoLocale += "Nessun numero di telefono memorizzato";
						((TextView)findViewById(R.id.info_text)).setText(infoLocale);
					}
				}
			});
			
			addFavoriteButton = (Button) findViewById(R.id.addfav_button);
			delFavoriteButton = (Button) findViewById(R.id.delfav_button);
			
			DbAdapter db = new DbAdapter(context);
			db.open();
			if(isFav = db.isFav(id)){
				addFavoriteButton.setVisibility(View.GONE);
				delFavoriteButton.setVisibility(View.VISIBLE);
			}
			db.close();

			addFavoriteButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					isFav = true;
					manageFavoriteStatus();
				}
			});

			delFavoriteButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					isFav = false;
					manageFavoriteStatus();
				}
			});
			

			TextView text = (TextView)findViewById(R.id.info_text);
			
			if (id != null) {
				
				try {
				
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = factory.newDocumentBuilder();
		
					URL url = new URL(Util.baseURL + "getinfo.php?id=" + id);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setDoOutput(true);
					conn.connect();
					InputStream in = conn.getInputStream();
		
					Document doc = docBuilder.parse(in);
					doc.getDocumentElement().normalize();
					
					if (doc.getDocumentElement().hasChildNodes()) {
						NodeList info = doc.getDocumentElement().getChildNodes();
						for (int i = 0; i < info.getLength(); i++)
							if (info.item(i).getNodeType() == Node.ELEMENT_NODE) {
								Element el = ((Element) info.item(i));
								text.append(Util.capFirst(el.getNodeName()) + ": " 
									+ el.getChildNodes().item(0).getNodeValue() + "\n");
								if(el.getNodeName().equals("telefono"))
									tel = el.getChildNodes().item(0).getNodeValue();
								if (el.getNodeName().equals("orari"))
									orari = el.getChildNodes().item(0).getNodeValue();
							}
					}

					// composizione semaforo
					if (orari != "") {

						ImageView imgContenitor = (ImageView) findViewById(R.id.trafficlight_image);
						int in2 = 0;
						int in1 = checkHour(orari.substring(0, 11));

						if (orari.contains(";"))
							in2 = checkHour(orari.substring(12, 23));
						if (in1 == 1 || in2 == 1)
							imgContenitor.setImageDrawable(context
									.getResources().getDrawable(R.drawable.trafficgreen));
						else if (in1 == 0 && in2 == 0)
							imgContenitor.setImageDrawable(context
									.getResources().getDrawable(R.drawable.trafficred));
						else
							imgContenitor.setImageDrawable(context
									.getResources().getDrawable(R.drawable.trafficamber));

					}

				} catch (Exception e) {

					Util.l(e);

					// cannot connect or XML error
				}
				
				route = (Button) findViewById(R.id.route); 
				route.setOnClickListener(new View.OnClickListener(){
					public void onClick(View v) {
						if(context.routeFactory(id))
							dismiss();
					}
					
				});

			}
			
			
			
			else {
			
				text.setText(title);
			}
		}
	}
	
	private void manageFavoriteStatus() {

		DbAdapter db = new DbAdapter(context);
		db.open();
		if (isFav) {
			if (db.addFav(new Integer(id.intValue())) >= 0) {
				Toast.makeText(context,
					context.getResources()
						.getString(R.string.addedtofav_text), 3).show();
				addFavoriteButton.setVisibility(View.GONE);
				delFavoriteButton.setVisibility(View.VISIBLE);
			}
		} else {
			if (db.delFav(new Integer(id.intValue())) > 0) {
				Toast.makeText(context,
					context.getResources()
						.getString(R.string.deletedfromfav_text), 3).show();
				addFavoriteButton.setVisibility(View.VISIBLE);
				delFavoriteButton.setVisibility(View.GONE);
			}
		}
		db.close();

	}
	
	// red = 0, green = 1, yellow = 2
	private int checkHour(String orari) {

		final GregorianCalendar c = (GregorianCalendar) Calendar.getInstance();
		int oraNow = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
		int ora1 = Integer.parseInt(orari.substring(0, 2)) * 60
				+ Integer.parseInt(orari.substring(3, 5));
		int ora2 = Integer.parseInt(orari.substring(6, 8)) * 60
				+ Integer.parseInt(orari.substring(9, 11));

		if (ora1 < ora2) {
			if (oraNow > ora2 || oraNow < ora1)
				return 0;
			if ((oraNow > ora2 - 15 && oraNow < ora2) || (oraNow > ora1 && oraNow < ora1 + 15))
				return 2;
			return 1;
		}

		else {
			if (oraNow > ora2 && oraNow < ora1)
				return 0;
			if ((oraNow > ora2 - 15 && oraNow < ora2) || (oraNow > ora1 && oraNow < ora1 + 15))
				return 2;
			return 1;
		}
	}
}
