package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "behovstype", visible = true)
sealed interface AvklaringsbehovLøsning {
    fun definisjon(): Definisjon {
        if (this.javaClass.isAnnotationPresent(JsonTypeName::class.java)) {
            return Definisjon.entries.first {
                @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                it.kode == AvklaringsbehovKode.valueOf(this.javaClass.getDeclaredAnnotation(JsonTypeName::class.java).value)
            }
        }
        throw IllegalStateException("Utvikler-feil:" + this.javaClass.getSimpleName() + " er uten JsonTypeName annotation.")
    }

    fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat
}

fun utledSubtypesTilAvklaringsbehovLøsning(): List<Class<*>> {
    return AvklaringsbehovLøsning::class.sealedSubclasses.flatMap {
        if (it.isSealed)
            it.sealedSubclasses.map { it.java }
        else
            listOf(it.java)
    }.toList()
}