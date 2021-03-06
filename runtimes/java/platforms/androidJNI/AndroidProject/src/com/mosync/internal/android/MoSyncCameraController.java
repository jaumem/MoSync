package com.mosync.internal.android;


import static com.mosync.internal.android.MoSyncHelpers.SYSLOG;

import com.mosync.internal.generated.IX_WIDGET;
import com.mosync.internal.generated.MAAPI_consts; 

import com.mosync.java.android.MoSync;
import com.mosync.nativeui.ui.widgets.MoSyncCameraPreview;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/**
 * 
 * A class that controls the behavior of camera and the camera preview
 * @author Ali Sarrafi
 *
 */
public class MoSyncCameraController {
	
	/**
	 * Thread Synchronization variables used to synchronize the 
	 * snapshot call and the call backs
	 * 
	 */
	private boolean dataReady;
	private ReentrantLock lock;
	private Condition condition;
	
	private Camera mCamera;

	/**
	 * A flag that indicates the format of the image to be taken
	 */
	private boolean rawMode;
	/**
	 * A private attribute used to keep track of 
	 * the data place holder in each snapshot
	 */
	private int resourceIndex;

	/**
	 * Arrays to store user assigned sizes
	 */
	List <Integer> userWidths;
	List <Integer> userHeights;

	/**
	 * The MoSync Camera Preview object that handles the surfaceview 
	 * created by either the NativeUIWidget or the full screen call. 
	 */
	private MoSyncCameraPreview mPreview;
		

	/**
	 * The MoSync thread object.
	 */
	public MoSyncThread mMoSyncThread;

	
	/**
	 * Constructor.
	 * @param thread The MoSync thread.
	 */
	public MoSyncCameraController(MoSyncThread thread)
	{
		mMoSyncThread = thread;
		lock = new ReentrantLock();
		mPreview = null;
		condition = lock.newCondition();
		dataReady = false;
		userWidths = new ArrayList<Integer>();
		userHeights = new ArrayList<Integer>();
		mCamera = Camera.open();
		rawMode = false;
	}

	/**
	 * @return The Activity object.
	 */
	public Activity getActivity()
	{
		return mMoSyncThread.getActivity();
	}
	
	/**
	 * An equivalent to maCameraNumber syscall that
	 * queries the number of available cameras
	 * @return number of cameras on the device
	 */
	public static int numberOfCameras()
	{
		//Currently Anroid API Level 8 only supports one camera
		return 1;
	}

	/**
	 * A function to provide the information on supported picture
	 *  sizes to the user
	 * @return total number of supported sizes for the selected camera
	 */
	public int getNumberOfPictureSizes()
	{
		List <Camera.Size> sizeList;
		Camera.Parameters parameters = mCamera.getParameters();
		sizeList = parameters.getSupportedPictureSizes();
		return sizeList.size();
	}
	
	/**
	 * Adds a custom size to the list of sizes  requested by the user
	 * @param index the index used for addressing the specific size
	 * @param width width of the desired picture
	 * @param height height of the desired picutre
	 * @return RES_OK
	 */
	public int addSize(int index, int width, int height)
	{
		//TODO: move the optimal calculation here for better performance
		userWidths.add(index, new Integer(width));
		userHeights.add(index, new Integer(height));
		return MAAPI_consts.MA_CAMERA_RES_OK;
	}
	
	/**
	 * Sets the MoSyncCameraPreview object to be controlled
	 * @param preview an object of MoSyncCameraPreview that has already been initialized
	 */
	public void setPreview(MoSyncCameraPreview preview)
	{
		mPreview = preview;
		if(mPreview.mCamera == null)
		{
			mPreview.mCamera = mCamera;
			mPreview.initiateCamera();
		}
	}
	
	/**
	 * Sets the active camera 
	 * @param CameraNumber index of the camera from the available cameras
	 * @return RES_OK
	 */
	public int setActiveCamera(int CameraNumber)
	{
		//android API Level 8 only support one camera
		return 1;
	}

	/**
	 * Checks to see if a view is setup already
	 * @return false if there is no view true if there is a view
	 */
	public boolean hasView()
	{
		if(mPreview == null)
			return false;
		else
			return true;
	}
	
	/**
	 * 
	 * @return a reference to the preview that is currently being used for the camera
	 */
	public MoSyncCameraPreview getPreview()
	{
		return mPreview;
	}
	
	/**
	 * Starts the preview
	 */
	public int cameraStart()
	{
		try
		{
			if(mPreview.mCamera == null)
			{
				mPreview.mCamera = mCamera;
				mPreview.initiateCamera();
			}
			mCamera.startPreview();
		} 
		catch (Exception e) 
		{
			SYSLOG(e.getMessage());
			return MAAPI_consts.MA_CAMERA_RES_FAILED;
		}
		
		return MAAPI_consts.MA_CAMERA_RES_OK;
	}
	
	/**
	 * Called to stop the preview on the camera
	 */
	public int cameraStop()
	{
		try
		{
			mCamera.stopPreview();
		} 
		catch (Exception e) 
		{
			return MAAPI_consts.MA_CAMERA_RES_FAILED;
		}
		
		return MAAPI_consts.MA_CAMERA_RES_OK;
	}
	
	
	/**
	 * Takes a picture using the camera, waits for the callbacks
	 * and sends the data to MoSync Program
	 */
	public int cameraSnapshot(int formatIndex, int placeHolder)
	{
		if(formatIndex >= 0)
		{
			setNewSize(formatIndex);
		}
		resourceIndex = placeHolder;

		mPreview.mCamera.takePicture(null, rawCallback, jpegCallback);

		lock.lock();
		  try 
		  {
			  while (dataReady == false) 
			  {
				  condition.await();
			  }
			  dataReady = false;   
			  return MAAPI_consts.MA_CAMERA_RES_OK;
		  } 
		  catch (InterruptedException e) 
		  {
			  return MAAPI_consts.MA_CAMERA_RES_FAILED;
		  } 
		  finally
		  {
			  lock.unlock();
		  }
	}
	
	public int setCameraProperty(String key, String value)
	{
		Camera.Parameters param = mCamera.getParameters();
		if(key.equals(MAAPI_consts.MA_CAMERA_IMAGE_FORMAT))
		{
			if(value.equals(MAAPI_consts.MA_CAMERA_IMAGE_RAW))
			{
				rawMode = true;
			}
			else
			{
				//default mode is jpeg
				rawMode = false;
			}
		}
		else if(key.equals(MAAPI_consts.MA_CAMERA_FOCUS_MODE))
		{
			if(value.equals(MAAPI_consts.MA_CAMERA_FOCUS_AUTO))
			{
				mCamera.autoFocus(null);
			}
			else if(value.equals(MAAPI_consts.MA_CAMERA_FOCUS_MACRO))
			{
				if(false == param.getSupportedFocusModes().contains(value))
				{
					return MAAPI_consts.MA_CAMERA_RES_VALUE_NOTSUPPORTED;
				}
				mCamera.autoFocus(null);
			}
			else if(false == param.getSupportedFocusModes().contains(value))
			{
				mCamera.cancelAutoFocus();
				return MAAPI_consts.MA_CAMERA_RES_VALUE_NOTSUPPORTED;
			}
			else
				mCamera.cancelAutoFocus();

			param.setFocusMode(value);
		}
		else if(key.equals(MAAPI_consts.MA_CAMERA_FLASH_MODE))
		{
			if(true == param.getSupportedFlashModes().contains(value))
			{
				param.setFlashMode(value);
			}
			else
			{
				return MAAPI_consts.MA_CAMERA_RES_VALUE_NOTSUPPORTED;
			}
		}
		else
		{
			param.set(key, value);
		}
		try
		{
			mCamera.setParameters(param);
		}
		catch (Exception e)
		{
			return MAAPI_consts.MA_CAMERA_RES_FAILED;
		}
		return MAAPI_consts.MA_CAMERA_RES_OK;
	}
	
	public int getCameraPorperty(String key,
			int memBuffer, 
			int memBufferSize)
	{
		Camera.Parameters param = mCamera.getParameters();
		String result;
		if(key.equals(MAAPI_consts.MA_CAMERA_IMAGE_FORMAT))
		{
			if(rawMode == true)
				result = MAAPI_consts.MA_CAMERA_IMAGE_RAW;
			else
				result = MAAPI_consts.MA_CAMERA_IMAGE_JPEG;
		}
		else if(key.equals(MAAPI_consts.MA_CAMERA_FLASH_SUPPORTED))
		{
			if( param.getSupportedFlashModes() != null )
			{
				if(param.getSupportedFlashModes().size() == 1)
				{
					result = "false";
				}
				else
				{
					result = "true";
				}
			}
			else
			{
				result = "false";
			}
		}
		else if(key.equals(MAAPI_consts.MA_CAMERA_ZOOM_SUPPORTED))
		{
			if(param.isZoomSupported())
				result = "true";
			else
				result = "false";
		}
		else
			result = param.get(key);
		if(result == null)
			return MAAPI_consts.MA_CAMERA_RES_INVALID_PROPERTY_VALUE;
		
		if( result.length( ) + 1 > memBufferSize )
		{
			Log.e( "MoSync", "maCameraGetProperty: Buffer size " + memBufferSize + 
					" too short to hold buffer of size: " + result.length( ) + 1 );
			return MAAPI_consts.MA_CAMERA_RES_FAILED;
		}
		
		byte[] ba = result.getBytes();

		// Write string to MoSync memory.
		mMoSyncThread.mMemDataSection.position( memBuffer );
		mMoSyncThread.mMemDataSection.put( ba );
		mMoSyncThread.mMemDataSection.put( (byte)0 );
		
		return result.length( );
	}
	
	/**
	 * Releases the camera in case of pause or exit
	 */
	public void releaseCamera()
	{
		if(mCamera != null)
		{
			mCamera.release();
			if (mPreview != null)
			{
				mPreview.mCamera = null;
			}
			mCamera = null;
		}
	}
	/**
	 * Releases the camera in case of pause or exit
	 */
	public void acquireCamera()
	{
		if(mCamera == null)
		{
			mCamera = Camera.open();
			if(mPreview != null)
			{
				mPreview.mCamera = mCamera;
			}
		}
	}
	
	/**
	 * a wrapper to set a new picture size when taking a snapshot 
	 */
	private void setNewSize(int formatIndex)
	{
		Camera.Parameters parameters = mCamera.getParameters();
		List <Camera.Size> supportedSizes =  parameters.getSupportedPictureSizes();
		int width = userWidths.get(formatIndex).intValue(); 
		int height = userHeights.get(formatIndex).intValue();
		Camera.Size optimalPictureSize = mPreview.getOptimalSize(supportedSizes, width, height);
		parameters.setPictureSize(optimalPictureSize.width,optimalPictureSize.height);
    	try
    	{
			mCamera.setParameters(parameters);
    	}
    	catch (Exception e)
    	{
    		SYSLOG("FAILED TO SET the PARAMETERS");
    	}
	}
	

	/** 
	 * Handles data for raw picture 
	 */
	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			if(rawMode == true)
			{
				lock.lock();
				try {
					mMoSyncThread.nativeCreateBinaryResource(resourceIndex, data.length);
					ByteBuffer byteBuffer = mMoSyncThread.mBinaryResources.get(resourceIndex);
					byteBuffer.put(data);
					dataReady = true;
					condition.signalAll();
				}
				catch (Exception e) {
					SYSLOG("Failed to create the data pool");
				}
				finally {
					lock.unlock();
				}
			}
		}
	};

	/** 
	 * Handles data for jpeg picture
	 */
	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			if(rawMode == false)
			{
				lock.lock();
				try {
					mMoSyncThread.nativeCreateBinaryResource(resourceIndex, data.length);
					ByteBuffer byteBuffer = mMoSyncThread.mBinaryResources.get(resourceIndex);
					byteBuffer.put(data);
					dataReady = true;
					condition.signalAll();
				}
				catch (Exception e) {
					SYSLOG("Failed to create the data pool");
				}
				finally {
					lock.unlock();
				}
			}
		}
	};	
}
