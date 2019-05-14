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
    assignMission(Game.creeps.values, mainSpawn)
    runCreepWork(Game.creeps, mainSpawn)
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
        if (creeps.count { it.memory.role == role } < role.maxSize) {

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

enum class Mission {
    HARVEST,
    SPAWNER,
    REFILL,
    CONTROLLER,
    CONSTRUCTION,
    REPAIR,
    HEAL,
    UNASSIGNED
}

fun assignMission(creeps: Array<Creep>, spawn: StructureSpawn) {
    for (creep in creeps) {

        val targets = spawn.room.find(FIND_CONSTRUCTION_SITES)
        val repairTargets = spawn.room.find(FIND_STRUCTURES).filter { it.hits < (it.hitsMax / 2) }
        val refillTargets = spawn.room.find(FIND_MY_STRUCTURES).filter { it.structureType == STRUCTURE_EXTENSION }.filter { it.energ }
        if (creep.memory.mission == Mission.UNASSIGNED) {
            when {
                creep.carry.energy == 0 -> creep.memory.mission = Mission.HARVEST
                spawn.energy < spawn.energyCapacity -> creep.memory.mission = Mission.SPAWNER

                targets.isNotEmpty() -> creep.memory.mission = Mission.CONSTRUCTION
                repairTargets.isNotEmpty() -> creep.memory.mission = Mission.REPAIR
                spawn.energy == spawn.energyCapacity -> creep.memory.mission = Mission.CONTROLLER

            }
        }
        if (creep.memory.mission != Mission.UNASSIGNED) {
           when {
               creep.carry.energy == creep.carryCapacity && creep.memory.mission == Mission.HARVEST -> creep.memory.mission = Mission.UNASSIGNED
           }
        }
    }
}

fun runCreepWork(creeps: Record<String, Creep>, spawn: StructureSpawn) {
    for ((_, creep) in creeps) {

        val controller = spawn.room.controller
        val sources = spawn.room.find(FIND_SOURCES)
        val targets = spawn.room.find(FIND_CONSTRUCTION_SITES)
        val repairTargets = spawn.room.find(FIND_STRUCTURES).filter { it.hits < (it.hitsMax) }
        if(creep.memory.mission == Mission.HARVEST) {
            when {
                (creep.carry.energy == creep.carryCapacity) -> creep.memory.mission = Mission.UNASSIGNED
                (creep.harvest(sources[0]) == ERR_NOT_IN_RANGE) -> creep.moveTo(sources[0].pos)
                (creep.harvest(sources[0]) != ERR_NOT_IN_RANGE) -> creep.harvest(sources[0])
            }
        }
        if (creep.memory.mission == Mission.SPAWNER) {
            when {
                (spawn.energy == spawn.energyCapacity) -> creep.memory.mission = Mission.UNASSIGNED
                (creep.carry.energy == 0) -> creep.memory.mission = Mission.UNASSIGNED
                (creep.transfer(spawn, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) -> (creep.moveTo(spawn.pos))
                (creep.transfer(spawn, RESOURCE_ENERGY) != ERR_NOT_IN_RANGE) -> (creep.transfer(spawn, RESOURCE_ENERGY))
            }
        }
        if (creep.memory.mission == Mission.CONSTRUCTION) {
            when {
                targets.isEmpty() -> creep.memory.mission = Mission.UNASSIGNED
                (creep.carry.energy == 0) -> creep.memory.mission = Mission.UNASSIGNED
                creep.build(targets[0]) == ERR_NOT_IN_RANGE -> creep.moveTo(targets[0].pos)
                creep.build(targets[0]) != ERR_NOT_IN_RANGE -> creep.build(targets[0])
            }
        }
        if (creep.memory.mission == Mission.REPAIR) {
            when {
                repairTargets.isEmpty() -> creep.memory.mission = Mission.UNASSIGNED
                creep.carry.energy == 0 -> creep.memory.mission = Mission.UNASSIGNED
                creep.repair(repairTargets[0]) == ERR_NOT_IN_RANGE -> creep.moveTo(repairTargets[0].pos)
                creep.repair(repairTargets[0]) != ERR_NOT_IN_RANGE -> creep.repair(repairTargets[0])

            }
        }
        if (creep.memory.mission == Mission.CONTROLLER) {
            when {
                (spawn.energy < spawn.energyCapacity) -> creep.memory.mission = Mission.UNASSIGNED
                (creep.carry.energy == 0) -> creep.memory.mission = Mission.UNASSIGNED
                (creep.transfer(controller!!, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) -> (creep.moveTo(controller.pos))
                (creep.transfer(controller!!, RESOURCE_ENERGY) != ERR_NOT_IN_RANGE) -> (creep.transfer(controller!!, RESOURCE_ENERGY))
            }
        }

    }
}

var CreepMemory.role by memory(Role.UNASSIGNED)
var CreepMemory.mission by memory(Mission.UNASSIGNED)
