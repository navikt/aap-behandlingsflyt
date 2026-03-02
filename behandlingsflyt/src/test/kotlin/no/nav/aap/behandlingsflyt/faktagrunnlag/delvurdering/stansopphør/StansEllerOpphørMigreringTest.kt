package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.flyt.AbstraktFlytOrkestratorTest
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class StansEllerOpphørMigreringTest: AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {
    @Test
    fun `kan hente sak uten stansOpphør grunnlag og og lage det`(){
        val sak = happyCaseFørstegangsbehandling()
        StansEllerOpphørMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()

        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)
        dataSource.transaction { connection ->
            val stansRepo = postgresRepositoryRegistry.provider(connection).provide<StansOpphørRepository>()
            assertThat(stansRepo.hentHvisEksisterer(behandling.id)).isNotNull

        }
    }
}