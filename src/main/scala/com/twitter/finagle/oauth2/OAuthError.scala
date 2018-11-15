package com.twitter.finagle.oauth2

import com.twitter.finagle.http.{Response, Version, Status}

abstract class OAuthError(val statusCode: Int, val description: String) extends Exception {

  def this(description: String) = this(400, description)

  def errorType: String

  final def toResponse: Response = {
    val bearer = Seq("error=\"" + errorType + "\"") ++
      (if (!description.isEmpty) Seq("error_description=\"" + description + "\"") else Nil)

    val rep = Response(Version.Http11, Status(statusCode))
    rep.headerMap.add("WWW-Authenticate", "Bearer " + bearer.mkString(", "))

    rep
  }
}

final class InvalidRequest(description: String = "") extends OAuthError(description) {
  def errorType: String = "invalid_request"
}

final class InvalidClient(description: String = "") extends OAuthError(401, description) {
  def errorType: String = "invalid_client"
}

final class UnauthorizedClient(description: String = "") extends OAuthError(401, description) {
  def errorType: String = "unauthorized_client"
}

final class RedirectUriMismatch(description: String = "") extends OAuthError(401, description) {
  def errorType: String = "redirect_uri_mismatch"
}

final class AccessDenied(description: String = "") extends OAuthError(401, description) {
  def errorType: String = "access_denied"
}

final class UnsupportedResponseType(description: String = "") extends OAuthError(description) {
  def errorType: String = "unsupported_response_type"
}

final class InvalidGrant(description: String = "") extends OAuthError(401, description) {
   def errorType: String = "invalid_grant"
}

final class UnsupportedGrantType(description: String = "") extends OAuthError(description) {
  def errorType: String = "unsupported_grant_type"
}

final class InvalidScope(description: String = "") extends OAuthError(401, description) {
  def errorType: String = "invalid_scope"
}

final class InvalidToken(description: String = "") extends OAuthError(401, description) {
  def errorType: String = "invalid_token"
}

final class ExpiredToken extends OAuthError(401, "The access token expired") {
  def errorType: String = "invalid_token"
}

final class InsufficientScope(description: String = "") extends OAuthError(401, description) {
  def errorType: String = "insufficient_scope"
}
