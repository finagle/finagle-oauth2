package com.twitter

import com.twitter.finagle.http._
import com.twitter.finagle.oauth2._

package object finagle {

  trait OAuth2 {

    private[this] def headersToMap(headers: HeaderMap) = (for {
      key <- headers.values
    } yield (key, headers.getAll(key).toSeq)).toMap

    private[this] def paramsToMap(params: ParamMap) = (for {
      key <- params.values
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
    extends Filter[Request, Response, OAuth2Request[U], Response] with OAuth2 {

    def apply(req: Request, service: Service[OAuth2Request[U], Response]) =
      authorize(req, dataHandler) flatMap { authInfo =>
        service(OAuth2Request(authInfo, req))
      } handle {
        case e: OAuthError => e.toHttpResponse
      }
  }
}
