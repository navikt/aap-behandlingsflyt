package no.nav.aap.behandlingsflyt.behandling.krav

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.kravGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("api/behandling/{referanse}/grunnlag/krav") {
        getGrunnlag<BehandlingReferanse, KravGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            påkrevdRolle = Definisjon.VURDER_KRAV.løsesAv
        ) { req ->
            val response = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val kravRepository: KravRepository = repositoryProvider.provide()
                val mottattDokumentRepository: MottattDokumentRepository = repositoryProvider.provide()

                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                val kravGrunnlag = kravRepository.hentHvisEksisterer(behandlingId = behandling.id)
                val vedtattKravGrunnlag =
                    behandling.forrigeBehandlingId?.let { kravRepository.hentHvisEksisterer(behandlingId = it) }

                val nyeVurderinger = kravGrunnlag
                    ?.vurderinger?.filter { it.vurdertIBehandling == behandling.id }.orEmpty()
                    .sortedBy { it.journalpostId.identifikator }
                    .map { it.somDto() }

                val sisteVedtatte =
                    vedtattKravGrunnlag?.gjeldendeVurderinger().orEmpty().map { it.somDto() }


                val søknaderUtenKravvurdering =
                    mottattDokumentRepository.hentDokumenterAvType(behandling.id, InnsendingType.SØKNAD)
                        .filter { søknad -> nyeVurderinger.any { it.journalpostId == søknad.referanse.asJournalpostId } }
                        .map { it.tilSøknadUtenKravDto() }

                KravGrunnlagDto(
                    harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                    nyeVurderinger = nyeVurderinger,
                    vedtatteVurderinger = sisteVedtatte,
                    søknaderUtenKravvurdering = søknaderUtenKravvurdering,
                )

            }
            respond(response)

        }
    }
}

fun MottattDokument.tilSøknadUtenKravDto() = SøknadUtenKravDto(
    journalpostId = this.referanse.asJournalpostId,
    mottattTidspunkt = this.mottattTidspunkt
)