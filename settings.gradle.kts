rootProject.name = "simple-bank"
pluginManagement {
    repositories {
        mavenCentral()
    }
}
include(
    "bank-configuration",
    "gateway-server",
    "bank-accounts",
    "bank-exchange-generator",
    "bank-cash",
    "bank-notifications",
    "bank-blocker",
    "bank-transfer",
    "bank-exchange",
    "bank-front",
    "commons")