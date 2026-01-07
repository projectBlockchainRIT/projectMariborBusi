package com.example.projektna.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektna.data.api.model.BusRoute
import com.example.projektna.data.api.model.BusStop
import com.example.projektna.data.api.model.DelayResponse
import com.example.projektna.data.repository.BusRouteRepository
import com.example.projektna.data.repository.BusStopRepository
import com.example.projektna.data.repository.DelayRepository
import com.example.projektna.util.Resource
import kotlinx.coroutines.launch

class EventsViewModel : ViewModel() {

    private val busStopRepository = BusStopRepository()
    private val busRouteRepository = BusRouteRepository()
    private val delayRepository = DelayRepository()

    private val _busStops = MutableLiveData<Resource<List<BusStop>>>()
    val busStops: LiveData<Resource<List<BusStop>>> = _busStops

    private val _routes = MutableLiveData<Resource<List<BusRoute>>>()
    val routes: LiveData<Resource<List<BusRoute>>> = _routes

    private val _delaySubmitResult = MutableLiveData<Resource<DelayResponse>>()
    val delaySubmitResult: LiveData<Resource<DelayResponse>> = _delaySubmitResult

    init {
        loadBusStops()
        loadRoutes()
    }

    fun loadBusStops() {
        viewModelScope.launch {
            _busStops.value = Resource.Loading()
            _busStops.value = busStopRepository.getAllStops()
        }
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _routes.value = Resource.Loading()
            _routes.value = busRouteRepository.getAllRoutes()
        }
    }

    fun submitDelay(
        stationId: Long,
        stationName: String,
        lineId: String,
        delayMinutes: Int
    ) {
        viewModelScope.launch {
            _delaySubmitResult.value = Resource.Loading()
            _delaySubmitResult.value = delayRepository.submitDelay(
                stationId = stationId,
                stationName = stationName,
                lineId = lineId,
                delayMinutes = delayMinutes
            )
        }
    }

    fun clearDelayResult() {
        _delaySubmitResult.value = null
    }
}
