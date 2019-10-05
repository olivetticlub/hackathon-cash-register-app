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


    @POST("merchants")
    fun createMerchant(@Body body: MerchantCreationRequest): Call<MerchantCreationResponse>
}


data class CouponCreationRequest(val merchant: String, val description: String, val count: Int)
data class CouponCreationResponse(val merchant: String, val description: String, val count: Int)


data class MerchantCreationRequest(
    val name: String,
    val vatNumber: String,
    val ateco: String,
    val address: String
)

data class MerchantCreationResponse(
    val name: String,
    val vatNumber: String,
    val ateco: String,
    val address: String,
    val deals: List<Deal>
)

data class Deal(
    val merchant: String,
    val description: String,
    val generated_coupons_count: String,
    val consumed_coupons_count: String,
    val id: Int
)
