//package com.example.warehousetet
//
//import IntTransferProducts
//import android.util.Log
//import org.apache.xmlrpc.client.XmlRpcClient
//import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
//import java.net.URL
//
//class OdooXmlRpcClient(private val credentialManager: CredentialManager) {
//
//    private fun getClientConfig(endpoint: String): XmlRpcClientConfigImpl? {
//        return try {
//            val fullUrl = "${Constants.URL}/xmlrpc/2/$endpoint"
//            Log.d("OdooXmlRpcClient", "Connecting to: $fullUrl")
//            XmlRpcClientConfigImpl().apply {
//                serverURL = URL(fullUrl)
//            }.also {
//                Log.d("OdooXmlRpcClient", "Config set with URL: ${it.serverURL}")
//            }
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error setting up client config: ${e.localizedMessage}")
//            null
//        }
//    }
//
//    suspend fun login(username: String, password: String): Int {
//        return try {
//            val config = getClientConfig("common")
//            if (config == null) {
//                Log.e("OdooXmlRpcClient", "Client configuration is null, aborting login.")
//                return -1
//            }
//            val client = XmlRpcClient().also { it.setConfig(config) }
//            val params = listOf(Constants.DATABASE, username, password, emptyMap<String, Any>())
//            val result = client.execute("authenticate", params) as Int
//            if (result > 0) credentialManager.storeUserCredentials(username, password, result)
//            result
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error during login: ${e.localizedMessage}")
//            -1
//        }
//    }
//
//    suspend fun fetchReceipts(): List<Receipt> {
//        val config = getClientConfig("object")
//        if (config == null) {
//            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchReceipts.")
//            return emptyList()
//        }
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        // Updated domain with additional condition for 'state' field
//        val domain = listOf(
//            listOf("picking_type_id.code", "=", "incoming"), // Only 'incoming' operation types
//            listOf("state", "=", "assigned"), // Only receipts with 'Ready' state
//            "|",
//            listOf("user_id", "=", false), // No responsible user
//            listOf("user_id", "=", userId) // Or the current user is the responsible user
//        )
//        val fields = listOf("id", "name", "date", "user_id", "state","origin") // Include 'state' if you want to verify it in the result
//
//        val params = listOf(
//            Constants.DATABASE,
//            userId,
//            password,
//            "stock.picking",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val result = client.execute("execute_kw", params) as Array<Any>
//            result.mapNotNull { it as? Map<String, Any> }.mapNotNull { map ->
//                val receiptUserId = map["user_id"] as? Int
//                // Since the domain already ensures we only fetch relevant receipts,
//                // this additional check might not be necessary unless you need to perform
//                // further filtering.
//                Receipt(
//                    id = map["id"] as Int,
//                    name = map["name"] as String,
//                    date = map["date"].toString(),
//                    origin = map["origin"].toString(),
//                    // Include user_id if your Receipt class supports it
//                    // Include state if you want to use it for further validation or logic
//                )
//            }.also {
//                Log.d("OdooXmlRpcClient", "Fetched ${it.size} receipts with 'Ready' state")
//            }
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching receipts: ${e.localizedMessage}", e)
//            emptyList()
//        }
//    }
//
//    suspend fun fetchProductsForReceipt(receiptId: Int): List<Product> {
//        val config = getClientConfig("object")
//        if (config == null) {
//            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchProductsForReceipt.")
//            return emptyList()
//        }
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        val fields = listOf("product_summary")
//
//        val params = listOf(
//            Constants.DATABASE,
//            userId,
//            password,
//            "stock.picking",
//            "search_read",
//            listOf(listOf(listOf("id", "=", receiptId))),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val result = client.execute("execute_kw", params) as Array<Any>
//            val productsSummary = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["product_summary"].toString() else ""
//            parseProductSummary(productsSummary)
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching products for receipt: ${e.localizedMessage}", e)
//            emptyList()
//        }
//    }
//
//private fun parseProductSummary(summary: String): List<Product> {
//    return summary.split(", ").mapNotNull { productString ->
//        // Split each product entry into ID, Name, and Quantity based on ':'
//        val parts = productString.split(":").map { it.trim() }
//        if (parts.size == 3) {
//            try {
//                val id = parts[0].toInt() // Convert ID to Int
//                val name = parts[1]
//                val quantity = parts[2].toDouble() // Convert Quantity to Double
//                Product(id = id, name = name, quantity = quantity)
//            } catch (e: Exception) {
//                Log.e("parseProductSummary", "Error parsing product summary: $e")
//                null // In case of formatting or conversion issue, return null to exclude this entry
//            }
//        } else {
//            null // Exclude if the product string format does not match expected parts count
//        }
//    }
//}
//
//    suspend fun fetchProductImageByName(productName: String): String? {
//        val config = getClientConfig("object") ?: return null
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val domain = listOf(listOf("name", "=", productName))
//        val fields = listOf("image_1920")
//
//        val params = listOf(
//            Constants.DATABASE,
//            credentialManager.getUserId(),
//            credentialManager.getPassword() ?: "",
//            "product.product",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val results = client.execute("execute_kw", params) as? Array<Any>
//            if (!results.isNullOrEmpty()) {
//                val productData = results[0] as Map<String, Any>
//                productData["image_1920"] as? String
//            } else null
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching product image: ${e.localizedMessage}")
//            null
//        }
//    }
//
//    suspend fun fetchProductBarcodeByName(productName: String): String? {
//        val config = getClientConfig("object") ?: return null
//        val client = XmlRpcClient().also { it.setConfig(config) }
//
//        val domain = listOf(listOf("name", "=", productName))
//        val fields = listOf("barcode")
//
//        val params = listOf(
//            Constants.DATABASE,
//            credentialManager.getUserId(),
//            credentialManager.getPassword() ?: "",
//            "product.template",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val results = client.execute("execute_kw", params) as? Array<Any>
//            if (!results.isNullOrEmpty()) {
//                val productData = results[0] as Map<String, Any>
//                productData["barcode"] as? String
//            } else null
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching product barcode: ${e.localizedMessage}")
//            null
//        }
//    }
//
//    suspend fun fetchProductTrackingByName(productName: String): String? {
//        val config = getClientConfig("object")
//        if (config == null) {
//            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchProductTrackingByName.")
//            return null
//        }
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId() // Use CredentialManager to get the user ID
//        val password = credentialManager.getPassword() ?: "" // Use CredentialManager to get the password
//        val database = Constants.DATABASE // Use Constants to get the database name
//
//        // Define the search domain to find the product by its name
//        val domain = listOf(listOf("name", "=", productName))
//        // Specify the fields to fetch; we're only interested in the 'tracking' field
//        val fields = listOf("tracking")
//
//        val params = listOf(
//            database,
//            userId,
//            password,
//            "product.template",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val result = client.execute("execute_kw", params) as Array<Any>
//            if (result.isNotEmpty()) {
//                val productData = result[0] as Map<*, *>
//                productData["tracking"] as? String
//            } else {
//                null
//            }
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching product tracking: ${e.localizedMessage}", e)
//            null
//        }
//    }
//
//
//    suspend fun fetchSerialNumbersByProductName(productName: String): List<String>? {
//        val config = getClientConfig("object") ?: return null
//        val client = XmlRpcClient().also { it.setConfig(config) }
//
//        // Assuming 'product_id' is linked by a field 'name' in 'product.product' model
//        // and serial numbers are stored in 'stock.lot' model under the field 'name'.
//        val productDomain = listOf(listOf("name", "=", productName))
//        val productFields = listOf("id") // We only need the product ID to link to its serial numbers
//
//        val productParams = listOf(
//            Constants.DATABASE,
//            credentialManager.getUserId(),
//            credentialManager.getPassword() ?: "",
//            "product.product",
//            "search_read",
//            listOf(productDomain),
//            mapOf("fields" to productFields)
//        )
//
//        try {
//            val productResults = client.execute("execute_kw", productParams) as? Array<Any>
//            val productId = (productResults?.firstOrNull() as? Map<*, *>)?.get("id") as? Int ?: return null
//
//            // Now fetch serial numbers linked to this product ID in 'stock.lot' model
//            val serialDomain = listOf(listOf("product_id", "=", productId))
//            val serialFields = listOf("name") // The field that stores the serial number
//
//            val serialParams = listOf(
//                Constants.DATABASE,
//                credentialManager.getUserId(),
//                credentialManager.getPassword() ?: "",
//                "stock.lot",
//                "search_read",
//                listOf(serialDomain),
//                mapOf("fields" to serialFields)
//            )
//
//            val serialResults = client.execute("execute_kw", serialParams) as? Array<Any>
//            return serialResults?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching serial numbers: ${e.localizedMessage}", e)
//            return null
//        }
//    }
//
//    suspend fun fetchInternalTransfersWithProductDetails(): List<InternalTransfers> {
//        val config = getClientConfig("object")
//        if (config == null) {
//            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchInternalTransfers.")
//            return emptyList()
//        }
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        val domain = listOf(
//            listOf("state", "=", "assigned"),
//            listOf("picking_type_id.code", "=", "internal")
//        )
//        val fields = listOf("id", "name", "scheduled_date", "origin", "state")
//
//
//        val params = listOf(
//            Constants.DATABASE,
//            userId,
//            password,
//            "stock.picking",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val result = client.execute("execute_kw", params) as Array<Any>
//            result.mapNotNull { it as? Map<String, Any> }
//                .mapNotNull { map ->
//                    val transferId = map["id"] as Int
//                    val transferName = map["name"] as String
//                    val transferDate = map["scheduled_date"].toString()
//                    val sourceDocument = map["origin"] as? String ?: ""
//
//                    val products = fetchProductsForInternalTransfer(transferId)
//
//                    // Ensure the fetched products are mapped to IntTransferProducts
//                    val intTransferProductsList = products.map { product ->
//                        IntTransferProducts(
//                            name = product.name,
//                            quantity = product.quantity,
//                            transferDate = transferDate
//                        )
//                    }
//
//                    InternalTransfers(
//                        id = transferId,
//                        transferName = transferName,
//                        transferDate = transferDate,
//                        sourceDocument = sourceDocument,
//                        productDetails = intTransferProductsList
//                    )
//                }.also {
//                    Log.d("OdooXmlRpcClient", "Fetched ${it.size} internal transfers with product details.")
//                }
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching internal transfers: ${e.localizedMessage}", e)
//            emptyList()
//        }
//    }
//
//    private suspend fun fetchProductsForInternalTransfer(transferId: Int): List<Product> {
//        val config = getClientConfig("object")
//        if (config == null) {
//            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchProductsForInternalTransfer.")
//            return emptyList()
//        }
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        // Adjust the domain filter to specifically fetch the desired internal transfer by ID.
//        // The following assumes internal transfers are distinguished within your setup and
//        // the `internal_transfer_summary` field exists and is computed for these records.
//        val domain = listOf(listOf("id", "=", transferId))
//        val fields = listOf("internal_transfer_summary")  // Use the custom computed field for internal transfers
//
//        val params = listOf(
//            Constants.DATABASE,
//            userId,
//            password,
//            "stock.picking",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val result = client.execute("execute_kw", params) as Array<Any>
//            // Extract the internal_transfer_summary for the specified transfer
//            val internalTransferSummary = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["internal_transfer_summary"].toString() else ""
//            parseInternalTransferSummary(internalTransferSummary)  // You may need to implement or adjust this method
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching products for internal transfer: ${e.localizedMessage}", e)
//            emptyList()
//        }
//    }
//
//private fun parseInternalTransferSummary(summary: String): List<Product> {
//    // Check if the summary is not empty
//    if (summary.isEmpty()) return emptyList()
//
//    return summary.split(", ").mapNotNull { productString ->
//        // Expected format: "productID:productName: quantity"
//        val parts = productString.split(": ")
//        if (parts.size == 3) {  // Adjusting for the expected three parts
//            val productId = parts[0].toIntOrNull()  // Convert ID to Integer
//            val productName = parts[1]
//            val productQuantity = parts[2].toDoubleOrNull()  // Convert quantity to Double
//            if (productId != null && productQuantity != null) {
//                Product(id = productId, name = productName, quantity = productQuantity)  // Including ID in the constructor
//            } else {
//                null  // If conversion fails, exclude this product from the list
//            }
//        } else {
//            null  // Exclude if the product string format is unexpected
//        }
//    }
//}
//
//    suspend fun fetchTransferIdByReference(transferReference: String): Int? {
//        // Assuming getClientConfig, XmlRpcClient, userId, and password are accessible here
//        val config = getClientConfig("object") ?: return null
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        val domain = listOf(listOf("name", "=", transferReference))
//        val fields = listOf("id")
//
//        val params = listOf(
//            Constants.DATABASE,
//            userId,
//            password,
//            "stock.picking",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val result = client.execute("execute_kw", params) as? Array<Map<String, Any>> ?: arrayOf()
//            if (result.isNotEmpty()) result[0]["id"] as? Int else null
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching transfer ID for reference $transferReference: ${e.localizedMessage}", e)
//            null
//        }
//    }
//
//suspend fun fetchAndLogBuyerDetails(pickingName: String): BuyerDetails? {
//    Log.d("OdooXmlRpcClient", "Starting fetchAndLogBuyerDetails for pickingName: $pickingName")
//    val config = getClientConfig("object")
//    if (config == null) {
//        Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchBuyerDetailsForReceipt.")
//        return null
//    }
//    Log.d("OdooXmlRpcClient", "Configuration loaded successfully.")
//
//    val client = XmlRpcClient().also { it.setConfig(config) }
//    val userId = credentialManager.getUserId()
//    val password = credentialManager.getPassword() ?: ""
//
//    val fields = listOf("name", "buyer_id")
//    val params = listOf(
//        Constants.DATABASE,
//        userId,
//        password,
//        "stock.picking",
//        "search_read",
//        listOf(listOf(listOf("name", "=", pickingName))),
//        mapOf("fields" to fields)
//    )
//
//    return try {
//        Log.d("OdooXmlRpcClient", "Executing XML-RPC call for stock.picking.")
//        val result = client.execute("execute_kw", params) as Array<Any>
//        Log.d("OdooXmlRpcClient", "XML-RPC call executed, processing results.")
//
//        if (result.isNotEmpty()) {
//            val picking = result[0] as Map<String, Any>
//            Log.d("OdooXmlRpcClient", "Picking found: ${picking["name"]}")
//
//            val buyerId = (picking["buyer_id"] as? Array<Any>)?.firstOrNull() as? Int
//            if (buyerId != null) {
//                Log.d("OdooXmlRpcClient", "Found buyer ID: $buyerId, fetching buyer details.")
//                fetchUserDetails(buyerId)
//            } else {
//                Log.d("OdooXmlRpcClient", "No buyer associated with this picking.")
//                null
//            }
//        } else {
//            Log.d("OdooXmlRpcClient", "Picking not found.")
//            null
//        }
//    } catch (e: Exception) {
//        Log.e("OdooXmlRpcClient", "Error fetching buyer details for receipt: ${e.localizedMessage}", e)
//        null
//    }
//}
//    private suspend fun fetchUserDetails(userId: Int): BuyerDetails? {
//        Log.d("OdooXmlRpcClient", "Starting fetchUserDetails for userId: $userId")
//        val config = getClientConfig("object")
//        if (config == null) {
//            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchUserDetails.")
//            return null
//        }
//        Log.d("OdooXmlRpcClient", "Configuration loaded successfully for user details fetch.")
//
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val authUserId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        val fields = listOf("name", "login")
//        val params = listOf(
//            Constants.DATABASE,
//            authUserId,
//            password,
//            "res.users",
//            "search_read",
//            listOf(listOf(listOf("id", "=", userId))),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            Log.d("OdooXmlRpcClient", "Executing XML-RPC call for res.users.")
//            val result = client.execute("execute_kw", params) as Array<Any>
//            Log.d("OdooXmlRpcClient", "XML-RPC call executed, processing user details.")
//
//            if (result.isNotEmpty()) {
//                val userDetails = result[0] as Map<String, Any>
//                Log.d("OdooXmlRpcClient", "User details found: ${userDetails["login"]},${userDetails["name"]} ")
//
//                BuyerDetails(
//                    id = userId,
//                    name = userDetails["name"].toString(),
//                    login = userDetails["login"].toString()
//                )
//            } else {
//                Log.d("OdooXmlRpcClient", "User not found.")
//                null
//            }
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching user details: ${e.localizedMessage}", e)
//            null
//        }
//    }
//
//
//}
//
//


package com.example.warehousetet

import IntTransferProducts

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.MalformedURLException
import java.net.URL



class OdooXmlRpcClient(val credentialManager: CredentialManager) {

//    private fun getClientConfig(endpoint: String): XmlRpcClientConfigImpl? {
//        try {
//            val fullUrl = "${Constants.URL}/xmlrpc/2/$endpoint"
//            Log.d("OdooXmlRpcClient", "Connecting to: $fullUrl")
//            return XmlRpcClientConfigImpl().apply {
//                serverURL = URL(fullUrl)
//                Log.d("OdooXmlRpcClient", "Config set with URL: $serverURL")
//            }
//        } catch (e: MalformedURLException) {
//            Log.e("OdooXmlRpcClient", "Malformed URL Exception: ${e.localizedMessage}")
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error setting up client config: ${e.localizedMessage}")
//        }
//        return null
//    }
private fun getClientConfig(endpoint: String): XmlRpcClientConfigImpl? {
    try {
        val fullUrl = "${Constants.URL}/xmlrpc/2/$endpoint"
        Log.d("OdooXmlRpcClient", "Connecting to: $fullUrl")
        return XmlRpcClientConfigImpl().apply {
            serverURL = URL(fullUrl)
            isEnabledForExtensions = true  // Enable extensions if needed
            // This setting is for Python's XMLRPC library. For Java, we need to ensure we are not sending null values as Java's XMLRPC does not have a direct equivalent setting.
            Log.d("OdooXmlRpcClient", "Config set with URL: $serverURL")
        }
    } catch (e: MalformedURLException) {
        Log.e("OdooXmlRpcClient", "Malformed URL Exception: ${e.localizedMessage}")
    } catch (e: Exception) {
        Log.e("OdooXmlRpcClient", "Error setting up client config: ${e.localizedMessage}")
    }
    return null
}

    //    suspend fun login(username: String, password: String): Int {
//        return try {
//            val config = getClientConfig("common")
//            if (config == null) {
//                Log.e("OdooXmlRpcClient", "Client configuration is null, aborting login.")
//                return -1
//            }
//            val client = XmlRpcClient().also { it.setConfig(config) }
//            val params = listOf(Constants.DATABASE, username, password, emptyMap<String, Any>())
//            val result = client.execute("authenticate", params) as Int
//            Log.d("OdooXmlRpcClient", "Raw server response for login: $result")
//            if (result > 0) credentialManager.storeUserCredentials(username, password, result)
//            result
//            Log.d("OdooXmlRpcClient", "UserId: $result")
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error during login: ${e.localizedMessage}")
//            -1
//        }
//    }
    suspend fun login(username: String, password: String): Int {
        try {
            val config = getClientConfig("common")
            if (config == null) {
                Log.e("OdooXmlRpcClient", "Client configuration is null, aborting login.")
                return -1
            }

            val client = XmlRpcClient().also { it.setConfig(config) }
            val params = arrayOf(Constants.DATABASE, username, password, emptyMap<String, Any>())

            // Execute the authenticate method and expect an Integer result for userId
            val result = client.execute("authenticate", params) as? Int ?: -1
            Log.d("OdooXmlRpcClient", "Received UserID from login attempt: $result")

            if (result > 0) {
                credentialManager.storeUserCredentials(username, password, result)
                Log.d("OdooXmlRpcClient", "Login successful with UserID: $result before storing in prefs")
            } else {
                Log.e("OdooXmlRpcClient", "Login failed with UserID: $result")
            }

            return result
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error during login: ${e.localizedMessage}", e)
            return -1
        }
    }


    suspend fun fetchReceipts(): List<Receipt> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchReceipts.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        // Updated domain with additional condition for 'state' field
        val domain = listOf(
            listOf("picking_type_id.code", "=", "incoming"), // Only 'incoming' operation types
            listOf("state", "=", "assigned"), // Only receipts with 'Ready' state
            "|",
            listOf("user_id", "=", false), // No responsible user
            listOf("user_id", "=", userId) // Or the current user is the responsible user
        )
        val fields = listOf(
            "id",
            "name",
            "date",
            "user_id",
            "state",
            "origin"
        ) // Include 'state' if you want to verify it in the result

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            result.mapNotNull { it as? Map<String, Any> }.mapNotNull { map ->
                val receiptUserId = map["user_id"] as? Int
                // Since the domain already ensures we only fetch relevant receipts,
                // this additional check might not be necessary unless you need to perform
                // further filtering.
                Receipt(
                    id = map["id"] as Int,
                    name = map["name"] as String,
                    date = map["date"].toString(),
                    origin = map["origin"].toString(),
                    // Include user_id if your Receipt class supports it
                    // Include state if you want to use it for further validation or logic
                )
            }.also {
                Log.d("OdooXmlRpcClient", "Fetched ${it.size} receipts with 'Ready' state")
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching receipts: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    suspend fun fetchProductsForReceipt(receiptId: Int): List<Product> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting fetchProductsForReceipt."
            )
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val fields = listOf("product_summary")

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(listOf(listOf("id", "=", receiptId))),
            mapOf("fields" to fields)
        )

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            val productsSummary =
                if (result.isNotEmpty()) (result[0] as Map<String, Any>)["product_summary"].toString() else ""
            parseProductSummary(productsSummary)
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error fetching products for receipt: ${e.localizedMessage}",
                e
            )
            emptyList()
        }
    }

    private fun parseProductSummary(summary: String): List<Product> {
        return summary.split(", ").mapNotNull { productString ->
            // Split each product entry into ID, Name, and Quantity based on ':'
            val parts = productString.split(":").map { it.trim() }
            if (parts.size == 3) {
                try {
                    val id = parts[0].toInt() // Convert ID to Int
                    val name = parts[1]
                    val quantity = parts[2].toDouble() // Convert Quantity to Double
                    Product(id = id, name = name, quantity = quantity)
                } catch (e: Exception) {
                    Log.e("parseProductSummary", "Error parsing product summary: $e")
                    null // In case of formatting or conversion issue, return null to exclude this entry
                }
            } else {
                null // Exclude if the product string format does not match expected parts count
            }
        }
    }

    suspend fun fetchProductImageByName(productName: String): String? {
        val config = getClientConfig("object") ?: return null
        val client = XmlRpcClient().also { it.setConfig(config) }
        val domain = listOf(listOf("name", "=", productName))
        val fields = listOf("image_1920")

        val params = listOf(
            Constants.DATABASE,
            credentialManager.getUserId(),
            credentialManager.getPassword() ?: "",
            "product.product",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val results = client.execute("execute_kw", params) as? Array<Any>
            if (!results.isNullOrEmpty()) {
                val productData = results[0] as Map<String, Any>
                productData["image_1920"] as? String
            } else null
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching product image: ${e.localizedMessage}")
            null
        }
    }

    suspend fun fetchProductBarcodeByName(productName: String): String? {
        val config = getClientConfig("object") ?: return null
        val client = XmlRpcClient().also { it.setConfig(config) }

        val domain = listOf(listOf("name", "=", productName))
        val fields = listOf("barcode")

        val params = listOf(
            Constants.DATABASE,
            credentialManager.getUserId(),
            credentialManager.getPassword() ?: "",
            "product.template",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val results = client.execute("execute_kw", params) as? Array<Any>
            if (!results.isNullOrEmpty()) {
                val productData = results[0] as Map<String, Any>
                productData["barcode"] as? String
            } else null
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching product barcode: ${e.localizedMessage}")
            null
        }
    }


    suspend fun fetchProductTrackingAndExpirationByName(productName: String): Pair<String?, Boolean?>? {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting fetchProductTrackingAndExpirationByName."
            )
            return null
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""
        val database = Constants.DATABASE

        val domain = listOf(listOf("name", "=", productName))
        val fields = listOf("tracking", "use_expiration_date")

        val params = listOf(
            database,
            userId,
            password,
            "product.template",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            if (result.isNotEmpty()) {
                val productData = result[0] as Map<*, *>
                val tracking = productData["tracking"] as? String
                val useExpirationDate = productData["use_expiration_date"] as? Boolean
                Pair(tracking, useExpirationDate)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error fetching product tracking and expiration date: ${e.localizedMessage}",
                e
            )
            null
        }
    }

    //    suspend fun fetchSerialNumbersByProductName(productName: String): List<String>? {
//        val config = getClientConfig("object") ?: return null
//        val client = XmlRpcClient().also { it.setConfig(config) }
//
//        // Assuming 'product_id' is linked by a field 'name' in 'product.product' model
//        // and serial numbers are stored in 'stock.lot' model under the field 'name'.
//        val productDomain = listOf(listOf("name", "=", productName))
//        val productFields = listOf("id") // We only need the product ID to link to its serial numbers
//
//        val productParams = listOf(
//            Constants.DATABASE,
//            credentialManager.getUserId(),
//            credentialManager.getPassword() ?: "",
//            "product.product",
//            "search_read",
//            listOf(productDomain),
//            mapOf("fields" to productFields)
//        )
//
//        try {
//            val productResults = client.execute("execute_kw", productParams) as? Array<Any>
//            val productId = (productResults?.firstOrNull() as? Map<*, *>)?.get("id") as? Int ?: return null
//
//            // Now fetch serial numbers linked to this product ID in 'stock.lot' model
//            val serialDomain = listOf(listOf("product_id", "=", productId))
//            val serialFields = listOf("name") // The field that stores the serial number
//
//            val serialParams = listOf(
//                Constants.DATABASE,
//                credentialManager.getUserId(),
//                credentialManager.getPassword() ?: "",
//                "stock.lot",
//                "search_read",
//                listOf(serialDomain),
//                mapOf("fields" to serialFields)
//            )
//
//            val serialResults = client.execute("execute_kw", serialParams) as? Array<Any>
//            return serialResults?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching serial numbers: ${e.localizedMessage}", e)
//            return null
//        }
//    }
    suspend fun fetchLotAndSerialNumbersByProductId(productId: Int): List<String>? {
        val config = getClientConfig("object") ?: return null
        val client = XmlRpcClient().also { it.setConfig(config) }

        // Now directly use the provided productId to fetch serial numbers linked to this product ID in 'stock.lot' model
        val serialDomain = listOf(listOf("product_id", "=", productId))
        val serialFields = listOf("name") // The field that stores the serial number

        val serialParams = listOf(
            Constants.DATABASE,
            credentialManager.getUserId(),
            credentialManager.getPassword() ?: "",
            "stock.lot",
            "search_read",
            listOf(serialDomain),
            mapOf("fields" to serialFields)
        )

        try {
            val serialResults = client.execute("execute_kw", serialParams) as? Array<Any>
            return serialResults?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error fetching serial numbers for product ID $productId: ${e.localizedMessage}",
                e
            )
            return null
        }
    }


    suspend fun fetchInternalTransfersWithProductDetails(): List<InternalTransfers> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting fetchInternalTransfers."
            )
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(
            listOf("state", "=", "assigned"),
            listOf("picking_type_id.code", "=", "internal")
        )
        val fields = listOf("id", "name", "scheduled_date", "origin", "state")


        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            result.mapNotNull { it as? Map<String, Any> }
                .mapNotNull { map ->
                    val transferId = map["id"] as Int
                    val transferName = map["name"] as String
                    val transferDate = map["scheduled_date"].toString()
                    val sourceDocument = map["origin"] as? String ?: ""

                    val products = fetchProductsForInternalTransfer(transferId)

                    // Ensure the fetched products are mapped to IntTransferProducts
                    val intTransferProductsList = products.map { product ->
                        IntTransferProducts(
                            name = product.name,
                            quantity = product.quantity,
                            transferDate = transferDate
                        )
                    }

                    InternalTransfers(
                        id = transferId,
                        transferName = transferName,
                        transferDate = transferDate,
                        sourceDocument = sourceDocument,
                        productDetails = intTransferProductsList
                    )
                }.also {
                    Log.d(
                        "OdooXmlRpcClient",
                        "Fetched ${it.size} internal transfers with product details."
                    )
                }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching internal transfers: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    private suspend fun fetchProductsForInternalTransfer(transferId: Int): List<Product> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting fetchProductsForInternalTransfer."
            )
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        // Adjust the domain filter to specifically fetch the desired internal transfer by ID.
        // The following assumes internal transfers are distinguished within your setup and
        // the `internal_transfer_summary` field exists and is computed for these records.
        val domain = listOf(listOf("id", "=", transferId))
        val fields =
            listOf("internal_transfer_summary")  // Use the custom computed field for internal transfers

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            // Extract the internal_transfer_summary for the specified transfer
            val internalTransferSummary =
                if (result.isNotEmpty()) (result[0] as Map<String, Any>)["internal_transfer_summary"].toString() else ""
            parseInternalTransferSummary(internalTransferSummary)  // You may need to implement or adjust this method
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error fetching products for internal transfer: ${e.localizedMessage}",
                e
            )
            emptyList()
        }
    }

    private fun parseInternalTransferSummary(summary: String): List<Product> {
        // Check if the summary is not empty
        if (summary.isEmpty()) return emptyList()

        return summary.split(", ").mapNotNull { productString ->
            // Expected format: "productID:productName: quantity"
            val parts = productString.split(": ")
            if (parts.size == 3) {  // Adjusting for the expected three parts
                val productId = parts[0].toIntOrNull()  // Convert ID to Integer
                val productName = parts[1]
                val productQuantity = parts[2].toDoubleOrNull()  // Convert quantity to Double
                if (productId != null && productQuantity != null) {
                    Product(
                        id = productId,
                        name = productName,
                        quantity = productQuantity
                    )  // Including ID in the constructor
                } else {
                    null  // If conversion fails, exclude this product from the list
                }
            } else {
                null  // Exclude if the product string format is unexpected
            }
        }
    }

    suspend fun fetchTransferIdByReference(transferReference: String): Int? {
        // Assuming getClientConfig, XmlRpcClient, userId, and password are accessible here
        val config = getClientConfig("object") ?: return null
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(listOf("name", "=", transferReference))
        val fields = listOf("id")

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val result =
                client.execute("execute_kw", params) as? Array<Map<String, Any>> ?: arrayOf()
            if (result.isNotEmpty()) result[0]["id"] as? Int else null
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error fetching transfer ID for reference $transferReference: ${e.localizedMessage}",
                e
            )
            null
        }
    }

    suspend fun fetchAndLogBuyerDetails(pickingName: String): BuyerDetails? {
        Log.d("OdooXmlRpcClient", "Starting fetchAndLogBuyerDetails for pickingName: $pickingName")
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting fetchBuyerDetailsForReceipt."
            )
            return null
        }
        Log.d("OdooXmlRpcClient", "Configuration loaded successfully.")

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val fields = listOf("name", "buyer_id")
        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(listOf(listOf("name", "=", pickingName))),
            mapOf("fields" to fields)
        )

        return try {
            Log.d("OdooXmlRpcClient", "Executing XML-RPC call for stock.picking.")
            val result = client.execute("execute_kw", params) as Array<Any>
            Log.d("OdooXmlRpcClient", "XML-RPC call executed, processing results.")

            if (result.isNotEmpty()) {
                val picking = result[0] as Map<String, Any>
                Log.d("OdooXmlRpcClient", "Picking found: ${picking["name"]}")

                val buyerId = (picking["buyer_id"] as? Array<Any>)?.firstOrNull() as? Int
                if (buyerId != null) {
                    Log.d("OdooXmlRpcClient", "Found buyer ID: $buyerId, fetching buyer details.")
                    fetchUserDetails(buyerId)
                } else {
                    Log.d("OdooXmlRpcClient", "No buyer associated with this picking.")
                    null
                }
            } else {
                Log.d("OdooXmlRpcClient", "Picking not found.")
                null
            }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error fetching buyer details for receipt: ${e.localizedMessage}",
                e
            )
            null
        }
    }



    private suspend fun fetchUserDetails(userId: Int): BuyerDetails? {
        Log.d("OdooXmlRpcClient", "Starting fetchUserDetails for userId: $userId")
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchUserDetails.")
            return null
        }
        Log.d("OdooXmlRpcClient", "Configuration loaded successfully for user details fetch.")

        val client = XmlRpcClient().also { it.setConfig(config) }
        val authUserId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val fields = listOf("name", "login")
        val params = listOf(
            Constants.DATABASE,
            authUserId,
            password,
            "res.users",
            "search_read",
            listOf(listOf(listOf("id", "=", userId))),
            mapOf("fields" to fields)
        )

        return try {
            Log.d("OdooXmlRpcClient", "Executing XML-RPC call for res.users.")
            val result = client.execute("execute_kw", params) as Array<Any>
            Log.d("OdooXmlRpcClient", "XML-RPC call executed, processing user details.")

            if (result.isNotEmpty()) {
                val userDetails = result[0] as Map<String, Any>
                Log.d(
                    "OdooXmlRpcClient",
                    "User details found: ${userDetails["login"]},${userDetails["name"]} "
                )

                BuyerDetails(
                    id = userId,
                    name = userDetails["name"].toString(),
                    login = userDetails["login"].toString()
                )
            } else {
                Log.d("OdooXmlRpcClient", "User not found.")
                null
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching user details: ${e.localizedMessage}", e)
            null
        }
    }


    suspend fun updateMoveLinesByPicking(
        pickingId: Int,
        productId: Int,
        serialNumber: String,
        expirationDate: String?
    ) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting updateMoveLinesByPicking."
            )
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        // Ensure the format of expirationDate matches Odoo's expectations, e.g., "YYYY-MM-DD HH:MM:SS"
        val formattedExpirationDate = "$expirationDate 00:00:00"

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line", // Assuming this is the model where your custom method is defined
            "create_update_move_line", // The name of your custom method
            listOf(
                pickingId,
                productId,
                serialNumber,
                formattedExpirationDate
            ) // Parameters for your custom method
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d("OdooXmlRpcClient", "Successfully updated/created stock.move.line record.")
            } else {
                Log.e("OdooXmlRpcClient", "Failed to update/create stock.move.line record.")
            }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error executing updateMoveLinesByPicking: ${e.localizedMessage}"
            )
        }
    }

    suspend fun updateMoveLinesWithoutExpiration(
        pickingId: Int,
        productId: Int,
        serialNumber: String
    ) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting updateMoveLinesWithoutExpiration."
            )
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line", // Ensure this matches the model where your method is defined
            "create_update_move_line_without_expiration", // Use the adjusted method name that excludes expiration date
            listOf(pickingId, productId, serialNumber) // Parameters excluding the expiration date
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d(
                    "OdooXmlRpcClient",
                    "Successfully updated/created stock.move.line record without expiration date."
                )
            } else {
                Log.e(
                    "OdooXmlRpcClient",
                    "Failed to update/create stock.move.line record without expiration date."
                )
            }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error executing updateMoveLinesWithoutExpiration: ${e.localizedMessage}"
            )
        }
    }

    //    create_update_move_line_for_pick
    suspend fun updateMoveLinesForPick(
        lineId: Int,
        pickingId: Int,
        serialNumber: String,
        productId: Int,
    ) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting updateMoveLinesWithoutExpiration."
            )
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line", // Ensure this matches the model where your method is defined
            "update_move_line_with_serial_number", // Use the adjusted method name that excludes expiration date
            listOf(
                lineId,
                pickingId,
                serialNumber,
                productId,
            ) // Parameters excluding the expiration date
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d(
                    "OdooXmlRpcClient",
                    "Successfully updated/created stock.move.line record without expiration date."
                )
            } else {
                Log.e(
                    "OdooXmlRpcClient",
                    "Failed to update/create stock.move.line record without expiration date."
                )
            }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error executing updateMoveLinesWithoutExpiration: ${e.localizedMessage}"
            )
        }
    }
    suspend fun updateMoveLinesForReceipt(
        lineId: Int,
        pickingId: Int,
        serialNumber: String
    ) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting updateMoveLinesWithoutExpiration."
            )
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line", // Ensure this matches the model where your method is defined
            "write_lot_name_for_receiving", // Use the adjusted method name that excludes expiration date
            listOf(
                lineId,
                pickingId,
                serialNumber
            ) // Parameters excluding the expiration date
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d(
                    "OdooXmlRpcClient",
                    "Successfully updated/created stock.move.line record without expiration date."
                )
            } else {
                Log.e(
                    "OdooXmlRpcClient",
                    "Failed to update/create stock.move.line record without expiration date."
                )
            }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error executing updateMoveLinesWithoutExpiration: ${e.localizedMessage}"
            )
        }
    }
    suspend fun updateMoveLineQuantityForReceipt(
        lineId: Int,
        pickingId: Int,
        quantity: Int
    ) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting updateMoveLinesWithoutExpiration."
            )
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line", // Ensure this matches the model where your method is defined
            "write_quantity_for_receiving", // Use the adjusted method name that excludes expiration date
            listOf(
                lineId,
                pickingId,
                quantity
            ) // Parameters excluding the expiration date
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d(
                    "OdooXmlRpcClient",
                    "Successfully updated/created stock.move.line record without expiration date."
                )
            } else {
                Log.e(
                    "OdooXmlRpcClient",
                    "Failed to update/create stock.move.line record without expiration date."
                )
            }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error executing updateMoveLinesWithoutExpiration: ${e.localizedMessage}"
            )
        }
    }

    suspend fun updateMoveLinesByPickingWithLot(
        pickingId: Int,
        productId: Int,
        lotName: String,
        quantity: Int,
        expirationDate: String?
    ) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting updateMoveLinesByPickingWithLot."
            )
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        // Format the expiration date if not null; else pass null
        val formattedExpirationDate = expirationDate?.let { "$it 00:00:00" }

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line",
            "create_update_move_line_with_lot", // Updated method name for lots
            listOf(pickingId, productId, lotName, quantity, formattedExpirationDate)
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d(
                    "OdooXmlRpcClient",
                    "Successfully updated/created stock.move.line record with lot."
                )
            } else {
                Log.e(
                    "OdooXmlRpcClient",
                    "Failed to update/create stock.move.line record with lot."
                )
            }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error executing updateMoveLinesByPickingWithLot: ${e.localizedMessage}"
            )
        }
    }

    suspend fun updateMoveLinesWithoutExpirationWithLot(
        pickingId: Int,
        productId: Int,
        lotName: String,
        quantity: Int,
        location: String
    ) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting updateMoveLinesWithoutExpirationWithLot."
            )
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line",
            "create_update_move_line_lot_without_expiration",
            listOf(pickingId, productId, lotName, quantity, location)
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d(
                    "OdooXmlRpcClient",
                    "Successfully updated/created stock.move.line record without expiration date for lot."
                )
            } else {
                Log.e(
                    "OdooXmlRpcClient",
                    "Failed to update/create stock.move.line record without expiration date for lot."
                )
            }
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error executing updateMoveLinesWithoutExpirationWithLot: ${e.localizedMessage}"
            )
        }
    }

//    suspend fun validateOperation(pickingId: Int): Boolean {
//        return try {
//            val username = credentialManager.getUsername()
//            val password = credentialManager.getPassword()
//
//            if (username == null || password == null) {
//                Log.e("OdooXmlRpcClient", "Credentials are null, aborting changePickState.")
//                return false
//            }
//
//            val db = Constants.DATABASE
//            val config = XmlRpcClientConfigImpl().apply {
//                serverURL = URL("${Constants.URL}xmlrpc/2/object")
//            }
//            val client = XmlRpcClient()
//            client.setConfig(config)
//
//            val userId = login(username, password)
//            if (userId <= 0) {
//                Log.e("OdooXmlRpcClient", "Login failed, cannot change pick state.")
//                return false
//            }
//
//            val validateParams = listOf(
//                db,
//                userId,
//                password,
//                "stock.picking",
//                "button_validate",
//                listOf(listOf(pickingId))
//            )
//
//            client.execute("execute_kw", validateParams).let {
//                Log.d("OdooXmlRpcClient", "Picking validated successfully. ${pickingId}")
//                return true
//            }
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error during changePickState: ${e.message}", e)
//            false
//        }
//    }
    suspend fun validateOperation(pickingId: Int): Boolean {
        val username = credentialManager.getUsername()
        val password = credentialManager.getPassword()

        if (username == null || password == null) {
            Log.e("OdooXmlRpcClient", "Credentials are null, aborting changePickState.")
            return false
        }

        val db = Constants.DATABASE
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "XML RPC Client configuration is null.")
            return false
        }

        val client = XmlRpcClient().apply {
            setConfig(config)
        }

        val userId = login(username, password)
        if (userId <= 0) {
            Log.e("OdooXmlRpcClient", "Login failed, cannot change pick state.")
            return false
        }

        val validateParams = listOf(
            db,
            userId,
            password,
            "stock.picking",
            "button_validate",
            listOf(pickingId)  // Ensure this is correctly populated and not null
        )

        validateParams.forEach {
            if (it == null) Log.e("OdooXmlRpcClient", "Null found in validateParams: $validateParams")
        }

        return try {
            val result = client.execute("execute_kw", validateParams) as? Boolean
            result ?: false  // Default to false if null or non-Boolean was returned
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error during changePickState: ${e.message}", e)
            false
        }
    }





//    suspend fun validateOperation(pickingId: Int): Boolean {
//        return try {
//            val username = credentialManager.getUsername()
//            val password = credentialManager.getPassword()
//
//            if (username == null || password == null) {
//                Log.e("OdooXmlRpcClient", "Credentials are null, aborting changePickState.")
//                return false
//            }
//
//            val db = Constants.DATABASE
//            val config = XmlRpcClientConfigImpl().apply {
//                serverURL = URL("${Constants.URL}xmlrpc/2/object")
//            }
//            val client = XmlRpcClient()
//            client.setConfig(config)
//
//            val userId = login(username, password)
//            if (userId <= 0) {
//                Log.e("OdooXmlRpcClient", "Login failed, cannot change pick state.")
//                return false
//            }
//
//            val validateParams = listOf(
//                db,
//                userId,
//                password,
//                "stock.picking",
//                "button_validate",
//                listOf(listOf(pickingId))
//            )
//
//            client.execute("execute_kw", validateParams).let {
//                Log.d("OdooXmlRpcClient", "Picking validated successfully. Picking ID: $pickingId")
//                return true
//            }
//        } catch (e: XmlRpcException) {
//            Log.e("OdooXmlRpcClient", "Odoo returned an error: ${e.message}", e)
//            false
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error during changePickState: ${e.message}", e)
//            false
//        }
//    }


//    suspend fun fetchPicks(): List<Pick> {
//        val config = getClientConfig("object")
//        if (config == null) {
//            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchDeliveryOrders.")
//            return emptyList()
//        }
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        // Adjust domain to include 'PICK' in the name
//        val domain = listOf(
//            listOf("picking_type_id.code", "=", "internal"),
//            listOf("state", "in", listOf("assigned", "waiting")),
//            "|",
//            listOf("user_id", "=", false),
//            listOf("user_id", "=", userId),
//            listOf("name", "ilike", "PICK") // Only fetch records where the name contains 'PICK'
//        )
//        val fields = listOf("id", "name", "date", "user_id", "state", "origin")
//
//        val params = listOf(
//            Constants.DATABASE,
//            userId,
//            password,
//            "stock.picking",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val result = client.execute("execute_kw", params) as Array<Any>
//            result.mapNotNull { it as? Map<String, Any> }.mapNotNull { map ->
//                Pick(
//                    id = map["id"] as Int,
//                    name = map["name"] as String,
//                    date = map["date"] as String,
//                    origin = map["origin"].toString(),
//                )
//            }.also {
//                Log.d("OdooXmlRpcClient", "Fetched ${it.size} picks with 'PICK' in the name")
//            }
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching picks: ${e.localizedMessage}", e)
//            emptyList()
//        }
//    }

    //suspend fun fetchPicks(): List<Pick> {
//    val config = getClientConfig("object")
//    if (config == null) {
//        Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchPicks.")
//        return emptyList()
//    }
//    val client = XmlRpcClient().also { it.setConfig(config) }
//    val userId = credentialManager.getUserId()
//    val password = credentialManager.getPassword() ?: ""
//
//    val domain = listOf(
//        listOf("picking_type_id.code", "=", "internal"),
//        listOf("state", "in", listOf("assigned", "waiting")),
//        "|",
//        listOf("user_id", "=", false),
//        listOf("user_id", "=", userId),
//        listOf("name", "ilike", "PICK")
//    )
//    val fields = listOf(
//        "id",
//        "name",
//        "date",
//        "user_id",
//        "state",
//        "origin",
//        "location_id",
//        "location_dest_id"
//    )
//
//    val params = listOf(
//        Constants.DATABASE,
//        userId,
//        password,
//        "stock.picking",
//        "search_read",
//        listOf(domain),
//        mapOf("fields" to fields)
//    )
//
//    return try {
//        val result = client.execute("execute_kw", params) as Array<Any>
//        result.mapNotNull { it as? Map<String, Any> }.mapNotNull { map ->
//            Pick(
//                id = map["id"] as Int,
//                name = map["name"] as String,
//                date = map["date"] as String,
//                origin = map["origin"].toString(),
//                locationId = (map["location_id"] as? Array<Any>)?.get(0)?.toString()?.toIntOrNull(),
//                locationDestId = (map["location_dest_id"] as? Array<Any>)?.get(0)?.toString()
//                    ?.toIntOrNull()
//            )
//        }.also { picks ->
//            Log.d("OdooXmlRpcClient", "Fetched ${picks.size} picks with 'PICK' in the name")
//            picks.forEach { pick ->
//                Log.d(
//                    "OdooXmlRpcClient",
//                    "Pick ID: ${pick.id}, Name: ${pick.name}, Location ID: ${pick.locationId}, Destination Location ID: ${pick.locationDestId}"
//                )
//            }
//        }
//    } catch (e: Exception) {
//        Log.e("OdooXmlRpcClient", "Error fetching picks: ${e.localizedMessage}", e)
//        emptyList()
//    }
//}
    suspend fun fetchPicks(): List<Pick> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchPicks.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(
            listOf("picking_type_id.code", "=", "internal"),
            listOf("state", "in", listOf("assigned", "waiting")),
            "|",
            listOf("user_id", "=", false),
            listOf("user_id", "=", userId),
            listOf("name", "ilike", "PICK")
        )
        val fields = listOf(
            "id",
            "name",
            "date",
            "user_id",
            "state",
            "origin"
        )

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            Log.d("OdooXmlRpcClient", "Making XML-RPC call with UserID: ${credentialManager.getUserId()}")

            val picksResult = client.execute("execute_kw", params) as Array<Any>
            val picks = picksResult.mapNotNull { it as? Map<String, Any> }.mapNotNull { map ->
                Pick(
                    id = map["id"] as Int,
                    name = map["name"] as String,
                    date = map["date"] as String,
                    origin = map["origin"].toString(),
                    locationId = null, // Set these to null initially
                    locationDestId = null
                )
            }

            // For each pick, fetch the locations from stock.move.line model
            picks.forEach { pick ->
                val moveLineDomain = listOf(
                    listOf("picking_id", "=", pick.id)
                )
                val moveLineFields = listOf("location_id", "location_dest_id")
                val moveLineParams = listOf(
                    Constants.DATABASE,
                    userId,
                    password,
                    "stock.move.line",
                    "search_read",
                    listOf(moveLineDomain),
                    mapOf(
                        "fields" to moveLineFields,
                        "limit" to 1
                    ) // Assuming one move line per pick for simplification
                )

                val moveLineResult = client.execute("execute_kw", moveLineParams) as Array<Any>
                val moveLine = moveLineResult.mapNotNull { it as? Map<String, Any> }.firstOrNull()

                pick.locationId = (moveLine?.get("location_id") as? Array<Any>)?.get(1)?.toString()
                pick.locationDestId =
                    (moveLine?.get("location_dest_id") as? Array<Any>)?.get(1)?.toString()
            }

            // Log the picks with location names
//            picks.forEach { pick ->
//                Log.d(
//                    "fetchPicks OXPC",
//                    "Pick ID: ${pick.id}, Name: ${pick.name}, Location: ${pick.locationId}, Destination Location: ${pick.locationDestId}"
//                )
//            }

            picks
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching picks: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    suspend fun fetchProductsForDeliveryOrder(deliveryOrderId: Int): List<Product> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e(
                "OdooXmlRpcClient",
                "Client configuration is null, aborting fetchProductsForDeliveryOrder."
            )
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val fields = listOf("delivery_order_product_summary")

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(listOf(listOf("id", "=", deliveryOrderId))),
            mapOf("fields" to fields)
        )

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            val deliveryOrderSummary =
                if (result.isNotEmpty()) (result[0] as Map<String, Any>)["delivery_order_product_summary"].toString() else ""
            parseDelivProductSummary(deliveryOrderSummary)
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error fetching products for delivery order: ${e.localizedMessage}",
                e
            )
            emptyList()
        }
    }

    private fun parseDelivProductSummary(summary: String): List<Product> {
        // The parsing logic remains the same as it's based on the summary text structure
        return summary.split(", ").mapNotNull { productString ->
            val parts = productString.split(":").map { it.trim() }
            if (parts.size == 3) {
                try {
                    val id = parts[0].toInt()
                    val name = parts[1]
                    val quantity = parts[2].toDouble()
                    Product(id = id, name = name, quantity = quantity)
                } catch (e: Exception) {
                    Log.e("parseProductSummary", "Error parsing product summary: $e")
                    null
                }
            } else {
                null
            }
        }
    }


    //    suspend fun fetchProductIdFromPackagingBarcode(barcode: String): Int? {
//        val config = getClientConfig("object") ?: return null
//        val client = XmlRpcClient().also { it.setConfig(config) }
//
//        val domain = listOf(listOf("barcode", "=", barcode))
//        val fields = listOf("product_id")
//        val params = listOf(
//            Constants.DATABASE,
//            credentialManager.getUserId(),
//            credentialManager.getPassword() ?: "",
//            "product.packaging",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val results = client.execute("execute_kw", params) as? Array<Any>
//            if (!results.isNullOrEmpty()) {
//                val packagingData = results[0] as Map<String, Any>
//                // Assuming product_id is stored as an Array[Any] (common in Odoo for many2one fields)
//                val productId = (packagingData["product_id"] as? Array<Any>)?.firstOrNull() as? Int
//                productId
//            } else null
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching product ID from packaging barcode: ${e.localizedMessage}")
//            null
//        }
//    }
    suspend fun fetchProductIdFromPackagingBarcode(barcode: String): Pair<Int?, Double?>? {
        val config = getClientConfig("object") ?: return null
        val client = XmlRpcClient().also { it.setConfig(config) }

        val domain = listOf(listOf("barcode", "=", barcode))
        val fields = listOf("product_id", "qty")
        val params = listOf(
            Constants.DATABASE,
            credentialManager.getUserId(),
            credentialManager.getPassword() ?: "",
            "product.packaging",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val results = client.execute("execute_kw", params) as? Array<Any>
            if (!results.isNullOrEmpty()) {
                val packagingData = results[0] as Map<String, Any>
                // Assuming product_id is stored as an Array[Any] (common in Odoo for many2one fields)
                val productId = (packagingData["product_id"] as? Array<Any>)?.firstOrNull() as? Int
                val quantity = packagingData["qty"] as? Double
                Pair(productId, quantity)
            } else null
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error fetching product ID and quantity from packaging barcode: ${e.localizedMessage}"
            )
            null
        }
    }

    suspend fun createStockMoveLineForUntrackedProduct(
        pickingId: Int,
        productId: Int,
        quantity: Double,
        locationName: String
    ): Boolean {
        return try {
            val config = getClientConfig("object")
            val client = XmlRpcClient().also { it.setConfig(config) }

            // Assuming you have these methods or equivalent ways to get the userID and password
            val userId = credentialManager.getUserId()
            val password = credentialManager.getPassword() ?: ""

            val params = listOf(
                Constants.DATABASE,
                userId,
                password,
                "stock.move.line",
                "create_move_line_for_untracked",  // Method name in your custom module
                listOf(pickingId, productId, quantity, locationName)
            )

            val result = client.execute("execute_kw", params) as? Boolean ?: false
            Log.d("OdooXmlRpcClient", "Stock move line creation result: $result")
            result
        } catch (e: Exception) {
            Log.e(
                "OdooXmlRpcClient",
                "Error creating stock move line for untracked product: ${e.message}",
                e
            )
            false
        }
    }

    suspend fun fetchMoveLinesByPickingId(pickingId: Int): List<MoveLine> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchMoveLinesByPickingId.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(listOf("id", "=", pickingId))
        val fields = listOf("move_lines_summary")  // Assuming this field contains the move lines summary

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            val moveLinesSummary = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["move_lines_summary"].toString() else ""
            val moveLines = parseMoveLinesSummary(moveLinesSummary)

            // Log each MoveLine
            moveLines.forEach { moveLine ->
                Log.d("MoveLineData", "ID: ${moveLine.id}, Product ID: ${moveLine.productId}, Product Name: ${moveLine.productName}, Lot ID: ${moveLine.lotId}, Lot Name: ${moveLine.lotName}, Quantity: ${moveLine.quantity}, LocationId: ${moveLine.locationId}, Location Name:${moveLine.locationName}" +
                        "Location dest: ${moveLine.locationDestName}")
            }
//            Log.d("OdooXmlRpcClient", "Raw server response: ${result.toList()}")


            moveLines
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching move lines for picking ID: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    private fun parseMoveLinesSummary(summary: String): List<MoveLine> {
        return summary.split(", ").mapNotNull { lineString ->
            val parts = lineString.split(":")
            if (parts.size >= 8) {  // Make sure there are at least 6 parts
                try {
                    val productId = parts[0].toInt()
                    val productName = parts[1]
                    val lotId = parts[2].takeIf { it.isNotEmpty() && it != "None" }?.toIntOrNull()
                    val lotName = parts[3].takeIf { it != "None" } ?: ""
                    val quantity = parts[4].toDouble()
                    val id = parts[5].toInt()
                    val locationId = parts.getOrNull(6)?.toIntOrNull() ?: -1  // Optional: Default to -1 if not present
                    val locationName = parts.getOrNull(7) ?: "Unknown" // Optional: Default to "Unknown" if not present
                    val locationDestId = parts.getOrNull(8)?.toIntOrNull() ?: -1
                    val locationDestName = parts.getOrNull(9) ?: "Unknown"

                    MoveLine(
                        id = id,
                        productId = productId,
                        productName = productName,
                        lotId = lotId,
                        lotName = lotName,
                        quantity = quantity,
                        locationId = locationId,
                        locationName = locationName,
                        locationDestId = locationDestId,
                        locationDestName = locationDestName,

                    )
                } catch (e: Exception) {
                    Log.e("parseMoveLinesSummary", "Error parsing move lines summary: ${e.localizedMessage}")
                    null
                }
            } else {
                null
            }
        }
    }



    suspend fun fetchResultPackagesByPickingId(pickingId: Int): List<PackageInfo> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchResultPackagesByPickingId.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(listOf("id", "=", pickingId))
        val fields = listOf("result_packages")  // Fetching result_packages field

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        Log.d("OdooXmlRpcClient", "Fetching result packages for picking ID: $pickingId with params: $params")

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            Log.d("OdooXmlRpcClient", "Raw server response for result packages: ${result.toList()}")

            val resultPackages = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["result_packages"].toString() else ""
            val parsedPackages = parseResultPackages(resultPackages)

            Log.d("OdooXmlRpcClient", "Parsed result packages: $parsedPackages")
            parsedPackages
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching result packages for picking ID: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    private fun parseResultPackages(packages: String): List<PackageInfo> {
        return packages.split(", ").mapNotNull { packageString ->
            val parts = packageString.split(":")
            if (parts.size == 2) {
                try {
                    val packageId = parts[0].toInt()
                    val packageName = parts[1]
                    PackageInfo(packageId, packageName)
                } catch (e: Exception) {
                    Log.e("parseResultPackages", "Error parsing package info: ${e.localizedMessage}")
                    null
                }
            } else {
                null
            }
        }
    }


    suspend fun putMoveLineInNewPack(moveLineId: Int) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting putMoveLineInPack.")
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line", // This should match the model where the 'put_in_pack' method is defined
            "put_in_pack", // The name of the method defined in the Odoo model
            listOf(moveLineId) // Parameter for the method
        )

        try {
            val result = withContext(Dispatchers.IO) {
                client.execute("execute_kw", params)
            } as? Map<*, *> // Cast the result to a Map if possible

            if (result?.get("success") == true) {
                Log.d("OdooXmlRpcClient", "Successfully executed put_in_pack for move line ID: $moveLineId, package created: ${result["package_name"]}")
            } else {
                Log.e("OdooXmlRpcClient", "Failed to execute put_in_pack for move line ID: $moveLineId, result was unexpected: $result")
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error executing putMoveLineInPack: ${e.localizedMessage}", e)
        }
    }

    suspend fun updatePickingImage(pickingId: Int, imageBase64: String) {
        val config = getClientConfig("object") ?: run {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updatePickingImage.")
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val methodName = "update_picking_image"
        val model = "stock.picking"
        val args = listOf(pickingId, imageBase64)  // Ensure this is a list
        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            model,
            methodName,
            args
        )

        try {
            val result = client.execute("execute_kw", params) as Map<String, Any>
            Log.d("OdooXmlRpcClient", "Image update result for picking ID $pickingId: $result")
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error updating image for picking ID $pickingId: ${e.localizedMessage}", e)
        }
    }

//    suspend fun fetchReceiptMoveLinesByPickingId(pickingId: Int): List<ReceiptMoveLine> {
//        val config = getClientConfig("object")
//        if (config == null) {
//            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchReceiptMoveLinesByPickingId.")
//            return emptyList()
//        }
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        val domain = listOf(listOf("id", "=", pickingId))
//        val fields = listOf("receipt_move_lines")  // Request the computed field
//
//        val params = listOf(
//            Constants.DATABASE,
//            userId,
//            password,
//            "stock.picking",
//            "search_read",
//            listOf(domain),
//            mapOf("fields" to fields)
//        )
//
//        return try {
//            val result = client.execute("execute_kw", params) as Array<Any>
//            val moveLinesSummary = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["receipt_move_lines"].toString() else ""
//            parseReceiptMoveLinesSummary(moveLinesSummary)
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error fetching receipt move lines for picking ID: ${e.localizedMessage}", e)
//            emptyList()
//        }
//    }
//
//    private fun parseReceiptMoveLinesSummary(summary: String): List<ReceiptMoveLine> {
//        return summary.split(", ").mapNotNull { lineString ->
//            val parts = lineString.split(":")
//            if (parts.size >= 8) {  // Ensure there are enough parts to extract all fields
//                try {
//                    val productId = parts[0].toInt()
//                    val productName = parts[1]
//                    val lotName = parts[2].takeIf { it != "None" } ?: ""
//                    val quantity = parts[3].toDouble()
//                    val id = parts[4].toInt()
//                    val locationDestId = parts[5].toInt()
//                    val locationDestName = parts[6]
//                    val expirationDate = parts[7].takeIf { it != "None" } ?: ""
//
//                    ReceiptMoveLine(
//                        id = id,
//                        productId = productId,
//                        productName = productName,
//                        lotName = lotName,
//                        quantity = quantity,
//                        locationDestId = locationDestId,
//                        locationDestName = locationDestName,
//                        expirationDate = expirationDate
//                    )
//                } catch (e: Exception) {
//                    Log.e("parseReceiptMoveLinesSummary", "Error parsing receipt move lines summary: ${e.localizedMessage}")
//                    null
//                }
//            } else {
//                null
//            }
//        }
//    }

    suspend fun fetchReceiptMoveLinesByPickingId(pickingId: Int): List<ReceiptMoveLine> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchReceiptMoveLinesByPickingId.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(listOf("id", "=", pickingId))
        val fields = listOf("receipt_move_lines")  // Request the computed field

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            val moveLinesSummary = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["receipt_move_lines"].toString() else ""
            parseReceiptMoveLinesSummary(moveLinesSummary)
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching receipt move lines for picking ID: ${e.localizedMessage}", e)
            emptyList()
        }
    }
    private fun parseReceiptMoveLinesSummary(summary: String): List<ReceiptMoveLine> {
        return summary.split(", ").mapNotNull { lineString ->
            val parts = lineString.split(":")
            if (parts.size >= 10) {  // Ensure there are enough parts to extract all fields
                try {
                    val productId = parts[0].toInt()
                    val productName = parts[1]
                    val lotName = parts[2].takeIf { it != "None" } ?: ""
                    val quantity = parts[3].toDouble()
                    val productUomQty = parts[4].toDouble()
                    val moveLineId = parts[5].toInt()
                    val locationDestId = parts[6].toInt()
                    val locationDestName = parts[7]
                    val expirationDate = parts[8].takeIf { it != "None" } ?: ""
                    val totalQuantity = parts[9].toDouble()
                    ReceiptMoveLine(
                        id = moveLineId,
                        productId = productId,
                        productName = productName,
                        lotName = lotName,
                        quantity = quantity,
                        locationDestId = locationDestId,
                        locationDestName = locationDestName,
                        expirationDate = expirationDate,
                        expectedQuantity = productUomQty,
                        totalQuantity = totalQuantity
                    )
                } catch (e: Exception) {
                    Log.e("parseReceiptMoveLinesSummary", "Error parsing receipt move lines summary: ${e.localizedMessage}")
                    null
                }
            } else {
                null
            }
        }
    }

    suspend fun updateMoveLineQuantityUntracked(moveLineId: Int, pickingId: Int, productId: Int, quantity: Double) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLineQuantity.")
            return
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val methodName = "update_move_line_quantity"
        val model = "stock.move.line"
        val args = listOf(moveLineId, pickingId, productId, quantity)  // Arguments to pass to the method

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            model,
            methodName,
            args
        )

        try {
            val result = withContext(Dispatchers.IO) {
                client.execute("execute_kw", params) as Boolean  // Assuming the method returns a boolean
            }
            Log.d("OdooXmlRpcClient", "Update quantity result for move line ID $moveLineId: $result")
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error updating quantity for move line ID $moveLineId: ${e.localizedMessage}", e)
        }
    }

    suspend fun updateMoveLineSerialNumber(moveLineId: Int, pickingId: Int, productId: Int, lotName: String): Boolean {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLineLotName.")
            return false
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line",
            "update_move_line_lot_name",
            listOf(moveLineId, pickingId, productId, lotName)
        )

        return try {
            val result = client.execute("execute_kw", params) as? Boolean ?: false
            Log.d("OdooXmlRpcClient", "Lot name update result for move line ID $moveLineId: $result")
            result
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error updating lot name for move line ID: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun updateMoveLineSerialExpirationDate(moveLineId: Int, pickingId: Int, productId: Int, lotName: String, expirationDate: String): Boolean {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLineDetails.")
            return false
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line",
            "update_move_line_serial_expiration_date",
            listOf(moveLineId, pickingId, productId, lotName, expirationDate)
        )

        return try {
            val result = withContext(Dispatchers.IO) {
                client.execute("execute_kw", params) as? Boolean ?: false
            }
            Log.d("OdooXmlRpcClient", "Details update result for move line ID $moveLineId: $result")
            result
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error updating details for move line ID $moveLineId: ${e.localizedMessage}", e)
            false
        }
    }
    suspend fun updateMoveLineLotAndQuantity(moveLineId: Int, pickingId: Int, productId: Int, lotName: String, quantity: Int): Boolean {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLineLotAndQuantity.")
            return false
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""
        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line",
            "update_move_line_lot_and_quantity",
            listOf(moveLineId, pickingId, productId, lotName, quantity)
        )

        return try {
            withContext(Dispatchers.IO) {
                val result = client.execute("execute_kw", params) as Boolean
                Log.d("OdooXmlRpcClient", "Update lot and quantity result for move line ID $moveLineId: $result")
                result
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error updating lot and quantity for move line ID $moveLineId: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun updateMoveLineLotExpiration(moveLineId: Int, pickingId: Int, productId: Int, lotName: String, quantity: Int, expirationDate: String): Boolean {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLineLotExpiration.")
            return false
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line",
            "update_move_line_lot_expiration",
            listOf(moveLineId, pickingId, productId, lotName, quantity, expirationDate)
        )

        return try {
            withContext(Dispatchers.IO) {
                val result = client.execute("execute_kw", params) as Boolean
                Log.d("OdooXmlRpcClient", "Update lot, quantity, and expiration result for move line ID $moveLineId: $result")
                result
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error updating lot, quantity, and expiration for move line ID $moveLineId: ${e.localizedMessage}", e)
            false
        }
    }


//    suspend fun createMoveLineForReceiving(pickingId: Int, productId: Int, lotName: String, quantity: Double): Map<String, Any>? {
//        val config = getClientConfig("object")
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        val methodName = "create_move_line_for_receiving"
//        val model = "stock.move.line"
//        val args = listOf(pickingId, productId, lotName, quantity)  // Ensure this is a list
//
//        val params = listOf(
//            Constants.DATABASE,
//            userId,
//            password,
//            model,
//            methodName,
//            args
//        )
//
//        return try {
//            val result = withContext(Dispatchers.IO) {
//                client.execute("execute_kw", params) as Map<*, *>
//            }
//            result as Map<String, Any>
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error creating move line for receiving: ${e.localizedMessage}", e)
//            null
//        }
//    }
suspend fun createMoveLineForReceiving(pickingId: Int, productId: Int, lotName: String, quantity: Double): Map<String, Any>? {
    val config = getClientConfig("object")
    val client = XmlRpcClient().also { it.setConfig(config) }
    val userId = credentialManager.getUserId()
    val password = credentialManager.getPassword() ?: ""

    val methodName = "create_move_line_for_receiving"
    val model = "stock.move.line"
    val args = listOf(pickingId, productId, lotName, quantity)

    val params = listOf(
        Constants.DATABASE,
        userId,
        password,
        model,
        methodName,
        args
    )

    return try {
        val result = withContext(Dispatchers.IO) {
            client.execute("execute_kw", params) as? Map<String, Any>
        }
        result?.also {
            if (it["success"] == true) {
                Log.d("OdooXmlRpcClient", "Move line created successfully with line_id: ${it["line_id"]}")
            } else {
                Log.e("OdooXmlRpcClient", "Failed to create move line: ${it["error"] ?: "Unknown error"}")
            }
        }
    } catch (e: Exception) {
        Log.e("OdooXmlRpcClient", "Error creating move line for receiving: ${e.localizedMessage}", e)
        null
    }
}




//    suspend fun createMoveLineWithExpirationForReceiving(pickingId: Int, productId: Int, lotName: String, quantity: Double, expirationDate: String): Map<String, Any>? {
//        val config = getClientConfig("object")
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = credentialManager.getUserId()
//        val password = credentialManager.getPassword() ?: ""
//
//        val methodName = "create_move_line_for_receiving_with_expiration"
//        val model = "stock.move.line"
//        val args = listOf(pickingId, productId, lotName, quantity, expirationDate)  // Ensure this is a list
//
//        val params = listOf(
//            Constants.DATABASE,
//            userId,
//            password,
//            model,
//            methodName,
//            args
//        )
//
//        return try {
//            val result = withContext(Dispatchers.IO) {
//                client.execute("execute_kw", params) as Map<*, *>
//            }
//            result as Map<String, Any>
//        } catch (e: Exception) {
//            Log.e("OdooXmlRpcClient", "Error creating move line for receiving: ${e.localizedMessage}", e)
//            null
//        }
//    }
suspend fun createMoveLineWithExpirationForReceiving(pickingId: Int, productId: Int, lotName: String, quantity: Double, expirationDate: String): Map<String, Any>? {
    val config = getClientConfig("object")
    val client = XmlRpcClient().also { it.setConfig(config) }
    val userId = credentialManager.getUserId()
    val password = credentialManager.getPassword() ?: ""

    val methodName = "create_move_line_for_receiving_with_expiration"
    val model = "stock.move.line"
    val args = listOf(pickingId, productId, lotName, quantity, expirationDate)  // Ensure this is correctly passed as a list

    val params = listOf(
        Constants.DATABASE,
        userId,
        password,
        model,
        methodName,
        args
    )

    Log.d("OdooXmlRpcClient", "Attempting to create move line with expiration: PickingID: $pickingId, ProductID: $productId, LotName: $lotName, Quantity: $quantity, ExpirationDate: $expirationDate")

    return try {
        val result = withContext(Dispatchers.IO) {
            client.execute("execute_kw", params) as? Map<String, Any>
        }
        result?.also {
            if (it["success"] == true) {
                Log.d("OdooXmlRpcClient", "Move line created successfully with line_id: ${it["line_id"]}")
            } else {
                Log.e("OdooXmlRpcClient", "Failed to create move line: ${it["error"] ?: "Unknown error"}")
            }
        }
    } catch (e: Exception) {
        Log.e("OdooXmlRpcClient", "Error creating move line for receiving: ${e.localizedMessage}. Params: $params", e)
        null
    }
}



}












