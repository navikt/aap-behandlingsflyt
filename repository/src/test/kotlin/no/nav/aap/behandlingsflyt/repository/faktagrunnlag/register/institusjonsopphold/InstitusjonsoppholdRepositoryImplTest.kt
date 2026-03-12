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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

        private fun hsOpphold(fom: LocalDate, tom: LocalDate) =
            Institusjonsopphold.nyttOpphold("HS", "H", fom, tom, "111222333", "Test Institusjon")

        private fun vurdering(
            periode: Periode,
            vurdertIBehandling: BehandlingId,
            begrunnelse: String = "En begrunnelse",
            faarFriKostOgLosji: Boolean = true,
            forsoergerEktefelle: Boolean = false,
            harFasteUtgifter: Boolean = false,
        ) = HelseinstitusjonVurdering(
            begrunnelse = begrunnelse,
            faarFriKostOgLosji = faarFriKostOgLosji,
            forsoergerEktefelle = forsoergerEktefelle,
            harFasteUtgifter = harFasteUtgifter,
            periode = periode,
            vurdertIBehandling = vurdertIBehandling,
            vurdertAv = "SAKSBEHANDLER",
            vurdertTidspunkt = LocalDateTime.now(),
        )
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

    // -------------------------------------------------------------------------
    // lagreHelseVurdering — grunnscenario
    // -------------------------------------------------------------------------

    @Test
    fun `lagreHelseVurdering - lagrer vurdering mot eget opphold`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = InstitusjonsoppholdRepositoryImpl(connection)

            val oppholdPeriode = Periode(1 januar 2026, 31 desember 2026)
            repo.lagreOpphold(behandling.id, listOf(hsOpphold(oppholdPeriode.fom, oppholdPeriode.tom)))
            repo.lagreHelseVurdering(
                behandling.id,
                listOf(vurdering(Periode(1 januar 2026, 30 juni 2026), behandling.id))
            )

            val grunnlag = requireNotNull(repo.hentHvisEksisterer(behandling.id))
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger).hasSize(1)
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger?.first()?.periode)
                .isEqualTo(Periode(1 januar 2026, 30 juni 2026))
        }
    }

    @Test
    fun `lagreHelseVurdering - lagrer og erstatter eksisterende vurdering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = InstitusjonsoppholdRepositoryImpl(connection)

            val oppholdPeriode = Periode(1 januar 2026, 31 desember 2026)
            repo.lagreOpphold(behandling.id, listOf(hsOpphold(oppholdPeriode.fom, oppholdPeriode.tom)))

            repo.lagreHelseVurdering(
                behandling.id,
                listOf(vurdering(Periode(1 januar 2026, 30 juni 2026), behandling.id, begrunnelse = "Første"))
            )
            repo.lagreHelseVurdering(
                behandling.id,
                listOf(vurdering(Periode(1 januar 2026, 30 juni 2026), behandling.id, begrunnelse = "Oppdatert"))
            )

            val grunnlag = requireNotNull(repo.hentHvisEksisterer(behandling.id))
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger).hasSize(1)
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger?.first()?.begrunnelse).isEqualTo("Oppdatert")
        }
    }

    @Test
    fun `lagreHelseVurdering - tom vurderingsliste fjerner helseoppholdvurderinger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = InstitusjonsoppholdRepositoryImpl(connection)

            val oppholdPeriode = Periode(1 januar 2026, 31 desember 2026)
            repo.lagreOpphold(behandling.id, listOf(hsOpphold(oppholdPeriode.fom, oppholdPeriode.tom)))
            repo.lagreHelseVurdering(
                behandling.id,
                listOf(vurdering(Periode(1 januar 2026, 30 juni 2026), behandling.id))
            )
            repo.lagreHelseVurdering(behandling.id, emptyList())

            val grunnlag = requireNotNull(repo.hentHvisEksisterer(behandling.id))
            assertThat(grunnlag.helseoppholdvurderinger).isNull()
        }
    }

    @Test
    fun `lagreHelseVurdering - feiler når vurderingsperiode ikke dekkes av noe opphold`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = InstitusjonsoppholdRepositoryImpl(connection)

            repo.lagreOpphold(
                behandling.id,
                listOf(hsOpphold(1 januar 2026, 31 desember 2026))
            )

            assertThrows<IllegalArgumentException> {
                repo.lagreHelseVurdering(
                    behandling.id,
                    listOf(vurdering(Periode(1 januar 2025, 30 juni 2025), behandling.id))
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // lagreHelseVurdering — revurdering: vurdering fra tidligere behandling
    // -------------------------------------------------------------------------

    @Test
    fun `lagreHelseVurdering - vurdering fra tidligere behandling valideres mot oppholdene til sin egen behandling`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val repo = InstitusjonsoppholdRepositoryImpl(connection)

            // Behandling 1: opphold og vurdering for lang periode
            val oppholdPeriode1 = Periode(1 januar 2026, 31 desember 2999)
            repo.lagreOpphold(behandling1.id, listOf(hsOpphold(oppholdPeriode1.fom, oppholdPeriode1.tom)))
            repo.lagreHelseVurdering(
                behandling1.id,
                listOf(vurdering(oppholdPeriode1, behandling1.id, begrunnelse = "Gammel vurdering"))
            )

            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            // Behandling 2: nytt kortere opphold
            val behandling2 = finnEllerOpprettRevurdering(connection, sak, behandling1.id)
            val oppholdPeriode2 = Periode(1 januar 2026, 3 september 2026)
            repo.lagreOpphold(behandling2.id, listOf(hsOpphold(oppholdPeriode2.fom, oppholdPeriode2.tom)))

            // Lagrer: ny vurdering (fra behandling2) + gammel vurdering (fra behandling1 med lang periode)
            val vurderinger = listOf(
                vurdering(oppholdPeriode2, behandling2.id, begrunnelse = "Ny vurdering"),
                vurdering(oppholdPeriode1, behandling1.id, begrunnelse = "Gammel vurdering"),
            )

            // Skal ikke kaste feil — gammel vurdering valideres mot oppholdene til behandling1
            repo.lagreHelseVurdering(behandling2.id, vurderinger)

            val grunnlag = requireNotNull(repo.hentHvisEksisterer(behandling2.id))
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger).hasSize(2)
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger?.map { it.begrunnelse })
                .containsExactlyInAnyOrder("Ny vurdering", "Gammel vurdering")
        }
    }

    @Test
    fun `lagreHelseVurdering - gammel vurdering med periode utenfor nytt opphold feiler uten riktig behandlingsid-oppslag`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val repo = InstitusjonsoppholdRepositoryImpl(connection)

            // Behandling 1: lang periode
            repo.lagreOpphold(behandling1.id, listOf(hsOpphold(1 januar 2026, 31 desember 2999)))
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            // Behandling 2: kort periode, uten noen opphold for den lange perioden
            val behandling2 = finnEllerOpprettRevurdering(connection, sak, behandling1.id)
            repo.lagreOpphold(behandling2.id, listOf(hsOpphold(1 januar 2026, 3 september 2026)))

            // Prøver å lagre en vurdering som tilhører behandling2, men med en periode
            // som bare finnes i behandling1 sitt opphold
            val vurderingMedFeilBehandlingsId = vurdering(
                periode = Periode(1 januar 2026, 31 desember 2999),
                vurdertIBehandling = behandling2.id,  // feil: peker på behandling2, men perioden tilhører behandling1
                begrunnelse = "Vurdering med feil behandlingsreferanse",
            )

            assertThrows<IllegalArgumentException> {
                repo.lagreHelseVurdering(behandling2.id, listOf(vurderingMedFeilBehandlingsId))
            }
        }
    }

    @Test
    fun `lagreHelseVurdering - flere opphold i en behandling, vurdering matches mot riktig opphold`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = InstitusjonsoppholdRepositoryImpl(connection)

            // To separate opphold i én behandling
            repo.lagreOpphold(
                behandling.id,
                listOf(
                    hsOpphold(1 januar 2026, 30 juni 2026),
                    hsOpphold(1 oktober 2026, 31 mars 2027),
                )
            )

            val vurderinger = listOf(
                vurdering(Periode(1 januar 2026, 30 juni 2026), behandling.id, begrunnelse = "Opphold 1"),
                vurdering(Periode(1 oktober 2026, 31 mars 2027), behandling.id, begrunnelse = "Opphold 2"),
            )

            repo.lagreHelseVurdering(behandling.id, vurderinger)

            val grunnlag = requireNotNull(repo.hentHvisEksisterer(behandling.id))
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger).hasSize(2)
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger?.map { it.begrunnelse })
                .containsExactlyInAnyOrder("Opphold 1", "Opphold 2")
        }
    }

    @Test
    fun `lagreHelseVurdering - to revurderinger med vurderinger fra ulike behandlinger valideres korrekt`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val repo = InstitusjonsoppholdRepositoryImpl(connection)

            // Behandling 1
            repo.lagreOpphold(behandling1.id, listOf(hsOpphold(1 januar 2026, 31 desember 2999)))
            repo.lagreHelseVurdering(
                behandling1.id,
                listOf(vurdering(Periode(1 januar 2026, 31 desember 2999), behandling1.id, begrunnelse = "B1"))
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            // Behandling 2: nytt kortere opphold + gammel lang vurdering fra B1
            val behandling2 = finnEllerOpprettRevurdering(connection, sak, behandling1.id)
            repo.lagreOpphold(behandling2.id, listOf(hsOpphold(1 januar 2026, 1 juli 2026)))
            repo.lagreHelseVurdering(
                behandling2.id,
                listOf(
                    vurdering(Periode(1 januar 2026, 1 juli 2026), behandling2.id, begrunnelse = "B2"),
                    vurdering(Periode(1 januar 2026, 31 desember 2999), behandling1.id, begrunnelse = "B1"),
                )
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling2.id, Status.AVSLUTTET)

            // Behandling 3: henter inn vurderinger fra både B1 og B2
            val behandling3 = finnEllerOpprettRevurdering(connection, sak, behandling2.id)
            repo.lagreOpphold(behandling3.id, listOf(hsOpphold(1 januar 2026, 5 oktober 2026)))
            repo.lagreHelseVurdering(
                behandling3.id,
                listOf(
                    vurdering(Periode(1 januar 2026, 5 oktober 2026), behandling3.id, begrunnelse = "B3"),
                    vurdering(Periode(1 januar 2026, 1 juli 2026), behandling2.id, begrunnelse = "B2"),
                    vurdering(Periode(1 januar 2026, 31 desember 2999), behandling1.id, begrunnelse = "B1"),
                )
            )

            val grunnlag = requireNotNull(repo.hentHvisEksisterer(behandling3.id))
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger).hasSize(3)
            assertThat(grunnlag.helseoppholdvurderinger?.vurderinger?.map { it.begrunnelse })
                .containsExactlyInAnyOrder("B1", "B2", "B3")
        }

    }

    private fun finnEllerOpprettRevurdering(
        connection: no.nav.aap.komponenter.dbconnect.DBConnection,
        sak: no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak,
        forrigeBehandlingId: BehandlingId,
    ): no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling {
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = forrigeBehandlingId,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(Vurderingsbehov.INSTITUSJONSOPPHOLD, LocalDateTime.now())
                ),
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
            )
        )
    }
}