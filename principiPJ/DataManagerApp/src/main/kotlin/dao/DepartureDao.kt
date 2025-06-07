package dao

import model.Departure

interface DepartureDao : CrudDao<Departure> {
    fun getDeparturesForDirection(directionId: Int): List<Departure>
    fun deleteAllForDirection(directionId: Int): Boolean
    fun deleteAllForStop(stopId: Int): Boolean
}