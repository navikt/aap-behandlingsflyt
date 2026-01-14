package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Avslag
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate

class VedtakslengdeSteg(
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val vedtakslengdeRepository: FakeVedtakslengdeRepository,
    private val unleashGateway: UnleashGateway,
    private val clock: Clock = Clock.systemDefaultZone()
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        underveisRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        vedtakslengdeRepository = FakeVedtakslengdeRepository(),
        unleashGateway = gatewayProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.Forlengelse)) {
            return Fullført
        }

        val vedtakslengdeRepository = FakeVedtakslengdeRepository()

        val vedtattUnderveis = kontekst.forrigeBehandlingId?.let { underveisRepository.hentHvisEksisterer(it) }
        val sisteVedtatteUnderveisperiode = vedtattUnderveis?.perioder?.maxByOrNull { it.periode.tom }
        val rettighetstypeTidslinje = vilkårsresultatRepository.hent(kontekst.behandlingId).rettighetstypeTidslinje()

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                // Initiell sluttdato skal samsvare med utledet i UnderveisService
                if (sisteVedtatteUnderveisperiode == null) {
                    val initiellSluttdato = utledInitiellSluttdato(kontekst.behandlingId, kontekst.rettighetsperiode)
                    vedtakslengdeRepository.lagre(
                        kontekst.behandlingId, VedtakslengdeGrunnlag(
                            dato = initiellSluttdato.tom,
                            utvidetMedHverdager = ÅrMedHverdager.FØRSTE_ÅR
                        )
                    )
                } else if (
                // I førsteomgang ønsker vi ikke utvide ved revurdering, men kan bli noe sånt:
                    skalUtvide(
                        forrigeSluttdato = sisteVedtatteUnderveisperiode.periode.tom,
                        rettighetstypeTidslinjeForInneværendeBehandling = rettighetstypeTidslinje
                    )
                ) {
                    utvidSluttdato(
                        kontekst.behandlingId,
                        kontekst.forrigeBehandlingId,
                        sisteVedtatteUnderveisperiode.periode.tom
                    )
                }
            }

            VurderingType.AUTOMATISK_OPPDATER_VILKÅR -> {
                if (sisteVedtatteUnderveisperiode == null) {
                    log.info("Ingen vedtakslengde å utvide")
                    return Fullført
                }
                if (
                    skalUtvide(
                        forrigeSluttdato = sisteVedtatteUnderveisperiode.periode.tom,
                        rettighetstypeTidslinjeForInneværendeBehandling = rettighetstypeTidslinje
                    )
                ) {
                    utvidSluttdato(
                        kontekst.behandlingId,
                        kontekst.forrigeBehandlingId,
                        sisteVedtatteUnderveisperiode.periode.tom
                    )
                }

            }

            else -> {} // Noop
        }


        return Fullført
    }

    fun skalUtvide(
        forrigeSluttdato: LocalDate,
        rettighetstypeTidslinjeForInneværendeBehandling: Tidslinje<RettighetsType>
    ): Boolean {
        return harFremtidigRettOrdinær(forrigeSluttdato, rettighetstypeTidslinjeForInneværendeBehandling)
                && LocalDate.now(clock).plusDays(28) >= forrigeSluttdato

    }

    fun utvidSluttdato(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId,
        forrigeSluttdato: LocalDate,
    ) {
        val forrigeUtvidelse = vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId)
        val utvidelseMedHverdager = when (forrigeUtvidelse?.utvidetMedHverdager) {
            null, ÅrMedHverdager.FØRSTE_ÅR -> ÅrMedHverdager.ANDRE_ÅR // Antar at man skal utvide med andre år dersom grunnlag ikke finnes
            ÅrMedHverdager.ANDRE_ÅR -> ÅrMedHverdager.TREDJE_ÅR
            ÅrMedHverdager.TREDJE_ÅR, ÅrMedHverdager.ANNET -> ÅrMedHverdager.ANNET
        }

        val nySluttdato = forrigeSluttdato.plussEtÅrMedHverdager(utvidelseMedHverdager)
        vedtakslengdeRepository.lagre(
            behandlingId, VedtakslengdeGrunnlag(
                dato = nySluttdato,
                utvidetMedHverdager = utvidelseMedHverdager
            )
        )
    }

    // Det finnes en fremtidig periode med ordinær rett og gjenværende kvote
    fun harFremtidigRettOrdinær(
        vedtattSluttdato: LocalDate,
        rettighetstypeTidslinjeForInneværendeBehandling: Tidslinje<RettighetsType>
    ): Boolean {
        val varighetstidslinje = VarighetRegel().simluer(rettighetstypeTidslinjeForInneværendeBehandling)
        return varighetstidslinje.begrensetTil(Periode(vedtattSluttdato.plusDays(1), Tid.MAKS))
            .segmenter()
            .any { varighetSegment -> varighetSegment.verdi.brukerAvKvoter.any { kvote -> kvote == Kvote.ORDINÆR } 
                        && varighetSegment.verdi !is Avslag
            }

    }

    private fun utledInitiellSluttdato(
        behandlingId: BehandlingId,
        rettighetsperiode: Periode
    ): Periode {
        val startdatoForBehandlingen =
            VirkningstidspunktUtleder(vilkårsresultatRepository).utledVirkningsTidspunkt(behandlingId)
                ?: rettighetsperiode.fom

        val sluttdatoForBehandlingen = startdatoForBehandlingen
            .plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)

        /**
         * For behandlinger som har passert alle vilkår og vurderinger med kortere rettighetsperiode
         * enn "sluttdatoForBehandlingen" så vil det bli feil å vurdere underveis lenger enn faktisk rettighetsperiode.
         */
        val sluttdatoForBakoverkompabilitet = minOf(rettighetsperiode.tom, sluttdatoForBehandlingen)

        return Periode(rettighetsperiode.fom, sluttdatoForBakoverkompabilitet)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VedtakslengdeSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_VEDTAKSLENGDE
        }
    }
}

class FakeVedtakslengdeRepository() {
    var vedtakslengdeGrunnlag: VedtakslengdeGrunnlag? = null

    fun hentHvisEksisterer(behandlingId: BehandlingId): VedtakslengdeGrunnlag? {
        return vedtakslengdeGrunnlag
    }

    fun lagre(
        behandlingId: BehandlingId,
        vedtakslengdeGrunnlag: VedtakslengdeGrunnlag
    ) {
        this.vedtakslengdeGrunnlag = vedtakslengdeGrunnlag
    }
}

data class VedtakslengdeGrunnlag(
    val dato: LocalDate,
    val utvidetMedHverdager: ÅrMedHverdager
)



