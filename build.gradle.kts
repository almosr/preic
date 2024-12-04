plugins {
    kotlin("jvm") version "2.0.21"
}

version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Main-Class" to "MainKt"
            )
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.compileClasspath.get().map { if (it.isDirectory()) it else zipTree(it) })
}

//Task for creating release archive out of the project
//build output and additional files
tasks.register<Zip>("createRelease") {

    archiveFileName.set("release_v${version}.zip")
    destinationDirectory.set(file("build"))

    from("build/libs") {
        include("preic*.jar")
        into("bin")
    }

    from(".") {
        include("CHANGELOG.md")
        include("LICENSE")
        include("README.md")
        include("examples/**")
        exclude("examples/**/build")
    }

    dependsOn(tasks.named("jar"))
}

