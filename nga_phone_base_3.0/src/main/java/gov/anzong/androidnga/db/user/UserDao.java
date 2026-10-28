package gov.anzong.androidnga.db.user;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import sp.phone.common.User;

/**
 * @author yangyihang
 */
@Dao
public interface UserDao {

    /** 按保存时的顺序读取账号。 */
    @Query("SELECT * FROM users ORDER BY rowid")
    List<User> loadUser();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void updateUsers(User... users);

    @Query("DELETE FROM users")
    void deleteAll();

    /** 用 [users] 整体替换已保存的账号，保留顺序；已移除的账号不会在下次启动时出现。 */
    @Transaction
    default void replaceAll(User... users) {
        deleteAll();
        updateUsers(users);
    }

}
