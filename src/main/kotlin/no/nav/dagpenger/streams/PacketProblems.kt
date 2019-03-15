package no.nav.dagpenger.streams

class PacketProblem(val originalJson: String, message: String) : RuntimeException() {

    override fun toString(): String {
        val results = StringBuffer()
        results.append("Errors and/or messages exist. Original JSON string is:\n\t")
        results.append(originalJson)
        results.append("Errors $message")
        results.append("\n")
        return results.toString()
    }
}