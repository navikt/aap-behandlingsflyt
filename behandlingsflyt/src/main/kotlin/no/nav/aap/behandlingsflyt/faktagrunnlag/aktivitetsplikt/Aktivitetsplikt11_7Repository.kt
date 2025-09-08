package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface Aktivitetsplikt11_7Repository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_7Grunnlag?
    fun lagre(behandlingId: BehandlingId, vurderinger: List<Aktivitetsplikt11_7Vurdering>)
    fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<Aktivitetsplikt11_7Vurdering>
    fun lagre(behandlingId: BehandlingId, vurdering: Aktivitetsplikt11_7Vurdering)
    fun lagreVarsel(behandlingId: BehandlingId, varsel: BrevbestillingReferanse)
    fun lagreFrist(behandlingId: BehandlingId, datoVarslet: LocalDate, svarfrist: LocalDate)
    fun lagreOverstyrtVarsel(behandlingId: BehandlingId, overstyrtVarsel: Aktivitetsplikt11_7OverstyrtVarsel)
}