package no.nav.aap.behandlingsflyt.tilgang

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.pip.IdentPåSak
import no.nav.aap.behandlingsflyt.pip.IdentPåSak.Companion.filterDistinctIdent
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.RelevanteIdenter
import no.nav.aap.tilgang.plugin.kontrakt.RelevanteIdenterResolver
import java.util.UUID
import javax.sql.DataSource

private fun List<IdentPåSak>.tilRelevanteIdenter(): RelevanteIdenter {
    return RelevanteIdenter(
        søker = this.filterDistinctIdent(IdentPåSak.Opprinnelse.PERSON),
        barn = this.filterDistinctIdent(IdentPåSak.Opprinnelse.BARN)
    )
}

fun relevanteIdenterForBehandlingResolver(
    repositoryRegistry: RepositoryRegistry,
    dataSource: DataSource
): RelevanteIdenterResolver {
    return RelevanteIdenterResolver { referanse ->
        dataSource.transaction(readOnly = true) { connection ->
            repositoryRegistry.provider(connection).provide<PipRepository>()
                .finnIdenterPåBehandling(BehandlingReferanse(UUID.fromString(referanse)))
                .tilRelevanteIdenter()
        }
    }
}

fun relevanteIdenterForSakResolver(
    repositoryRegistry: RepositoryRegistry,
    dataSource: DataSource
): RelevanteIdenterResolver {
    return RelevanteIdenterResolver { referanse ->
        dataSource.transaction(readOnly = true) { connection ->
            repositoryRegistry.provider(connection).provide<PipRepository>()
                .finnIdenterPåSak(Saksnummer(referanse))
                .tilRelevanteIdenter()
        }
    }
}
        