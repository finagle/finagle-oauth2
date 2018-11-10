package com.twitter.finagle.oauth2

import com.twitter.finagle.http.{HeaderMap, ParamMap}
import java.util.Base64

sealed abstract class Request(headers: HeaderMap, params: ParamMap) {

  final def header(name: String): Option[String] =
    headers.get(name)

  final def requireHeader(name: String): String =
    header(name).getOrElse(throw new InvalidRequest("required header: " + name))

  final def param(name: String): Option[String] =
    params.get(name)

  final def requireParam(name: String): String =
    param(name).getOrElse(throw new InvalidRequest("required parameter: " + name))
}

object Request {

  private[this] def tryDecode(encoded: String): Option[String] = {
    try Some(new String(Base64.getMimeDecoder.decode(encoded), "UTF-8"))
    catch {
      case _: IllegalArgumentException => None
    }
  }
  private[this] val BasicAuthPattern = "(?i)basic.*".r.pattern
  private[this] val WwwAuthorizationPattern = """^\s*(OAuth|Bearer)\s+([^\s\,]*)""".r

  private[this] def isBasicAuthHeader(header: String): Boolean =
    BasicAuthPattern.matcher(header).matches

  final class ProtectedResource(
    headers: HeaderMap,
    params: ParamMap
  ) extends Request(headers, params) {

    def oauthToken: Option[String] = param("oauth_token")
    def accessToken: Option[String] = param("access_token")
    def requireAccessToken: String = requireParam("access_token")

    def token: Option[String] = {
      def authorizationToken: Option[String] =
        for {
          authorization <- header("Authorization")
          matcher <- WwwAuthorizationPattern.findFirstMatchIn(authorization)
        } yield matcher.group(2)

      oauthToken.orElse(accessToken).orElse(authorizationToken)
    }
  }

  final class Authorization(
    headers: HeaderMap,
    params: ParamMap
  ) extends Request(headers, params) {

    def grantType: Option[String] = param("grant_type")
    def clientId: Option[String] = param("client_id")
    def clientSecret: Option[String] = param("client_secret")
    def scope: Option[String] = param("scope")
    def redirectUri: Option[String] = param("redirect_uri")
    def requireCode: String = requireParam("code")
    def requireUsername: String = requireParam("username")
    def requirePassword: String = requireParam("password")
    def requireRefreshToken: String = requireParam("refresh_token")

    def clientCredential: Option[ClientCredential] = {
      header("Authorization") match {
        case Some(authHeader) if isBasicAuthHeader(authHeader) =>
          val credentials = authHeader.substring(6)
          tryDecode(credentials).map(_.split(":", 2)) flatMap {
            case Array(clientId, clientSecret) => Some(ClientCredential(clientId, clientSecret))
            case Array(clientId) if clientId.nonEmpty => Some(ClientCredential(clientId, ""))
            case other => None
          }
        case _ => clientId.map { clientId =>
          ClientCredential(clientId, clientSecret.getOrElse(""))
        }
      }
    }
  }
}


