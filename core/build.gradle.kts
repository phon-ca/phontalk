plugins {
    id("phontalk.java-conventions")
}

base {
    archivesName = "phontalk-core"
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes("Automatic-Module-Name" to "ca.phon.phontalk")
    }
}

dependencies {
    api(libs.phon.project)
    implementation(libs.xml.resolver)
    implementation(libs.talkbank.schema)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.xmlunit)
    testImplementation(libs.phon.app)
    testImplementation(libs.phon.autotranscribe)
    testImplementation(libs.phon.ipadicts)
    testImplementation(libs.talkbank.chatter)
}

tasks.processResources {
    include("*.xsd", "catalog.cat")
}
