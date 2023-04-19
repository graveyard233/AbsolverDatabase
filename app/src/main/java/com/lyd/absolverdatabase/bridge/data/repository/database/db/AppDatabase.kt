package com.lyd.absolverdatabase.bridge.data.repository.database.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lyd.absolverdatabase.bridge.data.bean.*
import com.lyd.absolverdatabase.bridge.data.repository.database.dao.BilibiliVideoDAO
import com.lyd.absolverdatabase.bridge.data.repository.database.dao.TestDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Database(entities = [BilibiliVideo::class,TestA::class,TestB::class,TestABCrossRef::class], version = 1, exportSchema = false)
abstract class AppDatabase :RoomDatabase(){

    abstract fun videoDao() : BilibiliVideoDAO

    abstract fun testDao() : TestDAO

    companion object{

        @Volatile
        private var INSTANCE : AppDatabase ?= null

        fun getDatabase(context: Context) : AppDatabase {
            return INSTANCE ?: synchronized(this){
                val instant = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "absolver_database"
                ).addCallback(object :Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 在这里进行初始化工作
                        Log.i("AppDatabase", "database onCreate: start")

                        INSTANCE?.apply {

                        }
                    }
                }).build()
                INSTANCE = instant
                instant
            }
        }
    }

}