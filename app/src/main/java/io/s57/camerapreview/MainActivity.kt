package io.s57.camerapreview


import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.renderscript.RenderScript
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var  surfaceCamPreview: SurfaceCamPreview

    private  lateinit var imgReader : ImageReader

    private val requestCodePermission = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgReader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 2)

        imgReader.setOnImageAvailableListener({
            val image = it.acquireLatestImage()
            if (image == null) {
                Log.i("ZKAPP", "got null image");
             return@setOnImageAvailableListener
            }
            processImage(image)

            Log.i("ZKAPP","ZK --- got new image ------------");

            image.close()
        }, null)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), requestCodePermission)
        } else {
            setupCameraPreview()
        }

    }
    fun imageToBitmap(image: Image): Bitmap {
        val yuvImage = YuvImage(
            image.planes[0].buffer.array(),
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun yuv420ToBitmap(image: Image): Bitmap {
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        val size = Size(image.width, image.height)
        yuvImage.compressToJpeg(Rect(0, 0, size.width, size.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun processImage(image: Image?) {
        // Convert yuv image to bitmap
        surfaceCamPreview.setBitmap(yuv420ToBitmap(image!!))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermission) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCameraPreview()
            } else {
                finish()
            }
        }
    }

    private fun setupCameraPreview() {
        surfaceCamPreview = SurfaceCamPreview(this, null, imgReader)
        setContentView(surfaceCamPreview)
    }
}
