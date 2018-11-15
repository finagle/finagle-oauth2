package com.twitter.finagle.oauth2

import com.twitter.finagle.http.{HeaderMap, ParamMap}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class ClientCredentialFetcherSpec extends FlatSpec {

  it should "fetch Basic64" in {
    val request = new Request.Authorization(
      HeaderMap("Authorization" -> "Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU="),
      ParamMap()
    )

    val Some(c) = request.clientCredential
    c.clientId should be ("client_id_value")
    c.clientSecret should be ("client_secret_value")
  }

  it should "fetch Basic64 by case insensitive" in {
    val request = new Request.Authorization(
      HeaderMap("authorization" -> "Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU="),
      ParamMap()
    )

    val Some(c) = request.clientCredential
    c.clientId should be ("client_id_value")
    c.clientSecret should be ("client_secret_value")
  }

  it should "fetch empty client_secret" in {
    val request = new Request.Authorization(
      HeaderMap("Authorization" -> "Basic Y2xpZW50X2lkX3ZhbHVlOg=="),
      ParamMap()
    )

    val Some(c) = request.clientCredential
    c.clientId should be ("client_id_value")
    c.clientSecret should be ("")
  }

  it should "not fetch no Authorization key in header" in {
    val request = new Request.Authorization(
      HeaderMap("authorizatio" -> "Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU="),
      ParamMap()
    )

    request.clientCredential should be (None)
  }

  it should "not fetch invalidate Base64" in {
    val request = new Request.Authorization(
      HeaderMap("Authorization" -> "Basic basic"),
      ParamMap()
    )

    request.clientCredential should be (None)
  }

  it should "fetch parameter" in {
    val request = new Request.Authorization(
      HeaderMap(),
      ParamMap("client_id" -> "client_id_value", "client_secret" -> "client_secret_value")
    )

    val Some(c) = request.clientCredential
    c.clientId should be ("client_id_value")
    c.clientSecret should be ("client_secret_value")
  }

  it should "omit client_secret" in {
    val request = new Request.Authorization(
      HeaderMap(),
      ParamMap("client_id" -> "client_id_value")
    )

    val Some(c) = request.clientCredential
    c.clientId should be ("client_id_value")
    c.clientSecret should be ("")
  }

  it should "not fetch missing parameter" in {
    val request = new Request.Authorization(
      HeaderMap(),
      ParamMap("client_secret" -> "client_secret_value")
    )

    request.clientCredential should be (None)
  }

  it should "not fetch invalid parameter" in {
    val request = new Request.Authorization(HeaderMap("Authorization" -> ""), ParamMap())
    request.clientCredential should be (None)
  }
}
