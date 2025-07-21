package no.nav.aap.behandlingsflyt.behandling.oppfolgingsbehandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import javax.sql.DataSource


enum class HvemSkalFølgeOpp {
    NasjonalEnhet, Lokalkontor
}

enum class KonsekvensAvOppfølging {
    INGEN, OPPRETT_VURDERINGSBEHOV
}

data class OppfølgingsoppgaveGrunnlagResponse(
    val konsekvensAvOppfølging: KonsekvensAvOppfølging,
    val opplysningerTilRevurdering: List<ÅrsakTilBehandling>,
    val årsak: String,
    val vurdertAv: String?
)


data class AvklarOppfolgingsoppgaveGrunnlagResponse(
    val datoForOppfølging: LocalDate,
    val hvaSkalFølgesOpp: String,
    val hvemSkalFølgeOpp: HvemSkalFølgeOpp,
    val grunnlag: OppfølgingsoppgaveGrunnlagResponse? = null,
)


fun NormalOpenAPIRoute.avklarOppfolgingsoppgaveGrunnlag(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry
) {
    route("/api/behandling/{referanse}/grunnlag/oppfolgingsoppgave").getGrunnlag<BehandlingReferanse, AvklarOppfolgingsoppgaveGrunnlagResponse>(
        behandlingPathParam = BehandlingPathParam("referanse"),
        avklaringsbehovKode = Definisjon.AVKLAR_OPPFØLGINGSBEHOV_NAY.kode.toString()
    ) { req ->

        val respons = dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

            val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

            val oppfølgingsBehandlingRepository =
                repositoryProvider.provide<OppfølgingsBehandlingRepository>()
            val oppfølgingsbehandlingvurdering =
                oppfølgingsBehandlingRepository.hent(behandling.id)

            val dokument =
                requireNotNull(
                    MottaDokumentService(repositoryProvider.provide()).hentOppfølgingsBehandlingDokument(
                        behandling.id
                    )
                ) { "Skal alltid finnes dokumenter på oppfølgingsbehandlinger. BehandlingId: ${behandling.id}" }

            AvklarOppfolgingsoppgaveGrunnlagResponse(
                datoForOppfølging = dokument.datoForOppfølging,
                hvaSkalFølgesOpp = dokument.hvaSkalFølgesOpp,
                hvemSkalFølgeOpp = when (dokument.hvemSkalFølgeOpp) {
                    no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp.NasjonalEnhet -> HvemSkalFølgeOpp.NasjonalEnhet
                    no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp.Lokalkontor -> HvemSkalFølgeOpp.Lokalkontor
                },
                grunnlag = oppfølgingsbehandlingvurdering?.let {
                    OppfølgingsoppgaveGrunnlagResponse(
                        konsekvensAvOppfølging = when (it.konsekvensAvOppfølging) {
                            no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.KonsekvensAvOppfølging.INGEN -> KonsekvensAvOppfølging.INGEN
                            no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.KonsekvensAvOppfølging.OPPRETT_VURDERINGSBEHOV -> KonsekvensAvOppfølging.OPPRETT_VURDERINGSBEHOV
                        },
                        opplysningerTilRevurdering = it.opplysningerTilRevurdering,
                        årsak = it.årsak,
                        vurdertAv = it.vurdertAv
                    )
                })
        }

        respond(respons)
    }
}