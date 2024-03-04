import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IntTransferProducts(
    val name: String,
    val quantity: Double,
    val transferDate: String
) : Parcelable