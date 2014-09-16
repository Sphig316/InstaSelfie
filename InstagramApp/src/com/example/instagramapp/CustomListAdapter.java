package com.example.instagramapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class CustomListAdapter extends BaseAdapter
{
	private ArrayList<Bitmap> listData;
	private LayoutInflater layoutInflater;
	private Context context;
//	private static String FIRST_DOWNLOAD_URL = null; /*= "https://api.instagram.com/v1/tags/selfie/media/recent?type=image?access_token=1450779186.1fb234f.98c98c61ea78411b845a44c6d085aa6d&client_id=6383ca016b5344b6b55ccc44bacfc3b0";*/
	private static String NEXT_DOWNLOAD_URL;
	private ArrayList<String> urlList;
	private ImageCache mCache = null;
	private final LayoutParams BIG = new LayoutParams(300, 300);
	private final LayoutParams SMALL = new LayoutParams(120, 120);
	private static int smallCounter = 0;

	public CustomListAdapter(Context context, ArrayList<Bitmap> listData)
	{
		this.listData = listData;
		layoutInflater = LayoutInflater.from(context);
		BIG.gravity = Gravity.CENTER;
		SMALL.gravity = Gravity.CENTER;
	}

	public CustomListAdapter(Context context, String downloadUrl, int memoryClassBytes/*, ArrayList<Bitmap> listData*/)
	{
		this.context = context;
		layoutInflater = LayoutInflater.from(context);
		mCache = new ImageCache(memoryClassBytes / 6);
//		this.listData = listData;
		
		try
		{
			urlList = new GetDownloadUrls().execute(downloadUrl).get();
		}
		
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		catch (ExecutionException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public int getCount()
	{
//		return listData.size();
		return 0;
	}

	@Override
	public Object getItem(int position)
	{
//		return listData.get(position);
		return 0;
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder;
		
		if (convertView == null)
		{
			convertView = layoutInflater.inflate(R.layout.row_layout, null);
			holder = new ViewHolder();
			holder.instagramPic = (ImageView) convertView.findViewById(R.id.instagramPic);
			convertView.setTag(holder);
		}
		
		else
			holder = (ViewHolder) convertView.getTag();

		
		Bitmap bitmap = (Bitmap) listData.get(position);
		
		holder.instagramPic.setImageBitmap(bitmap);
		
		if (holder.instagramPic != null)
			new Image().loadToView(position, holder.instagramPic);

//		if (holder.instagramPic != null)
//		{1
//			if (bitmap != null)
//			{
//				if (smallCounter == 0 || smallCounter == 3)
//				{
//					holder.instagramPic.setImageBitmap(bitmap);
////					holder.instagramPic.setLayoutParams(BIG);
//					smallCounter = 0;
//				}
//
//				else
//				{
//					holder.instagramPic.setImageBitmap(bitmap);
////					holder.instagramPic.setLayoutParams(SMALL);
//				}
//
//				smallCounter++;
//			}
//			
//			else
//			{
//				holder.instagramPic.setImageDrawable(holder.instagramPic.getContext().getResources().getDrawable(R.drawable.ic_launcher));
//			}
//		}

		return convertView;
	}

	static class ViewHolder
	{
		ImageView instagramPic;
	}
	
	private class Image
	{
		private class imageDownloaderTask extends AsyncTask<String, Void, Bitmap>
		{
			private ImageView mTarget;
			
			public imageDownloaderTask(ImageView target)
			{
				mTarget = target;
			}
			
			@Override
			protected void onPreExecute()
			{
//				super.onPreExecute();
				mTarget.setTag(this);
			}

			@Override
			protected Bitmap doInBackground(String... urls)
			{
				long key = Long.parseLong(urls[0]);
				String url = urls[1];
				
				Bitmap result = null;
				
				if(url != null)
				{
					result = load(url);
					
					if(result != null)
					{
						mCache.put(key, result);
					}
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Bitmap result)
			{
				if(mTarget.getTag() == this)
				{
					mTarget.setTag(null);
				}
				
				if(result != null)
				{
					mTarget.setImageBitmap(result);
				}
				
				else if(mTarget.getTag() != null)
				{
					((imageDownloaderTask) mTarget.getTag()).cancel(true);
					mTarget.setTag(null);
				}
			}
			
		}
		
		public Bitmap load(String urlString)
		{
			BitmapFactory.Options option = new BitmapFactory.Options();
			Bitmap bitmap = null;
			
			// Downsizing image as it throws  OutOfMemory Exception for larger images
			option.inSampleSize = 16;
			
			final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
			final HttpGet getRequest = new HttpGet(urlString);
			try
			{
				HttpResponse response = client.execute(getRequest);
				final int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK)
				{
					Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + urlString);
					return null;
				}

				final HttpEntity entity = response.getEntity();
				if (entity != null)
				{
					InputStream inputStream = null;
					try
					{
						inputStream = entity.getContent();
						bitmap = BitmapFactory.decodeStream(inputStream, null, option);
						
						if (bitmap == null)
							bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
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
				Log.w("ImageDownloader", "Error while retrieving bitmap from " + urlString);
			}
			
			finally
			{
				if (client != null)
				{
					client.close();
				}
			}
			
			return bitmap;
		}
		
		public void loadToView(int key, ImageView view)
		{
			if(key == 0)
				return;
			
			Bitmap bitmap = getBitmapFromMemoryCache(key);
			
			if(bitmap == null)
			{
				final imageDownloaderTask task = (imageDownloaderTask) new imageDownloaderTask(view);
				
				view.setTag(task);
				task.execute("" + key, urlList.get(key));
			}
			
			else
			{
				view.setImageBitmap(bitmap);
			}
		}
		
		public Bitmap getBitmapFromMemoryCache(long key)
		{
			return (Bitmap) mCache.get(key);
		}
	}
	
	private class GetDownloadUrls extends AsyncTask<String, Void, ArrayList<String>>
	{
		
		@Override
		protected ArrayList<String> doInBackground(String... downloadUrl)
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
					setNextUrl(next_url);
					Log.d("Url", next_url);
					
					// Parse through the array for image links to download and as they are downloaded, load them on the screen
					for (int i = 0; i < object.length(); i++)
					{

						JSONObject jo = (JSONObject) object.get(i);
						JSONObject imagesJsonObj = (JSONObject) jo.getJSONObject("images");

						// Standard resolution
						JSONObject stdResJsonObject = (JSONObject) imagesJsonObj.getJSONObject("standard_resolution");
						String url = stdResJsonObject.get("url").toString();
						stringList.add(url);
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
			
			return stringList;
		}
	}
	
	private void setNextUrl(String nextUrl)
	{
		NEXT_DOWNLOAD_URL = nextUrl;
	}
}