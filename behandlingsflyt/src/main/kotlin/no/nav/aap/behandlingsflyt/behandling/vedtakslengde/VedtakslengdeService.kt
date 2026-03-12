package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class VedtakslengdeService(
    private val vedtakslengdeRepository: VedtakslengdeRepository,
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val rettighetstypeService: RettighetstypeService,
    private val stansOpphørRepository: StansOpphørRepository,
    private val unleashGateway: UnleashGateway,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    companion object {
        const val ANTALL_DAGER_FØR_UTVIDELSE = 28L
    }
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vedtakslengdeRepository =  repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider),
        stansOpphørRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun hentSakerAktuelleForUtvidelseAvVedtakslengde(datoForUtvidelse: LocalDate): Set<SakId> {
        return underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(datoForUtvidelse)
    }

    fun hentNesteVedtakslengdeUtvidelse(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?
    ): VedtakslengdeUtvidelse {
        val vedtakslengdeGrunnlag = forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) }
        val vedtattSluttdato = requireNotNull(hentVedtattSluttdato(forrigeBehandlingId, vedtakslengdeGrunnlag)) {
            "Kan ikke utlede vedtatt sluttdato for behandling $forrigeBehandlingId, som trengs for å vurdere utvidelse av vedtakslengde for behandling $behandlingId"
        }

        val nesteÅrligeUtvidelse = hentNesteÅrligeUtvidelse(vedtakslengdeGrunnlag?.gjeldendeVurdering())
        val vedtattSluttdatoUtvidetMedEttÅr = vedtattSluttdato.plussEtÅrMedHverdager(nesteÅrligeUtvidelse)
        val fremtidigBistandsbehovRettighetsperioder = hentPerioderMedBistandsbehovRettighet(vedtattSluttdato.plusDays(1), behandlingId)

        return when (fremtidigBistandsbehovRettighetsperioder) {
            // Ingen perioder å utvide for
            is BistandsbehovRettighetsperioder.IngenPerioder ->
                VedtakslengdeUtvidelse.IngenFremtidigBistandsbehovRettighet

            // Flere ikke-sammenhengende perioder eller en sammenhengende periode som starter senere enn forige vedtakSluttdato
            is BistandsbehovRettighetsperioder.FlereIkkeSammenhengendePerioder,
            is BistandsbehovRettighetsperioder.EnSammenhengendePeriodeFraSenereDato ->
                VedtakslengdeUtvidelse.Manuell(
                    forrigeSluttdato = vedtattSluttdato,
                    avslagsårsaker = emptySet(),
                )

            // En sammenhengende periode med bistandsbehovrettighet - kan vurderes for automatisk utvidelse
            is BistandsbehovRettighetsperioder.EnSammenhengendePeriodeFraAngittDato ->
                if (fremtidigBistandsbehovRettighetsperioder.periode.tom > vedtattSluttdatoUtvidetMedEttÅr) {
                    // Den vanlige varianten hvor vi utvider med et helt år
                    VedtakslengdeUtvidelse.Automatisk(
                        forrigeSluttdato = vedtattSluttdato,
                        nySluttdato = vedtattSluttdatoUtvidetMedEttÅr,
                    )
                } else {
                    // Har ett år eller mindre gjenstående med bistandsbehovrettighet - sjekker for årsaker
                    val stansEllerOpphørFom = fremtidigBistandsbehovRettighetsperioder.periode.tom.plusDays(1)
                    val avslagsårsaker = hentAvslagsårsakerVedStansEllerOpphør(behandlingId, stansEllerOpphørFom)

                    val kanBehandlesAutomatisk =
                        unleashGateway.isEnabled(BehandlingsflytFeature.UtvidVedtakslengdeUnderEttAr)
                                && gyldigForAutomatiskUtvidelseAvVedtakslengde(avslagsårsaker)

                    if (kanBehandlesAutomatisk) {
                        VedtakslengdeUtvidelse.Automatisk(
                            forrigeSluttdato = vedtattSluttdato,
                            nySluttdato = fremtidigBistandsbehovRettighetsperioder.periode.tom,
                            avslagsårsaker = avslagsårsaker,
                        )
                    } else {
                        VedtakslengdeUtvidelse.Manuell(
                            forrigeSluttdato = vedtattSluttdato,
                            avslagsårsaker = avslagsårsaker,
                        )
                    }
                }
        }
    }

    fun utvidVedtakslengde(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        vedtakslengdeUtvidelse: VedtakslengdeUtvidelse.Automatisk,
    ) {
        val vedtattVedtakslengdeGrunnlag = forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) }
        val nesteÅrligeUtvidelse = hentNesteÅrligeUtvidelse(vedtattVedtakslengdeGrunnlag?.gjeldendeVurdering())

        val vedtattVurderinger = vedtattVedtakslengdeGrunnlag?.vurderinger.orEmpty()
        val nyAutomatiskVurdering = VedtakslengdeVurdering(
            sluttdato = vedtakslengdeUtvidelse.nySluttdato,
            utvidetMed = nesteÅrligeUtvidelse,
            vurdertAv = SYSTEMBRUKER,
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(clock),
            begrunnelse = "Automatisk vurdert"
        )

        vedtakslengdeRepository.lagre(
            behandlingId, vedtattVurderinger + nyAutomatiskVurdering
        )
    }

    fun hentAvslagsårsakerVedStansEllerOpphør(
        behandlingId: BehandlingId,
        stansEllerOpphørFom: LocalDate
    ): Set<Avslagsårsak> {
        val stansOpphørGrunnlag = stansOpphørRepository.hentHvisEksisterer(behandlingId)
        val gjeldendeStansEllerOpphør = stansOpphørGrunnlag?.gjeldendeStansOgOpphør()
        val avslagsårsakerFørsteDagUtenBistandsbehovRettighet = gjeldendeStansEllerOpphør
            ?.filter { it.fom == stansEllerOpphørFom }
            ?.flatMap { it.vurdering.årsaker }
            ?.toSet() ?: emptySet()

        return `avslagsårsakerFørsteDagUtenBistandsbehovRettighet`
    }

    fun lagreGjeldendeSluttdato(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        rettighetsperiode: Periode,
    ) {
        val vedtattVedtakslengdeGrunnlag =
            forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(it) }
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtattVedtakslengdeGrunnlag)
        val vedtattUtvidelse = vedtattVedtakslengdeGrunnlag?.gjeldendeVurdering()?.utvidetMed
        val sluttdato = utledSluttdato(behandlingId, rettighetsperiode, vedtattSluttdato)

        val erSluttdatoEndret = vedtattVedtakslengdeGrunnlag == null || vedtattVedtakslengdeGrunnlag.gjeldendeVurdering()?.sluttdato != sluttdato

        if (erSluttdatoEndret) {
            log.info("Sluttdato endret fra $vedtattSluttdato til $sluttdato for behandling $behandlingId")

            val tidligereVurderinger = vedtattVedtakslengdeGrunnlag?.vurderinger.orEmpty()
            vedtakslengdeRepository.lagre(
                behandlingId, tidligereVurderinger + VedtakslengdeVurdering(
                    sluttdato = sluttdato,
                    utvidetMed = vedtattUtvidelse ?: ÅrMedHverdager.FØRSTE_ÅR,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(clock),
                    begrunnelse = "Automatisk vurdert"
                )
            )
        }
    }

    private fun utledSluttdato(
        behandlingId: BehandlingId,
        rettighetsperiode: Periode,
        vedtattSluttdato: LocalDate?,
    ): LocalDate {
        val rettighetstypeTidslinje = rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandlingId)
        val initiellSluttdato = utledInitiellSluttdato(behandlingId, rettighetsperiode).tom
        val gjeldendeSluttdato = vedtattSluttdato ?: utledInitiellSluttdato(behandlingId, rettighetsperiode).tom

        // Hvis ingen rettighetstyper brukes gjeldende sluttdato
        if (rettighetstypeTidslinje.isEmpty()) {
            log.info("Ingen rettighetstyper, bruker gjeldende sluttdato: $gjeldendeSluttdato")
            return gjeldendeSluttdato
        }

        val sluttdatoSisteUnntaksrettighet = rettighetstypeTidslinje.segmenter()
            .findLast { it.verdi in unntaksrettighetstyper() }
            ?.periode?.tom

        log.info("Sluttdato for siste unntaksrettighet: $sluttdatoSisteUnntaksrettighet")

        val sluttdatoSisteBistandsbehov = rettighetstypeTidslinje.segmenter()
            .findLast { it.verdi == RettighetsType.BISTANDSBEHOV }
            ?.periode?.tom

        log.info("Sluttdato for siste bistandsbehov: $sluttdatoSisteBistandsbehov")

        // Logikken rundt sluttdato når det ligger et bistandsbehov til slutt, må gåes opp. Inntil videre gis det
        // opp til initiell sluttdato / vedtatt sluttdato.
        val sluttdatoBistandsbehov = if (sluttdatoSisteBistandsbehov != null) {
            // Returnere til og med kvote-slutt dersom denne datoen kommer før utledet sluttdato
            listOfNotNull(sluttdatoSisteBistandsbehov, initiellSluttdato).min()
        } else null

        val kandidaterForSluttdato =  listOfNotNull(sluttdatoSisteUnntaksrettighet, sluttdatoBistandsbehov, vedtattSluttdato)

        // Tillater ikke innskrenkelse av vedtakslengde da forrige vedtak kan ha sendt over perioder til utbetaling
        val sluttdatoForBehandlingen = kandidaterForSluttdato.max()

        log.info("Setter sluttdato: $sluttdatoForBehandlingen")
        return sluttdatoForBehandlingen
    }

    @Deprecated("Den første varianten - denne vil utvide med ett år uavhengig av rettighetstype")
    fun lagreGjeldendeSluttdatoHvisIkkeEksisterer(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        rettighetsperiode: Periode,
    ) {
        val vedtakslengdeGrunnlag = forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) }
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtakslengdeGrunnlag)

        if (vedtakslengdeGrunnlag == null) {
            val sluttdato = vedtattSluttdato ?: utledInitiellSluttdato(behandlingId, rettighetsperiode).tom

            // Skal lagre ned vedtakslengde for eksisterende behandlinger som mangler dette
            log.info("Lagrer VedtakslengdeVurdering med sluttdato=$sluttdato")

            vedtakslengdeRepository.lagre(
                behandlingId, listOf(VedtakslengdeVurdering(
                    sluttdato = sluttdato,
                    utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(clock),
                    begrunnelse = "Automatisk vurdert"))
            )
        }
    }

    /**
     * Henter siste vedtatte sluttdato. Bruker den største verdien da vi ikke ønsker å redusere vedtakslengden.
     */
    private fun hentVedtattSluttdato(forrigeBehandlingId: BehandlingId?, vedtakslengdeGrunnlag: VedtakslengdeGrunnlag?): LocalDate? {
        val vedtattUnderveis = forrigeBehandlingId?.let { underveisRepository.hentHvisEksisterer(it) }
        val sluttdatoSisteVedtatteUnderveis = vedtattUnderveis?.perioder?.maxByOrNull { it.periode.tom }?.periode?.tom
        val sluttdatoSisteVedtatteVedtakslengdeVurdering = vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato

        return listOfNotNull(sluttdatoSisteVedtatteUnderveis, sluttdatoSisteVedtatteVedtakslengdeVurdering).maxOrNull()
    }

    private fun hentNesteÅrligeUtvidelse(forrigeUtvidelse: VedtakslengdeVurdering?): ÅrMedHverdager =
        when (forrigeUtvidelse?.utvidetMed) {
            null, ÅrMedHverdager.FØRSTE_ÅR -> ÅrMedHverdager.ANDRE_ÅR // Antar at man skal utvide med andre år dersom grunnlag ikke finnes
            ÅrMedHverdager.ANDRE_ÅR -> ÅrMedHverdager.TREDJE_ÅR
            ÅrMedHverdager.TREDJE_ÅR, ÅrMedHverdager.ANNET -> ÅrMedHverdager.ANNET
        }

    private fun utledInitiellSluttdato(
        behandlingId: BehandlingId,
        rettighetsperiode: Periode,
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

    /**
     * Henter perioder med bistandsbehov rettighetstype fom fraDato og frem i tid
     */
    private fun hentPerioderMedBistandsbehovRettighet(
        fraDato: LocalDate,
        behandlingId: BehandlingId,
    ): BistandsbehovRettighetsperioder {
        val rettighetstypeTidslinje = rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandlingId)
        val periodeMedMuligBistandsbehovRettighet = Periode(fraDato, Tid.MAKS)

        val perioder = rettighetstypeTidslinje
            .begrensetTil(periodeMedMuligBistandsbehovRettighet)
            .filter { rettighetstype -> rettighetstype.verdi == RettighetsType.BISTANDSBEHOV }
            .komprimer()
            .segmenter()
            .map { it.periode }

        return when {
            perioder.isEmpty() -> BistandsbehovRettighetsperioder.IngenPerioder
            perioder.size == 1 && perioder.single().fom == fraDato ->
                BistandsbehovRettighetsperioder.EnSammenhengendePeriodeFraAngittDato(perioder.single())
            perioder.size == 1 && perioder.single().fom > fraDato ->
                BistandsbehovRettighetsperioder.EnSammenhengendePeriodeFraSenereDato(perioder.single())
            else -> BistandsbehovRettighetsperioder.FlereIkkeSammenhengendePerioder(perioder)
        }
    }

    private fun gyldigForAutomatiskUtvidelseAvVedtakslengde(avslagsårsaker: Set<Avslagsårsak>): Boolean {
        return avslagsårsaker.isNotEmpty() && gyldigeAvslagsårsakerForAutomatiskBehandling().containsAll(avslagsårsaker)
    }

    private fun unntaksrettighetstyper() = setOf(
        RettighetsType.SYKEPENGEERSTATNING,
        RettighetsType.STUDENT,
        RettighetsType.VURDERES_FOR_UFØRETRYGD,
        RettighetsType.ARBEIDSSØKER
    )

    /**
     * Følgende avslagsårsaker er støttet ved automatisk behandling av vedtakslengde
     */
    private fun gyldigeAvslagsårsakerForAutomatiskBehandling() =
        setOf(
            Avslagsårsak.BRUKER_OVER_67,
            Avslagsårsak.IKKE_MEDLEM, // TODO ikke riktig Avslagstype?
            Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP,
            Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS,
            Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING,
            Avslagsårsak.ANNEN_FULL_YTELSE
        )
}

private sealed class BistandsbehovRettighetsperioder {
    data object IngenPerioder : BistandsbehovRettighetsperioder()
    data class EnSammenhengendePeriodeFraAngittDato(val periode: Periode) : BistandsbehovRettighetsperioder()
    data class EnSammenhengendePeriodeFraSenereDato(val periode: Periode) : BistandsbehovRettighetsperioder()
    data class FlereIkkeSammenhengendePerioder(val perioder: List<Periode>) : BistandsbehovRettighetsperioder()
}
