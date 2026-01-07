package com.example.projektna.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektna.data.api.model.BusRoute
import com.example.projektna.data.api.model.BusStop
import com.example.projektna.data.api.model.StopMetadata
import com.example.projektna.data.repository.BusRouteRepository
import com.example.projektna.data.repository.BusStopRepository
import com.example.projektna.util.Resource
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val busStopRepository = BusStopRepository()
    private val busRouteRepository = BusRouteRepository()

    private val _busStops = MutableLiveData<Resource<List<BusStop>>>()
    val busStops: LiveData<Resource<List<BusStop>>> = _busStops

    private val _selectedStop = MutableLiveData<BusStop?>()
    val selectedStop: LiveData<BusStop?> = _selectedStop

    private val _stopMetadata = MutableLiveData<Resource<StopMetadata>>()
    val stopMetadata: LiveData<Resource<StopMetadata>> = _stopMetadata

    private val _routes = MutableLiveData<Resource<List<BusRoute>>>()
    val routes: LiveData<Resource<List<BusRoute>>> = _routes

    private val _selectedRoute = MutableLiveData<Resource<BusRoute>>()
    val selectedRoute: LiveData<Resource<BusRoute>> = _selectedRoute

    private val _routeStations = MutableLiveData<Resource<List<BusStop>>>()
    val routeStations: LiveData<Resource<List<BusStop>>> = _routeStations

    fun loadBusStops() {
        viewModelScope.launch {
            _busStops.value = Resource.Loading()
            _busStops.value = busStopRepository.getAllStops()
        }
    }

    fun loadNearbyStops(latitude: Double, longitude: Double, radius: Int = 500) {
        viewModelScope.launch {
            _busStops.value = Resource.Loading()
            _busStops.value = busStopRepository.getNearbyStops(latitude, longitude, radius)
        }
    }

    fun loadStopDetails(stopId: Long) {
        viewModelScope.launch {
            _stopMetadata.value = Resource.Loading()
            _stopMetadata.value = busStopRepository.getStopById(stopId)
        }
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _routes.value = Resource.Loading()
            _routes.value = busRouteRepository.getAllRoutes()
        }
    }

    fun loadRouteDetails(lineId: Long) {
        viewModelScope.launch {
            _selectedRoute.value = Resource.Loading()
            _selectedRoute.value = busRouteRepository.getRouteByLineId(lineId)
        }

        loadRouteStations(lineId)
    }

    fun loadRouteStations(lineId: Long) {
        viewModelScope.launch {
            _routeStations.value = Resource.Loading()
            _routeStations.value = busRouteRepository.getStationsForRoute(lineId)
        }
    }
}
