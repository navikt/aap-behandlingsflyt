package no.nav.aap.behandlingsflyt.behandling.meldekort

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentSakDTO
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("api/meldekort/{saksnummer}") {
        authorizedGet<HentSakDTO, MeldekorteneDto>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer")
            ),
            null,
            modules = arrayOf(TagModule(listOf(Tags.Sak))),
        ) { req ->
            val meldekorteneDto = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val meldekortRepository = repositoryProvider.provide<MeldekortRepository>()
                val meldeperiodeRepository = repositoryProvider.provide<MeldeperiodeRepository>()
                val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
                val sak = repositoryProvider.provide<SakRepository>().hent(saksnummer = Saksnummer(req.saksnummer))
                val behandlingService = BehandlingService(
                    repositoryProvider = repositoryProvider,
                    gatewayProvider = gatewayProvider
                )
                val sisteFattedeVedtaksBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)

                sisteFattedeVedtaksBehandling?.let {
                    meldekortRepository.hentHvisEksisterer(it.id)?.meldekort()?.map { meldekort ->
                        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(it.id) ?: return@let null

                        val aktuellPeriode = underveisGrunnlag.somTidslinje().helePerioden()
                        val meldePeriodene = meldeperiodeRepository.hentMeldeperioder(it.id, aktuellPeriode)

                        val arbeidsperiode = arbeidsperiodeFraMeldekort(meldekort)
                        val meldekortetsPeriode = finnMeldekortetsPeriode(arbeidsperiode, meldePeriodene)

                        MeldekortDto(
                            meldekortId = meldekort.journalpostId.identifikator,
                            meldeperiode = meldekortetsPeriode,
                            dager = meldekort.timerArbeidPerPeriode.map { arbeid ->
                                DagDto(
                                    dato = arbeid.periode.fom,
                                    timerArbeidet = arbeid.timerArbeid.antallTimer.toDouble()
                                )
                            }
                        )
                    }
                }?.let { MeldekorteneDto(it.toSet()) }
            }

            respond(meldekorteneDto ?: MeldekorteneDto(emptySet()))
        }
    }
}

private fun finnMeldekortetsPeriode(
    arbeidsperiode: Periode?, meldeperioder: List<Periode>
): Periode? {
    return arbeidsperiode?.let {
        meldeperioder.firstOrNull {
            // Alle arbeidsperiodene i meldekortet må være innenfor en og samme meldekortperiode.
            // MeldekortPerioder overlapper ikke med hverandre.
            it.inneholder(arbeidsperiode)
        }
    }
}

/**
@return Tidsperioden meldekortet inneholder arbeidstimer for.
Merk at det kan være dager i perioden meldekortet gjelder for som det ikke er rapportert timer på.
@param meldekort -
 **/
private fun arbeidsperiodeFraMeldekort(meldekort: Meldekort): Periode? {
    val timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode
    if (timerArbeidPerPeriode.isEmpty()) {
        return null
    }
    val arbeidPerioder = timerArbeidPerPeriode.map { it.periode }
    val arbeidsPerioderStart = arbeidPerioder.minBy { it.fom }.fom
    val arbeidsPerioderSlutt = arbeidPerioder.maxBy { it.tom }.tom

    return Periode(arbeidsPerioderStart, arbeidsPerioderSlutt)
}

data class MeldekorteneDto(
    val meldekortene: Set<MeldekortDto>
)

data class MeldekortDto(
    val meldekortId: String,
    val meldeperiode: Periode?,
    val dager: List<DagDto>
)

data class DagDto(
    val dato: LocalDate,
    val timerArbeidet: Double
)
