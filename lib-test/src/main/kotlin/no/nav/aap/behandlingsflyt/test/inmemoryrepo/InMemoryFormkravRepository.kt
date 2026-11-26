package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

object InMemoryFormkravRepository : FormkravRepository {
    private val grunnlag = mutableMapOf<BehandlingId, FormkravGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): FormkravGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun lagre(
        behandlingId: BehandlingId,
        formkravVurdering: FormkravVurdering
    ) {
        grunnlag[behandlingId] = FormkravGrunnlag(
            vurdering = formkravVurdering,
            varsel = grunnlag[behandlingId]?.varsel
        )
    }

    override fun lagreVarsel(
        behandlingId: BehandlingId,
        varsel: BrevbestillingReferanse
    ) {
        TODO("Not yet implemented")
    }

    override fun lagreFrist(
        behandlingId: BehandlingId,
        datoVarslet: LocalDate,
        svarfrist: LocalDate
    ) {
        TODO("Not yet implemented")
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

}
