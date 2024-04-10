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
import java.net.URL



class OdooXmlRpcClient(val credentialManager: CredentialManager) {

    fun getClientConfig(endpoint: String): XmlRpcClientConfigImpl? {
        return try {
            val fullUrl = "${Constants.URL}/xmlrpc/2/$endpoint"
            Log.d("OdooXmlRpcClient", "Connecting to: $fullUrl")
            XmlRpcClientConfigImpl().apply {
                serverURL = URL(fullUrl)
            }.also {
                Log.d("OdooXmlRpcClient", "Config set with URL: ${it.serverURL}")
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error setting up client config: ${e.localizedMessage}")
            null
        }
    }

    suspend fun login(username: String, password: String): Int {
        return try {
            val config = getClientConfig("common")
            if (config == null) {
                Log.e("OdooXmlRpcClient", "Client configuration is null, aborting login.")
                return -1
            }
            val client = XmlRpcClient().also { it.setConfig(config) }
            val params = listOf(Constants.DATABASE, username, password, emptyMap<String, Any>())
            val result = client.execute("authenticate", params) as Int
            if (result > 0) credentialManager.storeUserCredentials(username, password, result)
            result
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error during login: ${e.localizedMessage}")
            -1
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
        val fields = listOf("id", "name", "date", "user_id", "state","origin") // Include 'state' if you want to verify it in the result

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
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchProductsForReceipt.")
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
            val productsSummary = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["product_summary"].toString() else ""
            parseProductSummary(productsSummary)
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching products for receipt: ${e.localizedMessage}", e)
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
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchProductTrackingAndExpirationByName.")
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
            Log.e("OdooXmlRpcClient", "Error fetching product tracking and expiration date: ${e.localizedMessage}", e)
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
            Log.e("OdooXmlRpcClient", "Error fetching serial numbers for product ID $productId: ${e.localizedMessage}", e)
            return null
        }
    }


    suspend fun fetchInternalTransfersWithProductDetails(): List<InternalTransfers> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchInternalTransfers.")
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
                    Log.d("OdooXmlRpcClient", "Fetched ${it.size} internal transfers with product details.")
                }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching internal transfers: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    private suspend fun fetchProductsForInternalTransfer(transferId: Int): List<Product> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchProductsForInternalTransfer.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        // Adjust the domain filter to specifically fetch the desired internal transfer by ID.
        // The following assumes internal transfers are distinguished within your setup and
        // the `internal_transfer_summary` field exists and is computed for these records.
        val domain = listOf(listOf("id", "=", transferId))
        val fields = listOf("internal_transfer_summary")  // Use the custom computed field for internal transfers

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
            val internalTransferSummary = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["internal_transfer_summary"].toString() else ""
            parseInternalTransferSummary(internalTransferSummary)  // You may need to implement or adjust this method
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching products for internal transfer: ${e.localizedMessage}", e)
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
                    Product(id = productId, name = productName, quantity = productQuantity)  // Including ID in the constructor
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
            val result = client.execute("execute_kw", params) as? Array<Map<String, Any>> ?: arrayOf()
            if (result.isNotEmpty()) result[0]["id"] as? Int else null
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching transfer ID for reference $transferReference: ${e.localizedMessage}", e)
            null
        }
    }

    suspend fun fetchAndLogBuyerDetails(pickingName: String): BuyerDetails? {
        Log.d("OdooXmlRpcClient", "Starting fetchAndLogBuyerDetails for pickingName: $pickingName")
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchBuyerDetailsForReceipt.")
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
            Log.e("OdooXmlRpcClient", "Error fetching buyer details for receipt: ${e.localizedMessage}", e)
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
                Log.d("OdooXmlRpcClient", "User details found: ${userDetails["login"]},${userDetails["name"]} ")

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


    suspend fun updateMoveLinesByPicking(pickingId: Int, productId: Int, serialNumber: String, expirationDate: String?) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLinesByPicking.")
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
            listOf(pickingId, productId, serialNumber, formattedExpirationDate) // Parameters for your custom method
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d("OdooXmlRpcClient", "Successfully updated/created stock.move.line record.")
            } else {
                Log.e("OdooXmlRpcClient", "Failed to update/create stock.move.line record.")
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error executing updateMoveLinesByPicking: ${e.localizedMessage}")
        }
    }

    suspend fun updateMoveLinesWithoutExpiration(pickingId: Int, productId: Int, serialNumber: String) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLinesWithoutExpiration.")
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
                Log.d("OdooXmlRpcClient", "Successfully updated/created stock.move.line record without expiration date.")
            } else {
                Log.e("OdooXmlRpcClient", "Failed to update/create stock.move.line record without expiration date.")
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error executing updateMoveLinesWithoutExpiration: ${e.localizedMessage}")
        }
    }
//    create_update_move_line_for_pick
    suspend fun updateMoveLinesForPick(pickingId: Int, productId: Int, serialNumber: String, location: String) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLinesWithoutExpiration.")
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
            "create_update_move_line_for_pick", // Use the adjusted method name that excludes expiration date
            listOf(pickingId, productId, serialNumber, location) // Parameters excluding the expiration date
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d("OdooXmlRpcClient", "Successfully updated/created stock.move.line record without expiration date.")
            } else {
                Log.e("OdooXmlRpcClient", "Failed to update/create stock.move.line record without expiration date.")
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error executing updateMoveLinesWithoutExpiration: ${e.localizedMessage}")
        }
    }
    suspend fun updateMoveLinesByPickingWithLot(pickingId: Int, productId: Int, lotName: String, quantity: Int, expirationDate: String?) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLinesByPickingWithLot.")
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
                Log.d("OdooXmlRpcClient", "Successfully updated/created stock.move.line record with lot.")
            } else {
                Log.e("OdooXmlRpcClient", "Failed to update/create stock.move.line record with lot.")
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error executing updateMoveLinesByPickingWithLot: ${e.localizedMessage}")
        }
    }

    suspend fun updateMoveLinesWithoutExpirationWithLot(pickingId: Int, productId: Int, lotName: String, quantity: Int, location: String) {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting updateMoveLinesWithoutExpirationWithLot.")
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
            "create_update_move_line_lot_without_expiration", // Adjusted method name for lots without expiration
            listOf(pickingId, productId, lotName, quantity, location)
        )

        try {
            val result = client.execute("execute_kw", params) as? Boolean
            if (result == true) {
                Log.d("OdooXmlRpcClient", "Successfully updated/created stock.move.line record without expiration date for lot.")
            } else {
                Log.e("OdooXmlRpcClient", "Failed to update/create stock.move.line record without expiration date for lot.")
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error executing updateMoveLinesWithoutExpirationWithLot: ${e.localizedMessage}")
        }
    }

    suspend fun validateOperation(pickingId: Int): Boolean {
        return try {
            val username = credentialManager.getUsername()
            val password = credentialManager.getPassword()

            if (username == null || password == null) {
                Log.e("OdooXmlRpcClient", "Credentials are null, aborting changePickState.")
                return false
            }

            val db = Constants.DATABASE
            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${Constants.URL}xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            val userId = login(username, password)
            if (userId <= 0) {
                Log.e("OdooXmlRpcClient", "Login failed, cannot change pick state.")
                return false
            }

            val validateParams = listOf(db, userId, password, "stock.picking", "button_validate", listOf(listOf(pickingId)))

            client.execute("execute_kw", validateParams).let {
                Log.d("OdooXmlRpcClient", "Picking validated successfully. ${pickingId}")
                return true
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error during changePickState: ${e.message}", e)
            false
        }
    }

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
                    mapOf("fields" to moveLineFields, "limit" to 1) // Assuming one move line per pick for simplification
                )

                val moveLineResult = client.execute("execute_kw", moveLineParams) as Array<Any>
                val moveLine = moveLineResult.mapNotNull { it as? Map<String, Any> }.firstOrNull()

                pick.locationId = (moveLine?.get("location_id") as? Array<Any>)?.get(1)?.toString()
                pick.locationDestId = (moveLine?.get("location_dest_id") as? Array<Any>)?.get(1)?.toString()
            }

            // Log the picks with location names
            picks.forEach { pick ->
                Log.d(
                    "OdooXmlRpcClient",
                    "Pick ID: ${pick.id}, Name: ${pick.name}, Location: ${pick.locationId}, Destination Location: ${pick.locationDestId}"
                )
            }

            picks
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching picks: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    suspend fun fetchProductsForDeliveryOrder(deliveryOrderId: Int): List<Product> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchProductsForDeliveryOrder.")
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
            val deliveryOrderSummary = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["delivery_order_product_summary"].toString() else ""
            parseDelivProductSummary(deliveryOrderSummary)
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching products for delivery order: ${e.localizedMessage}", e)
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
        Log.e("OdooXmlRpcClient", "Error fetching product ID and quantity from packaging barcode: ${e.localizedMessage}")
        null
    }
}


    suspend fun createStockMoveLineForUntrackedProduct(pickingId: Int, productId: Int, quantity: Double, locationName: String): Boolean {
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
            Log.e("OdooXmlRpcClient", "Error creating stock move line for untracked product: ${e.message}", e)
            false
        }
    }


}