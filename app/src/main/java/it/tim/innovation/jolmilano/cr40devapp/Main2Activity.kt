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


        create_merchant.setOnClickListener {
            service.createMerchant(
                MerchantCreationRequest(
                    "danielefogna",
                    "123123123",
                    "31.43.13",
                    "via del cazzettino"
                )
            )
                .enqueue(object :
                    Callback<MerchantCreationResponse> {
                    override fun onFailure(call: Call<MerchantCreationResponse>, t: Throwable) {
                        Toast.makeText(applicationContext, "errore", Toast.LENGTH_LONG).show()
                    }

                    override fun onResponse(
                        call: Call<MerchantCreationResponse>,
                        response: Response<MerchantCreationResponse>
                    ) {
                        Log.d("Main2Activity", response.body().toString())
                    }

                })
        }
    }


}
