package com.twitter.finagle.oauth2

final case class Grant(
  tokenType: String,
  accessToken: String,
  expiresIn: Option[Long],
  refreshToken: Option[String],
  scope: Option[String]
)

