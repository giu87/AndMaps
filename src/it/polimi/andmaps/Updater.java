package it.polimi.andmaps;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class Updater extends Activity {

	public static int deviceDbVer;
	public static int remoteDbVer;
	
	private Context context;
	
	private DbAdapter db = new DbAdapter(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.updater);
		
		context = this;

		String updateResult = "Aggiornamento completato!";

		db.open();

		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = factory.newDocumentBuilder();

			URL url = new URL(Util.baseURL + "updatedb.php?device_ver=" + deviceDbVer);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoOutput(true);
			conn.connect();
			InputStream in = conn.getInputStream();

			Document doc = docBuilder.parse(in);
			doc.getDocumentElement().normalize();


			/* Retrieves and stores new items, if any */
			if (doc.getElementsByTagName("newItems").getLength() > 0) {
				NodeList newList = doc.getElementsByTagName("newItems").item(0).getChildNodes();
				for (int i = 0; i < newList.getLength(); i++)
					if (newList.item(i).getNodeType() == Node.ELEMENT_NODE) {
						Element el = ((Element) newList.item(i));
						Util.l("Adding item " + el.getAttribute("nome"));
						//updateResult += "\n - " + el.getAttribute("nome");
						db.insertItem(new Integer(el.getAttribute("id")),
									 el.getAttribute("nome"),
									 new Integer(el.getAttribute("tipo")),
									 el.getAttribute("indirizzo"),
									 new Double(el.getAttribute("lat")),
									 new Double(el.getAttribute("lon")));
					}
			}

			/* Retrieves and stores modified items, if any */
			if (doc.getElementsByTagName("modItems").getLength() > 0) {
				NodeList modList = doc.getElementsByTagName("modItems").item(0).getChildNodes();
				for (int i = 0; i < modList.getLength(); i++)
					if (modList.item(i).getNodeType() == Node.ELEMENT_NODE) {
						Element el = ((Element) modList.item(i));
						//updateResult += "\n - " + el.getAttribute("nome") + " (modified)";
						db.updateItem(new Integer(el.getAttribute("id")),
									 el.getAttribute("nome"),
									 new Integer(el.getAttribute("tipo")),
									 el.getAttribute("indirizzo"),
									 new Double(el.getAttribute("lat")),
									 new Double(el.getAttribute("lon")));
					}
			}

			/* Retrieves and deletes deleted items, if any */
			if (doc.getElementsByTagName("delItems").getLength() > 0) {
				NodeList delList = doc.getElementsByTagName("delItems").item(0).getChildNodes();
				for (int i = 0; i < delList.getLength(); i++)
					if (delList.item(i).getNodeType() == Node.ELEMENT_NODE) {
						Element el = ((Element) delList.item(i));
						//updateResult += "\n - " + el.getAttribute("nome") + " (deleted)";
						db.deleteItem(new Integer(el.getAttribute("id")));
					}
			}


			//updateResult += "\n\n\nSono stati individuati i types:\n";

			/* Retrieves and stores new types, if any */
			if (doc.getElementsByTagName("newTypes").getLength() > 0) {
				NodeList newTypesList = doc.getElementsByTagName("newTypes").item(0).getChildNodes();
				for (int i = 0; i < newTypesList.getLength(); i++)
					if (newTypesList.item(i).getNodeType() == Node.ELEMENT_NODE) {
						Element el = ((Element) newTypesList.item(i));
						Util.l("Adding type " + el.getAttribute("nome"));
						//updateResult += "\n - " + el.getAttribute("nome");
						if (db.insertCategory(new Integer(el.getAttribute("id")),
								el.getAttribute("nome")) >= 0)
							if (!Util.downloadAndStoreImage(this,
								Util.baseURL + "images/" + el.getAttribute("id") + ".png",
								el.getAttribute("id") + ".png"))
								throw new Exception();
					}
			}

			/* Retrieves and stores modified types, if any */
			if (doc.getElementsByTagName("modTypes").getLength() > 0) {
				NodeList modTypesList = doc.getElementsByTagName("modTypes").item(0).getChildNodes();
				for (int i = 0; i < modTypesList.getLength(); i++)
					if (modTypesList.item(i).getNodeType() == Node.ELEMENT_NODE) {
						Element el = ((Element) modTypesList.item(i));
						//updateResult += "\n - " + el.getAttribute("nome") + " (modified)";
						if (db.updateCategory(new Integer(el.getAttribute("id")),
								el.getAttribute("nome")) > 0)
							if (!Util.downloadAndStoreImage(this,
								Util.baseURL + "images/" + el.getAttribute("id") + ".png",
								el.getAttribute("id") + ".png"))
								throw new Exception();
					}
			}

			/* Retrieves and deletes deleted types, if any */
			if (doc.getElementsByTagName("delTypes").getLength() > 0) {
				NodeList delTypesList = doc.getElementsByTagName("delTypes").item(0).getChildNodes();
				for (int i = 0; i < delTypesList.getLength(); i++)
					if (delTypesList.item(i).getNodeType() == Node.ELEMENT_NODE) {
						Element el = ((Element) delTypesList.item(i));
						//updateResult += "\n - " + el.getAttribute("nome") + " (deleted)";
						if (db.deleteCategory(new Integer(el.getAttribute("id"))) > 0)
							deleteFile(el.getAttribute("id") + ".png");
					}
			}


			//updateResult += "\n\n\nDatabase aggiornato alla v"
					//+ doc.getDocumentElement().getAttribute("ver") + ".0";


			/* Updates db version */
			db.setLastUpdateVersion(deviceDbVer = remoteDbVer =
				new Integer(doc.getDocumentElement().getAttribute("ver")));

		} catch (Exception e) {

			Util.l(e);
			updateResult = "Si sono verificati errori!";
			
			findViewById(R.id.ok_button).setVisibility(View.GONE);
			findViewById(R.id.retry_button).setVisibility(View.VISIBLE);
			
			db.reset();

		}

		db.close();


		((TextView) findViewById(R.id.downloaded_text)).setText(updateResult);
		
		
		findViewById(R.id.ok_button).setOnClickListener(
			new View.OnClickListener() {
				public void onClick(View clickedView) {
					finish();
				}
			});
		
		findViewById(R.id.retry_button).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View clickedView) {
						Intent updater = new Intent(context, Updater.class);
						context.startActivity(updater);
						finish();
					}
				});

	}

}
