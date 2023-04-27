package com.lyd.absolverdatabase.bridge.data.repository.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lyd.absolverdatabase.bridge.data.bean.AttackAltitude
import com.lyd.absolverdatabase.bridge.data.bean.AttackDirection
import com.lyd.absolverdatabase.bridge.data.bean.AttackToward
import com.lyd.absolverdatabase.bridge.data.bean.MoveEffect
import com.lyd.absolverdatabase.bridge.data.bean.MoveGP
import com.lyd.absolverdatabase.bridge.data.bean.MoveOrigin
import com.lyd.absolverdatabase.bridge.data.bean.StandSide

@Dao
interface MoveOriginDAO {

    @Query("delete from moveOrigin_tb")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsertAll(list :List<MoveOrigin>)

    @Query("select * from moveOrigin_tb")
    suspend fun allMoves() :List<MoveOrigin>

    @Query("select * from moveOrigin_tb where id = :id")
    suspend fun getMoveById(id :Int) :MoveOrigin

    @Query("select * from moveOrigin_tb where id in (:ids)")
    suspend fun getMovesByIds(ids :List<Int>) :List<MoveOrigin>

    @Query("select * from moveOrigin_tb where startSide = :startSide")
    suspend fun getMovesByStartSide(startSide: StandSide) :List<MoveOrigin>

    @Query("select * from moveOrigin_tb where endSide = :endSide")
    suspend fun getMovesByEndSide(endSide: StandSide) :List<MoveOrigin>

    @Query("select * from moveOrigin_tb where attackToward = :attackToward")
    suspend fun getMovesByAttackToward(attackToward: AttackToward) :List<MoveOrigin>

    @Query("select * from moveOrigin_tb where attackAltitude = :attackAltitude")
    suspend fun getMovesByAttackAltitude(attackAltitude: AttackAltitude) :List<MoveOrigin>

    @Query("select * from moveOrigin_tb where attackDirection = :attackDirection")
    suspend fun getMovesByAttackDirection(attackDirection: AttackDirection) :List<MoveOrigin>

    @Query("select * from moveOrigin_tb where effect = :effect")
    suspend fun getMovesByEffect(effect: MoveEffect) :List<MoveOrigin>

    /**
     * 查询的这种join可以获取到map，然后根据map的各种的操作符来检索move，eg keys 和 values
     * 依靠join的条件id相等，注意，这里的id相等是一对一的关系，假如是一对多要设置多的哪一方为value，且返回的结果为list
     * 然后从这里集合中检索数据
     * */
    @Query("select * from moveOrigin_tb " +
            "join moveGP_tb on moveOrigin_tb.id = moveGP_tb.id " +
            "where moveOrigin_tb.id in (:ids) ")
    suspend fun getMoveMapByIds(ids: List<Int>) :Map<MoveOrigin,MoveGP>

}