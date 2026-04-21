package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.help.assertTidslinjeEquals
import no.nav.aap.behandlingsflyt.prosessering.datadeling.UtledArenaVedtakstype
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtledArenavedtakFlytTest: AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {

    @Test
    fun `sender komprimert arena-vedtak til api-intern`() {
        val søknadsdato = LocalDate.now()
        val sak = happyCaseFørstegangsbehandling(søknadsdato)
        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        val arenavedtak = dataSource.transaction {
            UtledArenaVedtakstype(postgresRepositoryRegistry.provider(it), gatewayProvider)
                .utledVedtak(sak)
        }
        val vedtakId = dataSource.transaction {
            VedtakId(VedtakRepositoryImpl(it).hentId(behandling.id))
        }

        assertTidslinjeEquals(arenavedtak,
            tidslinjeOf(
                Periode(søknadsdato, søknadsdato.plusYears(1).minusDays(1)) to
                        UtledArenaVedtakstype.ArenaVedtak(
                            vedtakId = vedtakId,
                            vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD
                        )
                )
        )
        assertThat(arenavedtak.segmenter()).hasSize(1)
    }
}