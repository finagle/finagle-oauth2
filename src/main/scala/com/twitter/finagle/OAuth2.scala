package com.twitter.finagle

import com.twitter.finagle.http._
import com.twitter.finagle.oauth2._
import com.twitter.util.Future
import scala.util.parsing.json.JSONObject

trait OAuth2 {

  private[this] def headersToMap(headers: HeaderMap) = (for {
    key <- headers.keys
  } yield (key, headers.getAll(key).toSeq)).toMap

  private[this] def paramsToMap(params: ParamMap) = (for {
    key <- params.keys
  } yield (key, params.getAll(key).toSeq)).toMap

  def issueAccessToken[U](request: Request, dataHandler: DataHandler[U]) =
    TokenEndpoint.handleRequest(
      AuthorizationRequest(headersToMap(request.headerMap), paramsToMap(request.params)),
      dataHandler
    )

  def authorize[U](request: Request, dataHandler: DataHandler[U]) =
    ProtectedResource.handleRequest(
      ProtectedResourceRequest(headersToMap(request.headerMap), paramsToMap(request.params)),
      dataHandler
    )
}

case class OAuth2Request[U](authInfo: AuthInfo[U], underlying: Request) extends RequestProxy {
  def request: Request = underlying
}

class OAuth2Filter[U](dataHandler: DataHandler[U])
  extends Filter[Request, Response, OAuth2Request[U], Response] with OAuth2 with OAuthErrorHandler {

  override def handleError(e: OAuthError) = e.toHttpResponse

  def apply(req: Request, service: Service[OAuth2Request[U], Response]) =
    authorize(req, dataHandler) flatMap { authInfo =>
      service(OAuth2Request(authInfo, req))
    } handle {
      case e: OAuthError => handleError(e)
    }
}

class OAuth2Endpoint[U](dataHandler: DataHandler[U])
  extends Service[Request, Response] with OAuth2 with OAuthErrorHandler with OAuthTokenConverter {

  override def convertToken(token: GrantHandlerResult) = {
    val rep = Response(Version.Http11, Status.Ok)
    rep.setContentString(token.accessToken)

    rep
  }

  override def handleError(e: OAuthError) = e.toHttpResponse

  def apply(req: Request) =
    issueAccessToken(req, dataHandler) map convertToken handle {
      case e: OAuthError => handleError(e)
    }
}
