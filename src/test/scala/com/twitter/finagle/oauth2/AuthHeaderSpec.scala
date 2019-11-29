package com.twitter.finagle.oauth2

import com.twitter.finagle.http.{HeaderMap, ParamMap}
import org.scalatest._
import matchers.should.Matchers._
import org.scalatest.matchers
import org.scalatest.flatspec.AnyFlatSpec

class AuthHeaderSpec extends AnyFlatSpec {

  def createRequest(authorization: Option[String]): Request.ProtectedResource = authorization match {
    case Some(s) =>
      new Request.ProtectedResource(HeaderMap("Authorization" -> s), ParamMap())
    case _ =>
      new Request.ProtectedResource(HeaderMap(), ParamMap())
  }

  it should "match AuthHeader" in {
    createRequest(Some("OAuth token1")).token shouldBe Some("token1")
    createRequest(Some("Bearer token1")).token shouldBe Some("token1")
  }

  it should "doesn't match AuthHeader" in {
    createRequest(None).token shouldBe None
    createRequest(Some("OAuth")).token shouldBe None
    createRequest(Some("OAtu token1")).token shouldBe None
    createRequest(Some("oauth token1")).token shouldBe None
    createRequest(Some("Bearer")).token shouldBe None
    createRequest(Some("Beare token1")).token shouldBe None
    createRequest(Some("bearer token1")).token shouldBe None
  }

  it should "fetch parameter from OAuth" in {
    val req = createRequest(Some("""OAuth access_token_value,algorithm="hmac-sha256",nonce="s8djwd",signature="wOJIO9A2W5mFwDgiDvZbTSMK%2FPY%3D",timestamp="137131200""""))
    req.token shouldBe Some("access_token_value")
  }

  it should "fetch parameter from Bearer" in {
    val req = createRequest(Some("""Bearer access_token_value,algorithm="hmac-sha256",nonce="s8djwd",signature="wOJIO9A2W5mFwDgiDvZbTSMK%2FPY%3D",timestamp="137131200""""))
    req.token shouldBe Some("access_token_value")
  }
}
