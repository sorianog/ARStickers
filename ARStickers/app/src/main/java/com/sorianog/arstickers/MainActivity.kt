package com.sorianog.arstickers

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.PixelCopy
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var fragment: ArFragment
    var pointer = PointerDrawable()
    var isTracking = false
    var isHitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { takePhoto() }

        fragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        fragment.arSceneView.scene.addOnUpdateListener { frameTime ->
            fragment.onUpdate(frameTime)
            onUpdate()
        }

        initializeGallery()
    }

    private fun onUpdate() {
        val trackingChanged = updateTracking()
        val contentView : View = findViewById(android.R.id.content)

        if (trackingChanged) {
            if (isTracking) {
                contentView.overlay.add(pointer)
            } else {
                contentView.overlay.remove(pointer)
            }
            contentView.invalidate()
        }

        if (isTracking) {
            val hitTestChanged = updateHitTest()
            if (hitTestChanged) {
                pointer.setEnabled(isHitting)
                contentView.invalidate()
            }
        }
    }

    private fun updateTracking() : Boolean {
        val frame = fragment.arSceneView.arFrame
        val wasTracking = isTracking

        isTracking = frame != null && frame.camera.trackingState == TrackingState.TRACKING
        return isTracking != wasTracking
    }

    private fun updateHitTest() : Boolean {
        val frame = fragment.arSceneView.arFrame
        val point = getScreenCenter()
        val hits : List<HitResult>
        val wasHitting = isHitting
        isHitting = false

        if (frame != null) {
            hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting != isHitting
    }

    private fun getScreenCenter() : Point {
        val view : View = findViewById(android.R.id.content)
        return Point(view.width/2, view.height/2)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeGallery() {
        val gallery : LinearLayout = findViewById(R.id.gallery_layout)

        val andy = ImageView(this)
        andy.setImageResource(R.drawable.droid_thumb)
        andy.contentDescription = "andy"
        andy.setOnClickListener { addObject(Uri.parse("andy.sfb")) }
        gallery.addView(andy)

        val cabin = ImageView(this)
        cabin.setImageResource(R.drawable.cabin_thumb)
        cabin.contentDescription = "cabin"
        cabin.setOnClickListener { addObject(Uri.parse("Cabin.sfb")) }
        gallery.addView(cabin)

        val house = ImageView(this)
        house.setImageResource(R.drawable.house_thumb)
        house.contentDescription = "house"
        house.setOnClickListener { addObject(Uri.parse("House.sfb")) }
        gallery.addView(house)

        val igloo = ImageView(this)
        igloo.setImageResource(R.drawable.igloo_thumb)
        igloo.contentDescription = "igloo"
        igloo.setOnClickListener { addObject(Uri.parse("igloo.sfb")) }
        gallery.addView(igloo)
    }

    private fun addObject(model: Uri) {
        val frame = fragment.arSceneView.arFrame
        val pt : Point = getScreenCenter()
        val hits : List<HitResult>

        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())

            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    placeObject(fragment, hit.createAnchor(), model)
                }
            }
        }
    }

    private fun placeObject(fragment: ArFragment, anchor: Anchor, model: Uri) {
        ModelRenderable.builder()
            .setSource(fragment.context, model)
            .build()
            .thenAccept { renderable -> addNodeToScene(fragment, anchor, renderable) }
            .exceptionally { throwable ->
                val builder = AlertDialog.Builder(this)
                builder.setMessage(throwable.message)
                    .setTitle("Codelab error!")
                val dialog = builder.create()
                dialog.show()
                null
            }
    }

    private fun addNodeToScene(fragment: ArFragment, anchor: Anchor, renderable: Renderable) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(fragment.transformationSystem)

        node.renderable = renderable
        node.setParent(anchorNode)
        fragment.arSceneView.scene.addChild(node)
        node.select()
    }

    private fun generateFilename(): String {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
        return dir + File.separator + "Sceneform/" + date + "_screenshot.jpg";
    }

    private fun saveBitmapToDisk(bitmap: Bitmap, fileName: String) {
        val out = File(fileName)
        if (!out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }
        try {
            val outputStream = FileOutputStream(fileName)
            val outputData = ByteArrayOutputStream()

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
            outputData.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (ex: IOException) {
            throw IOException("Failed to save bitmap to disk", ex)
        }
    }

    private fun takePhoto() {
        val filename = generateFilename()
        val view = fragment.arSceneView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()

        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename)
                } catch (e: IOException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }

                val snackbar = Snackbar.make(findViewById(android.R.id.content), "Photo saved", Snackbar.LENGTH_LONG)
                snackbar.setAction("Open in Photos") {
                    val photoFile = File(filename)
                    val photoUri = FileProvider.getUriForFile(this, this.packageName, photoFile)
                    val intent = Intent(Intent.ACTION_VIEW, photoUri)
                    intent.setDataAndType(photoUri, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }.show()

            } else {
                val toast = Toast.makeText(this, ("Failed to copy pixels: $copyResult"), Toast.LENGTH_LONG)
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }
}
