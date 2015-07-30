package com.twitter.finagle

import com.twitter.util.Future
import com.twitter.finagle.oauth2._

trait OAuth2 {

  private[this] def headersToMap(headers: httpx.HeaderMap) = (for {
    key <- headers.keys
  } yield (key, headers.getAll(key).toSeq)).toMap

  private[this] def paramsToMap(params: httpx.ParamMap) = (for {
    key <- params.keys
  } yield (key, params.getAll(key).toSeq)).toMap

  def issueAccessToken[U](request: httpx.Request, dataHandler: DataHandler[U]): Future[GrantHandlerResult] =
    TokenEndpoint.handleRequest(
      AuthorizationRequest(headersToMap(request.headerMap), paramsToMap(request.params)),
      dataHandler
    )

  def authorize[U](request: httpx.Request, dataHandler: DataHandler[U]): Future[AuthInfo[U]] =
    ProtectedResource.handleRequest(
      ProtectedResourceRequest(headersToMap(request.headerMap), paramsToMap(request.params)),
      dataHandler
    )
}

case class OAuth2Request[U](authInfo: AuthInfo[U], httpRequest: httpx.Request)

class OAuth2Filter[U](dataHandler: DataHandler[U])
    extends Filter[httpx.Request, httpx.Response, OAuth2Request[U], httpx.Response]
    with OAuth2 with OAuthErrorHandler {

  override def handleError(e: OAuthError) = e.toHttpResponse

  def apply(req: httpx.Request, service: Service[OAuth2Request[U], httpx.Response]) =
    authorize(req, dataHandler) flatMap { authInfo =>
      service(OAuth2Request(authInfo, req))
    } handle {
      case e: OAuthError => handleError(e)
    }
}

class OAuth2Endpoint[U](dataHandler: DataHandler[U])
    extends Service[httpx.Request, httpx.Response]
    with OAuth2 with OAuthErrorHandler with OAuthTokenConverter {

  override def convertToken(token: GrantHandlerResult) = {
    val rep = httpx.Response(httpx.Version.Http11, httpx.Status.Ok)
    rep.setContentString(token.accessToken)

    rep
  }

  override def handleError(e: OAuthError) = e.toHttpResponse

  def apply(req: httpx.Request) =
    issueAccessToken(req, dataHandler) map convertToken handle {
      case e: OAuthError => handleError(e)
    }
}
