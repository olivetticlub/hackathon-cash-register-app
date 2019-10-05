package it.tim.innovation.jolmilano.cr40devapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_add_coupons.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddCouponsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_coupons)

        couponNumberPicker.minValue = 1
        couponNumberPicker.maxValue = 10
        couponNumberPicker.wrapSelectorWheel = false
        couponNumberPicker.displayedValues = (1..10).map { "${it * 10}" }.toTypedArray()


        discountPercentPicker.minValue = 1
        discountPercentPicker.maxValue = 19
        discountPercentPicker.wrapSelectorWheel = false
        discountPercentPicker.displayedValues = (1..19).map { "${it * 5}" }.toTypedArray()


        generateButton.setOnClickListener {
            OlivettiClubBackendServiceFactory.create().createCoupon(
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
