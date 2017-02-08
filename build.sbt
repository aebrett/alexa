val alexa = (
  project in file(".")
    settings Common.settings
    settings Dependencies.settings
    enablePlugins(Git.plugins: _*)
    settings Git.settings
    enablePlugins(Aws.plugins: _*)
    settings Aws.settings
)

