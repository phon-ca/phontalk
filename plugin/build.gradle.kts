plugins {
    id("phontalk.java-conventions")
}

base {
    archivesName = "phontalk-plugin"
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes("Automatic-Module-Name" to "ca.phon.phontalk.plugin")
    }
}

tasks.register<JavaExec>("runMacos") {
    group = "application"
    description = "Run Phon on macOS"

    mainClass.set("ca.phon.app.Main")
    classpath = sourceSets["main"].runtimeClasspath

    jvmArgs = listOf(
        "-Dphon.debug=true",
        "-Xdock:name=Phon",
        "-Xms1024m",
        "-Xmx4096m",
        "-Dswing.aatext=true",
        "-Dcom.apple.mrj.application.apple.menu.about.name=Phon",
        "-Dcom.apple.macos.smallTabs=true",
        "-Dapple.laf.useScreenMenuBar=true",
        "-Dcom.apple.mrj.application.live-resize=true",
        "-Dapple.awt.textantialiasing=on",
        "-Dapple.awt.graphics.UseQuartz=true",
        "-Dapple.awt.showGrowBox=true",
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8",
        "-Duser.language=en",
        "-Dlog4j2.debug=false",
        "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED"
    )
}

tasks.register<JavaExec>("runWindows") {
    group = "application"
    description = "Run Phon on Windows"

    mainClass.set("ca.phon.app.Main")
    classpath = sourceSets["main"].runtimeClasspath

    jvmArgs = listOf(
        "-Xms1024m",
        "-Xmx4096m",
        "-Dswing.aatext=true",
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8",
        "-Dsun.java2d.xrender=true",
        "-Duser.language=en",
        "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
    )
}

dependencies {
    implementation(project(":phontalk-core"))
    implementation(libs.phon.app)
    implementation(libs.xmlunit.core)
    implementation(libs.swingx.all)
    implementation(libs.jbreadcrumb)
    implementation(libs.native.dialogs)
    implementation(libs.commons.io)
    implementation(libs.commons.lang)
    implementation(libs.jsr305)

    testImplementation(libs.junit)
}
