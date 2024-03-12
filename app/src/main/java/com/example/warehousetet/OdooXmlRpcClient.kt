package com.example.warehousetet

import IntTransferProducts
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URL
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl


class OdooXmlRpcClient(private val credentialManager: CredentialManager) {

    private fun getClientConfig(endpoint: String): XmlRpcClientConfigImpl? {
        return try {
            val fullUrl = "${Constants.URL}xmlrpc/2/$endpoint"
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
            if (result > 0) {
                credentialManager.storeUserCredentials(username, password, result)
                // Assuming the session ID is part of the result or a separate call is made to obtain it
                // Here you would retrieve the session ID and store it
                val sessionId = "RETRIEVED_SESSION_ID" // Replace with actual retrieval logic
                credentialManager.storeSessionId(sessionId)
            }
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
        val fields = listOf("id", "name", "date", "user_id", "state") // Include 'state' if you want to verify it in the result

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

    suspend fun fetchProductTrackingByName(productName: String): String? {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchProductTrackingByName.")
            return null
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId() // Use CredentialManager to get the user ID
        val password = credentialManager.getPassword() ?: "" // Use CredentialManager to get the password
        val database = Constants.DATABASE // Use Constants to get the database name

        // Define the search domain to find the product by its name
        val domain = listOf(listOf("name", "=", productName))
        // Specify the fields to fetch; we're only interested in the 'tracking' field
        val fields = listOf("tracking")

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
                productData["tracking"] as? String
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching product tracking: ${e.localizedMessage}", e)
            null
        }
    }

    suspend fun fetchSerialNumbersByProductName(productName: String): List<String>? {
        val config = getClientConfig("object") ?: return null
        val client = XmlRpcClient().also { it.setConfig(config) }

        // Assuming 'product_id' is linked by a field 'name' in 'product.product' model
        // and serial numbers are stored in 'stock.lot' model under the field 'name'.
        val productDomain = listOf(listOf("name", "=", productName))
        val productFields = listOf("id") // We only need the product ID to link to its serial numbers

        val productParams = listOf(
            Constants.DATABASE,
            credentialManager.getUserId(),
            credentialManager.getPassword() ?: "",
            "product.product",
            "search_read",
            listOf(productDomain),
            mapOf("fields" to productFields)
        )

        try {
            val productResults = client.execute("execute_kw", productParams) as? Array<Any>
            val productId = (productResults?.firstOrNull() as? Map<*, *>)?.get("id") as? Int ?: return null

            // Now fetch serial numbers linked to this product ID in 'stock.lot' model
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

            val serialResults = client.execute("execute_kw", serialParams) as? Array<Any>
            return serialResults?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching serial numbers: ${e.localizedMessage}", e)
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
            result.mapNotNull { it as? Map<String, Any> }.mapNotNull { map ->
                val transferId = map["id"] as Int
                val transferName = map["name"] as String
                val transferDate = map["scheduled_date"].toString()
                val sourceDocument = map["origin"] as? String ?: ""

                val products = fetchProductsForInternalTransfer(transferId)

                // Adjust this part to include barcode fetching
                val intTransferProductsList = products.mapNotNull { product ->
                    // Assuming fetchProductBarcodeByName exists and fetches the barcode based on product name
                    val barcode = fetchProductBarcodeByName(product.name)
                    IntTransferProducts(
                        name = product.name,
                        quantity = product.quantity,
                        transferDate = transferDate,
                        sourceDocument = sourceDocument,
                        barcode = barcode // Include the fetched barcode
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


    suspend fun fetchProductsForInternalTransfer(transferId: Int): List<Product> {
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

    fun parseInternalTransferSummary(summary: String): List<Product> {
        // Check if the summary is not empty
        if (summary.isEmpty()) return emptyList()

        // Initialize a counter for product IDs
        var productIdCounter = 1

        return summary.split(", ").mapNotNull { productString ->
            val parts = productString.split(": ")
            if (parts.size == 2) {
                val productName = parts[0]
                val productQuantity = parts[1].toDoubleOrNull()  // Convert quantity to Double
                if (productQuantity != null) {
                    // Use a sequential ID and increment the counter for each product
                    val product = Product(
                        id = productIdCounter++,
                        name = productName,
                        quantity = productQuantity,
                        barcode = null,  // Assuming barcode is not available
                        trackingType = null  // Assuming trackingType is not available
                    )
                    product
                } else {
                    null  // If conversion fails, exclude this product from the list
                }
            } else {
                null  // Exclude if the product string format is unexpected
            }
        }
    }

    // Assuming this function is part of a class that has access to a Context object

    suspend fun changePickState(context: Context, pickName: String) {
        val credentialManager = CredentialManager(context)
        var sessionId = credentialManager.getSessionId()
        if (sessionId == null) {
            logError("Session ID is null, aborting changePickState.")
            return
        }

        // Log the retrieved session ID for debugging
        Log.d("OdooXmlRpcClient", "Using Session ID: $sessionId")

        val client = OkHttpClient.Builder().build()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        if (mediaType == null) {
            logError("Failed to create MediaType, aborting changePickState.")
            return
        }

        val requestBody = """{"jsonrpc": "2.0", "params": {"pick_name": "$pickName"}}""".toRequestBody(mediaType)
        val requestUrl = "${Constants.URL}pick_state_changer/change_state"

        var request = buildRequest(requestUrl, sessionId, requestBody)
        var response = makeRequest(client, request)

        if (response?.isSuccessful == false && isSessionExpired(response)) {
            // Attempt to renew the session
            val username = credentialManager.getUsername()
            val password = credentialManager.getPassword()
            if (username != null && password != null) {
                val userId = login(username, password) // Ensure this function is adapted to return the new session ID on success
                if (userId > 0) {
                    // If login is successful, retry the original request with the new session ID
                    sessionId = credentialManager.getSessionId() ?: return logError("Failed to retrieve new session ID after login.")
                    // Log the new session ID for debugging
                    Log.d("OdooXmlRpcClient", "New Session ID: $sessionId")
                    request = buildRequest(requestUrl, sessionId, requestBody)
                    response = makeRequest(client, request)
                }
            }
        }

        logResponse(response)
    }


    private fun buildRequest(url: String, sessionId: String, requestBody: RequestBody): Request {
        return Request.Builder()
            .url(url)
            .addHeader("Cookie", "session_id=$sessionId")
            .post(requestBody)
            .build()
    }

    private suspend fun makeRequest(client: OkHttpClient, request: Request): Response? {
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                logError("Exception occurred during request: ${e.message}")
                null
            }
        }
    }

    private fun logResponse(response: Response?) {
        if (response == null) {
            logError("Response is null.")
        } else if (!response.isSuccessful) {
            logError("Request failed with HTTP status code: ${response.code}")
        } else {
            val responseBody = response.body?.string()
            Log.d("OdooXmlRpcClient", "Successfully changed pick state. Response: $responseBody")
        }
    }

    private fun logError(message: String) {
        Log.e("OdooXmlRpcClient", message)
    }

    private fun isSessionExpired(response: Response): Boolean {
        val responseBody = response.body?.string()
        return responseBody?.contains("Session expired", ignoreCase = true) ?: false
    }


}


 