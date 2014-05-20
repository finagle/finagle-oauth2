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

  
}
