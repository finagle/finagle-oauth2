package com.twitter.finagle.oauth2

import org.scalatest._
import org.scalatest.Matchers._
import com.twitter.util.{Await, Future}

class PasswordSpec extends FlatSpec {

  it should "handle request" in {
    val password = new Password(new MockClientCredentialFetcher())
    val request = AuthorizationRequest(Map(), Map("username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all")))
    val grantHandlerResult = Await.result(password.handleRequest(request, new MockDataHandler() {
      override def findUser(username: String, password: String): Future[Option[MockUser]] =
        Future.value(Some(MockUser(10000, "username")))

      override def createAccessToken(authInfo: AuthInfo[MockUser]): Future[AccessToken] =
        Future.value(AccessToken("token1", Some("refreshToken1"), Some("all"), Some(3600), new java.util.Date()))
    }))
    grantHandlerResult.tokenType should be ("Bearer")
    grantHandlerResult.accessToken should be ("token1")
    grantHandlerResult.expiresIn should be (Some(3600))
    grantHandlerResult.refreshToken should be (Some("refreshToken1"))
    grantHandlerResult.scope should be (Some("all"))
  }

  class MockClientCredentialFetcher extends ClientCredentialFetcher {
    override def fetch(request: AuthorizationRequest): Option[ClientCredential] = Some(ClientCredential("clientId1", "clientSecret1"))
  }
}
