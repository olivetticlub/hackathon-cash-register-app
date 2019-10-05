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
package it.tim.innovation.jolmilano.olivetticlub.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import it.tim.innovation.jolmilano.olivetticlub.R
import it.tim.innovation.jolmilano.olivetticlub.model.Item
import it.tim.innovation.jolmilano.olivetticlub.utils.Utils
import it.tim.innovation.jolmilano.olivetticlub.viewmodels.ProductsViewModel
import it.tim.innovation.jolmilano.olivetticlub.viewmodels.TransactionsViewModel
import kotlinx.android.synthetic.main.fragment_list_products.*
import kotlinx.android.synthetic.main.product_item.view.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


/**
 * Created by Gaetano Dati on 05/07/2019
 */
class ProductsListFragment : Fragment() {

    private lateinit var mProductsViewModel: ProductsViewModel
    private lateinit var mTransactionViewModel: TransactionsViewModel
    private val list = mutableListOf<Item>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list_products, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { fragmentActivity ->
            mProductsViewModel = ViewModelProviders.of(this).get(ProductsViewModel::class.java)
            mTransactionViewModel =
                ViewModelProviders.of(this).get(TransactionsViewModel::class.java)

            productsList.adapter = ProductAdapter(list, context!!)

            productsList.setOnItemClickListener { adapterView, view, position, l ->
                mTransactionViewModel.insertItem(this.list[position])
            }

            mProductsViewModel.getAllItems().observe(this, Observer { list ->
                this.list.clear()
                this.list.addAll(list)
                (this.productsList.adapter as BaseAdapter).notifyDataSetChanged()

                if (list.isNotEmpty()) {
                    productsList.visibility = View.VISIBLE
                    noItemsTv.visibility = View.GONE
                } else {
                    productsList.visibility = View.GONE
                    noItemsTv.visibility = View.VISIBLE
                }
            })

            deleteAll.setOnClickListener {
                val builder = AlertDialog.Builder(fragmentActivity)
                builder.setTitle(R.string.delete_all_products_dialog_title)
                builder.setMessage(R.string.delete_all_products_dialog_message)
                builder.setPositiveButton(R.string.yes) { dialog, which ->
                    mProductsViewModel.deleteAllItems()
                    Utils.hideSoftKeyboard(fragmentActivity)
                    dialog.dismiss()
                }
                builder.setNegativeButton(R.string.no) { dialog, which ->
                    dialog.dismiss()
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }

            syncProducts.setOnClickListener {
                val token =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTmFtZSI6ImhhY2thdGhvbiIsInVzZXJUeXBlIjoicmVndWxhciIsImlhdCI6MTU2OTMzNDk0Mn0.wf6JYu6zt0gCxNPMPRWFae9vvlZrj9eaRAgXJIDP3kM"
                val baseURL = "https://www.selfscanner.net/wsbackend/users/hackathon/"
                selfScannerRestServiceApi(token, baseURL).products()
                    .enqueue(object : Callback<SelfScannerResponse> {
                        override fun onFailure(call: Call<SelfScannerResponse>, t: Throwable) {
                            Toast.makeText(context!!, t.localizedMessage, Toast.LENGTH_LONG).show()
                        }

                        override fun onResponse(
                            call: Call<SelfScannerResponse>,
                            response: Response<SelfScannerResponse>
                        ) {
                            response.body()?.let { response ->
                                response.data.forEach { product ->
                                    mProductsViewModel.insert(
                                        Item(
                                            product.productSku,
                                            product.productName,
                                            "",
                                            product.productImageUrl ?: "",
                                            product.productSku,
                                            product.productPrice
                                        )
                                    )
                                }
                            }
                        }

                    })

            }
        }
    }

    private fun selfScannerRestServiceApi(
        authenticationToken: String,
        baseURL: String
    ): SelfScannerRestServiceApi {
        val okHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $authenticationToken")
                .build()
            chain.proceed(request)
        }.build()

        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseURL)
            .build()

        return retrofit.create(SelfScannerRestServiceApi::class.java)
    }
}


interface SelfScannerRestServiceApi {

    @GET("products")
    fun products(): Call<SelfScannerResponse>

}

data class SelfScannerResponse(val code: Int, val data: List<Product>)

data class Product(
    val productSku: String,
    val productName: String,
    val productImageUrl: String?,
    val productPrice: Int
)

class ProductAdapter(private val productList: List<Item>, private val context: Context) :
    BaseAdapter() {

    override fun getView(position: Int, recycledView: View?, parent: ViewGroup?): View {

        val view = LayoutInflater.from(context).inflate(R.layout.product_item, null)

        view.productName.text = productList[position].product
        view.productPrice.text = "${productList[position].price}"



        return view
    }

    override fun getItem(p0: Int): Any {
        return Any()
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return productList.size
    }

}