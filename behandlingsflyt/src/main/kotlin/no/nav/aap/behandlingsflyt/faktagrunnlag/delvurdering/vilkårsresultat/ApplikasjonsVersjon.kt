package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import java.util.*

object ApplikasjonsVersjon {

    val versjon: String

    init {
        val file = this::class.java.classLoader.getResourceAsStream("version.properties")
        val properties = Properties()
        properties.load(file)
        versjon = properties.getProperty("project.version")
    }
}