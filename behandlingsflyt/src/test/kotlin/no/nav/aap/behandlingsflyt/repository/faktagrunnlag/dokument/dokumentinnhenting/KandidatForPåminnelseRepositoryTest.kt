package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.KandidatForPåminnelseRepositoryImpl
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class KandidatForPåminnelseRepositoryTest {
    private val clockTreUkerOgEnDagFremITid = fixedClock(LocalDate.now().plusWeeks(3).plusDays(1))

    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `skal ikke plukke behandlinger der opprettetDato ikke er lik definert dato for påminnelse`() {
        dataSource.transaction { connection ->
            val sak1 = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak1)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(
                behandlingId = behandling1.id,
                status = Status.UTREDES
            )

            // avklaringsbehov opprettes i dag, skal bare plukkes om bestillingOpprettetDato er i dag
            val avklaringsbehov1 = AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling1.id)
            avklaringsbehov1.leggTil(
                Definisjon.BESTILL_LEGEERKLÆRING,
                StegType.AVKLAR_SYKDOM,
                null,
                null,
                frist = LocalDate.now(clockTreUkerOgEnDagFremITid).plusDays(14)
            )

            val behandlingerMedBestillingOpprettetIDag =
                KandidatForPåminnelseRepositoryImpl(connection).finnKandidaterForPåminnelse(
                    LocalDate.now()

                )
            assertThat(behandlingerMedBestillingOpprettetIDag).contains(behandling1.referanse)

            val behandlingerMedBestillingOpprettetIGår =
                KandidatForPåminnelseRepositoryImpl(connection).finnKandidaterForPåminnelse(
                    LocalDate.now().minusDays(1)
                )
            assertThat(behandlingerMedBestillingOpprettetIGår).doesNotContain(behandling1.referanse)

            val behandlingerMedBestillingOpprettetIMorgen =
                KandidatForPåminnelseRepositoryImpl(connection).finnKandidaterForPåminnelse(
                    LocalDate.now().plusDays(1)
                )
            assertThat(behandlingerMedBestillingOpprettetIMorgen).doesNotContain(behandling1.referanse)
        }
    }

    @Test
    fun `skal plukke behandling selv om den ikke er på vent lenger`() {
        dataSource.transaction { connection ->
            val sak1 = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak1)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(
                behandlingId = behandling1.id,
                status = Status.UTREDES
            )

            // avklaringsbehov opprettes og lukkes i dag
            val avklaringsbehov1 = AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling1.id)
            avklaringsbehov1.leggTil(
                Definisjon.BESTILL_LEGEERKLÆRING,
                StegType.AVKLAR_SYKDOM,
                null,
                null,
                frist = LocalDate.now(clockTreUkerOgEnDagFremITid).plusDays(14)
            )
            avklaringsbehov1.løsAvklaringsbehov(
                definisjon = Definisjon.BESTILL_LEGEERKLÆRING,
                begrunnelse = "tatt av vent manuelt",
                endretAv = Bruker("saksbehandler"),
            )


            val kandidaterTreUkerFraIDag = KandidatForPåminnelseRepositoryImpl(connection).finnKandidaterForPåminnelse(
                LocalDate.now()
            )
            assertThat(kandidaterTreUkerFraIDag).contains(behandling1.referanse)
        }
    }

    @Test
    fun `skal ikke plukke behandling hvis det har kommet legeerklæring i mellomtiden`() {
        dataSource.transaction { connection ->
            val sak1 = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak1)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(
                behandlingId = behandling1.id,
                status = Status.UTREDES
            )

            // avklaringsbehov opprettes i dag
            val avklaringsbehov1 = AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling1.id)
            avklaringsbehov1.leggTil(
                Definisjon.BESTILL_LEGEERKLÆRING,
                StegType.AVKLAR_SYKDOM,
                null,
                null,
                frist = LocalDate.now(clockTreUkerOgEnDagFremITid).plusDays(14)
            )

            // svar på legeerklæring er nyere enn bestilling
            BehandlingRepositoryImpl(connection).oppdaterVurderingsbehovOgÅrsak(
                behandling = behandling1,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            type = Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                            oppdatertTid = LocalDateTime.now().plusDays(4)
                        )
                    ), årsak = ÅrsakTilOpprettelse.HELSEOPPLYSNINGER,
                    opprettet = LocalDateTime.now().plusDays(4)
                )
            )


            // forespørsel er besvart, behandling skal ikke plukkes som kandidat
            val kandidaterTreUkerFraIDag = KandidatForPåminnelseRepositoryImpl(connection).finnKandidaterForPåminnelse(
                LocalDate.now()
            )
            assertThat(kandidaterTreUkerFraIDag).doesNotContain(behandling1.referanse)
        }
    }
}