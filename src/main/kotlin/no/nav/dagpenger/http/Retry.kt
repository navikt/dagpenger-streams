package no.nav.dagpenger.http

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import mu.KotlinLogging
import org.slf4j.Logger

private val LOGGER = KotlinLogging.logger {}

fun <T : Any> retryFuelHttp(
    times: Int = 5,
    initialDelay: Long = 500,
    maxDelay: Long = 5000,
    factor: Double = 2.0,
    allowedErrorCodes: List<Int> = emptyList(),
    function: () -> Triple<Request, Response, Result<T, FuelError>>
): Triple<Request, Response, Result<T, FuelError>>
{
    var currentDelay = initialDelay
    repeat(times - 1) {

        val res = function()
        val (_, response, result) = res
        when {
            result is Result.Success -> return res
            result is Result.Failure && response.statusCode in allowedErrorCodes -> return res
            result is Result.Failure -> LOGGER.warn(result.getException())
        }

        LOGGER.info("Retrying in $currentDelay ms")
        Thread.sleep(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return function()
}

private fun Logger.warn(exception: Throwable) = error(exception.message ?: "Exception of type ${exception.javaClass}", exception)
