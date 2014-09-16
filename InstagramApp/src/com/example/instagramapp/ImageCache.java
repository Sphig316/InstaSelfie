package com.example.instagramapp;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

public class ImageCache extends LruCache<Long, Bitmap>
{
	public ImageCache(int maxSizeInBytes)
	{
		super(maxSizeInBytes);
	}

	@Override
	protected int sizeOf(Long key, Bitmap value)
	{
		return value.getByteCount();
	}
}