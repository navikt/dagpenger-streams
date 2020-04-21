package no.nav.dagpenger.streams

import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Predicate

// https://stackoverflow.com/a/48048516/10075690
fun <K, V> KStream<K, V>.kbranch(vararg predicates: (K, V) -> Boolean): Array<KStream<K, V>> {
    val arguments = predicates.map { Predicate { key: K, value: V -> it(key, value) } }
    return this.branch(*arguments.toTypedArray())
}
