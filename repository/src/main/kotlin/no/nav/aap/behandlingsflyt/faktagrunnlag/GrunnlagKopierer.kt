package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection

fun GrunnlagKopierer(connection: DBConnection): GrunnlagKopierer {
    return GrunnlagKopiererImpl(postgresRepositoryRegistry.provider(connection))
}
