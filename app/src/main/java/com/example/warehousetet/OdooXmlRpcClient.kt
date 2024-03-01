package com.example.warehousetet

import android.util.Log
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.URL

class OdooXmlRpcClient(private val credentialManager: CredentialManager) {

    private fun getClientConfig(endpoint: String): XmlRpcClientConfigImpl? {
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
        return summary.split(", ").mapNotNull {
            val parts = it.split(": ")
            if (parts.size == 2) {
                Product(name = parts[0], quantity = parts[1].toDoubleOrNull() ?: 0.0)
            } else null
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
        val fields = listOf("id", "name", "scheduled_date", "state")

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

                // Fetch products for this internal transfer
                val products = fetchProductsForInternalTransfer(transferId)

                // Assign the list of products directly to productDetails
                InternalTransfers(
                    id = transferId,
                    transferName = transferName,
                    transferDate = transferDate,
                    productDetails = products // Assign the List<Product> directly
                )
            }.also {
                Log.d("OdooXmlRpcClient", "Fetched ${it.size} internal transfers with product details")
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching internal transfers: ${e.localizedMessage}", e)
            emptyList()
        }
    }




    suspend fun fetchProductsForTransferReference(transferReference: String): List<Product> {
        val transferId = fetchTransferIdByReference(transferReference) ?: return emptyList()
        return fetchProductsForInternalTransfer(transferId)
    }


    suspend fun fetchProductNamesForTransfer(transferReference: String): List<String> {
        val config = getClientConfig("object") ?: return emptyList()
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        // Step 1: Find the specific stock.picking record by its reference
        val pickingDomain = listOf(listOf("name", "=", transferReference))
        val pickingFields = listOf("id")
        val pickingParams = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.picking",
            "search_read",
            listOf(pickingDomain),
            mapOf("fields" to pickingFields)
        )

        val pickingIds = try {
            val result = client.execute("execute_kw", pickingParams) as Array<Any>
            result.mapNotNull { it as? Map<String, Any> }.map { it["id"] as Int }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching stock.picking for reference $transferReference: ${e.localizedMessage}", e)
            return emptyList()
        }

        if (pickingIds.isEmpty()) return emptyList()

        // Step 2: Fetch associated stock.move records
        val moveDomain = listOf(listOf("picking_id", "in", pickingIds))
        val moveFields = listOf("product_id")
        val moveParams = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move",
            "search_read",
            listOf(moveDomain),
            mapOf("fields" to moveFields)
        )

        val productIds = try {
            val result = client.execute("execute_kw", moveParams) as Array<Any>
            result.mapNotNull { it as? Map<String, Any> }.flatMap { map ->
                (map["product_id"] as? Array<*>)?.toList()?.filterIsInstance<Int>() ?: emptyList()
            }.distinct()
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching stock.move for picking IDs $pickingIds: ${e.localizedMessage}", e)
            return emptyList()
        }

        // Step 3: Retrieve product names
        val productParams = listOf(
            Constants.DATABASE,
            userId,
            password,
            "product.product",
            "read",
            listOf(productIds),
            listOf("name")
        )

        return try {
            val result = client.execute("execute_kw", productParams) as Array<Any>
            result.mapNotNull { it as? Map<String, Any> }.mapNotNull { it["name"] as? String }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching product names for product IDs $productIds: ${e.localizedMessage}", e)
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

        return summary.split(", ").mapNotNull { productString ->
            val parts = productString.split(": ")
            if (parts.size == 2) {
                val productName = parts[0]
                val productQuantity = parts[1].toDoubleOrNull()  // Convert quantity to Double
                if (productQuantity != null) {
                    Product(name = productName, quantity = productQuantity)
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



}


 