
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val modJarBaseName =
    (findProperty("customArchiveBaseName") as String?)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: (findProperty("modId") as String?)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: "mcheli-reforged"

tasks.register<Jar>("devModJar") {
    dependsOn("compileJava", "processResources")
    archiveBaseName.set(modJarBaseName)
    archiveClassifier.set("dev-unzip")
    destinationDirectory.set(layout.buildDirectory.dir("devlibs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(layout.buildDirectory.dir("classes/java/main"))
    from(layout.buildDirectory.dir("resources/main")) {
        include("mcmod.info")
        include("assets/**")
        include("META-INF/**")
    }
}

tasks.named("build") {
    dependsOn("devModJar")
}

tasks.withType<JavaCompile>().configureEach {
    options.isFailOnError = true
}

tasks.register("printMainJavaSources") {
    doLast {
        val mainSources = sourceSets.getByName("main").java.files.map { it.absolutePath }.sorted()
        println("MAIN_JAVA_SOURCE_COUNT=" + mainSources.size)
        println("HAS_MCH_ENTITY_GUNNER=" + mainSources.any { it.endsWith("mcheli\\mob\\MCH_EntityGunner.java") })
        println("HAS_MCH_RENDER_GUNNER=" + mainSources.any { it.endsWith("mcheli\\mob\\MCH_RenderGunner.java") })
        println("HAS_MCH_GUI_SPAWN_GUNNER=" + mainSources.any { it.endsWith("mcheli\\mob\\MCH_GuiSpawnGunner.java") })
        println("HAS_MCH_ITEM_THROWABLE=" + mainSources.any { it.endsWith("mcheli\\throwable\\MCH_ItemThrowable.java") })
    }
}
