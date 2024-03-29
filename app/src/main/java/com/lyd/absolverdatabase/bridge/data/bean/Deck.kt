package com.lyd.absolverdatabase.bridge.data.bean

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lyd.absolverdatabase.utils.IntMutableListConverter
import com.lyd.absolverdatabase.utils.MoveBoxConverter
import com.lyd.absolverdatabase.utils.MoveBoxListConverter
import com.lyd.absolverdatabase.utils.SideUtil
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "deck_tb")
@TypeConverters(value = [
    IntMutableListConverter::class,
    MoveBoxListConverter::class,
    MoveBoxConverter::class
])
data class Deck(
    @PrimaryKey(autoGenerate = true)
    var deckId :Int = 0,
//    @PrimaryKey

    var name :String,

    var deckType :DeckType,
    var deckStyle: Style,
    var createTime :Long,
    var updateTime :Long,

    var note :String = "",// 卡组说明

    // 这里拿的是Move的id，以这种list来确定关系，不直接持有move实例来防止后期move出问题了这边也要改
    // 要用的时候靠id去move_tb那搜索就行
    // 序列攻击
    var sequenceUpperRight :MutableList<MoveBox>,
    var sequenceLowerRight :MutableList<MoveBox>,
    var sequenceUpperLeft :MutableList<MoveBox>,
    var sequenceLowerLeft :MutableList<MoveBox>,
    // 自选攻击
    var optionalUpperRight :MoveBox,
    var optionalLowerRight :MoveBox,
    var optionalUpperLeft :MoveBox,
    var optionalLowerLeft :MoveBox
) : Parcelable

/**专门存数据库的招式类*/
@Entity(tableName = "moveJs_tb")
data class MoveJson(
    @PrimaryKey
    val id: Int,
    val json :String// 这个专门存move的json文件，防止未来字段更新，而维护更新太难了
)

/**所有招式的父类，新增的招式需要继承此类*/
open class Move(
    @PrimaryKey
    val id :Int,// id
    val name: String,// 名称
    val name_en:String,// 英文名称
    val school :Style,// 流派

    var startSide: StandSide,// 起始站架 可变是为了镜像修改
    var endSide: StandSide,// 结束站架 可变是为了镜像修改

    val strength :Int,// 力度 1~3，分别为轻中重
    val attackRange :Float,// 攻击范围，这个CE有修改

    var attackToward :AttackToward,// 攻击朝向 可变是为了镜像修改
    val attackAltitude: AttackAltitude,// 攻击高度
    val attackDirection: AttackDirection,// 攻击走向

    val startFrame :Int,// 起手帧数，这个CE有修改
    val physicalWeakness :Float,// 削弱对手体力，这个CE有修改
    val physicalOutput :Float,// 自身体力消耗，这个CE有修改
    val hitAdvantageFrame :Int,// 击中优势帧，这个CE有修改
    val defenseAdvantageFrame :Int,// 防御优势帧，这个CE有修改
    val effect :String,// 招式效果，用于整合多个招式效果，但注意这个只是string，不是枚举

    val canHand :Boolean,// 徒手是否可用
    val canOriginSword :Boolean,// 原版招式在剑卡组里是否可用
    val canMirrorSword :Boolean,// 镜像招式在剑卡组里是否可用 因为有些招式在剑卡组里面不能使用镜像搜索（起始结束站架被动画限死），所以加这个字段，两个字段其中有一个是1就可以在剑卡组中使用，全0就不能在剑卡组中所以用
) {
    fun toMirror(){
        startSide = SideUtil.getMirrorSide(startSide)
        endSide = SideUtil.getMirrorSide(endSide)
        attackToward = AttackToward.getMirrorToward(attackToward)
    }
}

// TODO: 使用两个类来区分原版和CE版，我可以放心的使用数据库存两个表了，马上就把上面的带json的给删了
/**
 * 招式
 * 和CE版有修改的可以使用ArrayMap来存储版本
 * */
@Entity(tableName = "moveOrigin_tb")
class MoveOrigin(
//    @PrimaryKey
    id :Int,// id，保持和MoveJson的id一致，用于检索
    name: String,// 名称
    name_en:String,// 英文名称
    school :Style,// 流派
    startSide: StandSide,// 起始站架 可变是为了镜像修改
    endSide: StandSide,// 结束站架 可变是为了镜像修改
    strength :Int,// 力度 1~3，分别为轻中重
    attackRange :Float,// 攻击范围，这个CE有修改
    attackToward :AttackToward,// 攻击朝向 可变是为了镜像修改
    attackAltitude: AttackAltitude,// 攻击高度
    attackDirection: AttackDirection,// 攻击走向
    startFrame :Int,// 起手帧数，这个CE有修改
    physicalWeakness :Float,// 削弱对手体力，这个CE有修改
    physicalOutput :Float,// 自身体力消耗，这个CE有修改
    hitAdvantageFrame :Int,// 击中优势帧，这个CE有修改
    defenseAdvantageFrame :Int,// 防御优势帧，这个CE有修改
    effect :String,// 招式效果，用于整合多个招式效果，但注意这个只是string，不是枚举
    canHand :Boolean,// 徒手是否可用
    canOriginSword :Boolean,// 原版招式在剑卡组里是否可用
    canMirrorSword :Boolean,// 镜像招式在剑卡组里是否可用 因为有些招式在剑卡组里面不能使用镜像搜索（起始结束站架被动画限死），所以加这个字段，两个字段其中有一个是1就可以在剑卡组中使用，全0就不能在剑卡组中所以用
) :Move(id, name, name_en, school, startSide, endSide, strength, attackRange, attackToward, attackAltitude, attackDirection, startFrame, physicalWeakness, physicalOutput, hitAdvantageFrame, defenseAdvantageFrame, effect, canHand, canOriginSword, canMirrorSword)
{

//    fun toMirror(){
//        startSide = SideUtil.getMirrorSide(startSide)
//        endSide = SideUtil.getMirrorSide(endSide)
//        attackToward = AttackToward.getMirrorToward(attackToward)
//    }
}

@Entity(tableName = "moveCE_tb")
class MoveCE(
//    @PrimaryKey
    id :Int,// id，保持和MoveJson的id一致，用于检索
    name: String,// 名称
    name_en:String,// 英文名称
    school :Style,// 流派
    startSide: StandSide,// 起始站架
    endSide: StandSide,// 结束站架
    strength :Int,// 力度 1~3，分别为轻中重

    val strengthBonus :String,// 力量加成
    val agilityBonus :String,// 敏捷加成
    val flexibleBonus :String,// 机动加成

    attackRange :Float,// 攻击范围，这个CE有修改
    attackToward :AttackToward,// 攻击朝向
    attackAltitude: AttackAltitude,// 攻击高度
    attackDirection: AttackDirection,// 攻击走向
    startFrame :Int,// 起手帧数，这个CE有修改
    physicalWeakness :Float,// 削弱对手体力，这个CE有修改
    physicalOutput :Float,// 自身体力消耗，这个CE有修改
    hitAdvantageFrame :Int,// 击中优势帧，这个CE有修改
    defenseAdvantageFrame :Int,// 防御优势帧，这个CE有修改

    val cancelPoint :Int,// 取消点 帧长
    val hitFrame :String,// 打击帧 招式的打击判定长度，这个
    val yellowAttackFrame :Int,// 黄攻帧 打击帧后距离黄攻点的距离

    effect :String,// 招式效果，用于整合多个招式效果，但注意这个只是string，不是枚举
    canHand :Boolean,// 徒手是否可用
    canOriginSword :Boolean,// 原版招式在剑卡组里是否可用
    canMirrorSword :Boolean,// 镜像招式在剑卡组里是否可用 因为有些招式在剑卡组里面不能使用镜像搜索（起始结束站架被动画限死），所以加这个字段，两个字段其中有一个是1就可以在剑卡组中使用，全0就不能在剑卡组中所以用
) :Move(id, name, name_en, school, startSide, endSide, strength, attackRange, attackToward, attackAltitude, attackDirection, startFrame, physicalWeakness, physicalOutput, hitAdvantageFrame, defenseAdvantageFrame, effect, canHand, canOriginSword, canMirrorSword)
{
//    fun toMirror(){
//        startSide = SideUtil.getMirrorSide(startSide)
//        endSide = SideUtil.getMirrorSide(endSide)
//        attackToward = AttackToward.getMirrorToward(attackToward)
//    }
}


