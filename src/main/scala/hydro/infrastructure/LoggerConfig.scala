package hydro.infrastructure

import wvlet.log.LogFormatter.AppLogFormatter
import wvlet.log.{ LogFormatter, LogLevel }

case class LoggerConfig(
  level: LogLevel,
  formatter: LogFormatter,
)

object LoggerConfig {
  val default: LoggerConfig = LoggerConfig(LogLevel.DEBUG, AppLogFormatter)
}