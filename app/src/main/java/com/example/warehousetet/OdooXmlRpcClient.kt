package com.example.warehousetet


import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.xmlrpc.XmlRpcException
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.MalformedURLException
import java.net.URL



class OdooXmlRpcClient(val credentialManager: CredentialManager) {

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

    suspend fun fetchIntTransfers(): List<InternalTransfers> {
        val config = getClientConfig("object")
        if (config == null) {
            Log.e("OdooXmlRpcClient", "Client configuration is null, aborting fetchIntTransfers.")
            return emptyList()
        }
        val client = XmlRpcClient().also { it.setConfig(config) }
        val userId = credentialManager.getUserId()
        val password = credentialManager.getPassword() ?: ""

        val domain = listOf(
            listOf("picking_type_id.code", "=", "internal"),
//            listOf("state", "in", listOf("assigned", "waiting")),
            listOf("state", "in", listOf("assigned")),
            "|",
            listOf("user_id", "=", false),
            listOf("user_id", "=", userId),
            listOf("name", "ilike", "INT")
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

            val transfersResult = client.execute("execute_kw", params) as Array<Any>
            val transfers = transfersResult.mapNotNull { it as? Map<String, Any> }.mapNotNull { map ->
                InternalTransfers(
                    id = map["id"] as Int,
                    name = map["name"] as String,
                    date = map["date"] as String,
                    origin = map["origin"].toString(),
                    locationId = null, // Set these to null initially
                    locationDestId = null
                )
            }

            // For each transfer, fetch the locations from stock.move.line model
            transfers.forEach { transfer ->
                val moveLineDomain = listOf(
                    listOf("picking_id", "=", transfer.id)
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
                    ) // Assuming one move line per transfer for simplification
                )

                val moveLineResult = client.execute("execute_kw", moveLineParams) as Array<Any>
                val moveLine = moveLineResult.mapNotNull { it as? Map<String, Any> }.firstOrNull()

                transfer.locationId = (moveLine?.get("location_id") as? Array<Any>)?.get(1)?.toString()
                transfer.locationDestId =
                    (moveLine?.get("location_dest_id") as? Array<Any>)?.get(1)?.toString()
            }
            transfers
        } catch (e: Exception) {
            Log.e("OdooXmlRpcClient", "Error fetching internal transfers: ${e.localizedMessage}", e)
            emptyList()
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
    suspend fun fetchMoveLinesByOperationId(pickingId: Int): List<MoveLineOutGoing> {
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
            val moveLines = parseMoveLinesOutgoingSummary(moveLinesSummary)

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
    private fun parseMoveLinesOutgoingSummary(summary: String): List<MoveLineOutGoing> {
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


                    MoveLineOutGoing(
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
    suspend fun validateOperationDO(packingId: Int, context: Context): Boolean {
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

}












