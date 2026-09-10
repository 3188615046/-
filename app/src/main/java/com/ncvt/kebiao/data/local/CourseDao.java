package com.ncvt.kebiao.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public interface CourseDao {
    @Query("SELECT * FROM courses WHERE academicYear = :year AND semester = :sem")
    List<CourseEntity> getAllBySemester(String year, String sem);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CourseEntity> courses);

    @Query("DELETE FROM courses WHERE academicYear = :year AND semester = :sem")
    void deleteBySemester(String year, String sem);

    @Query("DELETE FROM courses")
    void deleteAll();

    @Query("SELECT * FROM courses")
    List<CourseEntity> getAll();

    @Transaction
    default void replaceBySemester(String year, String sem, List<CourseEntity> courses) {
        deleteBySemester(year, sem);
        insertAll(courses);
    }
}
