package com.twitter.finagle.oauth2

import java.util.Date
import com.twitter.util.Future

case class MockUser(id: Long, name: String)

class MockDataHandler extends DataHandler[MockUser] {

  def validateClient(clientId: String, clientSecret: String, grantType: String): Future[Boolean] =
    Future.value(false)

  def findUser(username: String, password: String): Future[Option[MockUser]] =
    Future.value(None)

  def createAccessToken(authInfo: AuthInfo[MockUser]): Future[AccessToken] =
    Future.value(AccessToken("", Some(""), Some(""), Some(0L), new Date()))

  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[MockUser]]] =
    Future.value(None)

  def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[MockUser]]] =
    Future.value(None)

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Future[Option[MockUser]] =
    Future.value(None)

  def findAccessToken(token: String): Future[Option[AccessToken]] =
    Future.value(None)

  def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[MockUser]]] =
    Future.value(None)

  def getStoredAccessToken(authInfo: AuthInfo[MockUser]): Future[Option[AccessToken]] =
    Future.value(None)

  def refreshAccessToken(authInfo: AuthInfo[MockUser], refreshToken: String): Future[AccessToken] =
    Future.value(AccessToken("", Some(""), Some(""), Some(0L), new Date()))
}
