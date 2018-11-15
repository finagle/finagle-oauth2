package com.twitter.finagle.oauth2

import com.twitter.finagle.http.{HeaderMap, ParamMap}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import com.twitter.util.{Await, Future}

class RefreshTokenSpec extends FlatSpec {

  it should "handle request" in {
    val refreshToken = GrantHandler.RefreshToken

    val request = new Request.Authorization(
      HeaderMap(),
      ParamMap(
        "refresh_token" -> "refreshToken1",
        "client_id" -> "clientId1",
        "client_secret" -> "clientSecret1"
      )
    )

    val grantHandlerResult = Await.result(refreshToken.handle(request, new MockDataHandler() {
      override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[MockUser]]] =
        Future.value(Some(AuthInfo(user = MockUser(10000, "username"),
                                   clientId = "clientId1", scope = None, redirectUri = None)))

      override def refreshAccessToken(authInfo: AuthInfo[MockUser], refreshToken: String): Future[AccessToken] =
        Future.value(AccessToken("token1", Some(refreshToken), None, Some(3600), new java.util.Date()))
    }))

    grantHandlerResult.tokenType should be ("Bearer")
    grantHandlerResult.accessToken should be ("token1")
    grantHandlerResult.expiresIn should be (Some(3600))
    grantHandlerResult.refreshToken should be (Some("refreshToken1"))
    grantHandlerResult.scope should be (None)
  }
}
