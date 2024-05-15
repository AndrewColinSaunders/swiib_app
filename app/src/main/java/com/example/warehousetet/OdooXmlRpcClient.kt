package com.example.warehousetet

import android.content.Context

import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.xmlrpc.XmlRpcException
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.URL



class OdooXmlRpcClient(val credentialManager: CredentialManager) {

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

    fun login(username: String, password: String): Int {
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

    fun fetchReceipts(): List<Receipt> {
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
            result.mapNotNull { it as? Map<*, *> }.mapNotNull { map ->
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
    suspend fun updateMoveLinesForPick(pickingId: Int, productId: Int, serialNumber: String) {
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

    suspend fun updateMoveLinesWithoutExpirationWithLot(pickingId: Int, productId: Int, lotName: String, quantity: Int) {
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
            listOf(pickingId, productId, lotName, quantity)
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

    suspend fun fetchDeliveryOrders(): List<DeliveryOrders> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchDeliveryOrders.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(
            listOf("picking_type_id.code", "=", "outgoing"), // Adjusted for delivery operations
            listOf("state", "in", listOf("assigned")),
            "|",
            listOf("user_id", "=", false),
            listOf("user_id", "=", userId),
            listOf("name", "ilike", "OUT") // Adjusted to delivery operation
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
            "stock.picking", // Model remains the same
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val ordersResult = client.execute("execute_kw", params) as Array<Any>
            val orders = ordersResult.mapNotNull { it as? Map<String, Any> }.mapNotNull { map ->
                DeliveryOrders(
                    id = map["id"] as Int,
                    name = map["name"] as String,
                    date = map["date"] as String,
                    origin = map["origin"].toString(),
                    locationId = null, // Set these to null initially
                    locationDestId = null
                )
            }

            // For each order, fetch the locations from stock.move.line model
            orders.forEach { order ->
                val moveLineDomain = listOf(
                    listOf("picking_id", "=", order.id)
                )
                val moveLineFields = listOf("location_id", "location_dest_id")
                val moveLineParams = listOf(
                    Constants.DATABASE,
                    userId,
                    password,
                    "stock.move.line",
                    "search_read",
                    listOf(moveLineDomain),
                    mapOf("fields" to moveLineFields, "limit" to 1) // Assuming one move line per order for simplification
                )

                val moveLineResult = client.execute("execute_kw", moveLineParams) as Array<Any>
                val moveLine = moveLineResult.mapNotNull { it as? Map<String, Any> }.firstOrNull()

                order.locationId = (moveLine?.get("location_id") as? Array<Any>)?.get(1)?.toString()
                order.locationDestId = (moveLine?.get("location_dest_id") as? Array<Any>)?.get(1)?.toString()
            }

            // Log the orders with location names
            orders.forEach { order ->
                Log.d(
                    "OdooXmlRpcClient",
                    "Order ID: ${order.id}, Name: ${order.name}, Location: ${order.locationId}, Destination Location: ${order.locationDestId}"
                )
            }

            orders
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching delivery orders: ${e.localizedMessage}", e)
            emptyList()
        }
    }


    suspend fun fetchPacks(): List<Pack> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchPacks.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(
            listOf("picking_type_id.code", "=", "internal"), // Adjusted to packing operation
            listOf("state", "in", listOf("assigned")),
            "|",
            listOf("user_id", "=", false),
            listOf("user_id", "=", userId),
            listOf("name", "ilike", "PACK") // Adjusted to packing operation
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
            "stock.picking", // Keep as is
            "search_read",
            listOf(domain),
            mapOf("fields" to fields)
        )

        return try {
            val packsResult = client.execute("execute_kw", params) as Array<Any>
            val packs = packsResult.mapNotNull { it as? Map<String, Any> }.mapNotNull { map ->
                Pack(
                    id = map["id"] as Int,
                    name = map["name"] as String,
                    date = map["date"] as String,
                    origin = map["origin"].toString(),
                    locationId = null, // Set these to null initially
                    locationDestId = null
                )
            }

            // For each pack, fetch the locations from stock.move.line model
            packs.forEach { pack ->
                val moveLineDomain = listOf(
                    listOf("picking_id", "=", pack.id) // Adjusted to picking operation
                )
                val moveLineFields = listOf("location_id", "location_dest_id")
                val moveLineParams = listOf(
                    Constants.DATABASE,
                    userId,
                    password,
                    "stock.move.line",
                    "search_read",
                    listOf(moveLineDomain),
                    mapOf("fields" to moveLineFields, "limit" to 1) // Assuming one move line per pack for simplification
                )

                val moveLineResult = client.execute("execute_kw", moveLineParams) as Array<Any>
                val moveLine = moveLineResult.mapNotNull { it as? Map<String, Any> }.firstOrNull()

                pack.locationId = (moveLine?.get("location_id") as? Array<Any>)?.get(1)?.toString()
                pack.locationDestId = (moveLine?.get("location_dest_id") as? Array<Any>)?.get(1)?.toString()
            }

            // Log the packs with location names
            packs.forEach { pack ->
                Log.d(
                    "OdooXmlRpcClient",
                    "Pack ID: ${pack.id}, Name: ${pack.name}, Location: ${pack.locationId}, Destination Location: ${pack.locationDestId}"
                )
            }

            packs
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching packs: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    suspend fun fetchMoveLinesByOperationId(pickingId: Int): List<MoveLineOutgoing> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchMoveLinesByPickingId.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(listOf("id", "=", pickingId))
        val fields = listOf("detailed_move_lines")  // Assuming this field contains the move lines summary

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
            val moveLinesSummary = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["detailed_move_lines"].toString() else ""
            val moveLines = parseMoveLinesSummary(moveLinesSummary)

            // Log each MoveLine
            moveLines.forEach { moveLine ->
                Log.d("MoveLineData", "ID: ${moveLine.lineId}, Product ID: ${moveLine.productId}, " +
                        "Product Name: ${moveLine.productName}, Lot ID: ${moveLine.lotId}, " +
                        "Lot Name: ${moveLine.lotName}, Quantity: ${moveLine.quantity}, " +
                        "LocationId: ${moveLine.locationDestId}, Location Name:${moveLine.locationDestName}")
            }
            Log.d("OdooXmlRpcClient", "Raw server response: ${result.toList()}")


            moveLines
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching move lines for picking ID: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    private fun parseMoveLinesSummary(summary: String): List<MoveLineOutgoing> {
        return summary.split(", ").mapNotNull { lineString ->
            val parts = lineString.split(":")
            if (parts.size >= 6) {  // Make sure there are at least 6 parts
                try {
                    val productId = parts[0].toInt()
                    val productName = parts[1]
                    val lotId = parts[2].takeIf { it.isNotEmpty() && it != "None" }?.toIntOrNull()
                    val lotName = parts[3].takeIf { it != "None" } ?: ""
                    val quantity = parts[4].toDouble()
                    val lineId = parts[5].toInt()
                    val locationDestId = parts.getOrNull(6)?.toIntOrNull() ?: -1  // Optional: Default to -1 if not present
                    val locationDestName = parts.getOrNull(7) ?: "Unknown" // Optional: Default to "Unknown" if not present
                    val resultPackageId = parts[8].takeIf { it.isNotEmpty() && it != "None" }?.toIntOrNull()
                    val resultPackageName = parts[9]


                    MoveLineOutgoing(
                        lineId = lineId,
                        productId = productId,
                        productName = productName,
                        lotId = lotId,
                        lotName = lotName,
                        quantity = quantity,
                        locationDestId = locationDestId,
                        locationDestName = locationDestName,
                        resultPackageId = resultPackageId,
                        resultPackageName = resultPackageName

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


    suspend fun putMoveLineInNewPack(moveLineId: Int, context: Context): Boolean {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting putMoveLineInPack.")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Operation aborted: Client configuration is missing.", Toast.LENGTH_LONG).show()
            }
            return false
        }

        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        if (userId == null || password.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Operation aborted: User credentials are missing.", Toast.LENGTH_LONG).show()
            }
            return false
        }

        val params = listOf(
            Constants.DATABASE,
            userId,
            password,
            "stock.move.line",
            "put_in_pack",
            listOf(moveLineId)
        )

        return try {
            val result = withContext(Dispatchers.IO) {
                client.execute("execute_kw", params)
            } as? Map<*, *>

            withContext(Dispatchers.Main) {
                if (result?.get("success") == true) {
                    Log.d("OdooXmlRpcClient", "Successfully executed put_in_pack for move line ID: $moveLineId, package created: ${result["package_name"]}")
                    Toast.makeText(context, "Package created: ${result["package_name"]}", Toast.LENGTH_LONG).show()
                    true
                } else {
                    Log.e("OdooXmlRpcClient", "Failed to execute put_in_pack for move line ID: $moveLineId, result was unexpected: $result")
                    Toast.makeText(context, "Failed to put move line in pack. Please try again.", Toast.LENGTH_LONG).show()
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error executing putMoveLineInPack: ${e.localizedMessage}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Unexpected error occurred: ${e.localizedMessage}.", Toast.LENGTH_LONG).show()
            }
            false
        }
    }




    suspend fun setPackageForMoveLine(pickingId: Int, moveLineId: Int, packageName: String): Boolean {
        return try {
            val config = getClientConfig("object")
            if (config == null) {
                Log.e("OdooXmlRpcClient", "XML RPC Client configuration is null, aborting setPackageForMoveLine.")
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
                "set_package_for_move_line",
                listOf(pickingId, moveLineId, packageName)
            )

            val result = client.execute("execute_kw", params) as? HashMap<*, *>
            if (result?.get("success") as? Boolean == true) {
                Log.d("OdooXmlRpcClient", "Package set successfully for move line: $result")
                true
            } else {
                Log.e("OdooXmlRpcClient", "Failed to set package for move line: ${result?.get("error")}")
                false
            }
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error setting package for move line: ${e.message}", e)
            false
        }
    }






    suspend fun validateOperation(packingId: Int, context: Context): Boolean {
        return withContext(Dispatchers.IO) { // Use IO dispatcher for network operations
            try {
                val username = credentialManager.getUsername()
                val password = credentialManager.getPassword()

                if (username == null || password == null) {
                    Log.e("OdooXmlRpcClient", "Credentials are null, aborting validation.")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Validation aborted: User credentials are missing.", Toast.LENGTH_LONG).show()
                    }
                    return@withContext false
                }

                val db = Constants.DATABASE
                val config = XmlRpcClientConfigImpl().apply {
                    serverURL = URL("${Constants.URL}xmlrpc/2/object")
                }

                val client = XmlRpcClient()
                client.setConfig(config)

                val userId = login(username, password)
                Log.d("OdooXmlRpcClient", "User ID before trying to validate: $userId")
                if (userId <= 0) {
                    Log.e("OdooXmlRpcClient", "Login failed, cannot execute validation.")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Login failed: Cannot authenticate user.", Toast.LENGTH_LONG).show()
                    }
                    return@withContext false
                }

                val validateParams = listOf(
                    db, userId, password, "stock.picking", "button_validate", listOf(listOf(packingId))
                )

                client.execute("execute_kw", validateParams).let {
                    Log.d("OdooXmlRpcClient", "Picking validated successfully.")
                    return@withContext true
                }
            } catch (e: XmlRpcException) {
                Log.e("OdooXmlRpcClient", "XML-RPC error during validation: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error during validation: ${e.localizedMessage}.", Toast.LENGTH_LONG).show()
                }
                false
            } catch (e: Exception) {
                Log.e("OdooXmlRpcClient", "General error during validation: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Unexpected error occurred: ${e.localizedMessage}.", Toast.LENGTH_LONG).show()
                }
                false
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

        return try {
            val result = client.execute("execute_kw", params) as Array<Any>
            val resultPackages = if (result.isNotEmpty()) (result[0] as Map<String, Any>)["result_packages"].toString() else ""
            parseResultPackages(resultPackages)
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

    suspend fun updateMoveLineQuantityForReceipt(lineId: Int, pickingId: Int, quantity: Int){
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
}