/* MIT License

Copyright (c) 2019 TIM S.p.A.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. */
package it.tim.innovation.jolmilano.olivetticlub

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.jolmi.elaconnector.messages.ElaResponse
import kotlinx.android.synthetic.main.activity_responses.*

/**
 * Created by Gaetano Dati on 08/07/2019
 */
class ResponsesActivity : AppCompatActivity() {

    private val TAG = ResponsesActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_responses)
        Log.d(TAG, "onCreate")
        intent?.let {
            Log.d(TAG, "intent != null")
            it.extras?.let { extras ->
                Log.d(TAG, "extras -> $extras")
                val elaList = extras.getSerializable("LIST") as ArrayList<ElaResponse>

                val builder = StringBuilder()

                for (i in elaList) {
                    builder.append(i.toString()).append("\n\n")
                }

                Log.d(TAG,"builder to String --> $builder")

                elaResponseTv.text = builder.toString()
            }
        }

        dismiss.setOnClickListener { finish() }
    }
}