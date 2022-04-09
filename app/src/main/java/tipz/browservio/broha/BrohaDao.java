package tipz.browservio.broha;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BrohaDao {
    @Query("SELECT * FROM broha")
    List<Broha> getAll();

    @Query("SELECT * FROM broha WHERE id LIKE :id LIMIT 1")
    Broha findById(int id);

    @Insert
    void insertAll(Broha... broha);

    @Query("DELETE FROM broha WHERE id = :id")
    void deleteById(int id);
}