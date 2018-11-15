package com.twitter.finagle

import com.twitter.util.Future
import com.twitter.finagle.oauth2._

/**
 * An entry point API to enable OAuth2 in Finagle services (server-side).
 *
 * Issuing a token:
 *
 * {{{
 *   import com.twitter.finagle.OAuth2
 *   import com.twitter.finagle.oauth2.{DataHandler, Grant}
 *   import com.twitter.finagle.http.Request
 *   import com.twitter.util.Future
 *
 *   val dataHandler: DataHandler[?] = ???
 *   val request: Request = ??? // contains client credentials
 *
 *   val grant: Future[Grant] = OAuth2.issueAccessToken(request, dataHandler)
 * }}}
 *
 * Authorizing a request:
 *
 * {{{
 *    import com.twitter.finagle.OAuth2
 *    import com.twitter.finagle.oauth2.{AuthInfo, DataHandler}
 *    import com.twitter.finagle.http.Request
 *    import com.twitter.util.Future
 *
 *    val dataHandler: DataHandler[?] = ???
 *    val request: Request = ??? // contains token
 *
 *    val authInfo: AuthInfo[?] = OAuth2.authorize(request, dataHandler)
 * }}}
 *
 * Note both `authorize` and `issueAccessToken` may resolve into `Future.exception` containing
 * [[OAuthError]], which could be converted into a barebones HTTP response via `.toResponse`:
 */
trait OAuth2 {

  def issueAccessToken[U](
    request: http.Request,
    dataHandler: DataHandler[U]
  ): Future[GrantResult] =
    issueAccessToken(new Request.Authorization(request.headerMap, request.params), dataHandler)

  def issueAccessToken[U](
    request: Request.Authorization,
    dataHandler: DataHandler[U]
  ): Future[GrantResult] = for {
    grantType <- request.grantType match {
      case Some(t) => Future.value(t)
      case None => Future.exception(new InvalidRequest("grant_type not found"))
    }
    handler <- GrantHandler.fromGrantType(grantType) match {
      case Some(h) => Future.value(h)
      case None => Future.exception(new UnsupportedGrantType("the grant_type isn't supported"))
    }
    credential <- request.clientCredential match {
      case Some(c) => Future.value(c)
      case None => Future.exception(new InvalidRequest("client credential not found"))
    }
    validated <- dataHandler.validateClient(credential.clientId, credential.clientSecret, grantType)
    result <-
      if (validated) handler.handle(request, dataHandler)
      else Future.exception(new InvalidClient())
  } yield result

  def authorize[U](
    request: http.Request,
    dataHandler: DataHandler[U]
  ): Future[AuthInfo[U]] =
    authorize(new Request.ProtectedResource(request.headerMap, request.params), dataHandler)

  def authorize[U](
    request: Request.ProtectedResource,
    dataHandler: DataHandler[U]
  ): Future[AuthInfo[U]] = for {
    accessToken <- request.token match {
      case Some(f) => Future.value(f)
      case None => Future.exception(new InvalidRequest("Access token was not specified"))
    }
    tokenOption <- dataHandler.findAccessToken(accessToken)
    token <- tokenOption match {
      case Some(t) =>
        if (dataHandler.isAccessTokenExpired(t)) Future.exception(new ExpiredToken())
        else Future.value(t)
      case None => Future.exception(new InvalidToken("Invalid access token"))
    }
    infoOption <- dataHandler.findAuthInfoByAccessToken(token)
    info <- infoOption match {
      case Some(i) => Future.value(i)
      case None => Future.exception(new InvalidToken("invalid access token"))
    }
  } yield info
}

object OAuth2 extends OAuth2
