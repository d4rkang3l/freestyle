lazy val root = (project in file("."))
  .settings(moduleName := "root")
  .settings(name := "freestyle")
  .settings(noPublishSettings: _*)
  .aggregate(allModules: _*)

lazy val freestyle = (crossProject in file("freestyle"))
  .settings(name := "freestyle")
  .jsSettings(sharedJsSettings: _*)
  .settings(libraryDependencies ++= Seq(%("scala-reflect", scalaVersion.value)))
  .settings(libraryDependencies += "com.47deg" %%% "iota-core" % "0.2.0-SNAPSHOT")
  .crossDepSettings(
    commonDeps ++ Seq(
      //%("iota-core"),
      %("cats-free"),
      %("shapeless"),
      %("monix-eval") % "test",
      %("monix-cats") % "test",
      %("cats-laws")  % "test",
      %("discipline") % "test"
    ): _*
  )

lazy val freestyleJVM = freestyle.jvm
lazy val freestyleJS  = freestyle.js

lazy val tagless = (crossProject in file("freestyle-tagless"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-tagless")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)
  .settings(resolvers += Resolver.bintrayRepo("kailuowang", "maven"))
  .settings(
    libraryDependencies += "com.kailuowang" %%% "mainecoon-core" % "0.0.5"
  )

lazy val taglessJVM = tagless.jvm
lazy val taglessJS  = tagless.js

lazy val tests = (project in file("tests"))
  .dependsOn(freestyleJVM)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= commonDeps ++ Seq(
      %("scala-reflect", scalaVersion.value),
      %%("pcplod") % "test"
    ),
    fork in Test := true,
    javaOptions in Test ++= {
      val excludedScalacOptions: List[String] = List("-Yliteral-types", "-Ypartial-unification")
      val options = (scalacOptions in Test).value.distinct
        .filterNot(excludedScalacOptions.contains)
        .mkString(",")
      val cp = (fullClasspath in Test).value.map(_.data).filter(_.exists()).distinct.mkString(",")
      Seq(
        s"""-Dpcplod.settings=$options""",
        s"""-Dpcplod.classpath=$cp"""
      )
    }
  )

lazy val docs = (project in file("docs"))
  .dependsOn(jvmFreestyleDeps: _*)
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := "docs",
    description := "freestyle docs"
  )
  .settings(
    libraryDependencies ++= Seq(
      %%("freestyle-cache-redis"),
      %%("freestyle-doobie"),
      %%("freestyle-fetch"),
      %%("freestyle-fs2"),
      %%("freestyle-http-akka"),
      %%("freestyle-http-finch"),
      %%("freestyle-http-http4s"),
      %%("freestyle-http-play"),
      %%("freestyle-monix"),
      %%("freestyle-slick"),
      %%("freestyle-twitter-util"),
      %%("doobie-h2-cats"),
      %%("http4s-dsl"),
      %%("play"),
      %("h2") % "test"
    )
  )
  .settings(
    scalacOptions in Tut ~= (_ filterNot Set("-Ywarn-unused-import", "-Xlint").contains)
  )
  .enablePlugins(MicrositesPlugin)

lazy val effects = (crossProject in file("freestyle-effects"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-effects")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val effectsJVM = effects.jvm
lazy val effectsJS  = effects.js

lazy val async = (crossProject in file("freestyle-async/async"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-async")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val asyncJVM = async.jvm
lazy val asyncJS  = async.js

lazy val asyncMonix = (crossProject in file("freestyle-async/monix"))
  .dependsOn(freestyle, async)
  .settings(name := "freestyle-async-monix")
  .crossDepSettings(
    commonDeps ++ Seq(
      %("monix-eval"),
      %("monix-cats")
    ): _*)
  .jsSettings(sharedJsSettings: _*)

lazy val asyncMonixJVM = asyncMonix.jvm
lazy val asyncMonixJS  = asyncMonix.js

lazy val asyncFs = (crossProject in file("freestyle-async/fs2"))
  .dependsOn(freestyle, async)
  .settings(name := "freestyle-async-fs2")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ Seq(%("fs2-core"), %("fs2-cats")): _*)

lazy val asyncFsJVM = asyncFs.jvm
lazy val asyncFsJS  = asyncFs.js

lazy val cache = (crossProject in file("freestyle-cache"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-cache")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val cacheJVM = cache.jvm
lazy val cacheJS  = cache.js

lazy val config = (project in file("freestyle-config"))
  .dependsOn(freestyleJVM)
  .settings(
    name := "freestyle-config",
    fixResources := {
      val testConf   = (resourceDirectory in Test).value / "application.conf"
      val targetFile = (classDirectory in (freestyleJVM, Compile)).value / "application.conf"
      if (testConf.exists) {
        IO.copyFile(
          testConf,
          targetFile
        )
      }
    },
    compile in Test := ((compile in Test) dependsOn fixResources).value
  )
  .settings(
    libraryDependencies ++= Seq(
      %("config", "1.2.1")
    ) ++ commonDeps
  )

lazy val logging = (crossProject in file("freestyle-logging"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-logging")
  .jvmSettings(
    libraryDependencies += %%("journal-core")
  )
  .jsSettings(
    libraryDependencies += %%%("slogging")
  )
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val loggingJVM = logging.jvm
lazy val loggingJS  = logging.js

addCommandAlias("debug", "; clean ; test")

addCommandAlias("validate", "; +clean ; +test; makeMicrosite")

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val jvmModules: Seq[ProjectReference] = Seq(
  freestyleJVM,
  taglessJVM,
  effectsJVM,
  asyncJVM,
  asyncMonixJVM,
  asyncFsJVM,
  cacheJVM,
  config,
  loggingJVM
)

lazy val jsModules: Seq[ProjectReference] = Seq(
  freestyleJS,
  taglessJS,
  effectsJS,
  asyncJS,
  asyncMonixJS,
  asyncFsJS,
  cacheJS,
  loggingJS
)

lazy val allModules: Seq[ProjectReference] = jvmModules ++ jsModules

lazy val jvmFreestyleDeps: Seq[ClasspathDependency] =
  jvmModules.map(ClasspathDependency(_, None))
