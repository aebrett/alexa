import com.gilt.aws.lambda.AwsLambdaPlugin
import com.gilt.aws.lambda.AwsLambdaPlugin.autoImport._

object Aws {

  lazy val plugins = Seq(AwsLambdaPlugin)

  lazy val settings = Seq(
    lambdaHandlers := Seq(
      "alexa-fembot" -> "org.bretts.alexa.fembot.FembotHandler"
    ),
    region := Some("eu-west-1")
  )
}
