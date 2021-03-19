package com.example.sound_cross_fade

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    lateinit var btn_play: ImageButton
    lateinit var txt_crossfade_progress: TextView
    lateinit var txt_crossfade_max: TextView
    lateinit var seek_cross_fade: SeekBar
    lateinit var tracks_items: Array<TrackAdapter>
    lateinit var img_fon: ImageView
    lateinit var controll_panel: LinearLayout

    lateinit var tracks: Array<Track>

    var FILE_SELECT_CODE = 100
    final var cross_fade_min = 2
    final var cross_fade_max = 10



    var playing = false
    var handler = Handler()
    lateinit var runnable: Runnable

    var id_track_now = 0
    lateinit var mediaPlayer_now: MediaPlayer
    lateinit var mediaPlayer_next: MediaPlayer

    var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tracks = arrayOf(Track(), Track())

        // Определяет элементы
        btn_play = findViewById(R.id.btn_play)
        txt_crossfade_progress = findViewById(R.id.txt_crossfade_progress)
        txt_crossfade_max = findViewById(R.id.txt_crossfade_max)
        seek_cross_fade = findViewById(R.id.seek_cross_fade)
        img_fon = findViewById(R.id.img_fon)
        controll_panel = findViewById(R.id.controll_panel)

        // Скрывает панель управления, дожидаясь выбора треков
        controll_panel.visibility = View.INVISIBLE

        tracks_items = arrayOf(
                TrackAdapter(
                        findViewById(R.id.btn_track_1_select),
                        findViewById(R.id.txt_track_1_name),
                        findViewById(R.id.img_track_1),
                        findViewById(R.id.time_line_1)
                ),
                TrackAdapter(
                        findViewById(R.id.btn_track_2_select),
                        findViewById(R.id.txt_track_2_name),
                        findViewById(R.id.img_track_2),
                        findViewById(R.id.time_line_2)
                )
        )

        seek_cross_fade.max = cross_fade_max - cross_fade_min
        seek_cross_fade.progress = 6 - cross_fade_min
        txt_crossfade_progress.text = (seek_cross_fade.progress + cross_fade_min).toString() + "s"

        // Устанавливает собятия
        btn_play.setOnClickListener{ PlayPause() }
        tracks_items.forEachIndexed(){index, track_item ->
            track_item.btn_track_select.setOnClickListener { fileChooser(index) }
        }

        seek_cross_fade.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                txt_crossfade_progress.text = (progress + cross_fade_min).toString() + "s"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        runnable = Runnable {
            createRunnableTrack()
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 0)
        }


        //-------------------------- Time line --------------------------
        // Для текущего трека
        setTimeLine(id_track_now, 0, 1)
        // Для следующего трека
        setTimeLine(nextTrackId(id_track_now), 0, 1)
        //---------------------------------------------------------------
    }

    override fun onPause() {
        super.onPause()

        // Принудительно ставит на паузу
        if (playing){
            PlayPause()
        }
    }

    // Получает текущий кроссфейд
    fun getGrossFade(): Int{
        return seek_cross_fade.progress + cross_fade_min
    }

    // Играть / поставить на паузу
    fun PlayPause(){
        tracks.forEach() { track ->
            if (track.uri == Uri.EMPTY) {
                return
            }
        }

        playing = !playing

        // Set PLAY
        if(playing){
            mediaPlayer_now.seekTo(currentPosition)

            // Смена картинки у кнопик
            btn_play.setImageDrawable(
                    ContextCompat.getDrawable(
                            applicationContext, // Context
                            R.drawable.ic_baseline_pause_24 // Drawable
                    )
            )

            // Показывает выбор файлов и cross fade
            tracks_items.forEach {
                it.btn_track_select.visibility = View.INVISIBLE
            }

            // Скрывает элементы
            seek_cross_fade.visibility = View.INVISIBLE
            txt_crossfade_progress.visibility = View.INVISIBLE
            txt_crossfade_max.visibility = View.INVISIBLE
        }
        // Set PAUSE
        else
        {
            currentPosition = mediaPlayer_now.currentPosition

            // Смена картинки у кнопик
            btn_play.setImageDrawable(
                    ContextCompat.getDrawable(
                            applicationContext, // Context
                            R.drawable.ic_baseline_play_arrow_24 // Drawable
                    )
            )

            // Скрывает выбор файлов и cross fade
            tracks_items.forEach {
                it.btn_track_select.visibility = View.VISIBLE
            }

            // Показывает элементы
            seek_cross_fade.visibility = View.VISIBLE
            txt_crossfade_progress.visibility = View.VISIBLE
            txt_crossfade_max.visibility = View.VISIBLE
        }
    }

    // Выбор трека
    fun fileChooser(btn_id: Int){
        intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.setType("audio/*")
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(
                Intent.createChooser(intent, "Select a File to Upload"),
                FILE_SELECT_CODE + btn_id)
        } catch (ex: ActivityNotFoundException) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                Toast.LENGTH_SHORT).show()
        }
    }

    // Когда трек выбран, занесты его в реестр
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK){
            if(data != null){
                // Установить трек
                setTrack((requestCode - FILE_SELECT_CODE), data.data!!)
            }else{
                Toast.makeText(this, "Track was not selected", Toast.LENGTH_LONG).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // Заносит трек в список
    fun setTrack(id: Int, uri: Uri){
        tracks.set(id, Track(this, uri))

        tracks_items[id].txt_track_name.text = tracks[id].name
        if (tracks[id].image != null){
            tracks_items[id].img_track.setImageBitmap(tracks[id].image)
        }else{
            tracks_items[id].img_track.setImageResource(R.drawable.note)
        }

        // Начать воспроизведение с первого терка
        startSettingsForAllTracks()

        // Установить медиаплееры
        try {
            if(id == id_track_now){
                mediaPlayer_now = MediaPlayer.create(this, tracks[id].uri)
                if (tracks[nextTrackId(id)].uri != Uri.EMPTY){
                    mediaPlayer_next = MediaPlayer.create(this, tracks[nextTrackId(id)].uri)
                }
            }else if(id == nextTrackId(id_track_now)){
                mediaPlayer_next = MediaPlayer.create(this, tracks[id].uri)
                if (tracks[id_track_now].uri != Uri.EMPTY){
                    mediaPlayer_now = MediaPlayer.create(this, tracks[id_track_now].uri)
                }
            }
        }catch (e: Exception){
            Toast.makeText(this, "Error opening file", Toast.LENGTH_LONG).show()
        }

        // Действие при завершении трека
        mediaPlayer_now.setOnCompletionListener{
            setNextTrack()
        }


        // Показывает панель управления, если выбраны оба трека
        var test_all_tracks = true
        tracks.forEach {
            if (it.uri == Uri.EMPTY){
                test_all_tracks = false
            }
        }
        if (test_all_tracks){
            handler.postDelayed(runnable, 1)
            controll_panel.visibility = View.VISIBLE
        }else{
            handler.removeCallbacks(runnable)
            controll_panel.visibility = View.INVISIBLE
        }
    }

    // Действия при проигрывании трека
    fun createRunnableTrack(){
        tracks.forEach() { track ->
            if (track.uri == Uri.EMPTY) {
                return
            }
        }


        // Изменение громкости
        if (playing) {
            var volume = 0f
            var crossfade_value = (getGrossFade() * 1000)
            if (crossfade_value > (mediaPlayer_now.duration / 2)){
                crossfade_value = mediaPlayer_now.duration / 2
            }
            // Начало кроссфейда
            if (mediaPlayer_now.currentPosition <= crossfade_value) {
                volume = (mediaPlayer_now.currentPosition) / (crossfade_value).toFloat()
                volume = volume * volume
                mediaPlayer_now.setVolume(volume, volume)
                if (mediaPlayer_next.isPlaying) {
                    mediaPlayer_next.setVolume(1f - volume, 1f - volume)
                }
            }
            // Конец кроссфейда
            else if (mediaPlayer_now.currentPosition >= (mediaPlayer_now.duration - crossfade_value)) {
                volume = (mediaPlayer_now.duration - mediaPlayer_now.currentPosition).toFloat() / crossfade_value.toFloat()
                volume = volume * volume
                // Запустить следующий трек
                if (tracks[nextTrackId(id_track_now)].status == Track.STATUS_STOP) {
                    tracks[nextTrackId(id_track_now)].status = Track.STATUS_PAUSE
                    mediaPlayer_next.seekTo(0)
                }
                mediaPlayer_now.setVolume(volume, volume)
                if (mediaPlayer_next.isPlaying) {
                    mediaPlayer_next.setVolume(1f - volume, 1f - volume)
                }
            }
            // Середина кроссфейда
            else {
                volume = 1f
                mediaPlayer_now.setVolume(volume, volume)
                mediaPlayer_next.setVolume(0f, 0f)
            }
        }

        // Для текущего трека
        if (playing && (tracks[id_track_now].status != Track.STATUS_PLAY)){
            tracks[id_track_now].status = Track.STATUS_PLAY
            mediaPlayer_now.start()
        }else if (!playing && (tracks[id_track_now].status != Track.STATUS_PAUSE)){
            tracks[id_track_now].status = Track.STATUS_PAUSE
            mediaPlayer_now.pause()
        }

        // Для следующего трека
        if ((tracks[nextTrackId(id_track_now)].status == Track.STATUS_PAUSE) ||
            (tracks[nextTrackId(id_track_now)].status == Track.STATUS_PLAY)){
            if (playing && (tracks[nextTrackId(id_track_now)].status != Track.STATUS_PLAY)){
                tracks[nextTrackId(id_track_now)].status = Track.STATUS_PLAY
                mediaPlayer_next.start()
            }else if (!playing && (tracks[nextTrackId(id_track_now)].status != Track.STATUS_PAUSE)){
                tracks[nextTrackId(id_track_now)].status = Track.STATUS_PAUSE
                mediaPlayer_next.pause()
            }
        }


        //-------------------------- Time line --------------------------
        // Для текущего трека
        setTimeLine(id_track_now, mediaPlayer_now.currentPosition, mediaPlayer_now.duration)
        // Для следующего трека
        setTimeLine(nextTrackId(id_track_now), mediaPlayer_next.currentPosition, mediaPlayer_next.duration)
        //---------------------------------------------------------------
    }

    // Запустить следующий трек
    fun setNextTrack(){
        if (!playing){return}

        //-------------------------- Time line --------------------------
        val la = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0f)
        tracks_items[id_track_now].time_line.setLayoutParams(la)
        //---------------------------------------------------------------

        // Останавливает первый трек
        mediaPlayer_now.stop()
        mediaPlayer_now.release()
        tracks[id_track_now].status = Track.STATUS_STOP

        // Ставит на место первого следующий трек
        id_track_now = nextTrackId(id_track_now)
        mediaPlayer_now = mediaPlayer_next
        mediaPlayer_next = MediaPlayer.create(this, tracks[nextTrackId(id_track_now)].uri)

        if (playing){
            if (!mediaPlayer_now.isPlaying){
                mediaPlayer_now.start()
            }
        }

        // Действие при завершении трека
        mediaPlayer_now.setOnCompletionListener{
            setNextTrack()
        }
    }

    fun setTimeLine(id: Int, currentPosition: Int, duration: Int){
        var la = LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            (((currentPosition.toFloat() / 500).toInt() * 500) / duration.toFloat()) * 100
        )
        tracks_items[id].time_line.setLayoutParams(la)
    }

    // Возвращает id следующего трека
    fun nextTrackId(index: Int): Int{
        if (index < (tracks.size - 1)){return (index + 1)}
        return  0
    }

    // Задать воиспрозведения с первого трека
    fun startSettingsForAllTracks(){
        currentPosition = 0
        id_track_now = 0
        tracks.forEachIndexed(){ i, track ->
            track.status = Track.STATUS_STOP
            if (i == 0){
                track.status = Track.STATUS_PAUSE
            }

            // Шкала времени трека (Сбросить в 0)
            val la = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,0f)
            tracks_items[i].time_line.setLayoutParams(la)
        }
    }
}