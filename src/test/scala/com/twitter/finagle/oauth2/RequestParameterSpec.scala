package com.twitter.finagle.oauth2

import com.twitter.finagle.http.{HeaderMap, ParamMap}
import matchers.should.Matchers._
import org.scalatest.matchers
import org.scalatest.flatspec.AnyFlatSpec

class RequestParameterSpec extends AnyFlatSpec {

  def createRequest(
    oauthToken: Option[String],
    accessToken: Option[String],
    another: Seq[(String, String)] = Seq.empty
  ): Request.ProtectedResource = {
    val params =
      oauthToken.fold(Seq.empty[(String, String)])(t => Seq("oauth_token" -> t)) ++
      accessToken.fold(Seq.empty[(String, String)])(t => Seq("access_token" -> t)) ++
      another

    new Request.ProtectedResource(HeaderMap(), ParamMap(params: _*))
  }

  it should "match RequestParameter" in {
    createRequest(Some("token1"), None).token shouldBe Some("token1")
    createRequest(None, Some("token2")).token shouldBe Some("token2")
    createRequest(Some("token1"), Some("token2")).token shouldBe Some("token1")
  }

  it should "doesn't match RequestParameter" in {
    createRequest(None, None).token shouldBe None
  }
}
