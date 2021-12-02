import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-28" % "5.16.0",
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"        % "0.56.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.12.4",
    "com.github.java-json-tools"    % "json-schema-validator"     % "2.2.14"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % "5.16.0"  % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % "0.56.0"  % Test,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.36.8"  % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.0.0"   % "test,it",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.3.0" % Test
  )

}
