package no.nav.dagpenger.streams

import java.lang.RuntimeException

class PacketException(val packet: Packet) : RuntimeException() {}