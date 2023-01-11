![Build and deploy](https://github.com/navikt/dagpenger-streams/workflows/Build%20and%20deploy/badge.svg)

[![](https://jitpack.io/v/navikt/dagpenger-streams.svg)](https://jitpack.io/#navikt/dagpenger-streams)


# Deprekert

Er i bruk av noen av dagpenger prosjektene. 
Skal ikke brukes av nye apper, rapid & rivers i stedet - https://github.com/navikt/rapids-and-rivers")

# Streams

A tiny convenience library for building Kafka Streams based services.

## Topic definitions

Available Topics are defined in the Topics singleton with their topic name and
serdes for key and value.

## Kafka Streams Service template

It provides an `Service` interface that can be enriched with the `BlurgObject`
for making it easier to build new `Services`.

A template for new services can be found in [/examples/StreamsApp.kt](/examples/StreamsApp.kt).
