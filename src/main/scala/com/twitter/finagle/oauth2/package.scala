package com.twitter.finagle

import com.twitter.finagle.http.{Status, Version, Response}
import scala.util.parsing.json.JSONObject

package object oauth2 {

  trait OAuthErrorHandler {
    def handleError(e: OAuthError): Response
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
    def convertToken(token: GrantHandlerResult): Response
  }

  trait OAuthTokenInJson extends OAuthTokenConverter {
    override def convertToken(token: GrantHandlerResult) = {
      val rep = Response(Version.Http11, Status.Ok)
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
