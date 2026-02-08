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
}
