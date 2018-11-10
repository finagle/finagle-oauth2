package com.twitter.finagle.oauth2

import java.util.Date

/**
 * Access token.
 *
 * @param token Access token is used to authentication.
 * @param refreshToken Refresh token is used to re-issue access token.
 * @param scope Inform the client of the scope of the access token issued.
 * @param expiresIn Expiration date of access token. Unit is seconds.
 * @param createdAt Access token is created date.
 */
case class AccessToken(
  token: String,
  refreshToken: Option[String],
  scope: Option[String],
  expiresIn: Option[Long],
  createdAt: Date
)
