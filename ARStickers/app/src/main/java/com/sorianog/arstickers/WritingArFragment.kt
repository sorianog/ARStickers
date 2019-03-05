package com.sorianog.arstickers

import android.Manifest
import android.graphics.Bitmap
import android.os.Environment
import com.google.ar.sceneform.ux.ArFragment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class WritingArFragment : ArFragment() {

    override fun getAdditionalPermissions(): Array<String> {
        val additionalPermissions = super.getAdditionalPermissions()
        val permissionLength = additionalPermissions?.size ?: 0
        val permissions = arrayOfNulls<String>(permissionLength + 1)

        permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (permissionLength > 0) {
            System.arraycopy(additionalPermissions, 0, permissions, 1, additionalPermissions.size)
        }

        return additionalPermissions
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
}