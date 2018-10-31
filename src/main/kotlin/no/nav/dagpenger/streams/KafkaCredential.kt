package no.nav.dagpenger.streams

data class KafkaCredential(val username : String, val password: String) {
    override fun toString(): String {
        return "username '$username' password '*******'"
    }
}