package com.example.francesco.ingredientsscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.IOException


/**
 * @param filePath name of a jpeg file to convert to bitmap
 * @return image converted to bitmap
 */
fun loadBitmapFromFile(filePath: String): Bitmap? {
    var bitmap = BitmapFactory.decodeFile(filePath)

    //rotate pic according to EXIF data (Francesco Pham)
    try {
        val exif = ExifInterface(filePath)
        val rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationInDegrees = exifToDegrees(rotation)
        val matrix = Matrix()
        if (rotation != 0) {
            matrix.preRotate(rotationInDegrees)
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return bitmap
}

/**
 * Convert exif orientation information from image into degree
 * @param exifOrientation Exif orientation value
 * @return degree
 */
fun exifToDegrees(exifOrientation: Int): Float {
    if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
        return 90f
    } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
        return 180f
    } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
        return 270f
    }
    return 0f
}