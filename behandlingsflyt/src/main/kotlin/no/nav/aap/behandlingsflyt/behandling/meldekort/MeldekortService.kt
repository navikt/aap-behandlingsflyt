package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class MeldekortService(
    private val meldekortRepository: MeldekortRepository,
    private val underveisRepository: UnderveisRepository,
    private val sakRepository: SakRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val flytJobbRepository: FlytJobbRepository,
    behandlingService: Lazy<BehandlingService>,
    journalføringService: Lazy<JournalføringService>,
    ansattInfoService: Lazy<AnsattInfoService>,
    mottattHendelseService: Lazy<MottattHendelseService>,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val behandlingService by behandlingService
    private val journalføringService by journalføringService
    private val ansattInfoService by ansattInfoService
    private val mottattHendelseService by mottattHendelseService

    private val logger = LoggerFactory.getLogger(MeldekortService::class.java)

    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
        clock: Clock = Clock.systemDefaultZone(),
    ) : this(
        meldekortRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
        flytJobbRepository = repositoryProvider.provide(),
        behandlingService = lazy { BehandlingService(repositoryProvider, gatewayProvider) },
        journalføringService = lazy { JournalføringService(gatewayProvider) },
        ansattInfoService = lazy { AnsattInfoService(gatewayProvider) },
        mottattHendelseService = lazy { MottattHendelseService(repositoryProvider) },
        clock = clock,
    )

    fun hentMeldeperioderMedMeldekort(saksnummer: Saksnummer): MeldeperioderMedMeldekortResponse {
        val sak = sakRepository.hent(saksnummer)
        val sisteFattedeVedtaksBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)

        val meldeperiodeMedMeldekort = sisteFattedeVedtaksBehandling?.let { behandling ->
            val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)
                ?: return@let null

            val meldeperioder = hentAktuelleMeldeperioderMedMeldepliktStatus(underveisGrunnlag)
            val meldekortGrunnlag = meldekortRepository.hentHvisEksisterer(behandling.id)
            val mottatteDokumenter = mottattDokumentRepository
                .hentDokumenterAvType(sak.id, InnsendingType.MELDEKORT)
                .associateBy { it.referanse }

            meldeperioder.map { (meldeperiode, oppfyltMeldeperiodeMedMeldepliktStatus) ->
                toMeldeperiodeMedMeldekortDto(
                    meldeperiode = meldeperiode,
                    oppfyltMeldeperiodeMedMeldepliktStatus = oppfyltMeldeperiodeMedMeldepliktStatus,
                    nyesteMeldekort = meldekortGrunnlag?.nyesteForMeldeperiode(meldeperiode),
                    tidligereMeldekort = meldekortGrunnlag?.tidligereForMeldeperiode(meldeperiode).orEmpty(),
                    mottatteDokumenter = mottatteDokumenter,
                )
            }
        }

        return MeldeperioderMedMeldekortResponse(
            meldeperioderMedMeldekort = meldeperiodeMedMeldekort?.toSet() ?: emptySet(),
        )
    }

    fun harRegistrertTimerForMeldeperiode(saksnummer: Saksnummer, meldeperiode: Periode): Boolean {
        return hentMeldeperioderMedMeldekort(saksnummer)
            .meldeperioderMedMeldekort
            .filter { it.meldeperiode == meldeperiode }
            .any { it.meldekort?.dager?.isNotEmpty() == true }
    }

    fun oppdaterMeldekort(oppdaterMeldekort: OppdaterMeldekort): OppdatertMeldekort {
        val sak = sakRepository.hent(oppdaterMeldekort.saksnummer)
        val behandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)
            ?: throw UgyldigForespørselException("Kan ikke opprette meldekort når ingen vedtak eksisterer i saken")
        val meldekortGrunnlag = meldekortRepository.hentHvisEksisterer(behandling.id)

        valider(behandling, meldekortGrunnlag, oppdaterMeldekort)

        val meldekort = oppdaterMeldekort.tilMeldekort()

        try {
            val journalpostId = journalføringService.journalfør(
                sak = sak,
                meldeperiode = oppdaterMeldekort.meldeperiode,
                meldekort = meldekort,
                oppdatertAv = oppdaterMeldekort.bruker,
                enhet = ansattInfoService.hentAnsattEnhet(oppdaterMeldekort.bruker.ident),
                tidspunkt = Instant.now(clock),
                meldeDato = oppdaterMeldekort.meldedato,
                korrigert = meldekortGrunnlag?.nyesteForMeldeperiode(oppdaterMeldekort.meldeperiode) != null
            )

            val mottattTidspunkt = utledetMottattTidspunkt(
                oppdaterMeldekort.meldedato,
                meldekortGrunnlag?.nyesteForMeldeperiodePåDato(oppdaterMeldekort.meldeperiode, oppdaterMeldekort.meldedato)
            )

            // Oppretter mottatt hendelse som prosesseres som en meldekort-behandling
            mottattHendelseService.registrerMottattHendelse(
                tilInnsending(sak, journalpostId, mottattTidspunkt, meldekort)
            )

            logger.info("Saksbehandler har opprettet/korrigert meldekort for saksnummer ${oppdaterMeldekort.saksnummer}")
            return OppdatertMeldekort(journalpostId)

        } catch (e: Exception) {
            logger.error(
                "En feil oppstod ved oppdatering av meldekort, dette må undersøkes da journalføring kan " +
                        "være fullført, mens igangsetting av behandlingen ikke har startet", e
            )
            throw e
        }
    }

    fun hentProsesseringStatus(saksnummer: Saksnummer): MeldekortProsesseringResponse {
        val sak = sakRepository.hent(saksnummer)
        return MeldekortProsesseringResponse(
            meldekortProsesseringStatus = hentProsesseringStatus(sak)
        )
    }

    private fun valider(
        behandling: BehandlingMedVedtak,
        meldekortGrunnlag: MeldekortGrunnlag?,
        oppdaterMeldekort: OppdaterMeldekort,
    ) {
        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)
            ?: throw UgyldigForespørselException("Fant ikke underveisgrunnlag for behandlingen")

        val meldedato = oppdaterMeldekort.meldedato
        val meldeperiode = oppdaterMeldekort.meldeperiode

        if (meldedato <= meldeperiode.tom) {
            throw UgyldigForespørselException(
                "Meldedatoen $meldedato må være etter sluttdatoen for meldeperioden som er ${meldeperiode.tom}"
            )
        }

        val aktuelleMeldeperioder = hentAktuelleMeldeperioderMedMeldepliktStatus(underveisGrunnlag).keys

        if (!aktuelleMeldeperioder.contains(meldeperiode)) {
            throw UgyldigForespørselException("Angitt meldeperiode $meldeperiode er ikke gyldig for vedtaket")
        }

        val nyesteMeldekort = meldekortGrunnlag?.nyesteForMeldeperiode(meldeperiode)

        if (nyesteMeldekort != null && oppdaterMeldekort.meldekortMedTimerRegistrert()) {
            val mottattDatoNyesteMeldekort = nyesteMeldekort.mottattTidspunkt.toLocalDate()
            if (meldedato < mottattDatoNyesteMeldekort) {
                throw UgyldigForespørselException(
                    "Du har satt meldedato $meldedato som er før siste mottatte meldekort for perioden " +
                            "som ble mottatt $mottattDatoNyesteMeldekort. Dette er ikke gyldig da det kun " +
                            "er timene i det sist leverte meldekortet som er gjeldende.")
            }
        }
    }

    private fun hentProsesseringStatus(
        sak: Sak
    ): MeldekortProsesseringStatus {
        val harVentendeMeldekortJobber = flytJobbRepository
            .hentJobberForSak(sak.id.toLong())
            .any { it.optionalParameter("brevkode") == InnsendingType.MELDEKORT.name }
        return if (harVentendeMeldekortJobber) {
            MeldekortProsesseringStatus.PROSESSERER_MELDEKORT
        } else {
            MeldekortProsesseringStatus.KLAR
        }
    }

    /**
     * Henter meldeperioder som kan være aktuelle å endre for saksbehandler. Følgende kriterier gjelder:
     * - Perioden må ha en rettighetstype
     * - Meldeperioder bakover i tid inkluderes
     * - Inneværende periode inkluderes
     * - Perioder frem i tid inkluderes ikke
     *
     * For at en bruker som i utgangspunktet ikke har oppfylt meldeplikten (ikke meldt seg til NKS, ikke sendt
     * inn meldekort) skal få utbetalt, må saksbehandler sørge for at meldeplikten oppfylles, enten ved
     * - Sette mottattdato på dokumentet innenfor meldevinduet. Dette er riktig dersom saksbehandler bare har vært
     *   treig med å legge inn timene.
     * - Gi fritak, dersom bruker skulle hatt fritak.
     * - Gi rimelig grunn, dersom bruker faktisk hadde rimelig grunn til ikke å ha meldt seg.
     */
    private fun hentAktuelleMeldeperioderMedMeldepliktStatus(
        underveisGrunnlag: UnderveisGrunnlag,
    ): Map<Periode, OppfyltMeldeperiodeMedMeldepliktStatus> {
        return underveisGrunnlag.perioder
            .filter { it.utfall == Utfall.OPPFYLT && it.periode.fom < LocalDate.now(clock) }
            .groupBy { it.meldePeriode }
            .mapValues { (_, underveisPerioder) ->
                val periodeMedMeldepliktStatus =
                    Tidslinje(underveisPerioder.map { Segment(it.periode, it.meldepliktStatus) })
                        .komprimer()
                        .segmenter()
                        .map { Pair(it.periode, it.verdi) }

                // Samme logikk som gjøres i meldekort-backend - returnerer en periode hvor start og slutt innskrenkes
                val periode = if (periodeMedMeldepliktStatus.isEmpty()) null
                else Periode(periodeMedMeldepliktStatus.first().first.fom, periodeMedMeldepliktStatus.last().first.tom)

                // Aktuelle meldeplikt statuser - duplikater fjernes
                val meldepliktStatus = periodeMedMeldepliktStatus.mapNotNull { it.second }.toSet()

                OppfyltMeldeperiodeMedMeldepliktStatus(
                    periode = periode,
                    meldepliktStatus = meldepliktStatus
                )
            }
    }
}

private fun tilInnsending(
    sak: Sak,
    journalpostId: JournalpostId,
    mottattTidspunkt: LocalDateTime,
    meldekort: MeldekortV0,
): Innsending = Innsending(
    saksnummer = sak.saksnummer,
    referanse = InnsendingReferanse(journalpostId),
    type = InnsendingType.MELDEKORT,
    kanal = Kanal.DIGITAL,
    mottattTidspunkt = mottattTidspunkt,
    melding = meldekort,
)

/**
 * Når saksbehandler setter en meldedato, vil den settes med tidspunkt kl 00:00 dersom ingen meldekort er registert på
 * datoen fra før. Dersom det ligger vurdering(er) fra før - settes tidspunktet til siste vurderingens tidspunkt. Ved
 * sortering vil meldekortet med seneste opprettet tidspunkt foretrekkes.
 */
private fun utledetMottattTidspunkt(meldedato: LocalDate, nyesteMeldekortPåDato: Meldekort?): LocalDateTime =
    if (nyesteMeldekortPåDato != null) {
        meldedato.atTime(nyesteMeldekortPåDato.mottattTidspunkt.toLocalTime())
    } else {
        meldedato.atStartOfDay()
    }

data class OppdatertMeldekort(
    val journalpostId: JournalpostId,
)

data class OppfyltMeldeperiodeMedMeldepliktStatus(
    val periode: Periode?,
    val meldepliktStatus: Set<MeldepliktStatus>,
)