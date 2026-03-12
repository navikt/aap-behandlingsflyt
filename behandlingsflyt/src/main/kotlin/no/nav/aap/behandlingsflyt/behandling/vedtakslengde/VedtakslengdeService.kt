package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
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
    private val clock: Clock = Clock.systemDefaultZone()
) {
    companion object {
        const val ANTALL_DAGER_FØR_UTVIDELSE = 28L
    }
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vedtakslengdeRepository =  repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider)
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
            val nesteUtvidelse = hentNesteUtvidelse(vedtakslengdeGrunnlag?.gjeldendeVurdering())
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
    ) {
        val vedtakslengdeGrunnlag = forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) }
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtakslengdeGrunnlag)

        if (vedtattSluttdato != null) {
            val forrigeUtvidelse = vedtakslengdeGrunnlag?.gjeldendeVurdering()
            val utvidelse = hentNesteUtvidelse(forrigeUtvidelse)

            val nySluttdato = vedtattSluttdato.plussEtÅrMedHverdager(utvidelse)
            val tidligereVurderinger = vedtakslengdeGrunnlag?.vurderinger.orEmpty()
            vedtakslengdeRepository.lagre(
                behandlingId, tidligereVurderinger + VedtakslengdeVurdering(
                    sluttdato = nySluttdato,
                    utvidetMed = utvidelse,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(clock),
                    begrunnelse = "Automatisk vurdert"
                )
            )
        } else {
            log.info("Behandling $behandlingId har ingen vedtatt sluttdato, ingen utvidelse nødvendig")
        }
    }

    fun lagreAutomatiskVedtakslengde(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        rettighetsperiode: Periode,
    ): LocalDate {
        // Automatisk beregning av sluttdato basert på vedtatte vurderinger og rettighetstype-tidslinje for inneværende behandling.
        val vedtattVedtakslengdeGrunnlag =
            forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(it) }
        val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(behandlingId)
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtattVedtakslengdeGrunnlag)
        val vedtattUtvidelse = vedtattVedtakslengdeGrunnlag?.gjeldendeVurdering()?.utvidetMed
        val sluttdato = utledSluttdato(behandlingId, rettighetsperiode, vedtattSluttdato)

        // Henter forrige automatiske vurdering for å sammenligne med ny automatisk beregnet sluttdato
        val sisteAutomatiskeVurdering = vedtakslengdeGrunnlag?.vurderinger.orEmpty()
            .filter { it.vurdertAutomatisk }
            .maxByOrNull { it.opprettet }

        if (sisteAutomatiskeVurdering?.sluttdato != sluttdato) {
            log.info("Automatisk vurdering av sluttdato endret fra $vedtattSluttdato til $sluttdato for behandling $behandlingId")

            val nyAutomatiskVurdering = VedtakslengdeVurdering(
                sluttdato = sluttdato,
                utvidetMed = vedtattUtvidelse ?: ÅrMedHverdager.FØRSTE_ÅR,
                vurdertAv = SYSTEMBRUKER,
                vurdertIBehandling = behandlingId,
                opprettet = Instant.now(clock),
                begrunnelse = "Automatisk vurdert"
            )

            val vedtatteVurderinger = vedtattVedtakslengdeGrunnlag?.vurderinger.orEmpty()
            val nyeVurderingerFraBehandlingen = vedtakslengdeGrunnlag?.vurderinger?.filter { it.vurdertIBehandling == behandlingId }.orEmpty()

            // Det kan kun være en ny manuell vurdering pr behandling
            val nyManuellVurderingFraBehandlingen = nyeVurderingerFraBehandlingen.filter { it.vurdertManuelt }.also {
                require(it.size <= 1) { "Det skal kun være opp til én manuell vurdering per behandling, fant ${it.size} for behandling $behandlingId" }
            }

            vedtakslengdeRepository.lagre(
                behandlingId = behandlingId,
                vurderinger = vedtatteVurderinger + nyManuellVurderingFraBehandlingen + nyAutomatiskVurdering
            )
        }
        return sluttdato
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

    private fun unntaksrettighetstyper(): List<RettighetsType> = listOf(
        RettighetsType.SYKEPENGEERSTATNING,
        RettighetsType.STUDENT,
        RettighetsType.VURDERES_FOR_UFØRETRYGD,
        RettighetsType.ARBEIDSSØKER
    )

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
            vedtakslengdeRepository.lagre(
                behandlingId, listOf(VedtakslengdeVurdering(
                    sluttdato = sluttdato,
                    utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(clock),
                    begrunnelse = "Automatisk vurdert"
                ))
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
}