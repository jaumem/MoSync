/* Copyright (C) 2011 MoSync AB

This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License, version 2, as published by
the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with this program; see the file COPYING.  If not, write to the Free
Software Foundation, 59 Temple Place - Suite 330, Boston, MA
02111-1307, USA.
*/

package com.mosync.nativeui.ui.custom;

import static com.mosync.internal.generated.MAAPI_consts.EVENT_TYPE_IMAGE_PICKER;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.mosync.internal.android.MoSyncThread;
import com.mosync.internal.android.MoSyncThread.ImageCache;
import com.mosync.java.android.R;


/*
 *  Helpers for the maOpenImageBrowser syscall.
 */
public class MoSyncImagePicker
{
	// Events for when the Ok or Cancel button are hit.
	final int PICKER_CANCELED = 0;
	final int PICKER_READY = 1;

	//-------------------------- IMPLEMENTATION --------------------------//

	/**
	 * Constructor.
	 * @param thread The MoSync thread.
	 */
	public MoSyncImagePicker(MoSyncThread thread,Hashtable<Integer, ImageCache> imageTable)
	{
		mMoSyncThread = thread;
		mImageTable = imageTable;
		mPaths = new ArrayList<String>();
		mNames = new ArrayList<String>();
		// Set the decoder options for the bitmaps.
		mDecodingOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
	}

	/*
	 * Open a custom dialog with a gallery view and preview
	 * for the selected item.
	 */
	public void loadGallery()
	{
		// Initialize the selected image handle.
		mImageHandle = -1;

		mPosition = -1;

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setCancelable(true);
		builder.setTitle("Image Picker");

		builder.setNegativeButton("Cancel",
		        new DialogInterface.OnClickListener() {

			        @Override
			        public void onClick(DialogInterface dialog, int which)
			        {
				        postImagePickerEvent(PICKER_CANCELED);
			        }
		        });

		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which)
			{
				if ( !mPaths.isEmpty() )
				{
					// Save the handle of the selected item and post event.
					mImageHandle = getSelectedImageHandle(mPaths.get(mPosition));
				}
				postImagePickerEvent(PICKER_READY);
			}
		});

		if ( getImagesUsingCursor() )
		{
			// Provide a linear layout with a gallery and a preview of the
			// selected item.
			LinearLayout layout = new LinearLayout(getActivity());
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setVerticalGravity(Gravity.CENTER);
			layout.setHorizontalGravity(Gravity.CENTER);
			layout.setLayoutParams(new LinearLayout.LayoutParams(
			        LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

			Gallery gallery = new Gallery(getActivity());
			gallery.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 100));
			gallery.setCallbackDuringFling(false);

			// Fill the gallery view with images.
			gallery.setAdapter(new CustomAdapter(getActivity()));
			layout.addView(gallery);

			final ImageView preview = new ImageView(getActivity());
			preview.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));

            Bitmap bitmap = BitmapFactory.decodeFile(mPaths.get(0), mDecodingOptions);
            // Scale the bitmap, as scrolling would take to long otherwise.
            Bitmap bitmapScaled = Bitmap.createScaledBitmap(bitmap, 300, 250, true);
            preview.setImageBitmap(bitmapScaled);
			preview.setScaleType(ScaleType.FIT_CENTER);

			layout.addView(preview);

			gallery.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                        int pos, long id)
                {
					Toast.makeText(getActivity(), mNames.get(pos) ,
					        Toast.LENGTH_SHORT).show();

					// Refresh the preview image.
		            Bitmap bitmap = BitmapFactory.decodeFile(mPaths.get(pos), mDecodingOptions);
		            Bitmap bitmapScaled = Bitmap.createScaledBitmap(bitmap, 300, 250, true);
		            preview.setImageBitmap(bitmapScaled);

		            // Save the position of the last selected images.
		            mPosition = pos;
                }

				@Override
                public void onNothingSelected(AdapterView<?> arg0)
                {

                }

			});

			builder.setView(layout);

		}
		else
		{
			builder.setMessage("No images were found on the device!");
		}

		AlertDialog alertDialog = builder.create();

		// Display the dialog until user provides a selection, or cancels the
		// request.
		alertDialog.show();
	}

	/**
	 * Post a message to the MoSync event queue.
	 * The state is: Ready or Canceled.
	 */
	private void postImagePickerEvent(int state)
	{
		int[] event = new int[3];
		event[0] = EVENT_TYPE_IMAGE_PICKER;
		event[1] = state;
		// If Cancel is clicked, the handle is -1.
		event[2] = mImageHandle;

		mMoSyncThread.postEvent(event);
	}

	/*
	 * Parse cursor and select only image paths and names.
	 * Copy them locally.
	 */
	private void parseCursor(Cursor aCursor)
	{
	    if (aCursor != null)
	    {

			if (aCursor.moveToFirst())
			{
		        String filePath;
		        String title;
		        int pathColumn = aCursor.getColumnIndex(
		            MediaStore.Images.Media.DATA);

		        int titleColumn = aCursor.getColumnIndex(
		            MediaStore.Images.Media.TITLE);

		        do {
		            // Get the field values
		            filePath = aCursor.getString(pathColumn);
		            title = aCursor.getString(titleColumn);

		            if ( filePath.endsWith(".jpg") || filePath.endsWith(".png") || filePath.endsWith(".jpeg") )
		            {
						mPaths.add(filePath);
						mNames.add(title);
		            }

		            // This is for test only.
//					Toast.makeText(getActivity(),"path= "+ filePath+" title="+title ,
//					        Toast.LENGTH_SHORT).show();

		        } while (aCursor.moveToNext());
		    }
	    }
	}

	private boolean getImagesUsingCursor()
	{
		mPaths.clear();
		mNames.clear();

		// Search for both internal and external images.
		Uri externalImages = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		Uri internalImages = MediaStore.Images.Media.INTERNAL_CONTENT_URI;

		// Which image properties are we querying.
		String[] projection = new String[]{
			MediaStore.Images.Media._ID,
			// Get the absolute path.
	        MediaStore.Images.Media.DATA,
	        // Get the title for display.
	        MediaStore.Images.Media.TITLE
	};

	    // Make the queries.
	    Cursor internalCursor = getActivity().getContentResolver().query(internalImages,
	            projection, // Which columns to return
	            "",         // Which rows to return (all rows)
	            null,       // Selection arguments (none)
	            ""          // Ordering
	            );

	    parseCursor(internalCursor);

	    Cursor externalCursor = getActivity().getContentResolver().query(externalImages,
	            projection, // Which columns to return
	            "",         // Which rows to return (all rows)
	            null,       // Selection arguments (none)
	            ""          // Ordering
	            );

	    parseCursor(externalCursor);

	    if ( !mPaths.isEmpty() )
	    {
			return true;
	    }
	    else
	    {
			Log.i("MoSync","maImagePickerOpen - no images!!!");
			return false;
	    }
	}

	/**
	 * @return The Activity object.
	 */
	private Activity getActivity()
	{
		return mMoSyncThread.getActivity();
	}

    /**
     * Get the handle of the selected item.
     * Further, post it in a EVENT_TYPE_IMAGE_PICKER event.
     * @return The new handle.
     */
    private int getSelectedImageHandle(final String absPath)
    {
        // Create handle.
        int dataHandle = mMoSyncThread.nativeCreatePlaceholder();

        Bitmap tempBitmap = BitmapFactory.decodeFile(absPath, mDecodingOptions);

			if(null == tempBitmap)
			{
				Log.i("MoSync","maImagePickerOpen Cannot create handle");
//				maPanic(1, "maImagePickerOpen: Unable to create handle");
			}

			mImageTable.put(dataHandle, new ImageCache(null, tempBitmap));

        return dataHandle;
    }

    public class CustomAdapter extends BaseAdapter
    {
        int mGalleryItemBackg;
        private Context mContext;

        public CustomAdapter(Context c)
        {
            mContext = c;
            TypedArray typArray = getActivity().obtainStyledAttributes(R.styleable.GalleryTheme);

            // Set the theme with a frame surrounding gallery items.
            mGalleryItemBackg = typArray.getResourceId(R.styleable.GalleryTheme_android_galleryItemBackground, 0);
            typArray.recycle();
        }

        public int getCount()
        {
			return mPaths.size();
        }

        public Object getItem(int position)
        {
            return position;
        }

        public long getItemId(int position)
        {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            ImageView imgView = new ImageView(mContext);

            Bitmap bitmap = BitmapFactory.decodeFile(mPaths.get(position), mDecodingOptions);
            Bitmap bitmapScaled = Bitmap.createScaledBitmap(bitmap, 150, 100, true);
            imgView.setImageBitmap(bitmapScaled);
            imgView.setLayoutParams(new Gallery.LayoutParams(LayoutParams.FILL_PARENT,
			        LayoutParams.FILL_PARENT));

            imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imgView.setBackgroundResource(mGalleryItemBackg);

            return imgView;
        }
    }

    //--------------------------   Members   --------------------------//

	/**
	 * The MoSync thread object.
	 */
    private MoSyncThread mMoSyncThread;

    /**
     * It has access to the image resource table.
     */
    private Hashtable<Integer, ImageCache> mImageTable;

    /**
     * List of absolute paths to all images on the device.
     */
	private List<String> mPaths;

	/**
	 * List of image names.
	 */
	private List<String> mNames;

	/**
	 * The handle of the selected image.
	 */
	private int mImageHandle = -1;

	/**
	 * The current image index.
	 */
	private int mPosition = -1;

	/**
	 * Decoding options used for bitmaps.
	 */
	BitmapFactory.Options mDecodingOptions = new BitmapFactory.Options() ;

}
