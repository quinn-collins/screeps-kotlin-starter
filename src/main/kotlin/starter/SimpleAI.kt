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

    BASIC_WORKER(arrayOf(WORK, CARRY, MOVE), maxSize = 3),

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
            MOVE), maxSize = 5),

    HAULER(arrayOf(CARRY, CARRY, MOVE), maxSize = 0),

    SCOUT(arrayOf(MOVE), maxSize = 0),

    CLAIMER(arrayOf(CLAIM, MOVE), maxSize = 0),

    UNASSIGNED(arrayOf(), maxSize = 0),

    HARVESTER(arrayOf(), maxSize = 0),

    UPGRADER(arrayOf(), maxSize = 0),

    BUILDER(arrayOf(), maxSize = 0)
}

private fun spawnCreeps(creeps: Array<Creep>, spawn: StructureSpawn) {
    val sources = spawn.room.find(FIND_SOURCES).sort { a,b ->
        (a.pos.x - b.pos.x) * (a.pos.x - b.pos.x)
         + (a.pos.y - b.pos.y) * (a.pos.y - b.pos.y)}
    val creepsWithSource1 = creeps.count { creep -> creep.memory.assignedSource == sources[0] }
    val creepsWithSource2 = creeps.count { creep -> creep.memory.assignedSource == sources[1].id }
    val assignedSource = if(creepsWithSource1 < creepsWithSource2) sources[0].id else sources[1].id

    for (role in Role.values()) {
        if (creeps.count { it.memory.role == role } < role.maxSize) {
            if (spawn.room.energyAvailable < role.body.sumBy { BODYPART_COST[it]!! }) {
                return
            }
            val newName = "${role.name}_${Game.time}"
            val code = spawn.spawnCreep(role.body, newName, options {
                memory = jsObject<CreepMemory> { this.role = role; this.mission = Mission.UNASSIGNED; this.assignedSource = assignedSource }
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
    REFILL,
    CONTROLLER,
    CONSTRUCTION,
    SPAWNER,
    REPAIR,
    HEAL,
    UNASSIGNED
}


fun assignMission(creeps: Array<Creep>, spawn: StructureSpawn) {
    for (creep in creeps) {
        val controller = spawn.room.controller
        val targets = spawn.room.find(FIND_CONSTRUCTION_SITES)
        val repairTargets = spawn.room.find(FIND_STRUCTURES).filter { it.hits < it.hitsMax/2 && it.hitsMax < 10000 }
        val refillTargets = creep.room.find(FIND_MY_STRUCTURES)
                .filter { (it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN) }
                .filter { it.unsafeCast<EnergyContainer>().energy < it.unsafeCast<EnergyContainer>().energyCapacity }

        if (creep.memory.mission == Mission.UNASSIGNED) {
            when {
                creep.carry.energy == 0 -> creep.memory.mission = Mission.HARVEST
                spawn.energy < spawn.energyCapacity -> creep.memory.mission = Mission.SPAWNER
                refillTargets.isNotEmpty() -> creep.memory.mission = Mission.REFILL
                controller!!.ticksToDowngrade < 2000 -> creep.memory.mission = Mission.CONTROLLER
                targets.isNotEmpty() -> creep.memory.mission = Mission.CONSTRUCTION
                repairTargets.isNotEmpty() -> creep.memory.mission = Mission.REPAIR
                else -> creep.memory.mission = Mission.CONTROLLER

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
        val assignedSource = Game.getObjectById(creep.memory.assignedSource) ?: sources[0]
        val targets = spawn.room.find(FIND_CONSTRUCTION_SITES)
        val repairTargets = spawn.room.find(FIND_STRUCTURES).filter { it.hits < it.hitsMax/2 && it.hitsMax < 10000 }
        val refillTargets = creep.room.find(FIND_MY_STRUCTURES)
                .filter { (it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN) }
                .filter { it.unsafeCast<EnergyContainer>().energy < it.unsafeCast<EnergyContainer>().energyCapacity }
        if(creep.memory.mission == Mission.HARVEST) {
            when {
                creep.carry.energy == creep.carryCapacity -> creep.memory.mission = Mission.UNASSIGNED
                creep.harvest(assignedSource) == ERR_NOT_IN_RANGE -> creep.moveTo(assignedSource.pos)
                creep.harvest(assignedSource) != ERR_NOT_IN_RANGE -> creep.harvest(assignedSource)
            }
        }
        if (creep.memory.mission == Mission.SPAWNER) {
            when {
                spawn.energy == spawn.energyCapacity -> creep.memory.mission = Mission.UNASSIGNED
                creep.carry.energy == 0 -> creep.memory.mission = Mission.UNASSIGNED
                creep.transfer(spawn, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE -> creep.moveTo(spawn.pos)
                creep.transfer(spawn, RESOURCE_ENERGY) != ERR_NOT_IN_RANGE -> creep.transfer(spawn, RESOURCE_ENERGY)
            }
        }
        if (creep.memory.mission == Mission.REFILL) {
            when {
                refillTargets.isEmpty() -> creep.memory.mission = Mission.UNASSIGNED
                creep.carry.energy == 0 -> creep.memory.mission = Mission.UNASSIGNED
                creep.transfer(refillTargets[0], RESOURCE_ENERGY) == ERR_NOT_IN_RANGE -> creep.moveTo(refillTargets[0])
                creep.transfer(refillTargets[0], RESOURCE_ENERGY) != ERR_NOT_IN_RANGE -> creep.transfer(refillTargets[0], RESOURCE_ENERGY)
            }

        }
        if (creep.memory.mission == Mission.CONSTRUCTION) {
            when {
                targets.isEmpty() -> creep.memory.mission = Mission.UNASSIGNED
                creep.carry.energy == 0 -> creep.memory.mission = Mission.UNASSIGNED
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
                spawn.energy < spawn.energyCapacity -> creep.memory.mission = Mission.UNASSIGNED
                creep.carry.energy == 0 -> creep.memory.mission = Mission.UNASSIGNED
                creep.transfer(controller!!, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE -> creep.moveTo(controller.pos)
                creep.transfer(controller!!, RESOURCE_ENERGY) != ERR_NOT_IN_RANGE -> creep.transfer(controller!!, RESOURCE_ENERGY)
            }
        }

    }
}

var CreepMemory.role by memory(Role.UNASSIGNED)
var CreepMemory.mission by memory(Mission.UNASSIGNED)
var CreepMemory.assignedSource: String? by memory()
