import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.GitVersioning

object Git {

  lazy val plugins = Seq(GitVersioning)

  lazy val settings = Seq(
    git.useGitDescribe := true
  )
}
