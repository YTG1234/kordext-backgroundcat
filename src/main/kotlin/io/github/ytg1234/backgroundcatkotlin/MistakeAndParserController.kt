package io.github.ytg1234.backgroundcatkotlin

import io.github.ytg1234.backgroundcatkotlin.util.internal.ConfigHolder
import io.github.ytg1234.backgroundcatkotlin.util.internal.logger

enum class Severity(val s: String) {
    NoSupport("❌"),
    Severe("!!"),
    Important("❗"),
    Warn("⚠")
}

enum class LogSource {
    MultiMc, NotMultiMc
}

private fun sourceFromLog(text: String): LogSource {
    return if (text.startsWith("MultiMC version")) LogSource.MultiMc
    else LogSource.NotMultiMc
}

data class Mistake(val severity: Severity, val message: String)

data class Log(val source: LogSource, val text: String) : CharSequence by text

fun interface Parser {
    fun parse(log: Log): Mistake?
    operator fun invoke(log: Log) = parse(log)
}

val parsers = mutableMapOf<String, Parser>()
val blockingParsers = mutableMapOf<String, Parser>()

fun addParser(id: String, parser: Parser) {
    if (id == "") throw IllegalArgumentException("Tried to add a parser for empty ID!")
    if (parsers[id] != null) throw IllegalArgumentException("Tried to add a parser for ID $id which was already added!")

    if (!ConfigHolder.isParserEnabled(id)) {
        logger.debug("Not adding parser with ID $id because it is not enabled.")
        return
    }

    parsers[id] = parser
}

fun withParser(id: String, parser: Log.() -> Mistake?) = addParser(id) { it.parser() }

fun addBlocking(id: String, parser: Parser) {
    if (id == "") throw IllegalArgumentException("Tried to add a blocking parser for empty ID!")
    if (parsers[id] != null) throw IllegalArgumentException("Tried to add a blocking parser for ID $id which was already added!")

    if (!ConfigHolder.isBlockingParserEnabled(id)) {
        logger.debug("Not adding blocking parser with ID $id because it is not enabled.")
        return
    }

    blockingParsers[id] = parser
}

fun withBlocking(id: String, parser: Log.() -> Mistake?) = addBlocking(id) { it.parser() }

fun mistakesFromLog(text: String): List<Mistake> {
    val mistakes = mutableListOf<Mistake>()
    val source = sourceFromLog(text)
    val log = Log(source, text)

    for ((_, parser) in blockingParsers) {
        val mistake = parser(log) ?: continue
        mistakes.add(mistake)
        return mistakes // Blocking parsers block other parsers from operating
    }

    for ((_, parser) in parsers) {
        val mistake = parser(log) ?: continue
        mistakes.add(mistake)
    }

    return mistakes
}
