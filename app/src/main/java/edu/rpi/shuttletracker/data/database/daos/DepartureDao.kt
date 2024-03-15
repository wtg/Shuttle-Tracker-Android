package edu.rpi.shuttletracker.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.rpi.shuttletracker.data.models.Departure
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartureDao {
    @Query("SELECT * FROM departures")
    fun getAllDepartures(): Flow<List<Departure>>

    @Query(
        """
        SELECT * FROM departures 
        ORDER BY stop_name
        """,
    )
    fun getAllDeparturesGrouped(): Flow<List<Departure>>

    @Query(
        """
        SELECT * FROM departures 
        WHERE stop_name == :name
        """,
    )
    fun getDepartures(name: String): Flow<List<Departure>>

    @Query(
        """
            SELECT * FROM departures
            WHERE id == :id
        """,
    )
    suspend fun getDeparture(id: Int): Departure?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeparture(departure: Departure): Long

    @Delete
    suspend fun deleteDeparture(departure: Departure)

    @Query("DELETE FROM departures")
    suspend fun nukeDepartures()
}
