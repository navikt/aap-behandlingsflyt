package no.nav.aap.behandlingsflyt.behandling.meldekort

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
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
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.motor.FlytJobbRepository
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

                val meldeperiodeMedMeldekort = sisteFattedeVedtaksBehandling?.let { behandling ->
                    val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id) ?: return@let null
                    val meldeperioderMedOppfyltePerioder =
                        hentAktuelleMeldeperioderMedOppfyltePerioder(underveisGrunnlag)
                    val meldekortene = meldekortRepository.hentHvisEksisterer(behandling.id)?.meldekort().orEmpty()
                    val mottatteDokumenter = mottattDokumentRepository
                        .hentDokumenterAvType(sak.id, InnsendingType.MELDEKORT)
                        .associateBy { it.referanse }

                    meldeperioderMedOppfyltePerioder.map { (meldeperiode, periode) ->
                        val meldekort = nyesteMeldekortForMeldeperiode(meldekortene, meldeperiode)
                        val tidligereMeldekortListe = tidligereMeldekortForMeldeperiode(meldekortene, meldeperiode)

                        if (meldekort != null) {
                            // Henter ut relevante metadata for meldekort hvor saksbehandler har korrigert timer
                            val innsendingReferanse = InnsendingReferanse(meldekort.journalpostId)
                            val mottattDokument = mottatteDokumenter[innsendingReferanse]
                            val meldekortData = mottattDokument?.strukturerteData<MeldekortV0>()?.data

                            // Fallback til bruker dersom meldekortData.opprettetAv er null ettersom den blir satt eksplisitt ved korrigering
                            val oppdatertAvSaksbehandler = meldekortData?.opprettetAv != null

                            MeldeperiodeMedMeldekortDto(
                                meldeperiode = meldeperiode,
                                periode = periode,
                                meldekort = meldekort.toDto(
                                    meldekortData?.begrunnelse,
                                    oppdatertAv = meldekortData?.opprettetAv,
                                    mottattDokument?.opprettetTid?.toLocalDate(),
                                    oppdatertAvSaksbehandler
                                ),
                                tidligereMeldekort = tidligereMeldekortListe.map { tidligere ->
                                    val ref = InnsendingReferanse(tidligere.journalpostId)
                                    val tidligereDokument = mottatteDokumenter[ref]
                                    val data = tidligereDokument?.strukturerteData<MeldekortV0>()?.data
                                    tidligere.toDto(
                                        data?.begrunnelse,
                                        oppdatertAv = meldekortData?.opprettetAv,
                                        tidligereDokument?.opprettetTid?.toLocalDate(),
                                        oppdatertAvSaksbehandler
                                    )
                                },
                            )
                        } else {
                            MeldeperiodeMedMeldekortDto(
                                meldeperiode = meldeperiode,
                                periode = periode,
                                meldekort = null,
                            )
                        }
                    }
                }

                MeldeperioderMedMeldekortResponse(
                    meldeperioderMedMeldekort = meldeperiodeMedMeldekort?.toSet() ?: emptySet(),
                )
            }

            respond(meldeperioderMedMeldekortResponse)
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
                val meldekortRepository = repositoryProvider.provide<MeldekortRepository>()
                val journalføringService = JournalføringService(gatewayProvider)
                val ansattInfoService = AnsattInfoService(gatewayProvider)
                val behandlingService = BehandlingService(repositoryProvider, gatewayProvider)

                val sak = sakRepository.hent(Saksnummer(req.saksnummer))
                val sisteFattedeVedtaksBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)

                val bruker = bruker()
                val meldeperiode = body.meldeperiode
                val meldekort = tilMeldekort(body, bruker)
                val tidspunkt = Instant.now(clock)
                val enhet = ansattInfoService.hentAnsattEnhet(bruker.ident) ?: "9999"
                val meldedato = body.meldeDato

                val journalpostId = journalføringService.journalfør(
                    sak = sak,
                    meldeperiode = meldeperiode,
                    meldekort = meldekort,
                    oppdatertAv = bruker,
                    enhet = enhet,
                    tidspunkt = tidspunkt
                )

                val meldekortene = sisteFattedeVedtaksBehandling
                    ?.let { meldekortRepository.hentHvisEksisterer(sisteFattedeVedtaksBehandling.id) }
                    ?.meldekort()
                    .orEmpty()

                val nyesteMeldekortPåDato = nyesteMeldekortForMeldeperiodePåDato(meldekortene, meldeperiode, meldedato)
                val mottattTidspunkt = utledetMottattTidspunkt(meldedato, nyesteMeldekortPåDato)
                val innsending = tilInnsending(sak, journalpostId, mottattTidspunkt, meldekort)

                // Oppretter mottatt hendelse som prosesseres som en meldekort-behandling
                MottattHendelseService(repositoryProvider).registrerMottattHendelse(innsending)

                OppdaterMeldekortResponse(
                    journalpostId = journalpostId.identifikator,
                    oppdatertTidspunkt = LocalDate.ofInstant(tidspunkt, ZoneId.of("Europe/Oslo")),
                )
            }

            respond(response)
        }

        route("prosessering") {
            authorizedGet<SaksnummerParameter, MeldekortProsesseringResponse>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                    sakPathParam = SakPathParam("saksnummer")
                ),
                null,
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()

                    val sak = sakRepository.hent(Saksnummer(req.saksnummer))
                    MeldekortProsesseringResponse(
                        meldekortProsesseringStatus = hentProsesseringStatus(flytJobbRepository, sak)
                    )
                }

                respond(response)
            }
        }

    }
}

private fun hentProsesseringStatus(
    flytJobbRepository: FlytJobbRepository,
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

internal fun tilMeldekort(oppdaterMeldekortRequest: OppdaterMeldekortRequest, vurdertAv: Bruker): MeldekortV0 {
    return MeldekortV0(
        harDuArbeidet = oppdaterMeldekortRequest.dager
            .takeIf { it.isNotEmpty() }?.let { it.sumOf { dag -> dag.timerArbeidet } > 0.0 },
        opprettetAv = vurdertAv.ident,
        begrunnelse = oppdaterMeldekortRequest.begrunnelse,
        timerArbeidPerPeriode = oppdaterMeldekortRequest.dager.map {
            ArbeidIPeriodeV0(
                fraOgMedDato = it.dato,
                tilOgMedDato = it.dato,
                timerArbeid = it.timerArbeidet,
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
 * Henter ut nyeste meldekort som sammenfaller med meldeperiode basert på innmeldte timer på en gitt dato
 */
private fun nyesteMeldekortForMeldeperiodePåDato(
    meldekortene: List<Meldekort>,
    meldeperiode: Periode,
    dato: LocalDate
): Meldekort? = meldekortene.lastOrNull { meldekort ->
    val arbeidsperiode = meldekort.arbeidsperiode()
    arbeidsperiode != null
            && meldeperiode.inneholder(arbeidsperiode)
            && meldekort.mottattTidspunkt.toLocalDate() == dato
}

/**
 * Henter ut alle tidligere meldekort for en meldeperiode, sortert synkende på mottattTidspunkt (nyest først).
 * Det nyeste meldekortet (som returneres av nyesteMeldekortForMeldeperiode) er ekskludert.
 */
private fun tidligereMeldekortForMeldeperiode(
    meldekortene: List<Meldekort>,
    meldeperiode: Periode
): List<Meldekort> {
    val nyeste = nyesteMeldekortForMeldeperiode(meldekortene, meldeperiode)
    return meldekortene
        .filter { meldekort ->
            val arbeidsperiode = meldekort.arbeidsperiode()
            arbeidsperiode != null && meldeperiode.inneholder(arbeidsperiode) && meldekort != nyeste
        }
        .sortedByDescending { it.mottattTidspunkt }
}

/**
 * Henter meldeperioder som kan være aktuelle å endre for saksbehandler. Følgende kriterier gjelder:
 * - Perioden må ha en rettighetstype
 * - Meldeperioder bakover i tid inkluderes
 * - Inneværende periode inkluderes
 * - Perioder frem i tid inkluderes ikke
 *
 * For at en bruker som i utgangspunktet ikke har oppfylt meldeplikten (ikke meldt seg til NKS, ikke sendt inn meldekort)
 * skal få utbetalt, må saksbehandler sørge for at meldeplikten oppfylles, enten ved
 * - Sette mottattdato på dokumentet innenfor meldevinduet. Dette er riktig dersom saksbehandler bare har vært treig med å legge inn timene.
 * - Gi fritak, dersom bruker skulle hatt fritak.
 * - Gi rimelig grunn, dersom bruker faktisk hadde rimelig grunn til ikke å ha meldt seg.
 */
private fun hentAktuelleMeldeperioderMedOppfyltePerioder(
    underveisGrunnlag: UnderveisGrunnlag,
): Map<Periode, Periode?> {
    return underveisGrunnlag.perioder
        .filter { it.utfall == Utfall.OPPFYLT }
        .groupBy({ it.meldePeriode }, { it.periode })
        .mapValues { (_, perioder) ->
            // Slå sammen til sammenhengende perioder hvis mulig
            val aktuellePerioder = Tidslinje(perioder.map { Segment(it, true) })
                .komprimer()
                .perioder()
                .toList()

            // Samme logikk som gjøres i meldekort-backend - returnerer en periode hvor start og slutt innskrenkes
            if (aktuellePerioder.isEmpty()) null
            else Periode(aktuellePerioder.first().fom, aktuellePerioder.last().tom)
        }
}