package no.nav.aap.behandlingsflyt.behandling.meldekort

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.Rolle
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    clock: Clock = Clock.systemDefaultZone()
) {
    route("/api/meldekort/{saksnummer}") {
        authorizedGet<SaksnummerParameter, MeldeperioderMedMeldekortResponse>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer")
            ),
            null,
            modules = arrayOf(TagModule(listOf(Tags.Sak))),
        ) { req ->
            val meldeperioderMedMeldekortResponse = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val meldekortRepository = repositoryProvider.provide<MeldekortRepository>()
                val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()
                val behandlingService = BehandlingService(repositoryProvider, gatewayProvider)

                val sak = sakRepository.hent(Saksnummer(req.saksnummer))
                val sisteFattedeVedtaksBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)

                sisteFattedeVedtaksBehandling?.let { behandling ->
                    val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id) ?: return@let null
                    val meldeperioder = hentAktuelleMeldeperioder(underveisGrunnlag, clock)
                    val meldekortene = meldekortRepository.hentHvisEksisterer(behandling.id)?.meldekort().orEmpty()

                    meldeperioder.map { meldeperiode ->
                        val meldekort = nyesteMeldekortForMeldeperiode(meldekortene, meldeperiode)

                        if (meldekort != null) {
                            // Henter metadata fra mottatt_dokument
                            val innsendingReferanse = InnsendingReferanse(meldekort.journalpostId)
                            val mottattDokument = mottattDokumentRepository.hent(innsendingReferanse)
                            val meldekortData = mottattDokument.strukturerteData<MeldekortV0>()?.data

                            MeldeperiodeMedMeldekortDto(
                                meldeperiode = meldeperiode,
                                meldekort = meldekort.toDto(meldekortData?.begrunnelse, meldekortData?.opprettetAv)
                            )
                        } else {
                            MeldeperiodeMedMeldekortDto(
                                meldeperiode = meldeperiode,
                                meldekort = null
                            )
                        }
                    }
                }?.let { MeldeperioderMedMeldekortResponse(it.toSet()) }
            }

            respond(meldeperioderMedMeldekortResponse ?: MeldeperioderMedMeldekortResponse(emptySet()))
        }

        authorizedPost<SaksnummerParameter, OppdaterMeldekortResponse, OppdaterMeldekortRequest>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer"),
                operasjon = Operasjon.SAKSBEHANDLE,
                påkrevdRolle = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
            ),
            modules = arrayOf(TagModule(listOf(Tags.Sak))),
        ) { req, body ->
            val response = dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val journalføringService = JournalføringService(gatewayProvider)
                val ansattInfoService = AnsattInfoService(gatewayProvider)

                val sak = sakRepository.hent(Saksnummer(req.saksnummer))
                val bruker = bruker()
                val meldeperiode = body.meldeperiode
                val meldekort = tilMeldekort(body, bruker)
                val tidspunkt = Instant.now(clock)
                val enhet = ansattInfoService.hentAnsattEnhet(bruker.ident) ?: "9999"

                val journalpostId = journalføringService.journalfør(sak, meldeperiode, meldekort, bruker, enhet, tidspunkt)
                val innsending = tilInnsending(sak, journalpostId, tidspunkt, meldekort)

                // TODO lagre ned "midlertidig tilstand her slik at saksbehandler kan se at vi prosesserer endringene?

                MottattHendelseService(repositoryProvider).registrerMottattHendelse(innsending)

                OppdaterMeldekortResponse(journalpostId.identifikator)
            }

            respond(response)
        }
    }
}

private fun tilInnsending(
    sak: Sak,
    journalpostId: JournalpostId,
    tidspunkt: Instant,
    meldekort: MeldekortV0
): Innsending = Innsending(
    saksnummer = sak.saksnummer,
    referanse = InnsendingReferanse(journalpostId),
    type = InnsendingType.MELDEKORT,
    kanal = Kanal.DIGITAL,
    mottattTidspunkt = LocalDateTime.ofInstant(tidspunkt, ZoneId.of("Europe/Oslo")),
    melding = meldekort,
)

private fun tilMeldekort(oppdaterMeldekortRequest: OppdaterMeldekortRequest, vurdertAv: Bruker): MeldekortV0 {
    return MeldekortV0(
        harDuArbeidet = oppdaterMeldekortRequest.dager.sumOf { it.timerArbeidet } > 0.0,
        opprettetAv = vurdertAv.ident,
        begrunnelse = oppdaterMeldekortRequest.begrunnelse,
        timerArbeidPerPeriode = oppdaterMeldekortRequest.dager.map {
            ArbeidIPeriodeV0(
                fraOgMedDato = it.dato,
                tilOgMedDato = it.dato,
                timerArbeid = it.timerArbeidet ?: 0.0,
            )
        }
    )
}

/**
 * Henter ut nyeste meldekort som sammenfaller med meldeperiode basert på innmeldte timer
 */
private fun nyesteMeldekortForMeldeperiode(
    meldekortene: List<Meldekort>,
    meldeperiode: Periode
): Meldekort? = meldekortene.lastOrNull { meldekort ->
    val arbeidsperiode = meldekort.arbeidsperiode()
    arbeidsperiode != null && meldeperiode.inneholder(arbeidsperiode)
}

/**
 * Henter meldeperioder som kan være aktuelle å endre for saksbehandler. Følgende kriterier gjelder:
 * - Perioden må ha en rettighetstype
 * - Meldeperioder bakover i tid inkluderes
 * - Inneværende periode inkluderes
 * - Perioder frem i tid inkluderes ikke
 *
 * Verdt å merke seg at dersom meldeplikten ikke er oppfylt for en periode, så vil Utfall == IKKE_OPPFYLT, mens
 * rettighetstypen vil fortsatt være satt. Dermed kan saksbehandler kunne sette timer i meldekortet for perioden.
 * Utfallet vil bli 0 utbetaling dersom saksbehandler ikke gjør annet enn å føre timer, og man er forbi meldevinduet.
 *
 * For at en bruker som i utgangspunktet ikke har oppfylt meldeplikten (ikke meldt seg til NKS, ikke sendt inn meldekort)
 * skal få utbetalt, må saksbehandler sørge for at meldeplikten oppfylles, enten ved
 * - Sette mottattdato på dokumentet innenfor meldevinduet. Dette er riktig dersom saksbehandler bare har vært treig med å legge inn timene.
 * - Gi fritak, dersom bruker skulle hatt fritak.
 * - Gi rimelig grunn, dersom bruker faktisk hadde rimelig grunn til ikke å ha meldt seg.
 */
private fun hentAktuelleMeldeperioder(
    underveisGrunnlag: UnderveisGrunnlag,
    clock: Clock
): List<Periode> {
    val meldeperioder = underveisGrunnlag.perioder
        .filter { it.rettighetsType != null }
        .map { it.meldePeriode }
        .sortedBy { it.fom }
        .takeWhile { it.fom < LocalDate.now(clock) }

    return meldeperioder
}