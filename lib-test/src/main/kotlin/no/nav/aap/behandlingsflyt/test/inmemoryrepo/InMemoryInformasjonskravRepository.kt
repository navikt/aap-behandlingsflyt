package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import java.time.Instant

object InMemoryInformasjonskravRepository : InformasjonskravRepository {
    private val mutex = Any()
    private val oppdateringer = HashMap<SakId, Map<InformasjonskravNavn, InformasjonskravOppdatert>>()

    override fun hentOppdateringer(
        sakId: SakId,
        krav: List<InformasjonskravNavn>
    ) = synchronized(mutex) {
        val oppdateringer = oppdateringer[sakId].orEmpty()
        krav.mapNotNull { oppdateringer[it] }
    }

    override fun registrerOppdateringer(
        sakId: SakId,
        behandlingId: BehandlingId,
        informasjonskrav: Map<InformasjonskravNavn, InformasjonskravInput?>,
        oppdatert: Instant,
        rettighetsperiode: Periode
    ) {
        synchronized(mutex) {
            oppdateringer[sakId] = oppdateringer[sakId].orEmpty() + informasjonskrav.mapValues { (navn, input) ->
                InformasjonskravOppdatert(
                    behandlingId = behandlingId,
                    navn = navn,
                    oppdatert = oppdatert,
                    forrigeInput = input?.let(DefaultJsonMapper::toJson),
                    rettighetsperiode = rettighetsperiode,
                )
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}