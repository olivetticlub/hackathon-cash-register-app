package it.tim.innovation.jolmilano.cr40devapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_add_coupons.*

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
    }
}
