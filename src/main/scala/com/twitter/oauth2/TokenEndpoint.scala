package com.twitter.oauth2

import com.twitter.util.Future

class TokenEndpoint {

  val fetcher = ClientCredentialFetcher

  val handlers = Map(
    "authorization_code" -> new AuthorizationCode(fetcher),
    "refresh_token" -> new RefreshToken(fetcher),
    "client_credentials" -> new ClientCredentials(fetcher),
    "password" -> new Password(fetcher)
  )

  def handleRequest[U](request: AuthorizationRequest, dataHandler: DataHandler[U]): Future[GrantHandlerResult] = for {
    grantType <- request.grantType match {
      case Some(t) => Future.value(t)
      case None => Future.exception(new InvalidRequest("grant_type not found"))
    }
    handler <- handlers.get(grantType) match {
      case Some(h) => Future.value(h)
      case None => Future.exception(new UnsupportedGrantType("the grant_type isn't supported"))
    }
    credential <- fetcher.fetch(request) match {
      case Some(c) => Future.value(c)
      case None => Future.exception(new InvalidRequest("client credential not found"))
    }
    validated <- dataHandler.validateClient(credential.clientId, credential.clientSecret, grantType)
    result <-
      if (validated) handler.handleRequest(request, dataHandler)
      else Future.exception(new InvalidClient)
  } yield result
}

object TokenEndpoint extends TokenEndpoint
