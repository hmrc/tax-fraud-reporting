import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "5.16.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % "0.56.0",
    "org.micchon"       %% "play-json-xml"             % "0.4.2"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % "5.16.0"  % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % "0.56.0"  % Test,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.36.8"  % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.0.0"   % "test,it",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.3.0" % Test
  )

}
