import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.antibully.data.ui.adapters.PostAdapter
import com.example.antibully.viewmodel.PostViewModel
import com.example.antibully.viewmodel.PostViewModelFactory


class FeedFragment : Fragment() {

    private lateinit var viewModel: AlertViewModel
    private lateinit var postViewModel: PostViewModel
    private lateinit var alertAdapter: AlertAdapter
    private lateinit var postAdapter: PostAdapter
    private var selectedAlertId: String? = null // Stores the clicked alert ID

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, AlertViewModelFactory())[AlertViewModel::class.java]
        postViewModel = ViewModelProvider(this, PostViewModelFactory())[PostViewModel::class.java]

        alertAdapter = AlertAdapter { alertId ->
            toggleComments(alertId) // When clicking an alert, show its comments
        }

        postAdapter = PostAdapter()

        val alertsRecyclerView = view.findViewById<RecyclerView>(R.id.alertsRecyclerView)
        alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        alertsRecyclerView.adapter = alertAdapter

        val commentsRecyclerView = view.findViewById<RecyclerView>(R.id.commentsRecyclerView)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentsRecyclerView.adapter = postAdapter

        lifecycleScope.launch {
            viewModel.allAlerts.collectLatest { alerts ->
                alertAdapter.submitList(alerts)
            }
        }

        // Fetch alerts
        viewModel.fetchAlerts()
    }

    private fun toggleComments(alertId: String) {
        if (selectedAlertId == alertId) {
            // If clicking the same alert, close comments
            selectedAlertId = null
            postAdapter.submitList(emptyList()) // Hide comments
        } else {
            selectedAlertId = alertId

            // Fetch and display posts for the selected alert
            postViewModel.getPostsForAlert(alertId).observe(viewLifecycleOwner) { posts ->
                postAdapter.submitList(posts)
            }
        }
    }
}




