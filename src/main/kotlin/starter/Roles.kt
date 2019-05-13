//package starter
//
//import screeps.api.*
//import screeps.api.structures.StructureController
//import screeps.api.structures.StructureSpawn
//
//
//enum class Roles {
//    UNASSIGNED,
//    HARVESTER,
//    BUILDER,
//    UPGRADER
//}
//
//
//fun Creep.upgrade(controller: StructureController) {
//    if (memory.building && carry.energy == 0) {
//        memory.building = false
//        say("🔄 harvest")
//    }
//    if (!memory.building && carry.energy == carryCapacity) {
//        memory.building = true
//        say("🚧 build")
//    }
//    if (memory.building) {
//        if (upgradeController(controller) == ERR_NOT_IN_RANGE) {
//            moveTo(controller.pos)
//        }
//    } else {
//        val sources = room.find(FIND_SOURCES)
//        if (harvest(sources[0]) == ERR_NOT_IN_RANGE) {
//            moveTo(sources[0].pos)
//        }
//    }
//}
//
//fun Creep.pause() {
//    if (memory.pause < 10) {
//        //blink slowly
//        if (memory.pause % 3 != 0) say("\uD83D\uDEAC")
//        memory.pause++
//    } else {
//        memory.pause = 0
//        memory.role = Roles.HARVESTER
//    }
//}
//
//fun Creep.build(assignedRoom: Room = this.room) {
//    if (memory.building && carry.energy == 0) {
//        memory.building = false
//        say("🔄 harvest")
//    }
//    if (!memory.building && carry.energy == carryCapacity) {
//        memory.building = true
//        say("🚧 build")
//    }
//
//    if (memory.building) {
//        val targets = assignedRoom.find(FIND_MY_CONSTRUCTION_SITES)
//        if (targets.isNotEmpty()) {
//            if (build(targets[0]) == ERR_NOT_IN_RANGE) {
//                moveTo(targets[0].pos)
//            }
//        }
//    } else {
//        val sources = room.find(FIND_SOURCES)
//        if (harvest(sources[0]) == ERR_NOT_IN_RANGE) {
//            moveTo(sources[0].pos)
//        }
//    }
//}
//
//
//fun Creep.harvest(fromRoom: Room = this.room, toRoom: Room = this.room) {
//    val sites = this.room.find(FIND_CONSTRUCTION_SITES)
//    val mainSpawn: StructureSpawn = Game.spawns.values.firstOrNull() ?: return
//    val targets = toRoom.find(FIND_MY_STRUCTURES)
//            .filter { (it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN) }
//            .filter { it.unsafeCast<EnergyContainer>().energy < it.unsafeCast<EnergyContainer>().energyCapacity }
//
//    if (memory.building && carry.energy == 0) {
//        memory.building = false
//        say("🔄 harvest")
//    }
//    if (!memory.building && carry.energy == carryCapacity) {
//        memory.building = true
//        say("🚧 build")
//    }
//
//    if (memory.building) {
//        if(mainSpawn.energy != mainSpawn.energyCapacity && targets.isNotEmpty()) {
//            if (transfer(targets[0], RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
//                    moveTo(targets[0].pos)
//            }
//
//        } else {
//            if (build(sites[0]) == ERR_NOT_IN_RANGE) {
//                moveTo(sites[0].pos)
//            }
//        }
//
//    } else {
//        val sources = room.find(FIND_SOURCES)
//        if (harvest(sources[0]) == ERR_NOT_IN_RANGE) {
//            moveTo(sources[0].pos)
//        }
//    }
//}

//fun Creep.harvest(fromRoom: Room = this.room, toRoom: Room = this.room) {
//    val sites = this.room.find(FIND_CONSTRUCTION_SITES)
//    val mainSpawn: StructureSpawn = Game.spawns.values.firstOrNull() ?: return
//    val targets = toRoom.find(FIND_MY_STRUCTURES)
//            .filter { (it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN) }
//            .filter { it.unsafeCast<EnergyContainer>().energy < it.unsafeCast<EnergyContainer>().energyCapacity }
//    if (carry.energy < carryCapacity) {
//        val sources = fromRoom.find(FIND_SOURCES)
//        if (harvest(sources[0]) == ERR_NOT_IN_RANGE) {
//            moveTo(sources[0].pos)
//        }
//    } else if(mainSpawn.energy != mainSpawn.energyCapacity){
//
//        if (targets.isNotEmpty()) {
//            if (transfer(targets[0], RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
//                moveTo(targets[0].pos)
//            }
//        }
//    } else {
//        if (build(sites[0]) == ERR_NOT_IN_RANGE) {
//            moveTo(sites[0].pos)
//        }
//    }
//}