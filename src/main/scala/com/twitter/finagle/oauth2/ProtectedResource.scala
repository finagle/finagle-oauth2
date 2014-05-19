package com.twitter.finagle.oauth2

import com.twitter.util.Future

trait ProtectedResource {

  val fetchers = Seq(AuthHeader, RequestParameter)

  def handleRequest[U](request: ProtectedResourceRequest, dataHandler: DataHandler[U]): Future[AuthInfo[U]] = for {
    fetcher <- fetchers.find { f =>
      f.matches(request)
    } match {
      case Some(f) => Future.value(f)
      case None => Future.exception(new InvalidRequest("Access token was not specified"))
    }
    tokenOption <- dataHandler.findAccessToken(fetcher.fetch(request).token)
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

object ProtectedResource extends ProtectedResource
