name := "ctbash"
version := "1.0"
scalaVersion := "2.11.8"

resolvers += Resolver.typesafeIvyRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= 
  Seq( // https://mvnrepository.com/artifact/org.scala-lang/scala-parser-combinators
      "org.scala-lang" % "scala-parser-combinators" % "2.11.0-M4",
      "org.slf4j" % "slf4j-parent" % "1.7.6",
      "ch.qos.logback"  %  "logback-classic"    % "1.2.1",
      // % test declares it as a test-only dependency
      "org.specs2" % "specs2_2.11" % "3.7" % "test",
      "com.github.scopt" %% "scopt" % "3.5.0")


//mainClass in assembly := Some("com.jakway.ctbash.Main")

//ignore anything named snippets.scala
excludeFilter in unmanagedSources := HiddenFileFilter || "snippets.scala"

//enable more warnings
scalacOptions in compile ++= Seq("-unchecked", "-deprecation", "-feature")
