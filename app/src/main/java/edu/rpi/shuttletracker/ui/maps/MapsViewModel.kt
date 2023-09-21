package edu.rpi.shuttletracker.ui.maps

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.repositories.ShuttleTrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val apiRepository: ShuttleTrackerRepository,
) : ViewModel() {

    private val busMarkers = HashMap<Int, Marker>()

    private val _mapsUiState = MutableStateFlow(MapsUIState(listOf()))
    val mapsUIState: StateFlow<MapsUIState> = _mapsUiState

    // for compose
    fun loadBuses() {
        viewModelScope.launch {
            Log.d("oncreate", "loadBuses: ${apiRepository.getBuses()}")
            _mapsUiState.value = _mapsUiState.value.copy(buses = apiRepository.getBuses())
        }
    }

    // for compose
    fun loadStops() {
        viewModelScope.launch {
            Log.d("oncreate", "loadStops: ${apiRepository.getStops()}")
            _mapsUiState.value = _mapsUiState.value.copy(stops = apiRepository.getStops())
        }
    }

    fun addBus(busNum: Int, bus: BoardBus) {
        viewModelScope.launch {
            apiRepository.addBus(busNum, bus)
        }
    }

    fun updateBoardedState() {
        _mapsUiState.value = _mapsUiState.value.copy(isBoarded = !_mapsUiState.value.isBoarded)
    }

    // TODO DELETE EVERYTHING UNDER FOR NORMAL VIEWS
    fun loadBuses(map: GoogleMap, context: Context) {
        viewModelScope.launch {
            Log.d("oncreate", "loadBuses: ${apiRepository.getBuses()}")
            _mapsUiState.value = _mapsUiState.value.copy(buses = apiRepository.getBuses())

            drawBuses(map, context)
        }
    }

    fun loadStops(map: GoogleMap) {
        viewModelScope.launch {
            Log.d("oncreate", "loadStops: ${apiRepository.getStops()}")
            _mapsUiState.value = _mapsUiState.value.copy(stops = apiRepository.getStops())

            drawStops(map)
        }
    }

    // TODO MOVE OUT OF VM WHEN IMPLEMENTING COMPOSE
    private fun drawStops(map: GoogleMap) {
        // draws stop markers
        mapsUIState.value.stops.forEach {
            map.addMarker(
                MarkerOptions().position(LatLng(it.latitude, it.longitude)).title(it.name).icon(
                    BitmapDescriptorFactory.fromAsset("simplecircle.png"),
                ),
            )
        }
    }

    // TODO MOVE OUT OF VM WHEN IMPLEMENTING COMPOSE
    private fun drawBuses(map: GoogleMap, context: Context) {
        // erasing markers for buses that don't exist
        with(mapsUIState.value.buses.map { it.id }.toSet()) {
            busMarkers.forEach { marker ->
                if (!this.contains(marker.key)) {
                    marker.value.remove()
                    busMarkers.remove(marker.key)
                } else {
                    marker.value.showInfoWindow()
                }
            }
        }

        mapsUIState.value.buses.forEach {
            val busPos = LatLng(it.latitude, it.longitude)
            if (busMarkers.containsKey(it.id)) {
                busMarkers[it.id]!!.position = busPos
                return@forEach
            }

            var busIcon = context.getString(R.string.GPS_bus)
            if (MapsActivity.colorblindMode.getMode()) {
                busIcon = if (it.type == "user") {
                    context.getString(R.string.colorblind_crowdsourced_bus)
                } else {
                    context.getString(R.string.colorblind_GPS_bus)
                }
            } else {
                if (it.type == "user") {
                    busIcon = context.getString(R.string.crowdsourced_bus)
                }
            }

            val marker = MarkerOptions().position(busPos).title("Bus " + it.id).icon(
                BitmapDescriptorFactory.fromAsset(busIcon),
            ).zIndex(1F).snippet("0 seconds ago")

            map.addMarker(marker)?.let { it1 -> busMarkers[it.id] = it1 }
        }
    }
}

data class MapsUIState(
    val buses: List<Bus> = listOf(),
    val stops: List<Stop> = listOf(),
    val isBoarded: Boolean = false,
)
