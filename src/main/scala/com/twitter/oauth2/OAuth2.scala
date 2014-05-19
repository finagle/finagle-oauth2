package com.twitter.oauth2

import com.twitter.finagle.http._

trait OAuth2 {

  protected[this] def oAuthErrorToHttpResponse(e: OAuthError) = {
    val bearer = Seq("error=\"" + e.errorType + "\"") ++ (
      if (!e.description.isEmpty) Seq("error_description=\"" + e.description + "\"")
      else Nil
    ).mkString(", ")

    val reply = Response()
    reply.setProtocolVersion(Version.Http11)
    reply.setStatusCode(e.statusCode)
    reply.headerMap.add("WWW-Authenticate", "Bearer " + bearer)

    reply
  }

  protected[this] def headersToMap(headers: HeaderMap) = (for {
    key <- headers.values
  } yield (key, headers.getAll(key).toSeq)).toMap

  protected[this] def paramsToMap(params: ParamMap) = (for {
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
