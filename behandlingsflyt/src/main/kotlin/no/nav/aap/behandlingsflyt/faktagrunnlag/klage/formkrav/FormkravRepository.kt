package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface FormkravRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): FormkravGrunnlag?
    fun lagre(behandlingId: BehandlingId, formkravVurdering: FormkravVurdering)
    fun lagreVarsel(behandlingId: BehandlingId, varsel: BrevbestillingReferanse)
    fun lagreFrist(behandlingId: BehandlingId, datoVarslet: LocalDate, svarfrist: LocalDate)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}