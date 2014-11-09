package com.ravi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class MyNewsReporter extends ListActivity {
	
	public static final String PREFS_NAME = "MyPrefsFile";
	
	 String[] DayOfWeek = {"Form Telangana, with Hyderabad as a shared capital", "Telangana agitators plan bandh", "Slump Alters Jobless Map in US, With South Hit Hard",
			   "Gold Rebounds After Biggest Three-Day Decline Since 2008 Spurs Purchase", "European Stocks Rally for Third Day; US Index Futures Advance", "Friday", "Saturday"
			 };
	 
	 String urls[] = null;
	 
	 private String[] splitWords(String s) {
		String t[] = s.split(";");
		for(int i=0; i<t.length; i++){
			t[i] = "\"" +t[i].trim() + "\"";
		}
		return t;
	 }
	 
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        String searchWords[] = splitWords(getPreferenceText());
        //String searchWords[] = "\"Telangana\",\"gold\"".split(",");
         
//		String searchWords[] = {"\"Telangana\"", "US+Debt+Crisis", "\"Sensex\"",  "\"Gold\"", "\"Silver\"", "europe+debt", "\"reliance+industries\"",};

		try {
			List newsList = getTopNews(searchWords);	
			String newsStr[] = getNewsAsStringArray(newsList);
			urls = getURLsAsStringArray(newsList);

	        setListAdapter(new ArrayAdapter<String>(this, 
	                android.R.layout.simple_list_item_1, newsStr));

	        ListView lv = getListView();
	        lv.setTextFilterEnabled(true);
	
	        
	        lv.setOnItemClickListener(new OnItemClickListener() {
	          public void onItemClick(AdapterView<?> parent, View view,
	              int position, long id) {
	        	  Intent i = new Intent(Intent.ACTION_VIEW, 
	        		       Uri.parse(urls[position]));
	        		startActivity(i);

	        		
	          }
	        });

			
		} catch(Throwable th) {
			th.printStackTrace();
		}
		

        
//        ListView lv = getListView();
//        lv.setTextFilterEnabled(true);
//
//        lv.setOnItemClickListener(new OnItemClickListener() {
//          public void onItemClick(AdapterView<?> parent, View view,
//              int position, long id) {
//            // When clicked, show a toast with the TextView text
//            Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
//                Toast.LENGTH_SHORT).show();
//          }
//        });

//      setContentView(R.layout.main);
    }
    

	public static List getTopNews(String searchWords[]) {
		List list = new ArrayList();
		for (int i = 0; i < searchWords.length; i++) {
			String googleResponse = readJSONResponeFromGoogle(searchWords[i]);
			System.err.println("\n" + searchWords[i]);
//			Log.e("MyNewsReporter", searchWords[i]);
			List tList = getTopResults(googleResponse);
			if(tList != null) {
				list.addAll(tList);
			}
		}
		return list;
	}
	
	private static List getTopResults(String googleResponse) {

		try {
			JSONObject root = new JSONObject(googleResponse);
			JSONObject responseData = root.getJSONObject("responseData");
			JSONArray results = responseData.getJSONArray("results");
			
			return filterLast24HoursResults(results);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	
	private static List filterLast24HoursResults(JSONArray results) {
	
		if(results == null) {
			return null;
		}
		
		List list = new ArrayList();
		if(results.length() > 0) {
			
			try {
				for(int i=0; i<results.length(); i++) {
					JSONObject newsResult = results.getJSONObject(i);
					String publishedDate = newsResult.getString("publishedDate");
					if(publishedDate.indexOf(getTodaysDateString()) != -1 || publishedDate.indexOf(getYesterdayDateString()) != -1) {
						list.add(newsResult);
					}
				}
				
				return list;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	private static String getTodaysDateString() {

		Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DATE);
        int month = cal.get(Calendar.MONTH) + 1;

        String monthStr = getMonthForInt(month);
        
        NumberFormat myFormat = NumberFormat.getInstance();
        myFormat.setMinimumIntegerDigits(2);
        String dayStr = myFormat.format(day);
        return dayStr + " " + monthStr;
	}
	
	private static String getYesterdayDateString() {

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1); // yesterday
        int day = cal.get(Calendar.DATE);
        int month = cal.get(Calendar.MONTH) + 1;

        String monthStr = getMonthForInt(month);
        
        NumberFormat myFormat = NumberFormat.getInstance();
        myFormat.setMinimumIntegerDigits(2);
        String dayStr = myFormat.format(day);
        return dayStr + " " + monthStr;
	}
	
	private static void printNewsResult(JSONObject newsResult) {

		try {
			//		System.out.println(firstResult);
			String newsMessage = newsResult.getString("titleNoFormatting");
			String urlString = newsResult.getString("unescapedUrl");
			String publishedDate = newsResult.getString("publishedDate");
			System.out.println(newsMessage + "   " + publishedDate);
//			System.out.println(urlString);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private static String readJSONResponeFromGoogle(String searchWord) {
//		System.out.println("Connecting to google...");

		// This example request includes an optional API key which you will need to
		// remove or replace with your own key.
		// Read more about why it's useful to have an API key.
		// The request also includes the userip parameter which provides the end
		// user's IP address. Doing so will help distinguish this legitimate
		// server-side traffic from traffic which doesn't come from an end-user.

		try {
			URL url = new URL(
					"http://ajax.googleapis.com/ajax/services/search/news?v=1.0&"
							+ "q=" + searchWord);
			URLConnection connection = url.openConnection();
			//connection.addRequestProperty("Referer", /* Enter the URL of your site here */);

			String line;
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			String googleResponse = builder.toString();
			return googleResponse;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static String readJSONResponeFromFile(String filepath) {
		try {
			String news = readFile(filepath);
			//			System.out.println(news);
			return news;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static void setProxySettings() {
		System.getProperties().put("http.proxyHost", "172.13.16.179");
		System.getProperties().put("http.proxyPort", "80");
		System.getProperties().put("http.proxyUser",
				"userid");
		System.getProperties().put("http.proxyPassword", "pwd");
	}

	private static String readFile(String sFileName_) throws IOException,
			FileNotFoundException {

		StringBuffer sFileContent = new StringBuffer(100000);

		FileReader frIni = new FileReader(sFileName_);
		if (frIni != null) {
			BufferedReader brIni = new BufferedReader(frIni);
			if (brIni != null) {
				while (brIni.ready()) {
					String sLine = brIni.readLine();
					if (sLine == null) {
						break;
					}
					sFileContent.append(sLine).append('\n');
				}
				brIni.close();
			}
			frIni.close();
		}

		return sFileContent.toString();
	}

	
	private static String getMonthForInt(int m) {
	    String month = "invalid";
	    DateFormatSymbols dfs = new DateFormatSymbols();
	    String[] months = dfs.getShortMonths();
	    if (m >= 1 && m <= 12 ) {
	        month = months[m-1];
	    }
	    return month;
	}

	private static String[] getNewsAsStringArray(List newsList) {

		String strArray[] = new String[newsList.size()];
		
		
		try {
			for(int i=0; i<newsList.size(); i++){
				JSONObject newsResult = (JSONObject) newsList.get(i);
				//		System.out.println(firstResult);
				String newsMessage = newsResult.getString("titleNoFormatting");
				String urlString = newsResult.getString("unescapedUrl");
				String publishedDate = newsResult.getString("publishedDate");
				System.out.println(newsMessage + "   " + publishedDate);
//				Log.e("MyNews", newsMessage);
//				System.out.println(urlString);
				strArray[i] = newsMessage;
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return strArray;
	}

	private static String[] getURLsAsStringArray(List newsList) {

		String strArray[] = new String[newsList.size()];
		
		try {
			for(int i=0; i<newsList.size(); i++){
				JSONObject newsResult = (JSONObject) newsList.get(i);
				//		System.out.println(firstResult);
				String newsMessage = newsResult.getString("titleNoFormatting");
				String urlString = newsResult.getString("unescapedUrl");
				String publishedDate = newsResult.getString("publishedDate");
				System.out.println(newsMessage + "   " + publishedDate);
//				System.out.println(urlString);
				strArray[i] = urlString;
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return strArray;
	}
	
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mymenu, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Toast.makeText(this, getPreferenceText(), Toast.LENGTH_SHORT).show();
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Title");
		alert.setMessage("Message");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setText(getPreferenceText());
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString();
		  setPreferenceText(value);
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
		
		
		
		return true;
	}
    
    private String getPreferenceText() {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getString("query", "gold");
    }
    
    private void setPreferenceText(String preferenceText) {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("query", preferenceText);

        // Don't forget to commit your edits!!!
        editor.commit();
    }
    
}
