package io.s57.camerapreview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.app.ActivityCompat

class SurfaceCamPreview(
    context: Context,
    attrs: AttributeSet? = null,
    val imageReader: ImageReader
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var mContext: Context
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var cameraManager: CameraManager;

    private val surfaceHolder: SurfaceHolder = holder.apply {
        addCallback(this@SurfaceCamPreview)
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    init {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        mContext = context
    }

     fun open( id : String) {
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }, null)
    }
    private fun createPreviewSession() {
        val previewSurface = imageReader.surface

        val surfaceList = listOf(previewSurface)

        cameraDevice.createCaptureSession(surfaceList, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                session.setRepeatingRequest(
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(previewSurface)
                    }.build(),
                    null,
                    null
                )
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                // Handle configuration failure
            }
        }, null)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        open("0")
        // Nothing to do here, the camera is opened during the view initialization.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Nothing to do here, the camera preview is updated automatically.
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        cameraCaptureSession.close()
        cameraDevice.close()
    }

    fun setBitmap(bitmap: Bitmap?) {
        if (surfaceHolder.surface == null) {
            return
        }

        val canvas = surfaceHolder.lockCanvas()
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
        surfaceHolder.unlockCanvasAndPost(canvas)

    }
}
