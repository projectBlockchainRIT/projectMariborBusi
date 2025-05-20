package dao

interface CrudDao<T> {
    fun getById(id: Int): T?
    fun insert(entity: T): Boolean
    fun update(entity: T): Boolean
    fun delete(id: Int): Boolean
}