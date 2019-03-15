package no.nav.dagpenger.streams

class PacketProblems(val originalJson: String) : RuntimeException() {

    private val errors = mutableListOf<String>()

    fun hasErrors() = errors.isNotEmpty()

    infix fun error(explanation: String) {
        errors.add(explanation)
    }

    override fun toString(): String {
        if (errors.isEmpty()) return "No errors detected in JSON:\n\t$originalJson"
        val results = StringBuffer()
        results.append("Errors and/or messages exist. Original JSON string is:\n\t")
        results.append(originalJson)
        append("Errors", errors, results)
        results.append("\n")
        return results.toString()
    }

    private fun append(label: String, messages: List<String>, results: StringBuffer) {
        if (messages.isEmpty()) return
        results.append("\n")
        results.append(label)
        results.append(": ")
        results.append(messages.size)
        for (message in messages) {
            results.append("\n\t")
            results.append(message)
        }
    }
}