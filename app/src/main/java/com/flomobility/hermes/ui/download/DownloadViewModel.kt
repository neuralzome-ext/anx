package com.flomobility.hermes.ui.download

import androidx.lifecycle.ViewModel
import com.flomobility.hermes.repositories.FloRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: FloRepository
) : ViewModel() {

//    private val _login = MutableLiveData<Event<Resource<LoginResponse>>>()
//    val login: LiveData<Event<Resource<LoginResponse>>> = _login
//
//    fun sendLoginRequest(user: LoginRequest) {
//        _login.value = Event(Resource.Loading())
//        viewModelScope.launch {
//            _login.value = Event(repository.login(user))
//        }
//    }

}