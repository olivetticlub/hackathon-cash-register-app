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

import android.app.Activity
import android.content.*
import android.hardware.display.DisplayManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.IBinder
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import it.jolmi.elaconnector.messages.Barcode
import it.jolmi.elaconnector.messages.ElaResponse
import it.jolmi.elaconnector.messages.Receipt
import it.jolmi.elaconnector.messages.ReceiptItem
import it.jolmi.elaconnector.messages.enums.*
import it.jolmi.elaconnector.messages.enums.CodeType
import it.jolmi.elaconnector.service.BroadcastValues
import it.jolmi.elaconnector.service.IElaResponseListener
import it.jolmi.elaconnector.service.printer.ElaPrinterLocalBinder
import it.jolmi.elaconnector.service.printer.IElaPrinter
import it.jolmi.elaconnector.work.ElaService
import it.tim.innovation.jolmilano.olivetticlub.enums.CouponType
import it.tim.innovation.jolmilano.olivetticlub.fragments.TransactionFragment
import it.tim.innovation.jolmilano.olivetticlub.model.QrCodeItem
import it.tim.innovation.jolmilano.olivetticlub.model.ReceiptLine
import it.tim.innovation.jolmilano.olivetticlub.presentation.MainPresentation
import it.tim.innovation.jolmilano.olivetticlub.presentation.TransactionPresentation
import it.tim.innovation.jolmilano.olivetticlub.utils.*
import it.tim.innovation.jolmilano.olivetticlub.viewmodels.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.ref.WeakReference
import java.net.URL


@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), TransactionFragment.FiscalReceipt {

    private val TAG = MainActivity::class.java.simpleName

    //Presentation
    private var mMainPresentation: MainPresentation? = null
    private var mTransactionPresentation: TransactionPresentation? = null

    //Library
    private var mCanOpenMenu = true
    private var mElaConnectorServiceIntent: Intent? = null
    private var elaConnectorService: IElaPrinter? = null
    private var mResponseList = ArrayList<ElaResponse>()
    private lateinit var mMainActivityViewModel: MainActivityViewModel
    private lateinit var mOperationsInterface: OperationsInterface
    private lateinit var mInsertFragmentInterface: InsertFragmentInterface
    private var mIsPromo20Switched = false
    private var mIsPromo40Switched = false
    private var mSocketConnected = false
    private var mQrCodeList = listOf<QrCodeItem>()
    private var invokeElaConnectorServiceCallback: (IElaPrinter) -> Unit = {}

    private var mSocketBroadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action?.equals(BroadcastValues.SOCKET_ACTION) == true) {
                    mSocketConnected = it.getBooleanExtra(BroadcastValues.SOCKET_STATUS, false)
                    val socketConnected: String = if (mSocketConnected) {
                        "CONNECTED"
                    } else {
                        "DISCONNECTED"
                    }

                    Utils.showSnackBar(rootLayout, socketConnected)
                    invalidateOptionsMenu()
                }
            }
        }
    }


    private fun connectPrint() {
        invokeElaConnectorService {
            it.connect("192.168.68.209", 9100)
        }
    }

    fun disconnectPrint() {
        invokeElaConnectorService {
            it.disconnect()
        }
    }

    private fun invokeElaConnectorService(invoke: (IElaPrinter) -> Unit) {
        if (elaConnectorService != null) {
            invoke(elaConnectorService!!)
        } else {
            invokeElaConnectorServiceCallback = invoke
            bindService(mElaConnectorServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private val mServiceConnection = object : ServiceConnection, IElaResponseListener {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected")
            elaConnectorService = (service as ElaPrinterLocalBinder).getService()
            invokeElaConnectorServiceCallback(elaConnectorService!!)
            invokeElaConnectorService { it.attachListener(this) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected $name")
            elaConnectorService = null
        }

        override fun onElaResponse(elaResponse: ElaResponse) {
            Log.d(TAG, "elaResponse --> $elaResponse")
            elaResponse.status?.let {
                if (it == Status.KO) {
                    //Only show popup when KO occurs
                    runOnUiThread {
                        Utils.showDialog(
                            this@MainActivity,
                            elaResponse::class.java.simpleName,
                            elaResponse.toString()
                        )
                    }
                }
                mResponseList.add(elaResponse)
            }
        }

        override fun onEmptyQueue() {
            Log.d(TAG, "onEmptyQueue")
            mCanOpenMenu = true
            mOperationsInterface.onOperationFinished()
        }

    }

    private fun manageElaConnectorConnection(host: String, port: Int) {
        invokeElaConnectorService {
            if (it.getConnectionStatus() == (ConnectionStatus.STATE_DISCONNECTED)) {
                it.connect(host, port)
            } else {
                it.disconnect()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mSocketBroadcast, IntentFilter(BroadcastValues.SOCKET_ACTION))
        mMainActivityViewModel =
            ViewModelProviders.of(this@MainActivity).get(MainActivityViewModel::class.java)

        val displayManager =
            applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val presentationDisplays =
            displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        Log.d(TAG, "Current device has " + presentationDisplays.size + " mMainPresentation display")

        Log.d(TAG, "storeID --> ${ConfigurationManager.getInstance(this).storeId}")

        if (presentationDisplays.isNotEmpty()) {

            val presentationDisplay = presentationDisplays[0]
            if (mMainPresentation == null) {
                mMainPresentation = MainPresentation(this, presentationDisplay)
            }
            if (mTransactionPresentation == null) {
                mTransactionPresentation = TransactionPresentation(this, presentationDisplay)
            }
            mMainPresentation?.show()
            mTransactionPresentation?.show()

        }

        mElaConnectorServiceIntent = Intent(this@MainActivity, ElaService::class.java)
        startService(mElaConnectorServiceIntent)
        bindService(mElaConnectorServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        if (ConfigurationManager.getInstance(this@MainActivity).storeId.equals("")) {
            StoreIdTask(this@MainActivity).execute()
        }

        mMainActivityViewModel.getAllItems().observe(this, Observer { list ->
            Log.d(TAG, "list --> $list")
            if (list.isNotEmpty()) {
                if (list.size != mQrCodeList.size) {
                    //It means that an Item has been added, no updates
                    mQrCodeList = list
                    if (mIsPromo20Switched || mIsPromo40Switched) {
                        mIsPromo20Switched = false
                        mIsPromo40Switched = false

                        val lastItemAdded = list[list.size - 1]
                        val configManager = ConfigurationManager.getInstance(this)
                        val url = URL(Utils.buildUrl(safeContext(), lastItemAdded))
                        Log.d(TAG, "urlBuilt --> $url")

                        try {
                            val barcodeEncoder = BarcodeEncoder()
                            val bitmap =
                                //We check if it's to Print a QrCode coupon or Barcode coupon
                                if (CouponType.fromInt(configManager.couponType) == CouponType.BARCODE) {
                                    //User selected to print a Barcode Coupon
                                    barcodeEncoder.encodeBitmap(
                                        /*3 Chars for StoreID*/ConfigurationManager.getInstance(this).storeId
                                            .plus(/*Special Char*/"-")
                                            .plus(lastItemAdded.timestampCreated.toString()),
                                        BarcodeFormat.CODE_39,
                                        250, 150
                                    )
                                } else {
                                    //User selected to print a QrCode Coupon
                                    barcodeEncoder.encodeBitmap(
                                        url.toString(),
                                        BarcodeFormat.QR_CODE,
                                        220,
                                        220
                                    )
                                }
                            mInsertFragmentInterface.setQrCodeImage(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        invokeElaConnectorService {
                            try {
                                if (CouponType.fromInt(configManager.couponType) == CouponType.BARCODE) {
                                    //Print Barcode
                                    val headerList = arrayListOf<String>()
                                    headerList.add("")
                                    headerList.add("")
                                    headerList.add(getString(R.string.show_code_to_shop))
                                    headerList.add(getString(R.string.to_get_discount))
                                    headerList.add("")

                                    val footerList = arrayListOf<String>()
                                    footerList.add("")
                                    footerList.add("")
                                    footerList.add("")
                                    footerList.add("")
                                    footerList.add("")

                                    val barcodeString =
                                        ConfigurationManager.getInstance(this)
                                            .storeId /*3 Chars for StoreID*/
                                            .plus(/*Special Char*/"-")
                                            .plus(lastItemAdded.timestampCreated.toString())

                                    val barcode = Barcode(
                                        headerList,
                                        footerList,
                                        CodeType.COD_39,
                                        barcodeString,
                                        StationType.RICEVUTA
                                    )

                                    it.printCoupon(barcode)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                e.message?.let { message ->
                                    Utils.showDialog(
                                        this@MainActivity,
                                        getString(R.string.error_dialog_title),
                                        message
                                    )
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            val checkable = it.findItem(R.id.setCouponType)
            val value = ConfigurationManager.getInstance(this).couponType
            checkable.setChecked(CouponType.fromInt(value) == CouponType.QR_CODE)

            val menuPower = it.findItem(R.id.connect)
            if (mSocketConnected) {
                menuPower.setIcon(R.drawable.power_on)
            } else {
                menuPower.setIcon(R.drawable.power_off)
            }
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.itemId.let { itemId ->
            if (itemId == R.id.connect) {
                invokeElaConnectorService { service: IElaPrinter ->
                    manageElaConnectorConnection(service.getHost(), service.getPort())
                }
                return true
            } else {
                if (mCanOpenMenu) {
                    return when (itemId) {
                        R.id.elaResponse -> {
                            Intent(this@MainActivity, ResponsesActivity::class.java).apply {
                                putExtra("LIST", mResponseList)
                                startActivity(this)
                            }
                            true
                        }
                        R.id.getStatusPrinter -> {
                            mOperationsInterface.onOperationOngoing()
                            invokeElaConnectorService { service -> service.getStatusPrinter() }
                            true
                        }
                        R.id.getStatusFiscmod -> {
                            mOperationsInterface.onOperationOngoing()
                            invokeElaConnectorService { service -> service.getStatusFiscmod() }
                            true
                        }
                        R.id.voidFiscmod -> {
                            mOperationsInterface.onOperationOngoing()
                            invokeElaConnectorService { service: IElaPrinter ->
                                service.cancelReceipt()
                            }
                            true
                        }
                        R.id.setIpAndPort -> {
                            val builder = AlertDialog.Builder(this@MainActivity)
                                .setTitle(getString(R.string.set_ip_and_port))
                            val inflater = layoutInflater
                            val dialogView = inflater.inflate(R.layout.ip_port_config_layout, null)
                            builder.setView(dialogView)

                            val ipEt = dialogView.findViewById<EditText>(R.id.ipField)
                            val portEt = dialogView.findViewById<EditText>(R.id.portField)

                            invokeElaConnectorService { service: IElaPrinter ->
                                ipEt.setText("" + service.getHost())
                                portEt.setText("" + service.getPort())
                            }

                            builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                                invokeElaConnectorService { service: IElaPrinter ->
                                    if (!ipEt.text.toString().equals("")) {
                                        service.setHost(ipEt.text.toString())
                                    }
                                    if (portEt.text != null) {
                                        portEt.text.toString().toIntOrNull()
                                            ?.let { service.setPort(it) }
                                    }
                                }
                                val imm =
                                    getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                                var view = currentFocus
                                if (view == null) {
                                    view = View(this@MainActivity)
                                }
                                imm.hideSoftInputFromWindow(view.windowToken, 0)
                                dialog.dismiss()
                            }
                            builder.create().show()
                            true
                        }
                        R.id.whoami -> {
                            invokeElaConnectorService {
                                it.whoami { message ->
                                    runOnUiThread {
                                        val builder = android.app.AlertDialog.Builder(this)
                                        builder.setTitle("Who am I")
                                        builder.setMessage(message)
                                        builder.setNeutralButton("Close") { dialog, which ->
                                        }
                                        builder.show()
                                    }
                                }
                            }
                            true
                        }
                        R.id.setConfigurationData -> {
                            val builder = AlertDialog.Builder(this@MainActivity)
                                .setTitle(getString(R.string.set_configuration_data))
                            val inflater = layoutInflater
                            val dialogView = inflater.inflate(R.layout.config_layout, null)
                            builder.setView(dialogView)

                            val queueDelayEt = dialogView.findViewById<EditText>(R.id.queueDelay)
                            val connectionTimeoutEt =
                                dialogView.findViewById<EditText>(R.id.connectionTimeout)
                            val connectionRetryEt =
                                dialogView.findViewById<EditText>(R.id.connectionRetry)

                            builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                                invokeElaConnectorService { service: IElaPrinter ->
                                    service.setConfigurationData(
                                        queueDelayEt.text.toString().toLongOrNull(),
                                        connectionTimeoutEt.text.toString().toIntOrNull(),
                                        connectionRetryEt.text.toString().toIntOrNull()
                                    )
                                }
                                val imm =
                                    getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                                var view = currentFocus
                                if (view == null) {
                                    view = View(this@MainActivity)
                                }
                                imm.hideSoftInputFromWindow(view.windowToken, 0)
                                dialog.dismiss()
                            }
                            builder.create().show()
                            true
                        }
                        R.id.getInfoPrinter -> {
                            val builder = AlertDialog.Builder(this@MainActivity)
                                .setTitle(getString(R.string.get_info_printer))
                            val inflater = layoutInflater
                            val dialogView = inflater.inflate(R.layout.get_info_layout, null)
                            builder.setView(dialogView)

                            val spinner = dialogView.findViewById<Spinner>(R.id.spinner)
                            val spinnerAdapter = ArrayAdapter(
                                this,
                                android.R.layout.simple_spinner_dropdown_item,
                                GetInfoPrinterInfo.values()
                            )
                            spinner.adapter = spinnerAdapter


                            builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                                spinner.selectedItem?.let { selectedItem: Any ->
                                    mOperationsInterface.onOperationOngoing()
                                    invokeElaConnectorService { service: IElaPrinter ->
                                        service.getInfoPrinter(selectedItem as GetInfoPrinterInfo)
                                    }
                                }
                                dialog.dismiss()
                            }
                            builder.create().show()
                            true
                        }
                        R.id.setCashier -> {
                            val builder = AlertDialog.Builder(this@MainActivity)
                                .setTitle(getString(R.string.set_cashier_name))
                            val editText = EditText(this@MainActivity)
                            editText.hint = getString(R.string.set_cashier_name)
                            editText.inputType =
                                InputType.TYPE_CLASS_TEXT; InputType.TYPE_TEXT_VARIATION_NORMAL
                            builder.setView(editText)
                            builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                                if (!editText.text.toString().equals("")) {
                                    mOperationsInterface.onOperationOngoing()
                                    val cashierName = editText.text.toString()
                                    invokeElaConnectorService {
                                        it.setCodeCashCashierWaiterFiscmod(
                                            cashierName,
                                            CodeCashCashierWaiterType.RICHIESTA_SET_CODICE_CASSIERE
                                        )
                                    }
                                }

                                val imm =
                                    getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                                var view = currentFocus
                                if (view == null) {
                                    view = View(this@MainActivity)
                                }
                                imm.hideSoftInputFromWindow(view.windowToken, 0)
                                dialog.dismiss()
                            }
                            builder.create().show()
                            true
                        }
                        R.id.setHeader -> {
                            val builder = AlertDialog.Builder(this@MainActivity)
                                .setTitle(getString(R.string.set_header))
                            val inflater = layoutInflater
                            val dialogView = inflater.inflate(R.layout.set_header_layout, null)
                            builder.setView(dialogView)

                            val headerFirstLineEt =
                                dialogView.findViewById<EditText>(R.id.headerFirstLine)
                            val headerSecondLineEt =
                                dialogView.findViewById<EditText>(R.id.headerSecondLine)
                            val headerThirdLineEt =
                                dialogView.findViewById<EditText>(R.id.headerThirdLine)
                            val headerFourthLineEt =
                                dialogView.findViewById<EditText>(R.id.headerFourthLine)

                            builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                                val headerList = ArrayList<String>()
                                headerList.add(headerFirstLineEt.text.toString())
                                headerList.add(headerSecondLineEt.text.toString())
                                headerList.add(headerThirdLineEt.text.toString())
                                headerList.add(headerFourthLineEt.text.toString())

                                Log.d(TAG, "HeaderList --> $headerList")
                                invokeElaConnectorService { service: IElaPrinter ->
                                    mOperationsInterface.onOperationOngoing()
                                    for (x in 0 until headerList.size) {
                                        service.setHeaderFooterFiscmod(
                                            x + 1,
                                            headerList[x],
                                            PrinterFontAttributes.NO_ATTRIBUTI,
                                            1,
                                            HeaderFooterType.HEADER,
                                            null,
                                            null
                                        )
                                    }
                                }
                                val imm =
                                    getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                                var view = currentFocus
                                if (view == null) {
                                    view = View(this@MainActivity)
                                }
                                imm.hideSoftInputFromWindow(view.windowToken, 0)
                                dialog.dismiss()
                            }
                            builder.create().show()
                            true
                        }
                        R.id.setCouponType -> {
                            ConfigurationManager.getInstance(this).couponType =
                                if (item != null) {
                                    if (item.isChecked) {
                                        CouponType.BARCODE.value
                                    } else {
                                        CouponType.QR_CODE.value
                                    }
                                } else {
                                    CouponType.BARCODE.value
                                }
                            true
                        }
                        else -> {
                            false
                        }
                    }
                } else {
                    return false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mMainPresentation?.show()
        mTransactionPresentation?.show()
    }

    override fun onDestroy() {
        mMainPresentation?.dismiss()
        mTransactionPresentation?.dismiss()
        stopService(mElaConnectorServiceIntent)
        unbindService(mServiceConnection)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSocketBroadcast)
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        mMainPresentation?.dismiss()
        mTransactionPresentation?.dismiss()
    }

    override fun onPrintFiscalReceipt(
        list: List<ReceiptLine>,
        discountTotal: Int,
        discountPercent: Int,
        isPromo20Switched: Boolean,
        isPromo40Switched: Boolean
    ) {
        if (!mSocketConnected) {
            Utils.showDialog(
                this,
                "Error",
                "Printer not connected"
            )
        } else {
            if (list.isNotEmpty()) {
                mCanOpenMenu = false
                mResponseList.clear()
                mOperationsInterface.onOperationOngoing()
                invokeElaConnectorService {

                    val receiptItemList = arrayListOf<ReceiptItem>()

                    for (item in list) {
                        val receiptItem = ReceiptItem(
                            item.barcodeId,
                            item.quantity,
                            item.prodName,
                            item.prodBrand,
                            item.price,
                            item.total
                        )
                        receiptItemList.add(receiptItem)
                    }

                    val receipt = Receipt(receiptItemList, discountTotal, discountPercent, true)

                    it.printReceipt(receipt)
                    it.disconnect()
                    broadcastPurchase()
                }
            }
        }
    }

    private fun broadcastPurchase() {
        val intent = Intent()
        intent.action = "olivetti.club.payment.completed"
        sendBroadcast(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let { keyEvent: KeyEvent ->
            Log.d(TAG, "unicodeChar --> ${keyEvent.unicodeChar}")
            mInsertFragmentInterface.onIntReceived(keyEvent.unicodeChar)
        }
        return true
    }

    fun setOperationListener(operationsInterface: OperationsInterface) {
        this.mOperationsInterface = operationsInterface
    }

    fun setInsertFragmentInterface(insertFragmentInterface: InsertFragmentInterface) {
        this.mInsertFragmentInterface = insertFragmentInterface
    }

    fun searchForQrCode(qrCode: String) {
        mOperationsInterface.searchForQrCodeCoupon(qrCode)
    }

    fun searchForBarcodeCoupon(barcode: String) {
        mOperationsInterface.searchForBarcodeCoupon(barcode)
    }

    private class GenerateQrCodeTask internal constructor(
        activity: MainActivity,
        private val discountType: String,
        private val title: String,
        private val description: String,
        private val imgUrl: String,
        private val discountPercent: Int,
        private val discount: Int = 0,
        private val timestampCreated: Long = System.currentTimeMillis()
    ) : AsyncTask<Void, Void, String?>() {

        private val mRef: WeakReference<MainActivity> = WeakReference(activity)

        override fun doInBackground(vararg params: Void?): String? {
            //Hardcoded STORE ID
            mRef.get()?.let {
                return Utils.generateCouponId(it.safeContext(), timestampCreated, discountPercent)
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            mRef.get()?.let { mainActivity: MainActivity ->
                result?.let { it: String ->
                    if (!it.equals("")) {
                        //Insert in DB or BE
                        val isToPrintQrCode =
                            CouponType.fromInt(ConfigurationManager.getInstance(mainActivity.safeContext()).couponType) == CouponType.QR_CODE
                        val qrCodeItem = QrCodeItem(
                            if (isToPrintQrCode) {
                                it
                            } else {
                                timestampCreated.toString()
                            },
                            discountType,
                            title,
                            description,
                            discount,
                            discountPercent,
                            timestampCreated,
                            timestampCreated.plus(2592000000),//1 Month,
                            0,
                            imgUrl
                        )

                        mainActivity.mMainActivityViewModel.insert(qrCodeItem)
                    }
                }
            }
        }
    }

    private class StoreIdTask internal constructor(activity: MainActivity) :
        AsyncTask<Void, Void, String>() {

        private val mRef: WeakReference<MainActivity> = WeakReference(activity)

        override fun doInBackground(vararg params: Void?): String {
            //Hardcoded STORE ID
            /*
            * Now We generate a StoreID which is 001 because of compatibility with Barcode Read action
            * */
            return /*Utils.getMd5("storeId")*/"001"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            mRef.get()?.let { mainActivity: MainActivity ->
                result?.let {
                    if (!it.equals("")) {
                        ConfigurationManager.getInstance(mainActivity.safeContext()).storeId = it
                        Log.d(mainActivity.TAG, "Created storeId --> $it")
                    }
                }
            }
        }
    }

}
