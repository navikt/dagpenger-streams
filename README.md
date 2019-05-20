[![CircleCI](https://circleci.com/gh/navikt/dagpenger-streams.svg?style=svg)](https://circleci.com/gh/navikt/dagpenger-streams)

[![](https://jitpack.io/v/navikt/dagpenger-streams.svg)](https://jitpack.io/#navikt/dagpenger-streams)

# Streams

A tiny convenience library for building Kafka Streams based services.

## Topic definitions

Available Topics are defined in the Topics singleton with their topic name and
serdes for key and value.

## Kafka Streams Service template

It provides an `Service` interface that can be enriched with the `BlurgObject`
for making it easier to build new `Services`.

A template for new services can be found in [/examples/StreamsApp.kt](/examples/StreamsApp.kt).
