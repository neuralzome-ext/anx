package com.flomobility.anx.hermes.ui.download

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flomobility.anx.hermes.other.Event
import com.flomobility.anx.hermes.repositories.FloRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: FloRepository
) : ViewModel() {

    private val _installStatus = MutableLiveData<Event<InstallStatus>>()
    val installStatus: LiveData<Event<InstallStatus>> = _installStatus

    fun setInstallStatus(status: InstallStatus) {
        this._installStatus.postValue(Event(status))
    }

    sealed class InstallStatus {
        object NotStarted: InstallStatus()
        object Installing: InstallStatus()
        class Failed(val code: Int): InstallStatus()
        object Success: InstallStatus()
    }

}
