package de.drgn.digicomp1x

import org.python.core.PyFunction
import org.python.core.PyList
import org.python.core.PyString
import java.io.File
import org.python.util.PythonInterpreter

fun main(args: Array<String>) {
	val lines = File(args[0]).readLines().map { it.trim() }.filter { it.isNotEmpty() && (it[0] != ';' || it == ";;") }

	val registers = mutableListOf<Char>()

	val outputMethods = mutableSetOf<OutputMethod>()

	val initialState = mutableMapOf<Char, Boolean>()

	var cycles = 0

	val pythonInterpreter = PythonInterpreter()

	fun getReg(char: Char) = registers.indexOf(char).also { if (it == -1) throw Exception("Undeclared register $char") }

	var inComment = false

	for (line in lines) {
		if (line == ";;") {
			inComment = !inComment
			continue
		}
		if (inComment) continue
		if (line[0] != '#') break
		val args = line.substringAfter(' ').split(',').map { it.trim() }

		fun parseRegisters(it: String): List<Char> {
			return when {
				it.matches(Regex("[A-Za-z0-9]\\.\\.[A-Za-z0-9]")) -> {
					(it.first()..it.last()).toList()
				}

				it.matches(Regex("[A-Za-z0-9]+")) -> {
					it.toList()
				}

				else -> throw Exception("Cannot parse regs arg $it")
			}
		}

		when (val dir = line.drop(1).substringBefore(' ')) {
			"regs" -> {
				registers += args.flatMap {
					parseRegisters(it)
				}
			}

			"out" -> {
				outputMethods += OutputMethod.Number(
					when (args[0]) {
						"dec" -> OutputMethod.Number.Format.Dec
						"bin" -> OutputMethod.Number.Format.Bin
						"ascii" -> OutputMethod.Number.Format.Ascii
						else -> throw Exception("Unknown output format ${args[0]}")
					},
					parseRegisters(args[1]).map(::getReg)
						.also { if (it.isEmpty()) throw Exception("Cannot output 0 bits") },
					getReg(args[2].single())
				)
			}

			"pre" -> {
				args.forEach {
					val registers = parseRegisters(it.substringBefore('='))
					val value = it.substringAfter('=').toInt().toString(2).takeLast(registers.size)
						.padStart(registers.size, '0')
					registers.forEachIndexed { i, r ->
						initialState[r] = value.getOrNull(i) == '1'
					}
				}
			}

			"cycles" -> {
				cycles = args.single().toInt()
			}

			"macros" -> {
				args.forEach {
					pythonInterpreter.exec(File(it).readText())
				}
			}

			else -> throw Exception("Invalid directive $dir")
		}
	}
	val parts = lines.dropWhile { it.startsWith('#') }.flatMap {
		val macroGroups = Regex("""@(?<name>[a-zA-Z_][a-zA-Z_0-9]*)\((?<args>.*)\)""").matchEntire(it)?.groups
			?: return@flatMap listOf(it)
		val function = pythonInterpreter.get(macroGroups["name"]!!.value, PyFunction::class.java)
			?: throw Exception("Unknown macro '${macroGroups["name"]!!.value}'")
		val result = function.__call__(macroGroups["args"]!!.value.split(',').map {
			PyString(it.trim())
		}.toTypedArray())
		when (result) {
			is PyString -> listOf(result.string)
			is PyList -> result.map { it.toString() }
			else -> throw Exception("Unsupported function return type $result")
		}
	}.mapNotNull {
		if (it == ";;") {
			inComment = !inComment
			return@mapNotNull null
		}
		if (inComment) return@mapNotNull null
		if (it.isBlank()) return@mapNotNull null

		val groups =
			Regex("""(?<mode>res|set|pres|pset)\s+(?<register>[A-Za-z0-9])\s*(?:(?::|\s)\s*(?<condition>.*))?""").matchEntire(
				it
			)?.groups
				?: throw Exception("Cannot parse line $it")

		Line(
			groups["mode"]!!.value.let { Mode.entries.find { mode -> it == mode.name.lowercase() } }!!,
			getReg(groups["register"]!!.value.single()),
			parseCondition(groups["condition"]?.value ?: "", registers)
		)
	}.toSet()

	val program = Program(
		parts,
		registers.size,
		outputMethods
	)

	val previousControls = BooleanArray(outputMethods.size)
	var currentState = registers.map { initialState[it] ?: false }
	for (i in 0..<cycles) {
		currentState = clock(program, currentState)

		program.outputMethods.forEachIndexed { i, method ->
			val newControl = currentState[method.control]
			if (newControl != previousControls[i]) {
				previousControls[i] = newControl

				when (method) {
					is OutputMethod.Number -> {
						val sb = StringBuilder()
						method.bits.forEach {
							sb.append(if (currentState[it]) '1' else '0')
						}
						when (method.format) {
							OutputMethod.Number.Format.Ascii -> print(sb.toString().toInt(2).toChar())
							OutputMethod.Number.Format.Dec -> println(sb.toString().toInt(2))
							OutputMethod.Number.Format.Bin -> println(
								sb.toString().toInt(2).toString(2).padStart(method.bits.size, '0')
							)
						}
					}
				}
			}
		}
	}
}

typealias Condition = Map<Int, Boolean>

data class Line(val mode: Mode, val register: Int, val condition: Condition)
typealias State = List<Boolean>

data class Program(val lines: Set<Line>, val registers: Int, val outputMethods: Set<OutputMethod>)
enum class Mode(val setTo: Boolean, val isPull: Boolean = false) {
	Set(true), Res(false), PSet(true, true), PRes(false, true)
}

sealed class OutputMethod(val control: Int) {
	class Number(val format: Format, val bits: List<Int>, control: Int) : OutputMethod(control) {
		enum class Format {
			Ascii, Dec, Bin
		}
	}
}

fun parseCondition(condition: String, registers: List<Char>): Condition {
	if (condition.isBlank()) return emptyMap()
	val result = mutableMapOf<Int, Boolean>()
	var condition = condition.replace(Regex("\\s"), "")
	fun getReg(char: Char) = registers.indexOf(char).also { if (it == -1) throw Exception("Undeclared register $char") }
	while (condition.isNotEmpty()) {
		if (condition[0] == '!') {
			result[getReg(condition[1])] = false
			condition = condition.drop(2)
		} else {
			result[getReg(condition[0])] = true
			condition = condition.drop(1)
		}
	}
	return result
}

fun clock(program: Program, state: State): State {
	val newState = mutableMapOf<Int, Triple<Int, Boolean, Boolean>>()
	program.lines.forEachIndexed { i, it ->
		if (it.condition.all { (register, value) -> state[register] == value }) {
			val existing = newState[it.register]

			val action = run {
				if (existing == null) true
				else if (existing.second != it.mode.setTo) {
					if (existing.third == it.mode.isPull) false
					else if (it.mode.isPull) null
					else true
				} else null
			}

			if (action == true) {
				newState[it.register] = Triple(i, it.mode.setTo, it.mode.isPull)
			} else if (action == false) {
				throw Exception("Attempting to ${if (it.mode.setTo) "set" else "res"} in $i after ${if (existing!!.second) "setting" else "resetting"} in ${newState[it.register]?.first}")
			}
		}
	}
	return state.mapIndexed { i, b ->
		newState[i]?.second ?: b
	}
}