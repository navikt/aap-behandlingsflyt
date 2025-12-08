package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

interface AvklaringsbehoveneDecorator {

    fun alle(): List<Avklaringsbehov>
    fun alleEkskludertAvbruttOgVentebehov(): List<Avklaringsbehov>

    fun erSattPåVent(): Boolean
    fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov?
    fun skalTilbakeføresEtterKvalitetssikring(): Boolean
    fun harÅpentBrevVentebehov(): Boolean

    fun erVurdertTidligereIBehandlingen(definisjon: Definisjon): Boolean
}
