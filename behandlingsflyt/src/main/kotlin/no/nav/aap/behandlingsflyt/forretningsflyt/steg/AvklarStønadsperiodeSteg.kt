package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class AvklarStønadsperiodeSteg(
    private val unleashGateway: UnleashGateway,
    private val kravRepository: KravRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.KravSteg)
        ) {
            return Fullført
        }

        when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering -> {
                if(kontekst.erMigreringFraArena()) {
                    vurderMigrertKravAutomatisk(kontekst)
                } else {
                    vurderAutomatisk(kontekst)
                }
            }

            else -> {}
        }

        return Fullført
    }

    private fun vurderAutomatisk(kontekst: FlytKontekstMedPerioder) {
        val gjeldendeRelevanteKrav =
            kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.gjeldendeRelevanteKrav().orEmpty()
        val vedtatteStønadsperiodeVurderinger = kontekst.forrigeBehandlingId?.let {
            stønadsperiodeRepository.hentHvisEksisterer(kontekst.forrigeBehandlingId)?.gjeldendeVurderinger()
        }.orEmpty()

        val kravSomManglerVurdering =
            gjeldendeRelevanteKrav.filter { krav ->
                val vedtatteStønadsperiodeForVurdering = vedtatteStønadsperiodeVurderinger.firstOrNull { it.referanse == krav.referanse }
                vedtatteStønadsperiodeForVurdering == null || (
                        vedtatteStønadsperiodeForVurdering.vurdertAv == SYSTEMBRUKER &&
                                vedtatteStønadsperiodeForVurdering.startDato != krav.muligRettFra
                        )
            }
        val nyeVurderinger = kravSomManglerVurdering.map { vurderStønadsperiode(it, kontekst) }

        stønadsperiodeRepository.lagre(kontekst.behandlingId, vedtatteStønadsperiodeVurderinger + nyeVurderinger)
    }

    // TODO: Vurdere om vi skal ha egen kravtype for migrering?
    private fun vurderMigrertKravAutomatisk(kontekst: FlytKontekstMedPerioder) {
        kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.gjeldendeMigrertKrav()?.let { migrertKrav ->
            stønadsperiodeRepository.lagre(kontekst.behandlingId, setOf(StønadsperiodeVurdering(
                referanse = migrertKrav.referanse,
                opprettet = Instant.now(),
                vurdertIBehandling = kontekst.behandlingId,
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Migrert stønadsperiode fra Arenasak ${migrertKrav.arenaSaksnummer}",
                harHattOrdinærSiste52Uker = false,
                harGjenværendeKvote = false,
                relevantKravType = RelevantKravType.MIGRERT_STØNADSPERIODE,
                startDato = migrertKrav.muligRettFra
            )))
        }
    }

    // TODO: Antar ny stønadsperiode enn så lenge
    private fun vurderStønadsperiode(krav: RelevantKrav, kontekst: FlytKontekstMedPerioder): StønadsperiodeVurdering {
        return StønadsperiodeVurdering(
            referanse = krav.referanse,
            opprettet = Instant.now(),
            vurdertIBehandling = kontekst.behandlingId,
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Automatisk vurdering",
            harHattOrdinærSiste52Uker = false,
            harGjenværendeKvote = false,
            relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
            startDato = krav.muligRettFra // Dum heuristikk på at stønadsperiode start = kravdato start
        )
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return AvklarStønadsperiodeSteg(
                unleashGateway = gatewayProvider.provide(),
                kravRepository = repositoryProvider.provide(),
                stønadsperiodeRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_STØNADSPERIODE
        }
    }

}