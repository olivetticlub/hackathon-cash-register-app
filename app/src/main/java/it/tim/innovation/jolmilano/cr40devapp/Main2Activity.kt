package it.tim.innovation.jolmilano.cr40devapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main2.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        create_coupons.setOnClickListener {
            createCoupons().createCoupon(CouponCreationRequest("danielefongo", "descrizione", 1))
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

    fun createCoupons(): OlivettiClubBackendApi {
        val olivettiClubBaseUrl = "http://olivetticlub.dallagi.dev:5000"
        val okHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .build()
            chain.proceed(request)
        }.build()

        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(olivettiClubBaseUrl)
            .build()

        return retrofit.create(OlivettiClubBackendApi::class.java)
    }

}

interface OlivettiClubBackendApi {

    @POST("coupons")
    fun createCoupon(@Body body: CouponCreationRequest): Call<CouponCreationResponse>
}


data class CouponCreationRequest(val merchant: String, val description: String, val count: Int)
data class CouponCreationResponse(val merchant: String, val description: String, val count: Int)
