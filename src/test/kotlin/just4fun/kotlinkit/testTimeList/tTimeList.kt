package just4fun.kotlinkit.testTimeList

import just4fun.kotlinkit.logL
import java.util.*


var NEXT = 0
val rx = Regex("""(\d*)(\D+)(\d*)""")
val commands = StringBuilder()
val list = TList()

fun main(args: Array<String>) {
	startConsole()
}


fun startConsole() {
	val scanner = Scanner(System.`in`)
	var go = true
	while (go) go = runCommand(scanner.nextLine())
}

fun runCommand(cmd: String): Boolean {
	if (cmd.startsWith("> ")) {
		cmd.splitToSequence(' ').drop(1).forEach { println("$it");runCommand(it) }
		return !commands.endsWith('x') && !commands.endsWith("x ")
	}
	val parts = rx.matchEntire(cmd)?.groupValues
	if (parts == null) {
		println("-> !!! Unrecognized command: $cmd"); return true
	}
	val com = parts[0]
	//	println("${parts[1]}${parts[2]}${parts[3]}")
	val s = parts[2]
	val n1 = parts[1].run { if (isEmpty()) -1 else toInt() }
	val n2 = parts[3].run { if (isEmpty()) -1 else toInt() }
	commands.append(parts[0]).append(" ")
	when (s) {
		"a" -> {
			list.add(if (n2 >= 0) n2 else NEXT++); logL(1, "LIST", list.dump())
		}
		"mCall" -> {
			if (n2 >= 0) list.remove(n2, if (n1 < 0) 0 else n1) else list.remove(); logL(1, "LIST", list.dump())
		}
		"c" -> {
			if (n2 >= 0) list.removeAfter(n2.toLong()) {} else list.clear { }; logL(1, "LIST", list.dump())
		}
		"x" -> run { logL(1, "COMMANDS", "${commands}"); return false }
	}
	return true
}
//val list = TList()
//println("$list\n${list.dump()}")
//val it0 = list.add(NEXT++)
//println("$list\n${list.dump()}")
//val it1 = list.add(NEXT++)
//println("$list\n${list.dump()}")
//val it2 = list.add(NEXT++)
//println("$list\n${list.dump()}")
//val it3 = list.add(NEXT++)
//println("$list\n${list.dump()}")
//list.remove(it1)
//println("$list\n${list.dump()}")
//list.removeAfter(2){println(" -    $it")}
//println("$list\n${list.dump()}")
//list.removeAfter(0){println(" -    $it")}
//println("$list\n${list.dump()}")


class TList: TimeList<TItem>() {
	val map = mutableMapOf<Int, MutableList<TItem>>()
	fun add(n: Int) {
		val list = map.getOrPut(n, { mutableListOf() })
		val item = TItem(n.toLong())
		list += item
		add(item)
	}
	
	fun remove(n: Int, index: Int) {
		val list = map[n]
		if (list == null || index >= list.size) return
		val e = list?.removeAt(index)
		if (e != null) remove(e)
	}
	
	override fun toString(): String = toList().joinToString(",")
}

class TItem(override val expires: Long): ListElement<TItem> {
	override var next: TItem? = null
	override fun hashCode(): Int = expires.toInt()
	override fun toString(): String = expires.toString()
}