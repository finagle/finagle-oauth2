package com.twitter.finagle.oauth2

import com.twitter.finagle.http.{HeaderMap, ParamMap}
import org.scalatest._
import matchers.should.Matchers._
import com.twitter.util.{Await, Future}
import org.scalatest.matchers
import org.scalatest.flatspec.AnyFlatSpec

class ClientCredentialsSpec extends AnyFlatSpec {

  it should "handle request" in {
    val clientCredentials = GrantHandler.ClientCredentials

    val request = new Request.Authorization(
      HeaderMap(),
      ParamMap(
        "scope" -> "all",
        "client_id" -> "clientId1",
        "client_secret" -> "clientSecret1"
      )
    )

    val grantHandlerResult = Await.result(clientCredentials.handle(request, new MockDataHandler() {
      override def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Future[Option[MockUser]] =
        Future.value(Some(MockUser(10000, "username")))

      override def createAccessToken(authInfo: AuthInfo[MockUser]): Future[AccessToken] =
        Future.value(AccessToken("token1", None, Some("all"), Some(3600), new java.util.Date()))
    }))

    grantHandlerResult.tokenType should be ("Bearer")
    grantHandlerResult.accessToken should be ("token1")
    grantHandlerResult.expiresIn should be (Some(3600))
    grantHandlerResult.refreshToken should be (None)
    grantHandlerResult.scope should be (Some("all"))
  }
}
