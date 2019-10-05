package it.tim.innovation.jolmilano.cr40devapp

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class OlivettiClubBackendServiceFactory {

    companion object {
        fun create(): OlivettiClubBackendApi {
            val olivettiClubBaseUrl = "http://olivetticlub.dallagi.dev:5000"

            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(olivettiClubBaseUrl)
                .build()

            return retrofit.create(OlivettiClubBackendApi::class.java)
        }
    }
    
}

interface OlivettiClubBackendApi {

    @POST("coupons")
    fun createCoupon(@Body body: CouponCreationRequest): Call<CouponCreationResponse>
}

data class CouponCreationRequest(val merchant: String, val description: String, val count: Int)
data class CouponCreationResponse(val merchant: String, val description: String, val count: Int)
