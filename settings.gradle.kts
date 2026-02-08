rootProject.name = "phontalk"

include("core", "plugin")

// Rename project modules to avoid coordinate clash with ca.phon:core and ca.phon:plugin
// (external Phon dependencies). Directories remain core/ and plugin/.
project(":core").name = "phontalk-core"
project(":plugin").name = "phontalk-plugin"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven {
            name = "TalkBank"
            url = uri("https://maven.pkg.github.com/talkbank/talkbank-xml-schema")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
        }
        maven {
            name = "PhonCA"
            url = uri("https://maven.pkg.github.com/phon-ca/phon")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
        }
        maven {
            name = "SpeechMetrics"
            url = uri("https://maven.pkg.github.com/speechmetrics/speechmetrics")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
        }
        maven {
            name = "GHedlund"
            url = uri("https://maven.pkg.github.com/ghedlund/jpraat")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
        }
    }
}
