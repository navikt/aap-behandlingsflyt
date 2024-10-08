package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

class BehovForAvklaringer(
    private val harUavklarteSoningsforhold: Boolean,
    private val harUavklarteHelseinstitusjonsOpphold: Boolean
) {
    fun harBehov(): Boolean {
        return harUavklarteSoningsforhold || harUavklarteHelseinstitusjonsOpphold
    }

    fun avklaringsbehov(): List<Definisjon> {
        if (!harBehov()) {
            return emptyList()
        }
        return when {
            harUavklarteSoningsforhold && harUavklarteHelseinstitusjonsOpphold -> listOf(Definisjon.AVKLAR_SONINGSFORRHOLD, Definisjon.AVKLAR_HELSEINSTITUSJON)
            harUavklarteSoningsforhold -> listOf(Definisjon.AVKLAR_SONINGSFORRHOLD)
            else -> listOf(Definisjon.AVKLAR_HELSEINSTITUSJON)
        }
    }

}