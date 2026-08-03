package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface UnderveisRepository : Repository {
    fun hent(behandlingId: BehandlingId): UnderveisGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): UnderveisGrunnlag?
    fun hentBulk(behandlingIds: List<BehandlingId>): Map<BehandlingId, UnderveisGrunnlag>
    fun lagre(
        behandlingId: BehandlingId,
        underveisperioder: List<Underveisperiode>,
        input: Faktagrunnlag
    )
    fun hentSakerMedSisteUnderveisperiodeFørDato(sisteUnderveisDato: LocalDate): Set<SakId>
    fun hentSakerForGRegulering(datoForGJustering: LocalDate, nyttGrunnbeløp: Beløp): Set<SakId>

    /**
     * Returnerer ubesvarte meldeperioder per sak for de angitte sakene.
     * En meldeperiode regnes som ubesvart når meldepliktStatus er IKKE_MELDT_SEG eller FØR_VEDTAK og meldeperioden er avsluttet.
     */
    fun hentUbesvarteMeldeperioderForDollyJobb(sakIds: List<SakId>, idag: LocalDate): Map<SakId, List<Periode>>

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
