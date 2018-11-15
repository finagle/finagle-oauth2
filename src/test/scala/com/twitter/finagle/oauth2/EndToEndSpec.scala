package com.twitter.finagle.oauth2

import com.twitter.finagle.OAuth2
import com.twitter.finagle.http.{HeaderMap, ParamMap}
import com.twitter.util.{Await, Future}
import java.util.Date
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class EndToEndSpec extends FlatSpec {

  def successfulAccessTokenHandler(): MockDataHandler = new MockDataHandler() {
    override def validateClient(clientId: String, clientSecret: String, grantType: String): Future[Boolean] =
      Future.value(true)

    override def findUser(username: String, password: String): Future[Option[MockUser]] =
      Future.value(Some(MockUser(10000, "username")))

    override def createAccessToken(authInfo: AuthInfo[MockUser]): Future[AccessToken] =
      Future.value(AccessToken("token1", None, Some("all"), Some(3600), new Date()))
  }

  def successfulAuthorizeDataHandler(): MockDataHandler = new MockDataHandler() {
    override def findAccessToken(token: String): Future[Option[AccessToken]] =
      Future.value(Some(AccessToken("token1", Some("refreshToken1"), Some("all"), Some(3600), new Date())))

    override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[MockUser]]] =
      Future.value(Some(AuthInfo(user = MockUser(10000, "username"), clientId = "clientId1", scope = Some("all"), redirectUri = None)))
  }

  it should "be handled request with token into header" in {
    val request = new Request.ProtectedResource(
      HeaderMap("Authorization" -> "OAuth token1"),
      ParamMap("username" -> "user", "password" -> "pass", "scope" -> "all")
    )

    val dataHandler = successfulAuthorizeDataHandler()
    Await.result(OAuth2.authorize(request, dataHandler)) should not be (null)
  }

  it should "be handled request with token into body" in {
    val request = new Request.ProtectedResource(
      HeaderMap(),
      ParamMap("access_token" -> "token1", "username" -> "user", "password" -> "pass", "scope" -> "all")
    )

    val dataHandler = successfulAuthorizeDataHandler()
    Await.result(OAuth2.authorize(request, dataHandler)) should not be (null)
  }

  it should "be lost expired" in {
    val request = new Request.ProtectedResource(
      HeaderMap("Authorization" -> "OAuth token1"),
      ParamMap("username" -> "user", "password" -> "pass", "scope" -> "all")
    )

    val dataHandler = new MockDataHandler() {
      override def findAccessToken(token: String): Future[Option[AccessToken]] =
        Future.value(Some(AccessToken("token1", Some("refreshToken1"), Some("all"), Some(3600), new Date(new Date().getTime() - 4000 * 1000))))

      override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[MockUser]]] =
        Future.value(Some(AuthInfo(user = MockUser(10000, "username"), clientId = "clientId1", scope = Some("all"), redirectUri = None)))

    }

    intercept[ExpiredToken] {
      Await.result(OAuth2.authorize(request, dataHandler))
    }
  }

  it should "be invalid request without token" in {
    val request = new Request.ProtectedResource(
      HeaderMap(),
      ParamMap("username" -> "user", "password" -> "pass", "scope" -> "all")
    )

    val dataHandler = successfulAuthorizeDataHandler()
    intercept[InvalidRequest] {
      Await.result(OAuth2.authorize(request, dataHandler))
    }
  }

  it should "be handled request" in {
    val request = new Request.Authorization(
      HeaderMap("Authorization" -> "Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU="),
      ParamMap("grant_type" -> "password", "username" -> "user", "password" -> "pass", "scope" -> "all")
    )

    val dataHandler = successfulAccessTokenHandler()
    Await.result(OAuth2.issueAccessToken(request, dataHandler)) should not be (null)
  }

  it should "be error if grant type doesn't exist" in {
    val request = new Request.Authorization(
      HeaderMap("Authorization" -> "Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU="),
      ParamMap("username" -> "user", "password" -> "pass", "scope" -> "all")
    )

    val dataHandler = successfulAccessTokenHandler()
    intercept[InvalidRequest] {
      Await.result(OAuth2.issueAccessToken(request, dataHandler))
    }
  }

  it should "be error if grant type is wrong" in {
    val request = new Request.Authorization(
      HeaderMap("Authorization" -> "Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU="),
      ParamMap("grant_type" -> "test", "username" -> "user", "password" -> "pass", "scope" -> "all")
    )

    val dataHandler = successfulAccessTokenHandler()
    intercept[UnsupportedGrantType] {
      Await.result(OAuth2.issueAccessToken(request, dataHandler))
    }
  }

  it should "be invalid request without client credential" in {
    val request = new Request.Authorization(
      HeaderMap(),
      ParamMap("grant_type" -> "password", "username" -> "user", "password" -> "pass", "scope" -> "all")
    )

    val dataHandler = successfulAccessTokenHandler()
    intercept[InvalidRequest] {
      Await.result(OAuth2.issueAccessToken(request, dataHandler))
    }
  }

  it should "be invalid client if client information is wrong" in {
    val request = new Request.Authorization(
      HeaderMap("Authorization" -> "Basic Y2xpZW50X2lkX3ZhbHVlOmNsaWVudF9zZWNyZXRfdmFsdWU="),
      ParamMap("grant_type" -> "password", "username" -> "user", "password" -> "pass", "scope" -> "all")
    )

    val dataHandler = new MockDataHandler() {
      override def validateClient(clientId: String, clientSecret: String, grantType: String): Future[Boolean] =
        Future.value(false)
    }

    intercept[InvalidClient] {
      Await.result(OAuth2.issueAccessToken(request, dataHandler))
    }
  }
}
