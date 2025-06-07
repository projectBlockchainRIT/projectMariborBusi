package dao

import model.Arrival

interface ArrivalDao : CrudDao<Arrival> {
    fun getArrivalsForDeparture(departureId: Int): Arrival?
} 