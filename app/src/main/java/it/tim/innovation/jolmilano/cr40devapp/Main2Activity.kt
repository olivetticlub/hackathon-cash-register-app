package it.tim.innovation.jolmilano.cr40devapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main2.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Main2Activity : AppCompatActivity() {

    val service = OlivettiClubBackendServiceFactory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        create_coupons.setOnClickListener {
            service.createCoupon(
                CouponCreationRequest(
                    "danielefongo",
                    "descrizione",
                    1
                )
            )
                .enqueue(object :
                    Callback<CouponCreationResponse> {
                    override fun onFailure(call: Call<CouponCreationResponse>, t: Throwable) {
                        Toast.makeText(applicationContext, "errore", Toast.LENGTH_LONG).show()
                    }

                    override fun onResponse(
                        call: Call<CouponCreationResponse>,
                        response: Response<CouponCreationResponse>
                    ) {
                        Log.d("Main2Activity", response.body().toString())
                    }

                })
        }
    }

}

