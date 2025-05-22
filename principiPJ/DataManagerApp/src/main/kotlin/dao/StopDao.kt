package dao

import model.Stop
import model.Departure

interface StopDao : CrudDao<Stop> {
    fun getDeparturesForStop(stopId: Int): List<Departure>
}
