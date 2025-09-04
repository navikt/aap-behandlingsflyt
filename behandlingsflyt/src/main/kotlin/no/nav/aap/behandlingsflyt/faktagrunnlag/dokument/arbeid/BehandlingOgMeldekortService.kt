package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import java.time.LocalDate

interface BehandlingOgMeldekortService {
    fun hentAlle(behandlingId: BehandlingId, fraOgMed: LocalDate? = null, tilOgMed: LocalDate? = null): List<Meldekort>
    fun hentAlle(sak: Sak, fraOgMed: LocalDate? = null): List<Pair<Behandling, List<Meldekort>>>
    fun hentAlle(ident: Ident, fraOgMed: LocalDate? = null): List<Pair<Behandling, List<Meldekort>>>
}