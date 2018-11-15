package com.twitter.finagle.oauth2

import com.twitter.finagle.http.{HeaderMap, ParamMap}
import org.scalatest._
import org.scalatest.Matchers._
import com.twitter.util.{Await, Future}

class AuthorizationCodeSpec extends FlatSpec {

  it should "handle request" in {
    val authorizationCode = GrantHandler.AuthorizationCode

    val request = new Request.Authorization(
      HeaderMap(),
      ParamMap(
        "code" -> "code1",
        "redirect_uri" -> "http://example.com/",
        "client_id" -> "clientId1",
        "client_secret" -> "clientSecret1"
      )
    )

    val grantHandlerResult = Await.result(authorizationCode.handle(request, new MockDataHandler() {
      override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[MockUser]]] =
        Future.value(Some(AuthInfo(user = MockUser(10000, "username"), clientId = "clientId1", scope = Some("all"), redirectUri = Some("http://example.com/"))))

      override def createAccessToken(authInfo: AuthInfo[MockUser]): Future[AccessToken] =
        Future.value(AccessToken("token1", Some("refreshToken1"), Some("all"), Some(3600), new java.util.Date()))
    }))
    grantHandlerResult.tokenType should be ("Bearer")
    grantHandlerResult.accessToken should be ("token1")
    grantHandlerResult.expiresIn should be (Some(3600))
    grantHandlerResult.refreshToken should be (Some("refreshToken1"))
    grantHandlerResult.scope should be (Some("all"))
  }

  it should "handle request if redirectUrl is none" in {
    val authorizationCode = GrantHandler.AuthorizationCode

    val request = new Request.Authorization(
      HeaderMap(),
      ParamMap(
        "code" -> "code1",
        "client_id" -> "clientId1",
        "client_secret" -> "clientSecret1"
      )
    )

    val grantHandlerResult = Await.result(authorizationCode.handle(request, new MockDataHandler() {
      override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[MockUser]]] =
        Future.value(Some(AuthInfo(user = MockUser(10000, "username"), clientId = "clientId1", scope = Some("all"), redirectUri = None)))

      override def createAccessToken(authInfo: AuthInfo[MockUser]): Future[AccessToken] =
        Future.value(AccessToken("token1", Some("refreshToken1"), Some("all"), Some(3600), new java.util.Date()))
    }))
    grantHandlerResult.tokenType should be ("Bearer")
    grantHandlerResult.accessToken should be ("token1")
    grantHandlerResult.expiresIn should be (Some(3600))
    grantHandlerResult.refreshToken should be (Some("refreshToken1"))
    grantHandlerResult.scope should be (Some("all"))
  }
}
