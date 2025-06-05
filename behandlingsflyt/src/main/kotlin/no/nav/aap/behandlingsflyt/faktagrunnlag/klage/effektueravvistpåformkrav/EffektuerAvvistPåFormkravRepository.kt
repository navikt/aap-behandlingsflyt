package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface EffektuerAvvistPåFormkravRepository: Repository {
    fun lagreVarsel(behandlingId: BehandlingId, varsel: BrevbestillingReferanse)
    fun hentHvisEksisterer(behandlingId: BehandlingId): EffektuerAvvistPåFormkravGrunnlag?
    fun lagreVurdering(
        behandlingId: BehandlingId,
        vurdering: EffektuerAvvistPåFormkravVurdering
    )
    fun lagreFrist(
        behandlingId: BehandlingId,
        datoVarslet: LocalDate,
        frist: LocalDate
    )
    
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}