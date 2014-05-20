OAuth2 Provider for Finagle
---------------------------------------

How to use?

#### Define a DataHandler implementation

```scala
// This DatatHandler is used for Client Credentials Flow Scheme,
// so we don't need to implement all of these methods.
// ''Long'' is our user (his ID)
object LocalDataHandler extends DataHandler[Long] {

  /**
   * We map 
   *  (a) users (ther IDs) aloong with granted tokens
   *  (b) tokens along with auth information
   */
  val forwardStorage = collection.mutable.Map[Long, AccessToken]()
  val backwardStorage = collection.mutable.Map[String, AuthInfo[Long]]()

  /**
   * Emulates the database.
   */
  val db = Map(
    1l -> ("Ivan", "12345"),
    2l -> ("John", "password")
  )

  /**
   * Issues a new ''l''-length token.
   */
  def issueToken(l: Int) = Random.alphanumeric.take(l).mkString

  /**
   * Issues a current date.
   */
  def issueDate = new java.util.Date()

  def validateClient(clientId: String, clientSecret: String, grantType: String) =
    Future.value(true)

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]) = {
    val user = db.find {
      case (_, (id, secret)) => (id == clientId) && (secret == clientSecret)
    } flatMap {
      case (id, _) => Some(id)
    }

    Future.value(user)
  }

  def createAccessToken(authInfo: AuthInfo[Long]) = {
    val token = AccessToken(issueToken(20), None, Some("all"), Some(3600), issueDate)
    forwardStorage += (authInfo.user -> token)
    backwardStorage += (token.token -> authInfo)

    Future.value(token)
  }

  def findAccessToken(token: String) =
    Future.value(forwardStorage.values.find { t => t.token == token })

  def findAuthInfoByAccessToken(accessToken: AccessToken) =
    Future.value(backwardStorage.get(accessToken.token))

  // We don't need these methods for this example, we can just return 
  // future of none. This will lead to a bad-request response for any
  // unsupported scheme.
  def findAuthInfoByCode(code: String) = Future.None
  def findUser(username: String, password: String) = Future.None
  def getStoredAccessToken(authInfo: AuthInfo[Long]) = Future.None
  def findAuthInfoByRefreshToken(refreshToken: String) = Future.None
  def refreshAccessToken(authInfo: AuthInfo[Long], refreshToken: String) = Future.never
}

```

