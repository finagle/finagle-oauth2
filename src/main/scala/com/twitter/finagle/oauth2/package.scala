package com.twitter.finagle

import scala.util.parsing.json.JSONObject

package object oauth2 {

  trait OAuthErrorHandler {
    def handleError(e: OAuthError): httpx.Response
  }

  trait OAuthErrorInJson extends OAuthErrorHandler {
    override def handleError(e: OAuthError) = {
      val rep = e.toHttpResponse
      val json = Map(
        "status" -> e.statusCode,
        "error" -> e.errorType,
        "description" -> e.description
      )

      rep.setContentTypeJson()
      rep.setContentString(JSONObject(json).toString())

      rep
    }
  }

  trait OAuthTokenConverter {
    def convertToken(token: GrantHandlerResult): httpx.Response
  }

  trait OAuthTokenInJson extends OAuthTokenConverter {
    override def convertToken(token: GrantHandlerResult) = {
      val rep = httpx.Response(httpx.Version.Http11, httpx.Status.Ok)
      val json = Map[String, Any](
        "access_token" -> token.accessToken,
        "token_type" -> token.tokenType
      ) ++ token.expiresIn.map(
        "expires_in" -> _
      ) ++ token.refreshToken.map(
        "refresh_token" -> _
      ) ++ token.scope.map(
        "scope" -> _
      )

      rep.setContentTypeJson()
      rep.setContentString(JSONObject(json).toString())

      rep
    }
  }
}
