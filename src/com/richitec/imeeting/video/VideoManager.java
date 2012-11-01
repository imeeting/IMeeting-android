package com.richitec.imeeting.video;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.richitec.commontoolkit.utils.VersionUtils;
import com.richitec.imeeting.constants.SystemConstants;

/**
 * Manage the camera resource, take charge of uploading and fetch live video
 * 
 * @author star
 * 
 */
public class VideoManager implements Camera.PreviewCallback,
		SurfaceHolder.Callback {
	enum CameraPosition {
		FrontCamera(1), BackCamera(0);

		private int code;

		private CameraPosition(int val) {
			this.code = val;
		}

		public int code() {
			return code;
		}
	}

	private CameraPosition currentCameraPostion;

	private Camera camera;
	private SurfaceView previewSurface;
	private ViewGroup previewSurfaceParent;
	private ECVideoEncoder videoEncoder;

	private boolean videoLiving;

	public VideoManager(Context context) {
		currentCameraPostion = CameraPosition.FrontCamera;
		previewSurface = new SurfaceView(context);
		previewSurface.getHolder().addCallback(this);
		previewSurface.getHolder().setType(
				SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		videoEncoder = new ECVideoEncoder();
		videoLiving = false;
	}

	public void setRtmpUrl(String rtmpUrl) {
		videoEncoder.setRtmpUrl(rtmpUrl);
	}

	public void setLiveName(String liveName) {
		videoEncoder.setLiveName(liveName);
	}

	public void setGroupId(String groupId) {
		videoEncoder.setGroupId(groupId);
	}

	public void setImgWidth(int imgWidth) {
		videoEncoder.setOutImgWidth(imgWidth);
	}

	public void setImgHeight(int imgHeight) {
		videoEncoder.setOutImgHeight(imgHeight);
	}

	private void releaseCamera() {
		if (camera != null) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
	}

	public boolean isVideoLiving() {
		return videoLiving;
	}

	public void attachVideoPreview(ViewGroup parentView) {
		previewSurfaceParent = parentView;
		if (previewSurfaceParent != null) {
			previewSurface.setLayoutParams(previewSurfaceParent
					.getLayoutParams());
			previewSurfaceParent.addView(previewSurface);
		}
	}

	public void detachVideoPreview() {
		if (previewSurfaceParent != null) {
			previewSurfaceParent.removeView(previewSurface);
		}
	}

	/**
	 * start to live video
	 * 
	 * @throws Exception
	 */
	public void startVideoLive() throws Exception {
		startVideoCapture();

		new Thread(new Runnable() {

			@Override
			public void run() {
				videoEncoder.setupVideoEncoder();
			}
		}).start();

		videoLiving = true;
	}

	private void startVideoCapture() throws Exception {
		camera = getCamera(currentCameraPostion);
		if (camera != null) {
			camera.setPreviewDisplay(previewSurface.getHolder());
			camera.setPreviewCallback(this);
			camera.startPreview();
			Log.d(SystemConstants.TAG, "start to live video");
		} else {
			throw new Exception("camera not found!");
		}
	}

	public void stopVideoLive() {
		releaseCamera();
		videoEncoder.releaseVideoEncoder();
		videoLiving = false;
	}

	public void switchCamera() {
		releaseCamera();
		if (currentCameraPostion == CameraPosition.FrontCamera) {
			currentCameraPostion = CameraPosition.BackCamera;
		} else {
			currentCameraPostion = CameraPosition.FrontCamera;
		}
		try {
			startVideoCapture();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@TargetApi(9)
	private Camera getCamera(CameraPosition position) {
		Camera camera = null;
		int sdk = android.os.Build.VERSION.SDK_INT;
		Log.d(SystemConstants.TAG, "version sdk: " + sdk);
		try {
			if (position == CameraPosition.FrontCamera) {
				if (sdk >= 9) {
					// android version is equal to or larger than API 9
					// we can use the following code
					int cameraNum = Camera.getNumberOfCameras();
					if (cameraNum > 1) {
						CameraInfo cinfo = new CameraInfo();
						for (int i = 0; i < cameraNum; i++) {
							Camera.getCameraInfo(i, cinfo);
							if (cinfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
								camera = Camera.open(i);
								camera.setDisplayOrientation(90);
								Parameters p = camera.getParameters();
								p.setPreviewSize(320, 240);
								camera.setParameters(p);
								currentCameraPostion = CameraPosition.FrontCamera;
								Log.d(SystemConstants.TAG,
										"open front camera ok!!!");
								break;
							}
						}
					}
				} else {
					// there is no way to get front camera below version 2.3
					// so return back camera
					camera = getCamera(CameraPosition.BackCamera);
				}
			} else {
				// get back camera
				camera = Camera.open(); // attempt to get back Camera instance
				if (sdk >= 8) {
					camera.setDisplayOrientation(90);
				}
				Parameters p = camera.getParameters();
				p.setPreviewSize(320, 240);
				camera.setParameters(p);
				currentCameraPostion = CameraPosition.BackCamera;
				Log.d(SystemConstants.TAG, "open back camera ok!!!");
			}
		} catch (Exception e) {
			Log.d(SystemConstants.TAG, "open camera failed");
			e.printStackTrace();
		}
		return camera;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		Size size = camera.getParameters().getPreviewSize();
		int w = size.width;
		int h = size.height;

		videoEncoder.processRawFrame(data, w, h);
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		try {
			if (camera != null) {
				camera.setPreviewDisplay(previewSurface.getHolder());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {

	}
}
