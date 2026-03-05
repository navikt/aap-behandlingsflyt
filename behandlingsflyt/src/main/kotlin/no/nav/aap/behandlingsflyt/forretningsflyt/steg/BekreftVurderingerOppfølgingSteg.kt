package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.Rolle

class BekreftVurderingerOppfølgingSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.BekreftVurderingerOppfolging)) {
            return Fullført
        }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val vedtakBehøverVurdering = vedtakBehøverVurdering(kontekst, avklaringsbehovene)
        val erTilstrekkeligVurdert = erTilstrekkeligVurdert(avklaringsbehovene)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.BEKREFT_VURDERINGER_OPPFØLGING,
            vedtakBehøverVurdering = { vedtakBehøverVurdering },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert },
            tilbakestillGrunnlag = {},
            kontekst = kontekst
        )

        return Fullført
    }

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
            /// Blir kanskje feil?
            return false
        }

        val sykdomsbehovLøstAvKontor = sykdomsbehovLøstAvKontorIDenneBehandlingen(avklaringsbehovene)
        return sykdomsbehovLøstAvKontor.isNotEmpty()
    }

    private fun erTilstrekkeligVurdert(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val sykdomsbehovSistLøstAvKontor = sykdomsbehovLøstAvKontorIDenneBehandlingen(avklaringsbehovene)
            .map { behov -> behov.aktivHistorikk.last() }
            .filter { it.status == Status.AVSLUTTET }
        val sistBekreftet =
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.BEKREFT_VURDERINGER_OPPFØLGING)?.aktivHistorikk?.last()?.tidsstempel

        return when {
            sykdomsbehovSistLøstAvKontor.isEmpty() -> true
            sistBekreftet == null -> false
            else -> sykdomsbehovSistLøstAvKontor.all { nyesteSykdomsløsning ->
                nyesteSykdomsløsning.tidsstempel.isBefore(
                    sistBekreftet
                )
            }
        }
    }

    private fun sykdomsbehovLøstAvKontorIDenneBehandlingen(avklaringsbehovene: Avklaringsbehovene): List<Avklaringsbehov> {
        return avklaringsbehovene
            .alle()
            .filter { it.løsesISteg().gruppe == StegGruppe.SYKDOM }
            .filterNot { it.løsesISteg() == type() }
            .filter { it.definisjon.løsesAv.contains(Rolle.SAKSBEHANDLER_OPPFOLGING) }
            .filter { it.aktivHistorikk.any { it.status == Status.AVSLUTTET } }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider,
        ): BehandlingSteg {
            return BekreftVurderingerOppfølgingSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                unleashGateway = gatewayProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.BEKREFT_VURDERINGER_OPPFØLGING
        }
    }
}