import play.api.Logger

package object logger {
  private val _log = Logger("root")

  def info(s: => String) {
    if (_log.isInfoEnabled) { _log.info(s) }
  }
}
