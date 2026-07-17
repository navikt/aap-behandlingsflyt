package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningMedPeriodiserteVurderinger
import no.nav.aap.behandlingsflyt.behandling.krav.KravService
import no.nav.aap.behandlingsflyt.behandling.krav.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.utils.tilNorskFormat
import no.nav.aap.behandlingsflyt.utils.toHumanReadable
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

class AvklaringsbehovValidering(
    private val repositoryProvider: RepositoryProvider,
    gatewayProvider: GatewayProvider
) {
    private val unleashGateway: UnleashGateway = gatewayProvider.provide()
    private val sakRepository: SakRepository = repositoryProvider.provide()
    private val kravService: KravService = KravService(repositoryProvider, gatewayProvider)

    fun validerPerioder(
        bruker: Bruker,
        avklaringsbehovene: Avklaringsbehovene,
        løsning: PeriodisertAvklaringsbehovLøsning<*>,
        kontekst: FlytKontekst,
    ) {
        if (løsning.definisjon().erFrivillig()
            && løsning.løsningerForPerioder.isEmpty()
        ) {
            return
        }

        validerMotKravPerioder(bruker, kontekst, løsning)
        validerMotPerioderVedtakBehøverVurdering(kontekst, løsning, avklaringsbehovene)
    }

    private fun validerMotKravPerioder(
        bruker: Bruker,
        kontekst: FlytKontekst,
        løsning: PeriodisertAvklaringsbehovLøsning<*>
    ) {
        if (løsning is LøsningMedPeriodiserteVurderinger) {
            val vedtatteVurderinger = kontekst.forrigeBehandlingId?.let {
                løsning.hentVurderinger(
                    kontekst.forrigeBehandlingId,
                    repositoryProvider
                )
            }.orEmpty()
            val nye = løsning.somVurderinger(bruker, kontekst.behandlingId) + vedtatteVurderinger
            val kravMedUgyldigLøsning =
                nårKravHarLøsning(løsning.definisjon(), nye.gjeldendeVurderinger(), kontekst)
                    .segmenter()
                    .filter { !it.verdi }
            if (kravMedUgyldigLøsning.isNotEmpty()) {
                throw UgyldigForespørselException(
                    "Mangler vurderinger på eller etter dato ${
                        kravMedUgyldigLøsning.map { it.periode.fom.tilNorskFormat() }.joinToString(",")
                    } på grunn av krav"
                )
            }
        }
    }

    private fun validerMotPerioderVedtakBehøverVurdering(
        kontekst: FlytKontekst,
        løsning: PeriodisertAvklaringsbehovLøsning<*>,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        val behovForDefinisjon = avklaringsbehovene.hentBehovForDefinisjon(løsning.definisjon())
        if (behovForDefinisjon != null) {

            val perioderDekketAvLøsning = løsning.løsningerForPerioder.sortedBy { it.fom }
                .somTidslinje { Periode(fom = it.fom, tom = it.tom ?: Tid.MAKS) }
                .map { true }.komprimer()

            val perioderDekketAvVedtatteVurderinger = kontekst.forrigeBehandlingId?.let {
                løsning.hentLagredeLøstePerioder(it, repositoryProvider)
            }.orEmpty().map { true }.komprimer()

            val perioderDekket = perioderDekketAvVedtatteVurderinger.kombiner(
                perioderDekketAvLøsning,
                StandardSammenslåere.prioriterHøyreSideCrossJoin()
            ).komprimer()

            val perioderSomSkalLøses =
                behovForDefinisjon.perioderVedtaketBehøverVurdering().orEmpty().somTidslinje { it }

            val perioderSomManglerLøsning =
                perioderSomSkalLøses.leftJoin(perioderDekket) { _, periodeILøsning ->
                    periodeILøsning != null
                }.filter { !it.verdi }.perioder().toSet()

            if (perioderSomManglerLøsning.isNotEmpty()) {
                throw UgyldigForespørselException("Du mangler vurdering for ${perioderSomManglerLøsning.toHumanReadable()}")
            }
        }
    }

    fun nårKravHarLøsning(
        definisjon: Definisjon,
        gjeldendeVurderinger: Tidslinje<out PeriodisertVurdering>? = null,
        kontekst: FlytKontekst,
    ): Tidslinje<Boolean> {
        if (gjeldendeVurderinger == null) {
            /**
             * TODO: Fjern nullable gjeldendeVurderinger når alle steg er oppdatert
             * Dette er caset der vi ikke har oppdatert steget til å sende inn gjeldende vurderinger inn i avklaringsbehovservice.
             * Dette skal på sikt gjøres for alle periodiserte steg, og da skal denne casen fjernes.
             */
            return Tidslinje.empty()
        }

        if (!unleashGateway.erPåskruddForSak(
                BehandlingsflytFeature.NyttKravPeriodiserteAvklaringsbehov,
                "saksnumre"
            ) { sakRepository.hent(kontekst.sakId).saksnummer }
        ) {
            return Tidslinje.empty()
        }
        return kravService.kravtypeTidslinje(kontekst)
            .map { kravPeriode, kravType ->
                erKravDekketAvLøsning(
                    kravPeriode,
                    kravType,
                    definisjon,
                    gjeldendeVurderinger
                )
            }
    }

    private fun erKravDekketAvLøsning(
        kravPeriode: Periode,
        kravType: RelevantKravType,
        definisjon: Definisjon, gjeldendeVurderinger: Tidslinje<out PeriodisertVurdering>,
    ): Boolean {
        return when (kravType) {
            RelevantKravType.NYTT_KRAV -> harVurderingPåEllerEtterMuligRettFra(gjeldendeVurderinger, kravPeriode)

            RelevantKravType.GJENOPPTAK_ETTER_STANS -> true
            RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR -> !definisjon.måRevurderesEtterOpphør || gjeldendeVurderinger.segmenter()
                .any { (vurderingPeriode, _) -> kravPeriode.inneholder(vurderingPeriode.fom) }
        }
    }

    private fun harVurderingPåEllerEtterMuligRettFra(
        gjeldendeVurderinger: Tidslinje<out PeriodisertVurdering>,
        kravPeriode: Periode
    ): Boolean = gjeldendeVurderinger.segmenter().any { (vurderingPeriode, _) ->
        kravPeriode.inneholder(vurderingPeriode.fom)
    }
}