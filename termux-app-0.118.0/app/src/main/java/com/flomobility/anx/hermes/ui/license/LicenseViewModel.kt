package com.flomobility.anx.hermes.ui.license

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flomobility.anx.hermes.other.Constants
import com.flomobility.anx.hermes.other.Event
import com.flomobility.anx.hermes.other.Resource
import com.flomobility.anx.hermes.repositories.FloRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val repository: FloRepository
): ViewModel() {

    private val _license = MutableLiveData<Event<Resource<String>>>()
    val license: LiveData<Event<Resource<String>>> = _license

    fun getLicense() {
        _license.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            _license.postValue(Event(repository.getEula(Constants.EULA_URL)))
        }
    }

}
