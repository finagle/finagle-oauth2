package com.twitter.finagle.oauth2

import com.twitter.util.Future

sealed abstract class GrantHandler {
  def handle[U](
    request: Request.Authorization,
    dataHandler: DataHandler[U]
  ): Future[GrantResult]

  protected def issueAccessToken[U](
    dataHandler: DataHandler[U],
    authInfo: AuthInfo[U]
  ): Future[GrantResult] = for {
    tokenOption <- dataHandler.getStoredAccessToken(authInfo)
    token <- tokenOption match {
      case Some(t) if dataHandler.isAccessTokenExpired(t) =>
        val refreshToken = t.refreshToken map { dataHandler.refreshAccessToken(authInfo, _) }
        refreshToken.getOrElse(dataHandler.createAccessToken(authInfo))
      case Some(t) => Future.value(t)
      case None => dataHandler.createAccessToken(authInfo)
    }
  } yield GrantResult(
    "Bearer",
    token.token,
    token.expiresIn,
    token.refreshToken,
    token.scope
  )
}

object GrantHandler {

  def fromGrantType(grantType: String): Option[GrantHandler] = grantType match {
    case "authorization_code" => Some(AuthorizationCode)
    case "refresh_token" => Some(RefreshToken)
    case "client_credentials" => Some(ClientCredentials)
    case "password" => Some(Password)
    case _ => None
  }

  object RefreshToken extends GrantHandler {
    def handle[U](
      request: Request.Authorization,
      dataHandler: DataHandler[U]
    ): Future[GrantResult] = {
      val clientCredential = request.clientCredential match {
        case Some(c) => Future.value(c)
        case None => Future.exception(new InvalidRequest("BadRequest"))
      }

      val refreshToken = request.requireRefreshToken

      for {
        credential <- clientCredential
        infoOption <- dataHandler.findAuthInfoByRefreshToken(refreshToken)
        info <- infoOption match {
          case Some(i) =>
            if (i.clientId != credential.clientId) Future.exception(new InvalidClient())
            else Future.value(i)
          case None => Future.exception(new InvalidGrant("NotFound"))
        }
        token <- dataHandler.refreshAccessToken(info, refreshToken)
      } yield GrantResult(
        "Bearer",
        token.token,
        token.expiresIn,
        token.refreshToken,
        token.scope
      )
    }
  }

  object Password extends GrantHandler {

    def handle[U](
      request: Request.Authorization,
      dataHandler: DataHandler[U]
    ): Future[GrantResult] = {
      val clientCredential = request.clientCredential match {
        case Some(c) => Future.value(c)
        case None => Future.exception(new InvalidRequest("BadRequest"))
      }

      val username = request.requireUsername
      val password = request.requirePassword
      val scope = request.scope

      for {
        credential <- clientCredential
        userOption <- dataHandler.findUser(username, password)
        user <- userOption match {
          case Some(u) => Future.value(u)
          case None => Future.exception(new InvalidGrant())
        }
        token <- issueAccessToken(dataHandler, AuthInfo(user, credential.clientId, scope, None))
      } yield token
    }
  }

  object ClientCredentials extends GrantHandler {

    def handle[U](
      request: Request.Authorization,
      dataHandler: DataHandler[U]
    ): Future[GrantResult] = {
      val clientCredential = request.clientCredential match {
        case Some(c) => Future.value(c)
        case None => Future.exception(new InvalidRequest("BadRequest"))
      }

      val scope = request.scope

      for {
        credential <- clientCredential
        userOption <- dataHandler.findClientUser(credential.clientId, credential.clientSecret, scope)
        user <- userOption match {
          case Some(u) => Future.value(u)
          case None => Future.exception(new InvalidGrant())
        }
        token <- issueAccessToken(dataHandler, AuthInfo(user, credential.clientId, scope, None))
      } yield token
    }
  }

  object AuthorizationCode extends GrantHandler {

    def handle[U](
      request: Request.Authorization,
      dataHandler: DataHandler[U]
    ): Future[GrantResult] = {
      val clientCredential = request.clientCredential match {
        case Some(c) => Future.value(c)
        case None => Future.exception(new InvalidRequest("BadRequest"))
      }

      val code = request.requireCode
      val redirectUri = request.redirectUri

      for {
        credential <- clientCredential
        infoOption <- dataHandler.findAuthInfoByCode(code)
        info <- infoOption match {
          case Some(i) =>
            if (i.clientId != credential.clientId)
              Future.exception(new InvalidClient())
            else if (i.redirectUri.isDefined && i.redirectUri != redirectUri)
              Future.exception(new RedirectUriMismatch())
            else Future.value(i)
          case None => Future.exception(new InvalidGrant())
        }
        token <- issueAccessToken(dataHandler, info)
      } yield token
    }
  }
}
