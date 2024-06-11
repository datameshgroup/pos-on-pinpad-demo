package au.com.dmg.terminalposdemo

//import androidx.compose.ui.text.TextStyle
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import au.com.dmg.fusion.Message
import au.com.dmg.fusion.MessageHeader
import au.com.dmg.fusion.data.CustomFieldType
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.MessageClass
import au.com.dmg.fusion.data.MessageType
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusion.data.UnitOfMeasure
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.request.paymentrequest.AmountsReq
import au.com.dmg.fusion.request.paymentrequest.CustomField
import au.com.dmg.fusion.request.paymentrequest.PaymentData
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest
import au.com.dmg.fusion.request.paymentrequest.PaymentTransaction
import au.com.dmg.fusion.request.paymentrequest.SaleData
import au.com.dmg.fusion.request.paymentrequest.SaleItem
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID
import au.com.dmg.fusion.response.SaleToPOIResponse
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID


class ActivityFuelProducts : ComponentActivity(), PaymentResultListener {
    private lateinit var paymentLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val cart = remember { Cart() }
            ShoppingCartScreen(context, paymentLauncher, cart)
        }
        // Initialize the ActivityResultLauncher
        paymentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Handle payment result here
            val data: Intent? = result.data
            val resultCode: Int = result.resultCode
            // Handle the result here using onPaymentResult method
            onPaymentResult(resultCode, data)
        }
    }
    override fun onPaymentResult(resultCode: Int, data: Intent?) {
        Log.d(
            "Response",
            data?.getStringExtra(Message.INTENT_EXTRA_MESSAGE)!!
        )
        var message: Message? = null
        message = try {
            Message.fromJson(data.getStringExtra(Message.INTENT_EXTRA_MESSAGE))
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading intent.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            return
        }

        val response = message!!.response

        if (response != null) {
            try {
                val globalClass = applicationContext as GlobalClass
                globalClass.response = response

                //parse response
                val mh: MessageHeader = response.messageHeader
                val mc = mh.messageCategory

                val pr = response.paymentResponse
                val paymentResult = pr!!.response.result.name
                val currentPaymentType = pr.paymentResult!!.paymentType

                // Add to completion page list
                if (paymentResult == "Success") {
                    if (currentPaymentType == PaymentType.FirstReservation) {
                        globalClass.response = response
                        globalClass.addPreauthorisation(response)
                    }
                }


                openActivityResult(this@ActivityFuelProducts, mc, response, message)
            } catch (e: java.lang.Exception) {
                Log.d("Error", "Invalid Response ==>" + e.message)
            }
        }
    }
}
interface PaymentResultListener {
    fun onPaymentResult(resultCode: Int, data: Intent?)
}

fun openActivityResult(context: Context, mc: MessageCategory?, r: SaleToPOIResponse?, message: Message) {
    val intent = Intent(context, ActivityResult::class.java)
    val bundle = Bundle()
    bundle.putSerializable("messageCategory", mc)
    bundle.putString("message", message.toString())
    intent.putExtras(bundle)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    intent.putExtra("prevClass", context.javaClass)
    context.startActivity(intent)
}


@Preview(showBackground = true)
@Composable
fun ShoppingCartScreenPreview() {
    val cart = Cart()
    cart.addItem(Item(ProductCode.NonFuel.displayName, 100.0000, 100900.0))
    cart.addItem(Item(ProductCode.FuelProductCodeFreedomFuelCard.displayName, 15.0, 1.0))
    cart.addItem(Item(ProductCode.FuelProductCode.displayName, 20.0, 2.0))
    ShoppingCartScreen(context = LocalContext.current, paymentLauncher = mockLauncher, cart = cart)
}
class MockLauncher : ActivityResultLauncher<Intent>() {
    override fun launch(input: Intent?, options: ActivityOptionsCompat?) {
        TODO("Not yet implemented")
    }
    override fun unregister() {}
    override fun getContract(): ActivityResultContract<Intent, *> {
        TODO("Not yet implemented")
    }
}
val mockLauncher = MockLauncher()

@Composable
fun ShoppingCartScreen(context: Context,paymentLauncher: ActivityResultLauncher<Intent>, cart: Cart) {
    var selectedItemCode by remember { mutableStateOf(ProductCode.FuelProductCodeShellCard) }
    var unitPrice by remember { mutableStateOf("10.00") }
    var itemQuantity by remember { mutableStateOf("1.0") }
    var expanded by remember { mutableStateOf(false) }


    Column(modifier = Modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
            TextField(
                value = selectedItemCode.displayName,
                onValueChange = {},
                label = { Text("Product") },
                readOnly = true,
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = true
                    },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    disabledTextColor = Color.Black
                )
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                ProductCode.values().forEach { item ->
                    DropdownMenuItem(onClick = {
                        selectedItemCode = item
                        expanded = false
                    }) {
                        Text(item.displayName)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = unitPrice,
            onValueChange = { unitPrice = it },
            label = { Text("Unit Price") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = itemQuantity,
            onValueChange = { itemQuantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val price = unitPrice.toDoubleOrNull()
                val quantity = itemQuantity.toDoubleOrNull() ?: 1.0
                if (price != null) {
                    cart.addItem(Item(code = selectedItemCode.name, price = price, quantity = quantity))
                    unitPrice = "10.00"
                    itemQuantity = "1.0"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(Color(ContextCompat.getColor(context, R.color.datameshBlue)))
        ) {
            Text("Add to Cart", color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        CartItemsList(cart)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Total Price: \$${cart.getTotalPrice()}",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold // Set the font weight to bold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val firstItem = cart.getItems().firstOrNull()
                val firstItemProductCode = ProductCode.values().find { it.name == firstItem?.code }
                val firstItemTestCase = firstItemProductCode?.testCase
                println("firstItemTestCase ---- ${firstItemProductCode?.testCase}")

                val paymentRequest = buildPaymentRequest(firstItemTestCase, buildSaleItems(cart) as MutableList<SaleItem>, cart.getTotalPrice())
                val intent = Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST)

                val message: Message = Message(paymentRequest)
                Log.d("Fuel PaymentRequest", message.toJson())

                intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson())
                // name of this app, that gets treated as the POS label by the terminal.
                // name of this app, that gets treated as the POS label by the terminal.
                intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, GlobalClass.APPLICATION_NAME)
                // version of of this POS app.
                // version of of this POS app.
                intent.putExtra(
                    Message.INTENT_EXTRA_APPLICATION_VERSION,
                    GlobalClass.APPLICATION_VERSION
                )

                paymentLauncher?.launch(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = cart.getItems().isNotEmpty(),
            colors = ButtonDefaults.buttonColors(Color(ContextCompat.getColor(context, R.color.datameshPurple)))
        ) {
            Text("PAY NOW", color = Color.White) // Set text color to white for better visibility
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val firstItem = cart.getItems().firstOrNull()
                val firstItemProductCode = ProductCode.values().find { it.name == firstItem?.code }
                val firstItemTestCase = firstItemProductCode?.testCase
                println("firstItemTestCase ---- ${firstItemProductCode?.testCase}")

                val paymentRequest = buildPreauthorisationRequest(firstItemTestCase, buildSaleItems(cart) as MutableList<SaleItem>, cart.getTotalPrice())
                val intent = Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST)

                val message: Message = Message(paymentRequest)
                Log.d("Fuel PaymentRequest", message.toJson())

                intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson())
                // name of this app, that gets treated as the POS label by the terminal.
                // name of this app, that gets treated as the POS label by the terminal.
                intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, GlobalClass.APPLICATION_NAME)
                // version of of this POS app.
                // version of of this POS app.
                intent.putExtra(
                    Message.INTENT_EXTRA_APPLICATION_VERSION,
                    GlobalClass.APPLICATION_VERSION
                )

                paymentLauncher?.launch(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = cart.getItems().isNotEmpty(),
            colors = ButtonDefaults.buttonColors(Color(ContextCompat.getColor(context, R.color.datameshPurple)))
        ) {
            Text("PREAUTHORIZE", color = Color.White) // Set text color to white for better visibility
        }
    }
}

fun buildPreauthorisationRequest(testCase: String?, saleItems: MutableList<SaleItem>, totalAmount: Double): SaleToPOIRequest?  {
    val serviceID = UUID.randomUUID().toString()
    val transactionID = UUID.randomUUID().toString()
    val preauthTimestamp = Instant.ofEpochMilli(System.currentTimeMillis())

    val paymentTransactionBuilder = PaymentTransaction.Builder()
        .amountsReq(
            AmountsReq.Builder()
                .currency("AUD")
                .requestedAmount(BigDecimal(totalAmount))
                .tipAmount(BigDecimal(0))
                .build()
        )
        .saleItems(saleItems)

    // Add SaleItem with zero amount if testCase is not empty
    if (!testCase.isNullOrEmpty()) {
        val testItemProduct = ProductCode.values().find { it.testCase == testCase }
        val testCaseProductCode = testItemProduct?.name
        println("testCaseProductCode ---- $testCaseProductCode")
        val customField = CustomField.Builder()
            .key(testCaseProductCode)
            .type(CustomFieldType.String)
            .value(1)
            .build();

        paymentTransactionBuilder.addSaleItem(
            SaleItem.Builder()
                .itemID(1000) // Set the itemID
                .productCode(testCase)
                .unitOfMeasure(UnitOfMeasure.Litre)
                .unitPrice(BigDecimal(0))
                .quantity(BigDecimal(1))
                .itemAmount(BigDecimal(0))
                .productLabel(testCase)
                .addCustomField(
                    customField
                )
                .build()
        )
    }

    return SaleToPOIRequest.Builder()
        .messageHeader(
            MessageHeader.Builder()
                .messageClass(MessageClass.Service)
                .messageCategory(MessageCategory.Payment)
                .messageType(MessageType.Request)
                .serviceID(serviceID)
                .saleID("f635ab18-09be-4205-963c-6f8ee8ebb409")
                .protocolVersion("3.1-dmg")
                .build()
        )
        .request(
            PaymentRequest.Builder()
                .saleData(
                    SaleData.Builder()
                        .operatorLanguage("en")
                        .saleTransactionID(
                            SaleTransactionID.Builder()
                                .timestamp(preauthTimestamp)
                                .transactionID(transactionID)
                                .build()
                        )
                        .build()
                )
                .paymentTransaction(paymentTransactionBuilder.build())
                .paymentData(
                    PaymentData.Builder()
                        .paymentType(PaymentType.FirstReservation)
                        .build()
                )
                .build()
        )
        .build()
}


@Composable
fun CartItemsList(cart: Cart) {
    LazyColumn {
        items(cart.getItems()) { item ->
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState(), enabled = true)
                            .weight(2f)
                    ) {
                        Text(
                            text = item.code,
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState(), enabled = true)
                            .weight(2f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "\$${item.price} x ${item.quantity}",
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState(), enabled = true)
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "\$${item.getItemAmount()}",
                            maxLines = 1
                        )
                    }

                    ClickableText(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = Color.Red, textDecoration = TextDecoration.None)) {
                                append("Remove")
                            }
                        },
                        onClick = { cart.removeItem(item) },
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

                }
            }
        }
    }
}




data class Item(
    val code: String,
    val price: Double,
    val quantity: Double
) {
    private var itemAmount: Double = 0.0

    fun calculateItemAmount() {
        itemAmount = price * quantity
    }

    fun getItemAmount(): Double {
        return itemAmount
    }
}

class Cart {
    private val items = mutableStateListOf<Item>()

    fun addItem(item: Item) {
        item.calculateItemAmount() // Calculate itemAmount
        items.add(item)
    }

    fun getItems(): List<Item> {
        return items
    }

    fun removeItem(item: Item) {
        items.remove(item)
    }

    fun getTotalPrice(): Double {
        return items.sumOf { it.getItemAmount() }
    }
}


fun buildSaleItems(cart: Cart): List<SaleItem> {
    val saleItems = mutableListOf<SaleItem>()
    var itemID = 1 // Initialize itemID with 1

    for (cartItem in cart.getItems()) {
        val saleItem = buildSaleItem(cartItem.code, cartItem.price, cartItem.quantity, cartItem.getItemAmount(), itemID++)
        saleItem?.let {
            saleItems.add(it)
        }
    }
    return saleItems
}

private fun buildSaleItem(productCode: String, unitPrice: Double, quantity: Double, itemAmount: Double, itemID: Int): SaleItem? {
    val builder = SaleItem.Builder()
        .itemID(itemID) // Set the itemID
        .productCode(productCode)
        .unitOfMeasure(UnitOfMeasure.Litre)
        .unitPrice(BigDecimal(unitPrice))
        .quantity(BigDecimal(quantity))
        .itemAmount(BigDecimal(itemAmount))
        .productLabel(productCode)

    if (productCode != ProductCode.NonFuel.name) {
        val customField = CustomField.Builder()
            .key(productCode)
            .type(CustomFieldType.String)
            .value(1)
            .build()
        builder.addCustomField(customField)
    }

    return builder.build()
}


private fun buildPaymentRequest(testCase: String?, saleItems: MutableList<SaleItem>, totalAmount: Double): SaleToPOIRequest? {
    val serviceID = UUID.randomUUID().toString()
    val transactionID = UUID.randomUUID().toString()

    val paymentTransactionBuilder = PaymentTransaction.Builder()
        .amountsReq(
            AmountsReq.Builder()
                .currency("AUD")
                .requestedAmount(BigDecimal(totalAmount))
                .tipAmount(BigDecimal(0))
                .build()
        )
        .saleItems(saleItems)

    // Add SaleItem with zero amount if testCase is not empty
    if (!testCase.isNullOrEmpty()) {
        val testItemProduct = ProductCode.values().find { it.testCase == testCase }
        val testCaseProductCode = testItemProduct?.name
        println("testCaseProductCode ---- $testCaseProductCode")
        val customField = CustomField.Builder()
            .key(testCaseProductCode)
            .type(CustomFieldType.String)
            .value(1)
            .build();

        paymentTransactionBuilder.addSaleItem(
            SaleItem.Builder()
                .itemID(1000) // Set the itemID
                .productCode(testCase)
                .unitOfMeasure(UnitOfMeasure.Litre)
                .unitPrice(BigDecimal(0))
                .quantity(BigDecimal(1))
                .itemAmount(BigDecimal(0))
                .productLabel(testCase)
                .addCustomField(
                    customField
                )
                .build()
        )
    }

    return SaleToPOIRequest.Builder()
        .messageHeader(
            MessageHeader.Builder()
                .messageClass(MessageClass.Service)
                .messageCategory(MessageCategory.Payment)
                .messageType(MessageType.Request)
                .serviceID(serviceID)
                .saleID("f635ab18-09be-4205-963c-6f8ee8ebb409")
                .protocolVersion("3.1-dmg")
                .build()
        )
        .request(
            PaymentRequest.Builder()
                .saleData(
                    SaleData.Builder()
                        .operatorLanguage("en")
                        .saleTransactionID(
                            SaleTransactionID.Builder()
                                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                .transactionID(transactionID)
                                .build()
                        )
                        .build()
                )
                .paymentTransaction(paymentTransactionBuilder.build())
                .paymentData(
                    PaymentData.Builder()
                        .paymentType(PaymentType.Normal)
                        .build()
                )
                .build()
        )
        .build()
}


enum class ProductCode(val displayName: String, val testCase: String) {
    NonFuel("Non-Fuel", ""),
    FuelProductCode("Generic / BP", "DMGTCF448"),
    FuelProductCodeShellCard("Shell Card", "DMGTCF449"),
    FuelProductCodeCaltexStarCard("Caltex StarCard", "DMGTCF450"),
    FuelProductCodeFleetCard("Fleet Card", "DMGTCF451"),
    FuelProductCodeMotorpass("Motorpass", "DMGTCF452"),
    FuelProductCodeUnitedFuelCard("United Fuel Card", "DMGTCF453"),
    FuelProductCodeAmpolCard("Ampol Card", "DMGTCF454"),
    FuelProductCodeTrinityFuelCard("Trinity Fuel Card", "DMGTCF455"),
    FuelProductCodeFreedomFuelCard("Freedom Fuel Card", "DMGTCF466"),
    FuelProductCodeLibertyCard("Liberty Card", "DMGTCF467")
}
