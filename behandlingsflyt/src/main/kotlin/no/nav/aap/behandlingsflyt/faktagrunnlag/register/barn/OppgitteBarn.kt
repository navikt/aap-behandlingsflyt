package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident

/**
 * Identene til alle barna som brukerne oppgir i søknaden.
 */
data class OppgitteBarn(val id: Long? = null, val identer: List<Ident>)
