package com.example.werun.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.werun.data.User
import com.example.werun.data.model.RunData
import com.example.werun.data.repository.FriendsRepository
import com.example.werun.data.repository.FirebaseRunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendProfileViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val runsRepository: FirebaseRunRepository
) : ViewModel() {

    private val _friend = MutableStateFlow<User?>(null)
    val friend: StateFlow<User?> = _friend.asStateFlow()

    private val _friendStats = MutableStateFlow(FriendStats())
    val friendStats: StateFlow<FriendStats> = _friendStats.asStateFlow()

    private val _recentRuns = MutableStateFlow<List<RunData>>(emptyList())
    val recentRuns: StateFlow<List<RunData>> = _recentRuns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadFriendProfile(friendId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = friendsRepository.getFriendById(friendId)
                if (result.isSuccess) {
                    _friend.value = result.getOrNull()
                }
            } catch (e: Exception) {
                // Xử lý lỗi (ví dụ: ghi log)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFriendStats(friendId: String) {
        viewModelScope.launch {
            try {
                val totalRuns = runsRepository.getUserRunsCount().getOrNull() ?: 0
                val totalDistance = runsRepository.getUserTotalDistance().getOrNull() ?: 0.0
                val totalTime = runsRepository.getUserTotalDuration().getOrNull() ?: 0L
                val runs = runsRepository.getUserRuns().collect { runs ->
                    val averageSpeed = if (totalRuns > 0) totalDistance / totalTime * 3600 / 1000 else 0.0
                    val bestTime = runs.minOfOrNull { it.duration } ?: 0L
                    val longestRun = runs.maxOfOrNull { it.distance } ?: 0.0
                    _friendStats.value = FriendStats(
                        totalRuns = totalRuns,
                        totalDistance = totalDistance,
                        totalTime = totalTime,
                        averageSpeed = averageSpeed,
                        bestTime = bestTime,
                        longestRun = longestRun
                    )
                }
            } catch (e: Exception) {
                // Xử lý lỗi
            }
        }
    }

    fun loadRecentRuns(friendId: String) {
        viewModelScope.launch {
            try {
                runsRepository.getUserRuns(2).collect { runs ->
                    _recentRuns.value = runs
                }
            } catch (e: Exception) {
                // Xử lý lỗi
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            try {
                val result = friendsRepository.removeFriend(friendId)
                if (!result.isSuccess) {
                    // Xử lý lỗi
                }
            } catch (e: Exception) {
                // Xử lý lỗi
            }
        }
    }
}