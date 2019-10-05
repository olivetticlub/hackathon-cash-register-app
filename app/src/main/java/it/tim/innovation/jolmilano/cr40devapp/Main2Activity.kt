package it.tim.innovation.jolmilano.cr40devapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main2.*

class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        go.setOnClickListener {
            Log.d(Main2Activity::class.java.simpleName, "ciaone")
        }
    }
}
