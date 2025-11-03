package no.nav.aap.behandlingsflyt.behandling.svarfraandreinstans.svarfraandreinstans

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.HÅNDTER_SVAR_FRA_ANDREINSTANS_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.svarFraAndreinstansGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("api/svar-fra-andreinstans/{referanse}/grunnlag/svar-fra-andreinstans") {
        getGrunnlag<BehandlingReferanse, SvarFraAndreinstansGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = HÅNDTER_SVAR_FRA_ANDREINSTANS_KODE
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) {
                val repositoryProvider = repositoryRegistry.provider(it)
                val behandling = repositoryProvider.provide<BehandlingRepository>()
                    .hent(req)

                val hendelse = repositoryProvider.provide<MottattDokumentRepository>()
                    .hentDokumenterAvType(behandling.id, InnsendingType.KABAL_HENDELSE)
                    .first()
                    .strukturerteData<KabalHendelseV0>()?.data
                requireNotNull(hendelse) { "Fant ikke tilhørende kabalhendelse" }
                
                val grunnlag = repositoryProvider.provide<SvarFraAndreinstansRepository>()
                    .hentHvisEksisterer(behandling.id)

                SvarFraAndreinstansGrunnlagDto(
                    svarFraAndreinstans = hendelse.tilDto(),
                    gjeldendeVurdering = grunnlag?.vurdering?.tilDto(),
                    harTilgangTilÅSaksbehandle = kanSaksbehandle()
                )
            }

            respond(respons)
        }
    }
}