package it.tim.innovation.jolmilano.cr40devapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main2.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

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
            olivettiClubBackend().createMerchant(MerchantCreationRequest("danielefogna", "123123123", "31.43.13", "via del cazzettino"))
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

    fun olivettiClubBackend(): OlivettiClubBackendApi {
        val olivettiClubBaseUrl = "http://olivetticlub.dallagi.dev:5000"

        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(olivettiClubBaseUrl)
            .build()

        return retrofit.create(OlivettiClubBackendApi::class.java)
    }

}

interface OlivettiClubBackendApi {

    @POST("coupons")
    fun createCoupon(@Body body: CouponCreationRequest): Call<CouponCreationResponse>

    @POST("merchants")
    fun createMerchant(@Body body: MerchantCreationRequest): Call<MerchantCreationResponse>
}


data class CouponCreationRequest(val merchant: String, val description: String, val count: Int)
data class CouponCreationResponse(val merchant: String, val description: String, val count: Int)


data class MerchantCreationRequest(val name: String, val vatNumber: String, val ateco: String, val address:String)
data class MerchantCreationResponse(val name: String, val vatNumber: String, val ateco: String, val address:String, val deals: List<Deal>)
data class Deal(val merchant:String, val description: String, val consumedCoupon: Int, val generatedCoupon:Int)