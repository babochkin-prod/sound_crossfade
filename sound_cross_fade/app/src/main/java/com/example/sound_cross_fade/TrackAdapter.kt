package com.example.sound_cross_fade

import android.view.View
import android.widget.*

class TrackAdapter {
    var btn_track_select: ImageButton
    var txt_track_name: TextView
    var img_track: ImageView
    var time_line: LinearLayout

    constructor(btn_track_select: ImageButton, txt_track_name: TextView, img_track: ImageView, time_line: LinearLayout){
        this.btn_track_select = btn_track_select
        this.txt_track_name = txt_track_name
        this.img_track = img_track
        this.time_line = time_line
    }
}