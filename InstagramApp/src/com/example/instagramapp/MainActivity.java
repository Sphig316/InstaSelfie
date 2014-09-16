package com.example.instagramapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

public class MainActivity extends Activity
{
	public static ArrayList<Bitmap> bitmapList = new ArrayList<Bitmap>();
	private static final String DOWNLOAD_URL = "https://api.instagram.com/v1/tags/selfie/media/recent?type=image?access_token=1450779186.1fb234f.98c98c61ea78411b845a44c6d085aa6d&client_id=6383ca016b5344b6b55ccc44bacfc3b0";
	private static int smallCounter = 0;
	private static String nextUrl = "";
	private static int asyncCounter = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
		
		Log.d("Memory", "" + memoryClassBytes);

		ListView listView = (ListView) findViewById(R.id.list);
		
		CustomListAdapter adapter = null;
		
		adapter = new CustomListAdapter(getApplicationContext(), DOWNLOAD_URL, memoryClassBytes);
		
//		try
//		{
//			adapter = new CustomListAdapter(getApplicationContext(), nextUrl, memoryClassBytes, new GetDownloadUrls().execute(DOWNLOAD_URL).get());
//		}
//		catch (InterruptedException e)
//		{
//			e.printStackTrace();
//		}
//		catch (ExecutionException e)
//		{
//			e.printStackTrace();
//		}
		
		listView.setAdapter(adapter);
		
	}
	
	private class GetDownloadUrls extends AsyncTask<String, Void, ArrayList<Bitmap>>
	{
		
		@Override
		protected ArrayList<Bitmap> doInBackground(String... downloadUrl)
		{
			String next_url = "";
			ArrayList<String> stringList = new ArrayList<String>();
			
			try
			{
				// Create URL connection to download each picture
				URL example = new URL(downloadUrl[0]);
				URLConnection tc;
				tc = example.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));

				String line;
				while ((line = in.readLine()) != null)
				{
					// Get JSON data from the link, extract the data
					JSONObject ob = new JSONObject(line);
					
					// Get the Data JSON array to parse for picture urls
					JSONArray object = ob.getJSONArray("data");
					JSONObject paginationObject = ob.getJSONObject("pagination");
					
					// Get the next url for the next download
					next_url = paginationObject.getString("next_url");
					nextUrl = next_url;
					Log.d("Url", next_url);
					
					// Parse through the array for image links to download and as they are downloaded, load them on the screen
					for (int i = 0; i < object.length(); i++)
					{

						JSONObject jo = (JSONObject) object.get(i);
						JSONObject imagesJsonObj = (JSONObject) jo.getJSONObject("images");

						// Standard resolution
						JSONObject stdResJsonObject = (JSONObject) imagesJsonObj.getJSONObject("standard_resolution");
						String url = stdResJsonObject.get("url").toString();
						bitmapList.add(downloadBitmap(url));
						// Log.d("url", url);
					}
				}
			}
			
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			catch (JSONException e)
			{
				e.printStackTrace();
			}
			
			return bitmapList;
		}
	}
	
	private Bitmap downloadBitmap(String url)
	{
		final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
		final HttpGet getRequest = new HttpGet(url);
		try
		{
			HttpResponse response = client.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK)
			{
				Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
				return null;
			}

			final HttpEntity entity = response.getEntity();
			if (entity != null)
			{
				InputStream inputStream = null;
				try
				{
					inputStream = entity.getContent();
					Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
					
//					if (smallCounter == 0 || smallCounter == 3)
//					{
//						int width = 500;
//						int height = 500;
//						bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
//						smallCounter = 0;
//					}
//					
//					else
//					{
//						int width = 320;
//						int height = 320;
//						bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
//					}
					
					smallCounter++;
					WeakReference<Bitmap> weakBitmap = new WeakReference<Bitmap>(bitmap);

					return weakBitmap.get();
				}
				finally
				{
					if (inputStream != null)
					{
						inputStream.close();
					}
					entity.consumeContent();
				}
			}
		}
		catch (Exception e)
		{
			// Could provide a more explicit error message for IOException or
			// IllegalStateException
			getRequest.abort();
			Log.w("ImageDownloader", "Error while retrieving bitmap from " + url);
		}
		finally
		{
			if (client != null)
			{
				client.close();
			}
		}
		return null;
	}

}
