package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.fraværfastsattaktivitet.VurderAktivitetsplikt11_8Service
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderAktivitetsplikt11_8Steg(
    private val vurderAktivitetsplikt11_8Service: VurderAktivitetsplikt11_8Service,
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider) : this(
        vurderAktivitetsplikt11_8Service = VurderAktivitetsplikt11_8Service(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val meldekort = vurderAktivitetsplikt11_8Service.foo(kontekst.behandlingId)
    }


    companion object : FlytSteg {

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderAktivitetsplikt11_8Steg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_AKTIVITETSPLIKT_11_8
        }
    }
}