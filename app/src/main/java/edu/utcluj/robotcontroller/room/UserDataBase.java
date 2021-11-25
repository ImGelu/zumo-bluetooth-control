package edu.utcluj.robotcontroller.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {User.class}, version = 1, exportSchema = false)
public abstract class UserDataBase extends RoomDatabase {
    public abstract UserDao getUserDao();
}