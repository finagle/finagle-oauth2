package com.twitter.oauth2

import com.twitter.util.Future

case class GrantHandlerResult(tokenType: String, accessToken: String, expiresIn: Option[Long], refreshToken: Option[String], scope: Option[String])

trait GrantHandler {

  def handleRequest[U](request: AuthorizationRequest, dataHandler: DataHandler[U]): Future[GrantHandlerResult]

  /**
   * Returns valid access token.
   */
  def issueAccessToken[U](dataHandler: DataHandler[U], authInfo: AuthInfo[U]): Future[GrantHandlerResult] = for {
    tokenOption <- dataHandler.getStoredAccessToken(authInfo)
    token <- tokenOption match {
      case Some(t) if dataHandler.isAccessTokenExpired(t) =>
        val refreshToken = t.refreshToken map { dataHandler.refreshAccessToken(authInfo, _) }
        refreshToken.getOrElse(dataHandler.createAccessToken(authInfo))
      case Some(t) => Future.value(t)
      case None => dataHandler.createAccessToken(authInfo)
    }
  } yield GrantHandlerResult(
    "Bearer",
    token.token,
    token.expiresIn,
    token.refreshToken,
    token.scope
  )
}

class RefreshToken(clientCredentialFetcher: ClientCredentialFetcher) extends GrantHandler {

  override def handleRequest[U](request: AuthorizationRequest, dataHandler: DataHandler[U]): Future[GrantHandlerResult] = {
    val clientCredential = clientCredentialFetcher.fetch(request) match {
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
    } yield GrantHandlerResult(
      "Bearer",
      token.token,
      token.expiresIn,
      token.refreshToken,
      token.scope
    )
  }
}

class Password(clientCredentialFetcher: ClientCredentialFetcher) extends GrantHandler {

  override def handleRequest[U](request: AuthorizationRequest, dataHandler: DataHandler[U]): Future[GrantHandlerResult] = {
    val clientCredential = clientCredentialFetcher.fetch(request) match {
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

class ClientCredentials(clientCredentialFetcher: ClientCredentialFetcher) extends GrantHandler {

  override def handleRequest[U](request: AuthorizationRequest, dataHandler: DataHandler[U]): Future[GrantHandlerResult] = {
    val clientCredential = clientCredentialFetcher.fetch(request) match {
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

class AuthorizationCode(clientCredentialFetcher: ClientCredentialFetcher) extends GrantHandler {

  override def handleRequest[U](request: AuthorizationRequest, dataHandler: DataHandler[U]): Future[GrantHandlerResult] = {
    val clientCredential = clientCredentialFetcher.fetch(request) match {
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
          if (i.clientId != credential.clientId) Future.exception(new InvalidClient())
          else if (i.redirectUri.isDefined && i.redirectUri != redirectUri) Future.exception(throw new RedirectUriMismatch())
          else Future.value(i)
        case None => Future.exception(new InvalidGrant())
      }
      token <- issueAccessToken(dataHandler, info)
    } yield token
  }
}
