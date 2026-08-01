package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.aktivitetsplikt.Aktivitetsplikt11_7Varsel
import no.nav.aap.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.brev.BrevbestillingReferanse
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface Aktivitetsplikt11_7Repository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_7Grunnlag?
    fun lagre(behandlingId: BehandlingId, vurderinger: List<Aktivitetsplikt11_7Vurdering>)

    fun lagreVarsel(behandlingId: BehandlingId, varsel: BrevbestillingReferanse)
    fun lagreFrist(behandlingId: BehandlingId, datoVarslet: LocalDate, svarfrist: LocalDate)
    fun hentVarselHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_7Varsel?
}