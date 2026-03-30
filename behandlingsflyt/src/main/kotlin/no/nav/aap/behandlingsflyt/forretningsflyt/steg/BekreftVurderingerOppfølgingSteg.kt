package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingService
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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.Rolle
import java.time.LocalDate

class BekreftVurderingerOppfølgingSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val mellomlagretVurderingService: MellomlagretVurderingService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.BekreftVurderingerOppfolging)) {
            return Fullført
        }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val vedtakBehøverVurdering = vedtakBehøverVurdering(kontekst, avklaringsbehovene)
        val erTilstrekkeligVurdert = erTilstrekkeligVurdert(avklaringsbehovene, kontekst.behandlingId)

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
        kontekst: FlytKontekstMedPerioder, avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    /// Blir kanskje feil?
                    return false
                }

                val sykdomsbehovLøstAvKontor = sykdomsbehovLøstAvKontorIDenneBehandlingen(avklaringsbehovene)
                return sykdomsbehovLøstAvKontor.isNotEmpty()
            }

            VurderingType.UTVID_VEDTAKSLENGDE, VurderingType.MIGRER_RETTIGHETSPERIODE, VurderingType.MELDEKORT, VurderingType.AUTOMATISK_BREV, VurderingType.EFFEKTUER_AKTIVITETSPLIKT, VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9, VurderingType.IKKE_RELEVANT -> false
        }
    }


    private fun erTilstrekkeligVurdert(
        avklaringsbehovene: Avklaringsbehovene, behandlingId: BehandlingId
    ): Boolean {
        val sykdomsbehovSistLøstAvKontor =
            sykdomsbehovLøstAvKontorIDenneBehandlingen(avklaringsbehovene).mapNotNull { behov ->
                behov.aktivHistorikk.lastOrNull { it.status == Status.AVSLUTTET }
                    ?.let { Pair(behov.definisjon, it) }
            }

        val sistBekreftet =
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.BEKREFT_VURDERINGER_OPPFØLGING)
                ?.aktivHistorikk
                ?.lastOrNull { endring -> endring.status == Status.AVSLUTTET }
                ?.tidsstempel

        val finnesMellomlagredeVurderingerForRelevanteBehov =
            mellomlagretVurderingService.hentMellomlagredeVurderingerFørSteg(
                behandlingId, type(), listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
            ).any { mellomlagretVurdering ->
                // Filtrer vekk avbrutte behov som kan ha mellomlagrede vurderinger som det ikke er mulig for saksbehandler å slette
                // Bør nok heller løses ved automatisk sletting ved avbrutt i avklaringsbehovservice
                sykdomsbehovSistLøstAvKontor.map { it.first.kode }.contains(mellomlagretVurdering.avklaringsbehovKode)
            }

        return when {
            finnesMellomlagredeVurderingerForRelevanteBehov -> false
            sykdomsbehovSistLøstAvKontor.isEmpty() -> true
            sistBekreftet == null -> false
            else -> sykdomsbehovSistLøstAvKontor.all { (_, nyesteSykdomsløsning) ->
                nyesteSykdomsløsning.tidsstempel.isBefore(
                    sistBekreftet
                )
            }
        }
    }

    private fun sykdomsbehovLøstAvKontorIDenneBehandlingen(avklaringsbehovene: Avklaringsbehovene): List<Avklaringsbehov> {
        return avklaringsbehovene.alle().filter { it.løsesISteg().gruppe == StegGruppe.SYKDOM }
            .filterNot { it.løsesISteg() == type() }
            .filter { it.definisjon.løsesAv.contains(Rolle.SAKSBEHANDLER_OPPFOLGING) }
            .filter {
                it.aktivHistorikk.any { endring ->
                    endring.status == Status.AVSLUTTET && (!Miljø.erProd() || endring.tidsstempel.toLocalDate().isAfter(
                        LocalDate.of(2026, 3, 25)
                    )) // Hack for å unngå at man må bekrefte behov som ble utført før steget fantes. Bør se på en bedre løsning
                }
            }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider,
        ): BehandlingSteg {
            return BekreftVurderingerOppfølgingSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                mellomlagretVurderingService = MellomlagretVurderingService(repositoryProvider),
                unleashGateway = gatewayProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.BEKREFT_VURDERINGER_OPPFØLGING
        }
    }
}