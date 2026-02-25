package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.utledStansEllerOpphør
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
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
import no.nav.aap.komponenter.tidslinje.Tidslinje
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
        unleashGateway = gatewayProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun hentSakerAktuelleForUtvidelseAvVedtakslengde(datoForUtvidelse: LocalDate): Set<SakId> {
        return underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(datoForUtvidelse)
    }

    fun skalUtvideSluttdato(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        datoForUtvidelse: LocalDate = LocalDate.now(clock).plusDays(ANTALL_DAGER_FØR_UTVIDELSE)
    ): Boolean {
        val vedtakslengdeGrunnlag = forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) }
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtakslengdeGrunnlag)

        if (vedtattSluttdato != null) {
            // Sjekker om det finnes en fremtidig perioder med ordinær rettighetstype (uavhengig av lengde)
            if (unleashGateway.isEnabled(BehandlingsflytFeature.UtvidVedtakslengdeUnderEttAr)) {
                val periodeMedFremtidigRettOrdinær = hentPeriodeMedFremtidigRettOrdinær(vedtattSluttdato, behandlingId)

                log.info("Behandling $behandlingId har periodeMedFremtidigRettOrdinær=$periodeMedFremtidigRettOrdinær og forrigeSluttdato=${vedtattSluttdato}")

                return datoForUtvidelse >= vedtattSluttdato && periodeMedFremtidigRettOrdinær != null
            }

            // Sjekker om det finnes en fremtidig periode med ordinær rettighetstype og at denne perioden er et helt år
            val nesteUtvidelse = hentNesteUtvidelse(vedtakslengdeGrunnlag?.vurdering)
            val utvidetSluttdato = vedtattSluttdato.plussEtÅrMedHverdager(nesteUtvidelse)

            val harFremtidigRettOrdinær = harFremtidigRettOrdinær(vedtattSluttdato, utvidetSluttdato, behandlingId)
            log.info("Behandling $behandlingId har harFremtidigRettOrdinær=$harFremtidigRettOrdinær og forrigeSluttdato=${vedtattSluttdato}")

            return datoForUtvidelse >= vedtattSluttdato && harFremtidigRettOrdinær
        } else {
            log.info("Behandling $behandlingId har ingen vedtatt sluttdato, ingen utvidelse nødvendig")
        }
        return false
    }

    fun utvidSluttdato(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        rettighetsperiode: Periode,
    ) {
        val vedtakslengdeGrunnlag = forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) }
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtakslengdeGrunnlag)

        if (vedtattSluttdato != null) {
            val forrigeUtvidelse = vedtakslengdeGrunnlag?.vurdering
            val utvidelse = hentNesteUtvidelse(forrigeUtvidelse)

            val nyForventetSluttdato = vedtattSluttdato.plussEtÅrMedHverdager(utvidelse)
            val periodeMedFremtidigRettOrdinær = hentPeriodeMedFremtidigRettOrdinær(vedtattSluttdato, behandlingId)

            if (unleashGateway.isEnabled(BehandlingsflytFeature.UtvidVedtakslengdeUnderEttAr)
                && periodeMedFremtidigRettOrdinær != null && periodeMedFremtidigRettOrdinær.tom < nyForventetSluttdato) {
                // Perioden med fremtidig ordinær rett utfyller ikke et helt år, bruker da siste dato og utleder
                // årsaken(e) til at ordinær rett ikke oppfylles slik at disse kan brukes for riktig tekst i brevet
                val avslagsårsaker = hentAvslagsårsaker(behandlingId, rettighetsperiode, periodeMedFremtidigRettOrdinær)

                // Sjekker at alle avslagsårsakene som utledes er gyldige for automatisk behandling
                verifiserGyldigeAvslagsårsakerForAutomatiskBehandling(avslagsårsaker)

                lagreVedtakslengdeVurdering(behandlingId, periodeMedFremtidigRettOrdinær.tom, utvidelse, avslagsårsaker)
            } else {
                // Utvider med ett år
                lagreVedtakslengdeVurdering(behandlingId, nyForventetSluttdato, utvidelse)
            }

        } else {
            log.info("Behandling $behandlingId har ingen vedtatt sluttdato, ingen utvidelse nødvendig")
        }
    }

    private fun verifiserGyldigeAvslagsårsakerForAutomatiskBehandling(
        avslagsårsaker: Set<Avslagsårsak>
    ) {
        if (avslagsårsaker.isEmpty()) {
            throw IllegalArgumentException("Forventer en avslagsårsak når ordinær rett er under ett år")
        }
        if (avslagsårsaker.any { it !in gyldigeAvslagsårsakerForAutomatiskBehandling() }) {
            throw IllegalArgumentException("Har avslagsårsaker $avslagsårsaker som ikke er gyldige for automatisk behandling ved utvidelse av vedtakslengde")
        }
    }

    private fun hentAvslagsårsaker(
        behandlingId: BehandlingId,
        rettighetsperiode: Periode,
        periodeMedFremtidigRettOrdinær: Periode
    ): Set<Avslagsårsak> {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val stansEllerOpphør = utledStansEllerOpphør(
            vilkårsresultat = vilkårsresultat,
            rettighetsperiode = rettighetsperiode
        )

        val stansEllerOpphørFom = periodeMedFremtidigRettOrdinær.tom.plusDays(1)
        val avslagsårsakerFørsteDagUtenOrdinærRettighet = stansEllerOpphør
            .filter { (fom, _) -> fom == stansEllerOpphørFom }.values
            .flatMap { it.årsaker }
            .toSet()

        return avslagsårsakerFørsteDagUtenOrdinærRettighet
    }

    fun lagreGjeldendeSluttdato(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        rettighetsperiode: Periode,
    ) {
        val vedtattVedtakslengdeGrunnlag =
            forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(it) }
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtattVedtakslengdeGrunnlag)
        val vedtattUtvidelse = vedtattVedtakslengdeGrunnlag?.vurdering?.utvidetMed
        val sluttdato = utledSluttdato(behandlingId, rettighetsperiode, vedtattSluttdato)

        val erSluttdatoEndret = vedtattVedtakslengdeGrunnlag == null || vedtattVedtakslengdeGrunnlag.vurdering.sluttdato != sluttdato

        if (erSluttdatoEndret) {
            log.info("Sluttdato endret fra $vedtattSluttdato til $sluttdato for behandling $behandlingId")

            lagreVedtakslengdeVurdering(behandlingId, sluttdato, vedtattUtvidelse ?: ÅrMedHverdager.FØRSTE_ÅR)
        }
    }

    private fun utledSluttdato(
        behandlingId: BehandlingId,
        rettighetsperiode: Periode,
        vedtattSluttdato: LocalDate?,
    ): LocalDate {
        val rettighetstypeTidslinje = rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandlingId)
        val initiellSluttdato = utledInitiellSluttdato(behandlingId, rettighetsperiode).tom

        // Ved avslag sett inntil ett år slik det var gjort tidligere - gå opp hva som er riktig å gjøre her
        if (rettighetstypeTidslinje.isEmpty()) {
            return initiellSluttdato
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
            lagreVedtakslengdeVurdering(behandlingId, sluttdato, ÅrMedHverdager.FØRSTE_ÅR)
        }
    }

    private fun lagreVedtakslengdeVurdering(
        behandlingId: BehandlingId,
        sluttdato: LocalDate,
        utvidelse: ÅrMedHverdager,
        sluttdatoBegrensetAv: Set<Avslagsårsak> = emptySet()
    ) {
        log.info("Lagrer VedtakslengdeVurdering med sluttdato=$sluttdato, utvidelse=$utvidelse og begrensetAv=$sluttdatoBegrensetAv")

        vedtakslengdeRepository.lagre(
            behandlingId, VedtakslengdeVurdering(
                sluttdato = sluttdato,
                sluttdatoBegrensetAv = sluttdatoBegrensetAv,
                utvidetMed = utvidelse,
                vurdertAv = SYSTEMBRUKER,
                vurdertIBehandling = behandlingId,
                opprettet = Instant.now(clock)
            )
        )
    }

    /**
     * Henter siste vedtatte sluttdato. Bruker den største verdien da vi ikke ønsker å redusere vedtakslengden.
     */
    private fun hentVedtattSluttdato(forrigeBehandlingId: BehandlingId?, vedtakslengdeGrunnlag: VedtakslengdeGrunnlag?): LocalDate? {
        val vedtattUnderveis = forrigeBehandlingId?.let { underveisRepository.hentHvisEksisterer(it) }
        val sluttdatoSisteVedtatteUnderveis = vedtattUnderveis?.perioder?.maxByOrNull { it.periode.tom }?.periode?.tom
        val sluttdatoSisteVedtatteVedtakslengdeVurdering = vedtakslengdeGrunnlag?.vurdering?.sluttdato

        return listOfNotNull(sluttdatoSisteVedtatteUnderveis, sluttdatoSisteVedtatteVedtakslengdeVurdering).maxOrNull()
    }

    private fun hentNesteUtvidelse(forrigeUtvidelse: VedtakslengdeVurdering?): ÅrMedHverdager =
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
     * Neste periode (hele året) er av type ordinær med gjenværende kvote
     */
    private fun harFremtidigRettOrdinær(
        vedtattSluttdato: LocalDate,
        utvidetSluttdato: LocalDate,
        behandlingId: BehandlingId,
    ): Boolean {
        val nyUtvidetVedtaksperiode = Periode(vedtattSluttdato.plusDays(1), utvidetSluttdato)
        val nyUtvidetVedtaksperiodeTidslinje = Tidslinje(nyUtvidetVedtaksperiode, true)
        val rettighetstypeTidslinje = rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandlingId)

        return rettighetstypeTidslinje
            .rightJoin(nyUtvidetVedtaksperiodeTidslinje) { rettighetstype, _ ->
                rettighetstype != null && rettighetstype.kvote == Kvote.ORDINÆR
            }
            .segmenter()
            .all { it.verdi }
    }

    /**
     * Henter neste periode med ordinær rettighetstype fra dagen etter gjeldende sluttdato for vedtaket
     */
    private fun hentPeriodeMedFremtidigRettOrdinær(
        vedtattSluttdato: LocalDate,
        behandlingId: BehandlingId,
    ): Periode? {
        val rettighetstypeTidslinje = rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandlingId)
        val dagenEtterVedtattSluttdato = vedtattSluttdato.plusDays(1)
        val fremtidigPeriodeMedMuligRett = Periode(dagenEtterVedtattSluttdato, Tid.MAKS)

        return rettighetstypeTidslinje
            .begrensetTil(fremtidigPeriodeMedMuligRett)
            .filter { rettighetstype -> rettighetstype.verdi.kvote == Kvote.ORDINÆR }
            .segmenter()
            .map { it.periode }
            .firstOrNull { it.fom == dagenEtterVedtattSluttdato }
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
            Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE, // TODO ikke riktig Avslagstype?
            Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP,
            Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS,
            Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING,
            Avslagsårsak.ANNEN_FULL_YTELSE
        )
}
