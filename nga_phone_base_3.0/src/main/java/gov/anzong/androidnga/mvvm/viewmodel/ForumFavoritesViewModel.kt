package gov.anzong.androidnga.mvvm.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import gov.anzong.androidnga.network.favorites.*

class ForumFavoritesViewModel : ViewModel() {
    private val repository = ForumFavoritesRepository.getInstance()
    var snapshot by mutableStateOf(repository.current())
        private set

    init {
        // 随 ViewModel 清除自动取消。
        viewModelScope.launch {
            repository.changes.collect {
                snapshot = if (it.accountId == gov.anzong.androidnga.account.UserManagerImpl.getInstance().userId.orEmpty()) it else repository.current()
            }
        }
    }

    fun refresh(force: Boolean = false) = repository.refresh(force)
    fun change(board: ForumFavoriteBoard, add: Boolean) = repository.change(board, add)
}
