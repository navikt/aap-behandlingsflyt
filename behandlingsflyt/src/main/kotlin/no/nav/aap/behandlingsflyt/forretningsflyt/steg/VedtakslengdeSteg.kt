package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Avslag
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
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
import java.time.Instant
import java.time.LocalDate

class VedtakslengdeSteg(
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val vedtakslengdeRepository: VedtakslengdeRepository,
    private val unleashGateway: UnleashGateway,
    private val clock: Clock = Clock.systemDefaultZone()
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        underveisRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        vedtakslengdeRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vedtattUnderveis = kontekst.forrigeBehandlingId?.let { underveisRepository.hentHvisEksisterer(it) }
        val sisteVedtatteUnderveisperiode = vedtattUnderveis?.perioder?.maxByOrNull { it.periode.tom }
        val rettighetstypeTidslinje = vilkårsresultatRepository.hent(kontekst.behandlingId).rettighetstypeTidslinje()

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (unleashGateway.isDisabled(BehandlingsflytFeature.ForlengelseIManuellBehandling)) {
                    return Fullført
                }

                lagreGjeldendeSluttdatoHvisIkkeEksisterer(sisteVedtatteUnderveisperiode, kontekst)
            }

            VurderingType.MIGRER_RETTIGHETSPERIODE -> {
                lagreGjeldendeSluttdatoHvisIkkeEksisterer(sisteVedtatteUnderveisperiode, kontekst)
            }

            VurderingType.UTVID_VEDTAKSLENGDE -> {
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
                } else {
                    log.info("Ingen utvidelse av vedtakslengde nødvendig")
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
        val forrigeUtvidelse = vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId)?.vurdering
        val utvidelseMedHverdager = when (forrigeUtvidelse?.utvidetMed) {
            null, ÅrMedHverdager.FØRSTE_ÅR -> ÅrMedHverdager.ANDRE_ÅR // Antar at man skal utvide med andre år dersom grunnlag ikke finnes
            ÅrMedHverdager.ANDRE_ÅR -> ÅrMedHverdager.TREDJE_ÅR
            ÅrMedHverdager.TREDJE_ÅR, ÅrMedHverdager.ANNET -> ÅrMedHverdager.ANNET
        }

        val nySluttdato = forrigeSluttdato.plussEtÅrMedHverdager(utvidelseMedHverdager)
        vedtakslengdeRepository.lagre(
            behandlingId, VedtakslengdeVurdering(
                sluttdato = nySluttdato,
                utvidetMed = utvidelseMedHverdager,
                vurdertAv = SYSTEMBRUKER,
                vurdertIBehandling = behandlingId,
                opprettet = Instant.now()
            )
        )
    }

    // Det finnes en fremtidig periode med ordinær rett og gjenværende kvote
    fun harFremtidigRettOrdinær(
        vedtattSluttdato: LocalDate,
        rettighetstypeTidslinjeForInneværendeBehandling: Tidslinje<RettighetsType>
    ): Boolean {
        val varighetstidslinje = VarighetRegel().simuler(rettighetstypeTidslinjeForInneværendeBehandling)
        return varighetstidslinje.begrensetTil(Periode(vedtattSluttdato.plusDays(1), Tid.MAKS))
            .segmenter()
            .any { varighetSegment ->
                varighetSegment.verdi.brukerAvKvoter.any { kvote -> kvote == Kvote.ORDINÆR }
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

        /**
         * Det første år inkluderes startdatoen, og en dag på slutten må trekkes ifra for at det skal bli 261 dager
         */
        val sluttdatoForBehandlingen = startdatoForBehandlingen
            .plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)

        /**
         * For behandlinger som har passert alle vilkår og vurderinger med kortere rettighetsperiode
         * enn "sluttdatoForBehandlingen" så vil det bli feil å vurdere underveis lenger enn faktisk rettighetsperiode.
         */
        val sluttdatoForBakoverkompabilitet = minOf(rettighetsperiode.tom, sluttdatoForBehandlingen)

        return Periode(rettighetsperiode.fom, sluttdatoForBakoverkompabilitet)
    }

    private fun lagreGjeldendeSluttdatoHvisIkkeEksisterer(
        sisteVedtatteUnderveisperiode: Underveisperiode?,
        kontekst: FlytKontekstMedPerioder
    ) {
        val vedtattVedtakslengdeGrunnlag =
            kontekst.forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(kontekst.forrigeBehandlingId) }

        if (vedtattVedtakslengdeGrunnlag == null) {
            val sluttdato = if (sisteVedtatteUnderveisperiode != null) {
                sisteVedtatteUnderveisperiode.periode.tom
            } else {
                // Initiell sluttdato skal samsvare med utledet i UnderveisService
                utledInitiellSluttdato(kontekst.behandlingId, kontekst.rettighetsperiode).tom
            }

            // Skal lagre ned vedtakslengde for eksisterende behandlinger som mangler dette
            vedtakslengdeRepository.lagre(
                kontekst.behandlingId, VedtakslengdeVurdering(
                    sluttdato = sluttdato,
                    utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = kontekst.behandlingId,
                    opprettet = Instant.now()
                )
            )
        }
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

