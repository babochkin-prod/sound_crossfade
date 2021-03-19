package com.example.sound_cross_fade

import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.widget.Toast
import java.io.File


class Track {
    var uri: Uri
    var  name: String
    var image: Bitmap? = null
    var status: Int = 0

    companion object {
        const val STATUS_PLAY = 1
        const val STATUS_PAUSE = 2
        const val STATUS_STOP = 3
    }

    constructor(){
        this.uri = Uri.EMPTY
        this.name = "EMPTY"
    }

    constructor(context: Context, uri: Uri){
        this.uri = uri
        this.name = getName(context, uri)

        try {
            this.image = getImage(context, uri)
        }catch (e: Exception){
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }




    //============================ Get metadata ============================

    // Получает название трека
    fun getName(context: Context, uri: Uri): String{
        // TITLE
        val metaRetriever = MediaMetadataRetriever()
        metaRetriever.setDataSource(context, uri)
        val artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        metaRetriever.release()

        if (artist != null && title != null) {
            return title + "\n" + "(" + artist + ")"
        }

        val path = uri.path
        return if(uri != Uri.EMPTY) {
            File(uri.path).name
        } else { "unknown" }
    }

    // Получает изображение трека
    fun getImage(context: Context, uri: Uri): Bitmap? {
        val metaRetriever = MediaMetadataRetriever()
        metaRetriever.setDataSource(context, uri)
        var bm: Bitmap? = null

        try {
            var ba = metaRetriever.getEmbeddedPicture()
            bm = getRoundBitmap(BitmapFactory.decodeByteArray(ba, 0, ba.size))
        }catch (e: java.lang.Exception){
            return bm
        }
        metaRetriever.release()
        return bm
    }
    // Делает картинку круглой
    fun getRoundBitmap(bitmap: Bitmap): Bitmap? {
        val min = Math.min(bitmap.width, bitmap.height)
        val bitmapRounded = Bitmap.createBitmap(min, min, bitmap.config)
        val canvas = Canvas(bitmapRounded)
        val paint = Paint()
        paint.setAntiAlias(true)
        paint.setShader(BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        canvas.drawRoundRect(
            RectF(0.0f, 0.0f, min.toFloat(), min.toFloat()),
            (min / 2).toFloat(),
            (min / 2).toFloat(),
            paint
        )
        return bitmapRounded
    }
}