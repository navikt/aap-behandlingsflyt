package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class RettighetstypeSteg() : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this()

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING,
            VurderingType.MIGRER_RETTIGHETSPERIODE -> {
                vurderOgPersisterRettighetstype(kontekst)
            }

            VurderingType.IKKE_RELEVANT,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MELDEKORT,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> {
                // Bruker persistert rettighetstype
            }
        }

        return Fullført
    }

    fun vurderOgPersisterRettighetstype(kontekst: FlytKontekstMedPerioder) {
        val vilkårsresultat = vurderKvote()
        // TODO: Utled rettighetstidslinje og lagre ned. Husk å lagre ned input til vurderingen som faktagrunnlag
    }

    fun vurderKvote() {
        // TODO: Vurder kvote (opphør § 11-12 og opphør § 11-13) og lagre ned som vilkår
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return RettighetstypeSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_RETTIGHETSTYPE
        }
    }
}