import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("cc.mewcraft.repo-conventions")
    id("cc.mewcraft.java-conventions")
    id("cc.mewcraft.deploy-conventions")
    alias(libs.plugins.pluginyml.paper)
}

project.ext.set("name", "MewUtils")

group = "cc.mewcraft.vane"
version = "1.25.0"
description = "Provides small features that do not fit into big ones"

dependencies {
    // internal
    implementation(project(":mewutils:base"))
    implementation(project(":spatula:guice"))
    implementation(project(":spatula:bukkit:command"))
    implementation(project(":spatula:bukkit:message"))
    implementation(project(":spatula:bukkit:utils"))
    implementation(libs.configurate)
    implementation(libs.anvilgui)

    // server
    compileOnly(libs.server.paper)

    // helper
    compileOnly(libs.helper)

    // standalone plugins
    compileOnly(libs.luckperms)
    compileOnly(libs.vault) { isTransitive = false }
    compileOnly(libs.papi) { isTransitive = false }
    compileOnly(libs.itemsadder)
    compileOnly(libs.protocollib)
    compileOnly(libs.essentials) { isTransitive = false }
}

paper {
    main = "cc.mewcraft.mewutils.MewUtils"
    name = project.ext.get("name") as String
    version = "${project.version}"
    description = project.description
    apiVersion = "1.19"
    authors = listOf("Nailm")
    serverDependencies {
        register("helper") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("Vault") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("ProtocolLib") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("PlaceholderAPI") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("Essentials") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("ItemsAdder") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}
