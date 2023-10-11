plugins {
    id("cc.mewcraft.repo-conventions")
    id("cc.mewcraft.java-conventions")
}

dependencies {
    // internal
    compileOnly(libs.configurate)
    compileOnly(project(":spatula:guice"))
    compileOnly(project(":spatula:bukkit:command"))
    compileOnly(project(":spatula:bukkit:message"))
    compileOnly(project(":spatula:bukkit:utils"))

    // server
    compileOnly(libs.server.paper)

    // helper
    compileOnly(libs.helper)
}