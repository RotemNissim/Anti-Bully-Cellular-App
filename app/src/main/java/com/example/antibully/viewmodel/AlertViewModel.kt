import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antibully.data.models.Alert
import kotlinx.coroutines.launch

class AlertViewModel(private val repository: AlertRepository) : ViewModel() {

    val allAlerts = repository.allAlerts

    fun fetchAlerts() = viewModelScope.launch {
        repository.fetchAlertsFromApi()
    }

    fun insert(alert: Alert) = viewModelScope.launch {
        repository.insert(alert)
    }

    fun delete(alert: Alert) = viewModelScope.launch {
        repository.delete(alert)
    }
}
