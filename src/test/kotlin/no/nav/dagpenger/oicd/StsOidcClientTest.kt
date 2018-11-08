package no.nav.dagpenger.oicd

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.RegexPattern
import no.nav.dagpenger.oidc.OidcToken
import no.nav.dagpenger.oidc.StsOidcClient
import no.nav.dagpenger.oidc.StsOidcClientException
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class StsOidcClientTest {

    @Rule
    @JvmField
    var wireMockRule = WireMockRule(wireMockConfig().dynamicPort())

    private val expires = 300L

    @Test
    fun `fetch open id token from sts server`() {

        stubFor(WireMock.get(WireMock.urlEqualTo("/rest/v1/sts/token/?grant_type=client_credentials&scope=openid"))
                .withHeader("Authorization", RegexPattern("Basic\\s[a-zA-Z0-9]*="))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body())
                )
        )

        val stsOidcClient = StsOidcClient(wireMockRule.url(""), "username", "password")
        val oidcToken: OidcToken = stsOidcClient.oidcToken()

        assertEquals(oidcToken, OidcToken("token", "openid", expires))
    }

    @Test
    fun `fetch open id token from sts server and token is cached `() {

        stubFor(WireMock.get(WireMock.urlEqualTo("/cached/rest/v1/sts/token/?grant_type=client_credentials&scope=openid"))
                .withHeader("Authorization", RegexPattern("Basic\\s[a-zA-Z0-9]*="))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body())
                )
        )
        val stsOidcClient = StsOidcClient(wireMockRule.url("cached"), "username", "password")

        val firstCall: OidcToken = stsOidcClient.oidcToken()

        assertEquals(firstCall, OidcToken("token", "openid", expires))

        val secondCall: OidcToken = stsOidcClient.oidcToken()

        assertEquals(secondCall, OidcToken("token", "openid", expires))

        verify(exactly(1), getRequestedFor(urlEqualTo("/cached/rest/v1/sts/token/?grant_type=client_credentials&scope=openid"))
                .withHeader("Authorization", RegexPattern("Basic\\s[a-zA-Z0-9]*=")))
    }

    @Test(expected = StsOidcClientException::class)
    fun `fetch open id token from sts on server error`() {

        stubFor(WireMock.get(WireMock.urlEqualTo("/rest/v1/sts/token/?grant_type=client_credentials&scope=openid"))
            .withHeader("Authorization", RegexPattern("Basic\\s[a-zA-Z0-9]*="))
            .willReturn(WireMock.serverError()
                .withHeader("Content-Type", "text/plain")
                .withBody("FAILED")
            )
        )

        val stsOidcClient = StsOidcClient(wireMockRule.url(""), "username", "password")
        stsOidcClient.oidcToken()
    }

    fun body() = """
                {
                    "access_token": "token",
                    "token_type" : "openid",
                    "expires_in" : $expires
                }

            """.trimIndent()
}