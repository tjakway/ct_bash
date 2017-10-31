name := "ctbash"
version := "1.0"
scalaVersion := "2.12.3"
 


resolvers += Resolver.typesafeIvyRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= 
  Seq(
      "org.scala-lang.modules" % "scala-parser-combinators_2.12" % "1.0.6",
      "org.slf4j" % "slf4j-parent" % "1.7.6",
      "ch.qos.logback"  %  "logback-classic"    % "1.2.1",
      "com.github.scopt" %% "scopt" % "3.5.0",
      "org.scala-lang" % "scala-compiler" % "2.12.3"
    )


//mainClass in assembly := Some("com.jakway.ctbash.Main")

//ignore anything named snippets.scala
excludeFilter in unmanagedSources := HiddenFileFilter || "snippets.scala"

//enable more warnings
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")
