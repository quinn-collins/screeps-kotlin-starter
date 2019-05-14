package starter

import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.utils.isEmpty
import screeps.utils.memory.memory
import screeps.utils.unsafe.delete
import screeps.utils.unsafe.jsObject


fun gameLoop() {
    val mainSpawn: StructureSpawn = Game.spawns.values.firstOrNull() ?: return

    houseKeeping(Game.creeps)
    spawnCreeps(Game.creeps.values, mainSpawn)
    assignMission(Game.creeps.values)
    runCreepWork(Game.creeps.values, mainSpawn.room)
}

private fun houseKeeping(creeps: Record<String, Creep>) {
    if (Game.creeps.isEmpty()) return  // this is needed because Memory.creeps is undefined

    for ((creepName, _) in Memory.creeps) {
        if (creeps[creepName] == null) {
            console.log("deleting obsolete memory entry for creep $creepName")
            delete(Memory.creeps[creepName])
        }
    }
}

enum class Role(val body: Array<BodyPartConstant>, val maxSize: Int = 0) {

    BASIC_WORKER(arrayOf(WORK, CARRY, MOVE), maxSize = 5),

    MINER(arrayOf(WORK, WORK, MOVE), maxSize = 0),

    MINER_BIG(arrayOf(
            WORK,
            WORK,
            WORK,
            WORK,
            WORK,
            MOVE,
            MOVE), maxSize = 0),

    BIG_WORKER(arrayOf(
            WORK,
            WORK,
            WORK,
            WORK,
            CARRY,
            MOVE,
            MOVE), maxSize = 1),

    HAULER(arrayOf(CARRY, CARRY, MOVE), maxSize = 0),

    SCOUT(arrayOf(MOVE), maxSize = 0),

    CLAIMER(arrayOf(CLAIM, MOVE), maxSize = 0),

    UNASSIGNED(arrayOf(), maxSize = 0)
}

private fun spawnCreeps(creeps: Array<Creep>, spawn: StructureSpawn) {

    for (role in Role.values()) {
        if(creeps.count {it.memory.role == role} < role.maxSize) {

            if (spawn.room.energyAvailable < role.body.sumBy { BODYPART_COST[it]!! }) {
                return
            }
            val newName = "${role.name}_${Game.time}"
            val code = spawn.spawnCreep(role.body, newName, options {
                memory = jsObject<CreepMemory> { this.role = role; this.mission = Mission.UNASSIGNED }
            })

            when (code) {
                OK -> console.log("spawning $newName with body ${role.body}")
                ERR_BUSY, ERR_NOT_ENOUGH_ENERGY -> run { } // do nothing
                else -> console.log("unhandled error code $code")
            }
            return
        }


    }
}

enum class Mission() {
    HARVEST,
    SPAWNER,
    CONTROLLER,
    CONSTRUCTION,
    REPAIR,
    HEAL,
    UNASSIGNED
}
fun assignMission(creeps: Array<Creep>) {
    for(creep in creeps) {
        if(creep.carry.energy == 0) {
            creep.memory.mission = Mission.HARVEST
        }
    }
}

fun runCreepWork(creeps: Array<Creep>, room: Room) {
    val sources = room.find(FIND_SOURCES)
   for(creep in creeps) {
        val sources = room.find(FIND_SOURCES)
        if (sources[0] == ERR_NOT_IN_RANGE) {
            moveTo(sources[0].pos)
        }
   }
}

//    val mainSpawn: StructureSpawn = Game.spawns.values.firstOrNull() ?: return
//
//    //delete memories of creeps that have passed away
//    houseKeeping(Game.creeps)
//
//    //make sure we have at least some creeps
//    spawnCreeps(Game.creeps.values, mainSpawn)
//
//    // build a few extensions so we can have 550 energy
//    val controller = mainSpawn.room.controller
//    if (controller != null && controller.level >= 2) {
//        when (controller.room.find(FIND_MY_STRUCTURES).count { it.structureType == STRUCTURE_EXTENSION }) {
//            0 -> controller.room.createConstructionSite(30, 17, STRUCTURE_EXTENSION)
//            1 -> controller.room.createConstructionSite(31, 17, STRUCTURE_EXTENSION)
//            2 -> controller.room.createConstructionSite(32, 17, STRUCTURE_EXTENSION)
//            3 -> controller.room.createConstructionSite(33, 17, STRUCTURE_EXTENSION)
//            4 -> controller.room.createConstructionSite(34, 17, STRUCTURE_EXTENSION)
//            5 -> controller.room.createConstructionSite(35, 17, STRUCTURE_EXTENSION)
//            6 -> controller.room.createConstructionSite(36, 17, STRUCTURE_EXTENSION)
//        }
//    } else if (controller != null && controller.level >= 3) {
//        when(controller.room.find(FIND_MY_STRUCTURES).count { it.structureType == STRUCTURE_TOWER}) {
//            0 -> controller.room.createConstructionSite(35, 24, STRUCTURE_TOWER)
//        }
//    }
//
//    //spawn a big creep if we have plenty of energy
//    for ((_, room) in Game.rooms) {
//        if (room.energyAvailable >= 550) {
//            mainSpawn.spawnCreep(
//                    arrayOf(
//                            WORK,
//                            WORK,
//                            WORK,
//                            WORK,
//                            CARRY,
//                            MOVE,
//                            MOVE
//                    ),
//                    "HarvesterBig_${Game.time}",
//                    options {
//                        memory = jsObject<CreepMemory> {
//                            this.role = Role.HARVESTER
//                        }
//                    }
//            )
//            console.log("hurray!")
//        }
//    }
//
//    for ((_, creep) in Game.creeps) {
//        when (creep.memory.role) {
//            Role.HARVESTER -> creep.harvest()
//            Role.BUILDER -> creep.build()
//            Role.UPGRADER -> creep.upgrade(mainSpawn.room.controller!!)
//            else -> creep.pause()
//        }
//    }
//
//}
//
//private fun spawnCreeps(
//        creeps: Array<Creep>,
//        spawn: StructureSpawn
//) {
//
//    val body = arrayOf<BodyPartConstant>(WORK, CARRY, MOVE)
//
//    if (spawn.room.energyAvailable < body.sumBy { BODYPART_COST[it]!! }) {
//        return
//    }
//
//    val role: Role = when {
//        creeps.count { it.memory.role == Role.HARVESTER } < 2 -> Role.HARVESTER
//
//        creeps.none { it.memory.role == Role.UPGRADER } -> Role.UPGRADER
//
//        spawn.room.find(FIND_MY_CONSTRUCTION_SITES).isNotEmpty() &&
//                creeps.count { it.memory.role == Role.BUILDER } < 2 -> Role.BUILDER
//
//        else -> return
//    }
//
//    val newName = "${role.name}_${Game.time}"
//    val code = spawn.spawnCreep(body, newName, options {
//        memory = jsObject<CreepMemory> { this.role = role }
//    })
//
//    when (code) {
//        OK -> console.log("spawning $newName with body $body")
//        ERR_BUSY, ERR_NOT_ENOUGH_ENERGY -> run { } // do nothing
//        else -> console.log("unhandled error code $code")
//    }
//}
//

var CreepMemory.building: Boolean by memory { false }
var CreepMemory.pause: Int by memory { 0 }
var CreepMemory.role by memory(Role.UNASSIGNED)
var CreepMemory.mission by memory(Mission.UNASSIGNED)
