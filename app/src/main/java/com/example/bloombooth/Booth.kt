import android.location.Location
import com.google.firebase.firestore.GeoPoint

data class Booth(
    val accs_cnt: Int = 0,
    val accs_condi: Int = 0,
    val booth_addr: String = "",
    val booth_cnt: Int = 0,
    val booth_contact: String = "",
    val booth_info: String = "",
    val booth_name: String = "",
    val booth_number: Int = 0,
    val booth_pic: String = "",
    val booth_loc: GeoPoint? = null,
    val retouching: Int = 0,
    var review_avg: Int = 0,
    var review_cnt: Int = 0
){
    fun getLocation(): Location? {
        return booth_loc?.let {
            Location("").apply {
                latitude = it.latitude
                longitude = it.longitude
            }
        }
    }

    var booth_id: String? = null
}