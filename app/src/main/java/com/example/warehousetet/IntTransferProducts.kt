import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IntTransferProducts(
    val name: String,
    val quantity: Double,
    val transferDate: String,
    val barcode: String? = null,
    var isScanned: Boolean = false, // Indicates if the product has been successfully scanned
    var sourceDocument: String
) : Parcelable