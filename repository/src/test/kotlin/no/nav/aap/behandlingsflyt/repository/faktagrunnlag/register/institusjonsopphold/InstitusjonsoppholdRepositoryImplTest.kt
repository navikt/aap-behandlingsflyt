package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class InstitusjonsoppholdRepositoryImplTest {
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
    fun `Tom tidslinje dersom ingen opphold finnes`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val institusjonsoppholdRepository = InstitusjonsoppholdRepositoryImpl(connection)
            val institusjonsoppholdTidslinje = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
            assertThat(institusjonsoppholdTidslinje).isNull()
        }
    }

    @Test
    fun `kan lagre og hente fra raw data fra gateway`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val institusjonsoppholdRepository = InstitusjonsoppholdRepositoryImpl(connection)
            val institusjonsopphold = listOf(
                Institusjonsopphold.nyttOpphold(
                    "AS",
                    "A",
                    LocalDate.now(),
                    LocalDate.now().plusDays(1),
                    "123456789",
                    "Azkaban"
                )
            )
            institusjonsoppholdRepository.lagreOpphold(behandling.id, institusjonsopphold)


            val institusjonsoppholdTidslinje =
                requireNotNull(institusjonsoppholdRepository.hent(behandling.id).oppholdene?.opphold)
            assertThat(institusjonsoppholdTidslinje).hasSize(1)
            assertThat(institusjonsoppholdTidslinje.first().verdi).isEqualTo(
                Institusjon(
                    Institusjonstype.AS,
                    Oppholdstype.A,
                    "123456789",
                    "Azkaban"
                )
            )
        }
    }


    @Test
    fun kopier() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val institusjonsoppholdRepository = InstitusjonsoppholdRepositoryImpl(connection)
            val institusjonsopphold = listOf(
                Institusjonsopphold.nyttOpphold(
                    "AS",
                    "A",
                    LocalDate.now(),
                    LocalDate.now().plusDays(1),
                    "123456789",
                    "Azkaban"
                )
            )
            institusjonsoppholdRepository.lagreOpphold(behandling.id, institusjonsopphold)
            val sak2 = sak(connection)
            val behandling2 = finnEllerOpprettBehandling(connection, sak2)

            institusjonsoppholdRepository.kopier(behandling.id, behandling2.id)

            val institusjonsoppholdTidslinje2 =
                requireNotNull(institusjonsoppholdRepository.hent(behandling2.id).oppholdene?.opphold)
            assertThat(institusjonsoppholdTidslinje2).hasSize(1)
            assertThat(institusjonsoppholdTidslinje2.first().verdi).isEqualTo(
                Institusjon(
                    Institusjonstype.AS,
                    Oppholdstype.A,
                    "123456789",
                    "Azkaban"
                )
            )

        }
    }

    @Test
    fun `migrer vurdert i behandling for`() {
        val periode = Periode(1 januar 2025, 1 desember 2025)
        val opphold = Institusjonsopphold(
            Institusjonstype.HS,
            Oppholdstype.H,
            periode.fom,
            periode.tom,
            "123456789",
            "Lærdal sykehus"
        )
        val institusjonsoppholdVurderingFørstegangsbehandling =
            HelseinstitusjonVurdering(
                "begrunnelse 1",
                false,
                false,
                true,
                periode,
                null,
                "saksbehandler",
                LocalDateTime.now()
            )
        val institusjonsoppholdVurderingRevurdering =
            HelseinstitusjonVurdering(
                "begrunnelse 2",
                true,
                false,
                false,
                periode,
                null,
                "saksbehandler",
                LocalDateTime.now()
            )

        val behandling = dataSource.transaction { connection ->
            val institusjonsoppholdRepository = InstitusjonsoppholdRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            institusjonsoppholdRepository.lagreOpphold(behandling.id, listOf(opphold))
            institusjonsoppholdRepository.lagreHelseVurdering(
                behandling.id,
                "saksbehandler",
                listOf(institusjonsoppholdVurderingFørstegangsbehandling)
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
            behandling
        }

        // Revurdering
        dataSource.transaction { connection ->
            val institusjonsoppholdRepository = InstitusjonsoppholdRepositoryImpl(connection)
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            val revurdering =
                behandlingRepo.opprettBehandling(
                    behandling.sakId,
                    TypeBehandling.Revurdering,
                    behandling.id,
                    VurderingsbehovOgÅrsak(
                        listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                        ÅrsakTilOpprettelse.SØKNAD
                    )
                )

            institusjonsoppholdRepository.kopier(behandling.id, revurdering.id)

            institusjonsoppholdRepository.lagreHelseVurdering(
                revurdering.id,
                "saksbehandler",
                listOf(institusjonsoppholdVurderingFørstegangsbehandling, institusjonsoppholdVurderingRevurdering)
            )

            institusjonsoppholdRepository.migrerInstitusjonsopphold()

            assertThat(
                institusjonsoppholdRepository.hentHvisEksisterer(revurdering.id)?.helseoppholdvurderinger?.vurderinger
                    ?.sortedBy { it.vurdertIBehandling?.id }
            ).usingRecursiveComparison()
                .ignoringFields("opprettetTid", "vurdertTidspunkt")
                .isEqualTo(
                    listOf(
                        institusjonsoppholdVurderingFørstegangsbehandling.copy(
                            vurdertIBehandling = behandling.id,
                        ),
                        institusjonsoppholdVurderingRevurdering.copy(
                            vurdertIBehandling = revurdering.id,
                        )
                    ).sortedBy { it.vurdertIBehandling?.id }
                )
        }
    }
}