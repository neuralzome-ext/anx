package com.flomobility.hermes.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flomobility.hermes.network.requests.LoginRequest
import com.flomobility.hermes.network.responses.LoginResponse
import com.flomobility.hermes.other.Event
import com.flomobility.hermes.other.Resource
import com.flomobility.hermes.repositories.FloRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: FloRepository
) : ViewModel() {

    private val _login = MutableLiveData<Event<Resource<LoginResponse>>>()
    val login: LiveData<Event<Resource<LoginResponse>>> = _login

    fun sendLoginRequest(user: LoginRequest) {
        _login.value = Event(Resource.Loading())
        viewModelScope.launch {
            _login.value = Event(repository.login(user))
        }
    }

}