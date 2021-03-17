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

    //Потом удалить
    lateinit var btn_scip: Button

    lateinit var tracks: Array<Track>

    var FILE_SELECT_CODE = 100
    final var cross_fade_min = 2
    final var cross_fade_max = 10



    var playing = false
    var handler = Handler()

    var id_track_now = 0
    lateinit var mediaPlayer_now: MediaPlayer
    lateinit var mediaPlayer_next: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tracks = arrayOf(Track(), Track())

        // Find elements
        btn_play = findViewById(R.id.btn_play)
        txt_crossfade_progress = findViewById(R.id.txt_crossfade_progress)
        txt_crossfade_max = findViewById(R.id.txt_crossfade_max)
        seek_cross_fade = findViewById(R.id.seek_cross_fade)
        img_fon = findViewById(R.id.img_fon)
        controll_panel = findViewById(R.id.controll_panel)

        btn_scip = findViewById(R.id.btn_scip)

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

        // Set listeners
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

        btn_scip.setOnClickListener{
            mediaPlayer_now.seekTo(mediaPlayer_now.duration - (11 * 1000))
        }
    }

    override fun onPause() {
        super.onPause()

        if (playing){
            PlayPause()
        }
    }

    fun getGrossFade(): Int{
        return seek_cross_fade.progress + cross_fade_min
    }

    fun PlayPause(){
        tracks.forEach() { track ->
            if (track.uri == Uri.EMPTY) {
                return
            }
        }

        // Set PLAY
        if(!playing){
            // Смена картинки у кнопик
            btn_play.setImageDrawable(
                    ContextCompat.getDrawable(
                            applicationContext, // Context
                            R.drawable.ic_baseline_pause_24 // Drawable
                    )
            )

            if(tracks[id_track_now].status == Track.STATUS_PAUSE){
                setPlayTrack(id_track_now)
            }

            // Показывает выбор файлов и cross fade
            tracks_items.forEach {
                it.btn_track_select.visibility = View.INVISIBLE
            }
            seek_cross_fade.visibility = View.INVISIBLE
            txt_crossfade_progress.visibility = View.INVISIBLE
            txt_crossfade_max.visibility = View.INVISIBLE
        }
        // Set PAUSE
        else
        {
            // Смена картинки у кнопик
            btn_play.setImageDrawable(
                    ContextCompat.getDrawable(
                            applicationContext, // Context
                            R.drawable.ic_baseline_play_arrow_24 // Drawable
                    )
            )

            if(tracks[id_track_now].status == Track.STATUS_PLAY){
                setPauseTrack(id_track_now)
            }

            // Скрывает выбор файлов и cross fade
            tracks_items.forEach {
                it.btn_track_select.visibility = View.VISIBLE
            }
            seek_cross_fade.visibility = View.VISIBLE
            txt_crossfade_progress.visibility = View.VISIBLE
            txt_crossfade_max.visibility = View.VISIBLE
        }


        playing = !playing
    }


    fun setPlayTrack(id: Int){
        if (tracks[nextTrackId(id)].status == Track.STATUS_PAUSE){
            tracks[nextTrackId(id)].status = Track.STATUS_PLAY
        }
        tracks[id].status = Track.STATUS_PLAY
        createRunnableTrack(id)
    }

    fun setPauseTrack(id: Int){
        if (tracks[nextTrackId(id)].status == Track.STATUS_PLAY){
            tracks[nextTrackId(id)].status = Track.STATUS_PAUSE
        }
        tracks[id].status = Track.STATUS_PAUSE

        createRunnableTrack(id)
    }




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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK){
            if(data != null){
                // Get id trackbar
                setTrack((requestCode - FILE_SELECT_CODE), data.data!!)
            }else{
                Toast.makeText(this, "Track was not selected", Toast.LENGTH_LONG).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

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

        // Действия при проигрывании трека
        tracks[id].runnable = Runnable {
            createRunnableTrack(id)
        }

        // Действие при завершении трека
        mediaPlayer_now.setOnCompletionListener{
            setNextTrack()
        }


        // Показывает панель управления, если выбраны оба трека
        controll_panel.visibility = View.VISIBLE
        tracks.forEach {
            if (it.uri == Uri.EMPTY){
                controll_panel.visibility = View.INVISIBLE
            }
        }
    }

    // Действия при проигрывании трека
    fun createRunnableTrack(id: Int){
        // Изменение громкости
        if (id == id_track_now) {
            var volume = 0f
            // Начало кроссфейда
            if (
                    // Длина дорожки больше кроссфейда
                    (mediaPlayer_now.duration > (getGrossFade() * 1000) &&
                            (mediaPlayer_now.currentPosition < (getGrossFade() * 1000))
                            ) ||
                    // Длина дорожки меньше или равна кроссфейду
                    (mediaPlayer_now.duration <= (getGrossFade() * 1000) &&
                            (mediaPlayer_now.currentPosition <= (mediaPlayer_now.duration / 2))
                            )

            ) {
                if (mediaPlayer_now.duration > (getGrossFade() * 1000)){
                    volume = (mediaPlayer_now.currentPosition) / (getGrossFade() * 1000).toFloat()
                }else{
                    volume = (mediaPlayer_now.currentPosition / (mediaPlayer_now.duration / 2)).toFloat()
                }
                mediaPlayer_now.setVolume(volume, volume)
                if (mediaPlayer_next.isPlaying) {
                    mediaPlayer_next.setVolume(1f - volume, 1f - volume)
                }
            }
            // Конец кроссфейда
            else if (
                    // Длина дорожки больше кроссфейда
                    (mediaPlayer_now.duration > (getGrossFade() * 1000) &&
                            (mediaPlayer_now.currentPosition > (mediaPlayer_now.duration - getGrossFade() * 1000))
                            ) ||
                    // Длина дорожки меньше или равна кроссфейду
                    (mediaPlayer_now.duration <= (getGrossFade() * 1000) &&
                            (mediaPlayer_now.currentPosition > (mediaPlayer_now.duration / 2))
                            )
            ) {
                // Длина дорожки больше кроссфейда
                if (mediaPlayer_now.duration > (getGrossFade() * 1000)){
                    volume = (mediaPlayer_now.duration - mediaPlayer_now.currentPosition).toFloat() / (getGrossFade() * 1000).toFloat()
                    // Запустить следующий трек
                    if (tracks[nextTrackId(id_track_now)].status == Track.STATUS_STOP) {
                        tracks[nextTrackId(id_track_now)].status = Track.STATUS_PLAY
                    }
                }
                // Длина дорожки меньше или равна кроссфейду
                else{
                    volume = (mediaPlayer_now.duration - mediaPlayer_now.currentPosition).toFloat() / (mediaPlayer_now.duration / 2).toFloat()
                    // Запустить следующий трек
                    if (tracks[nextTrackId(id_track_now)].status == Track.STATUS_STOP) {
                        tracks[nextTrackId(id_track_now)].status = Track.STATUS_PLAY
                    }
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

        // Запустить / Поставить на паузу
        if (id == id_track_now){
            if (tracks[id].status == Track.STATUS_PLAY){
                // Запускает текущий трек
                if (!mediaPlayer_now.isPlaying){
                    mediaPlayer_now.start()
                    handler.postDelayed(tracks[id_track_now].runnable, 0)
                }
                // Запускает следующий трек
                if (!mediaPlayer_next.isPlaying){
                    if (tracks[nextTrackId(id)].status == Track.STATUS_PLAY){
                        mediaPlayer_next.start()
                        handler.postDelayed(tracks[nextTrackId(id_track_now)].runnable, 0)
                    }
                }
            }else if (tracks[id].status == Track.STATUS_PAUSE){
                // Ставит на паузу текущий трек
                if (mediaPlayer_now.isPlaying){
                    mediaPlayer_now.pause()
                    handler.removeCallbacks(tracks[id_track_now].runnable)
                }
                // Ставит на паузу следующий трек
                if (mediaPlayer_next.isPlaying){
                    if (tracks[nextTrackId(id_track_now)].status == Track.STATUS_PAUSE){
                        mediaPlayer_next.pause()
                        handler.removeCallbacks(tracks[nextTrackId(id_track_now)].runnable)
                    }
                }
            }
        }


        //-------------------------- Time line --------------------------
        var progress_track = 0f
        if (id == id_track_now) {
            progress_track = (mediaPlayer_now.currentPosition.toFloat() * 100 / mediaPlayer_now.duration.toFloat())
        } else if (id == nextTrackId(id_track_now)) {
            progress_track = (mediaPlayer_next.currentPosition.toFloat() * 100 / mediaPlayer_next.duration.toFloat())
        }
        val la = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                progress_track
        )
        tracks_items[id].time_line.setLayoutParams(la)
        //---------------------------------------------------------------


        // Запустить runnable
        if (tracks[id].status == Track.STATUS_PLAY){
            handler.postDelayed(tracks[id].runnable, 1)
        } else if (tracks[id].status == Track.STATUS_PAUSE){
            handler.removeCallbacks(tracks[id].runnable)
        }
    }

    fun setNextTrack(){
        //-------------------------- Time line --------------------------
        val la = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0f)
        tracks_items[id_track_now].time_line.setLayoutParams(la)
        //---------------------------------------------------------------

        // Останавливает первый трек
        handler.removeCallbacks(tracks[id_track_now].runnable)
        mediaPlayer_now.seekTo(0)
        tracks[id_track_now].status = Track.STATUS_STOP

        // Ставит на место первого следующий трек
        id_track_now = nextTrackId(id_track_now)
        mediaPlayer_now.stop()
        //mediaPlayer_now.release()
        mediaPlayer_now = mediaPlayer_next
        mediaPlayer_next = MediaPlayer.create(this, tracks[nextTrackId(id_track_now)].uri)

        // Действие при завершении трека
        mediaPlayer_now.setOnCompletionListener{
            setNextTrack()
        }
    }

    // Возвращает id следующего трека
    fun nextTrackId(index: Int): Int{
        if (index < (tracks.size - 1)){return (index + 1)}
        return  0
    }

    // Задать воиспрозведения с первого трека
    fun startSettingsForAllTracks(){
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