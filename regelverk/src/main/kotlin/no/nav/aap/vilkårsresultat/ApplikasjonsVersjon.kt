package no.nav.aap.vilkårsresultat

import java.util.Properties

object ApplikasjonsVersjon {

    val versjon: String

    init {
        val file = this::class.java.classLoader.getResourceAsStream("behandlingsflyt-version.properties")
        val properties = Properties()
        properties.load(file)
        versjon = properties.getProperty("project.version")
    }
}