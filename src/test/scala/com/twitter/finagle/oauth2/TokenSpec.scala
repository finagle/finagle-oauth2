package com.twitter.finagle.oauth2

import org.scalatest._
import org.scalatest.Matchers._
import com.twitter.util.{Await, Future}

class TokenSpec extends FlatSpec {

  def successfulDataHandler() = new MockDataHandler() {
    override def validateClient(clientId: String, clientSecret: String, grantType: String): Future[Boolean] =
      Future.value(true)

    override def findUser(username: String, password: String): Future[Option[MockUser]] =
      Future.value(Some(MockUser(10000, "username")))

    override def createAccessToken(authInfo: AuthInfo[MockUser]): Future[AccessToken] =
      Future.value(AccessToken("token1", None, Some("all"), Some(3600), new java.util.Date()))
  }

  it should "be handled request" in {
    val request = AuthorizationRequest(
      Map("Authorization" -> Seq("Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU=")),
      Map("grant_type" -> Seq("password"), "username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all"))
    )

    val dataHandler = successfulDataHandler()
    Await.result(TokenEndpoint.handleRequest(request, dataHandler)) should not be (null)
  }

  it should "be error if grant type doesn't exist" in {
    val request = AuthorizationRequest(
      Map("Authorization" -> Seq("Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU=")),
      Map("username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all"))
    )

    val dataHandler = successfulDataHandler()
    intercept[InvalidRequest] {
      Await.result(TokenEndpoint.handleRequest(request, dataHandler))
    }
  }

  it should "be error if grant type is wrong" in {
    val request = AuthorizationRequest(
      Map("Authorization" -> Seq("Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU=")),
      Map("grant_type" -> Seq("test"), "username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all"))
    )

    val dataHandler = successfulDataHandler()
    intercept[UnsupportedGrantType] {
      Await.result(TokenEndpoint.handleRequest(request, dataHandler))
    }
  }

  it should "be invalid request without client credential" in {
    val request = AuthorizationRequest(
      Map(),
      Map("grant_type" -> Seq("password"), "username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all"))
    )

    val dataHandler = successfulDataHandler()
    intercept[InvalidRequest] {
      Await.result(TokenEndpoint.handleRequest(request, dataHandler))
    }
  }

  it should "be invalid client if client information is wrong" in {
    val request = AuthorizationRequest(
      Map("Authorization" -> Seq("Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU=")),
      Map("grant_type" -> Seq("password"), "username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all"))
    )

    val dataHandler = new MockDataHandler() {
      override def validateClient(clientId: String, clientSecret: String, grantType: String): Future[Boolean] =
        Future.value(false)
    }

    intercept[InvalidClient] {
      Await.result(TokenEndpoint.handleRequest(request, dataHandler))
    }
  }
}
