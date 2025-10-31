package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytrevurdering.AvbrytRevurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

internal class SykdomRepositoryImplTest {

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `kan lagre tom liste`() {
        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            sykdomRepo.lagre(behandling.id, emptyList())
            assertThat(sykdomRepo.hent(behandling.id).sykdomsvurderinger).isEmpty()
        }
    }

    @Test
    fun `kan lagre singleton-liste`() {
        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val sykdomsvurdering1 = sykdomsvurdering1(behandling.id)
            sykdomRepo.lagre(behandling.id, listOf(sykdomsvurdering1))
            assertThat(sykdomRepo.hent(behandling.id).sykdomsvurderinger).usingRecursiveComparison()
                .ignoringFields("id", "opprettet").isEqualTo(listOf(sykdomsvurdering1))
        }
    }

    @Test
    fun `kan lagre to elementer`() {
        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val sykdomsvurdering1 = sykdomsvurdering1(behandling.id)
            val sykdomsvurdering2 = sykdomsvurdering2(behandling.id)

            sykdomRepo.lagre(behandling.id, listOf(sykdomsvurdering1, sykdomsvurdering2))
            assertThat(sykdomRepo.hent(behandling.id).sykdomsvurderinger).usingRecursiveComparison()
                .ignoringFields("id", "opprettet").isEqualTo(
                    listOf(
                        sykdomsvurdering1, sykdomsvurdering2
                    )
                )
        }
    }

    @Test
    fun `lagre og hente ned yrkesskade-vurdering`() {
        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurdering = Yrkesskadevurdering(
                begrunnelse = "begr",
                relevanteSaker = listOf(
                    YrkesskadeSak(
                        referanse = "gokk", manuellYrkesskadeDato = LocalDate.now()
                    )
                ),
                erÅrsakssammenheng = true,
                andelAvNedsettelsen = Prosent(70),
                vurdertAv = "Grokki Grokk",
                vurdertTidspunkt = LocalDateTime.now()
            )
            sykdomRepo.lagre(behandling.id, vurdering)
            assertThat(sykdomRepo.hent(behandling.id).yrkesskadevurdering).usingRecursiveComparison()
                .ignoringFields("id", "vurdertTidspunkt").isEqualTo(vurdering)
        }
    }

    @Test
    fun `historikk viser kun vurderinger fra tidligere behandlinger`() {
        val (førstegangsbehandling, sykdomsvurdering1) = dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val førstegangsbehandling = finnEllerOpprettBehandling(connection, sak)

            val sykdomsvurdering1 = sykdomsvurdering1(førstegangsbehandling.id)

            sykdomRepo.lagre(førstegangsbehandling.id, listOf(sykdomsvurdering1))
            Pair(førstegangsbehandling, sykdomsvurdering1)
        }

        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val revurdering = revurdering(connection, førstegangsbehandling)

            val sykdomsvurdering2 = sykdomsvurdering2(revurdering.id)

            sykdomRepo.lagre(revurdering.id, listOf(sykdomsvurdering2))

            val historikk = sykdomRepo.hentHistoriskeSykdomsvurderinger(revurdering.sakId, revurdering.id)
            assertThat(historikk)
                .usingRecursiveComparison()
                .ignoringFields("id", "opprettet")
                .isEqualTo(listOf(sykdomsvurdering1))
        }
    }

    @Test
    fun `historikk viser kun vurderinger fra tidligere behandlinger og ikke inkluderer vurdering fra avbrutt revurdering`() {
        val (førstegangsbehandling, sykdomsvurdering1) = dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val førstegangsbehandling = finnEllerOpprettBehandling(connection, sak)

            val sykdomsvurdering1 = sykdomsvurdering1(førstegangsbehandling.id)

            sykdomRepo.lagre(førstegangsbehandling.id, listOf(sykdomsvurdering1))
            Pair(førstegangsbehandling, sykdomsvurdering1)
        }

        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val avbrytRevurderingRepo = AvbrytRevurderingRepositoryImpl(connection)
            val revurderingAvbrutt = revurdering(connection, førstegangsbehandling)

            // Marker revurderingen som avbrutt
            avbrytRevurderingRepo.lagre(
                revurderingAvbrutt.id, AvbrytRevurderingVurdering(
                    AvbrytRevurderingÅrsak.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                    "avbryte pga. feil",
                    Bruker("Z00000")
                )
            )

            val sykdomsvurdering2 = sykdomsvurdering2(revurderingAvbrutt.id)
            sykdomRepo.lagre(revurderingAvbrutt.id, listOf(sykdomsvurdering2))
        }

        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val revurdering = revurdering(connection, førstegangsbehandling)

            val sykdomsvurdering3 = sykdomsvurdering3(revurdering.id)

            sykdomRepo.lagre(revurdering.id, listOf(sykdomsvurdering3))

            val historikk = sykdomRepo.hentHistoriskeSykdomsvurderinger(revurdering.sakId, revurdering.id)
            assertThat(historikk).usingRecursiveComparison().ignoringFields("id", "opprettet")
                .isEqualTo(listOf(sykdomsvurdering1))
        }
    }

    @Test
    fun `test sletting`() {
        TestDataSource().use { dataSource ->
            dataSource.transaction { connection ->
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)
                val sykdomRepository = SykdomRepositoryImpl(connection)
                sykdomRepository.lagre(
                    behandling.id, listOf(
                        Sykdomsvurdering(
                            begrunnelse = "b1",
                            vurderingenGjelderFra = null,
                            vurderingenGjelderTil = null,
                            dokumenterBruktIVurdering = listOf(JournalpostId("1")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
                            yrkesskadeBegrunnelse = "b",
                            erArbeidsevnenNedsatt = true,
                            vurdertAv = Bruker("Z00000"),
                            vurdertIBehandling = behandling.id,
                            opprettet = Instant.now(),
                        )
                    )
                )
                assertDoesNotThrow {
                    sykdomRepository.slett(behandling.id)
                }
            }
        }
    }

    private companion object {
        private val fom = LocalDate.of(2020, 1, 1)
        private val periode = Periode(fom, fom.plusYears(3))
        private fun sykdomsvurdering1(behandlingId: BehandlingId?) = Sykdomsvurdering(
            begrunnelse = "b1",
            vurderingenGjelderFra = null,
            vurderingenGjelderTil = null,
            dokumenterBruktIVurdering = listOf(JournalpostId("1")),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            yrkesskadeBegrunnelse = "b",
            erArbeidsevnenNedsatt = true,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = behandlingId,
        )

        private fun sykdomsvurdering2(
            behandlingId: BehandlingId?,
            vurderingenGjelderFra: LocalDate = LocalDate.of(2020, 1, 1)
        ) = Sykdomsvurdering(
            begrunnelse = "b2",
            vurderingenGjelderFra = vurderingenGjelderFra,
            vurderingenGjelderTil = null,
            dokumenterBruktIVurdering = listOf(JournalpostId("2")),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            yrkesskadeBegrunnelse = null,
            erArbeidsevnenNedsatt = true,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = behandlingId,
        )

        private fun sykdomsvurdering3(behandlingId: BehandlingId) = Sykdomsvurdering(
            begrunnelse = "b3",
            vurderingenGjelderFra = LocalDate.of(2020, 2, 2),
            vurderingenGjelderTil = null,
            dokumenterBruktIVurdering = listOf(JournalpostId("3")),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            yrkesskadeBegrunnelse = "y",
            erArbeidsevnenNedsatt = true,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = behandlingId,
        )
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway, PersonRepositoryImpl(connection), SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private fun revurdering(connection: DBConnection, behandling: Behandling): Behandling {
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            behandling.sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = behandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)),
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
        )
    }

    @Test
    fun `migrer sykdomsvurderinger`() {
        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val sykdomsvurderingUtenVurdertIBehandling = sykdomsvurdering1(null)
            sykdomRepo.lagre(behandling.id, listOf(sykdomsvurderingUtenVurdertIBehandling))
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)
            val vurdering2fom = sak.rettighetsperiode.fom.plusMonths(2)
            val nyVurdering = sykdomsvurdering2(null, vurdering2fom)
            sykdomRepo.lagre(behandling2.id, listOf(sykdomsvurderingUtenVurdertIBehandling, nyVurdering))

            sykdomRepo.migrerSykdomsvurderinger()

            // DRY-RUN: Ingen endring
            assertThat(sykdomRepo.hent(behandling.id).sykdomsvurderinger).usingRecursiveComparison()
                .ignoringFields("id", "opprettet").isEqualTo(
                    listOf(
                        sykdomsvurderingUtenVurdertIBehandling
                    )
                )
            assertThat(sykdomRepo.hent(behandling2.id).sykdomsvurderinger).usingRecursiveComparison()
                .ignoringFields("id", "opprettet").isEqualTo(
                    listOf(
                        sykdomsvurderingUtenVurdertIBehandling,
                        nyVurdering
                    )
                )


//            assertThat(sykdomRepo.hent(behandling.id).sykdomsvurderinger).usingRecursiveComparison()
//                .ignoringFields("id", "opprettet").isEqualTo(
//                    listOf(
//                        sykdomsvurderingUtenVurdertIBehandling.copy(
//                            vurdertIBehandling = behandling.id,
//                            vurderingenGjelderFra = periode.fom
//                        )
//                    )
//                )
//            assertThat(sykdomRepo.hent(behandling2.id).sykdomsvurderinger).usingRecursiveComparison()
//                .ignoringFields("id", "opprettet").isEqualTo(
//                    listOf(
//                        sykdomsvurderingUtenVurdertIBehandling.copy(
//                            vurdertIBehandling = behandling.id,
//                            vurderingenGjelderFra = periode.fom
//                        ),
//                        nyVurdering.copy(vurdertIBehandling = behandling2.id)
//                    )
//                )
        }
    }
}