package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.verdityper.sakogbehandling.Ident

/**
 * identene til alle barna som brukerne oppgir i søknaden
 */
class OppgitteBarn(val id: Long? = null, val identer: Set<Ident>)
