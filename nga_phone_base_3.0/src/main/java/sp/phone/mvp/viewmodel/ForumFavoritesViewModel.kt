package sp.phone.mvp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import sp.phone.http.favorites.*

class ForumFavoritesViewModel : ViewModel() {
    private val repository = ForumFavoritesRepository.getInstance()
    var snapshot by mutableStateOf(repository.current())
        private set
    private val subscription = repository.changes.subscribe {
        snapshot = if (it.accountId == sp.phone.common.UserManagerImpl.getInstance().userId.orEmpty()) it else repository.current()
    }

    fun refresh(force: Boolean = false) = repository.refresh(force)
    fun change(board: ForumFavoriteBoard, add: Boolean) = repository.change(board, add)
    override fun onCleared() { subscription.dispose() }
}
