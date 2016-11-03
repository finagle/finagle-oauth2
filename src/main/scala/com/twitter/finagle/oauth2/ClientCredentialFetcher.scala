package com.twitter.finagle.oauth2

import java.util.Base64

case class ClientCredential(clientId: String, clientSecret: String)

trait ClientCredentialFetcher {

  private[this] val base64Decoder = Base64.getMimeDecoder
  private[this] def tryDecode(encoded: String): Option[String] = {
    try Some(new String(base64Decoder.decode(encoded), "UTF-8"))
    catch {
      case iae: IllegalArgumentException => None
    }
  }
  private[this] val basicAuthPattern = "(?i)basic.*".r.pattern
  private[this] def isBasicAuthHeader(header: String): Boolean =
    basicAuthPattern.matcher(header).matches

  def fetch(request: AuthorizationRequest): Option[ClientCredential] = {
    request.header("Authorization") match {
      case Some(authHeader) if isBasicAuthHeader(authHeader) =>
        val credentials = authHeader.substring(6)
        tryDecode(credentials).map(_.split(":", 2)) flatMap {
            case Array(clientId, clientSecret) => Some(ClientCredential(clientId, clientSecret))
            case Array(clientId) if clientId.nonEmpty => Some(ClientCredential(clientId, ""))
            case other => None
        }
      case _ => request.clientId.map { clientId =>
        ClientCredential(clientId, request.clientSecret.getOrElse(""))
      }
    }
  }
}

object ClientCredentialFetcher extends ClientCredentialFetcher
