package com.twitter.finagle.oauth2

/**
 * Authorized information.
 *
 * @param user Authorized user which is registered on system.
 * @param clientId Using client id which is registered on system.
 * @param scope Inform the client of the scope of the access token issued.
 * @param redirectUri This value is used by Authorization Code Grant.
 */
final case class AuthInfo[U](
  user: U,
  clientId: String,
  scope: Option[String],
  redirectUri: Option[String]
)
