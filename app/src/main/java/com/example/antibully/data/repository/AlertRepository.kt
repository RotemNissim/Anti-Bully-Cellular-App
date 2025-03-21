import com.example.antibully.data.api.MessageApiService
import com.example.antibully.data.api.ApiHelper
import com.example.antibully.data.models.Alert
import com.example.antibully.data.db.dao.AlertDao
import kotlinx.coroutines.flow.Flow

class AlertRepository(private val alertDao: AlertDao, private val apiService: MessageApiService) {

    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()

    suspend fun fetchAlertsFromApi() {
        val response = ApiHelper.safeApiCall { apiService.getAllFlaggedMessages() }
        response.onSuccess { apiAlerts ->
            val alerts = apiAlerts.map { apiMessage ->
                Alert(
                    postId = apiMessage.messageId,
                    reporterId = apiMessage.userId,
                    reason = apiMessage.reason ?: "No reason",
                    timestamp = System.currentTimeMillis()
                )
            }
            alertDao.insertAll(alerts) // Store in ROOM
        }
        response.onFailure { error ->
            println("API Error: ${error.message}") // Handle failure
        }
    }

    suspend fun insert(alert: Alert) {
        alertDao.insertAlert(alert)
    }

    suspend fun delete(alert: Alert) {
        alertDao.deleteAlert(alert)
    }
}
