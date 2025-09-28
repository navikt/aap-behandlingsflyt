package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

internal class SykdomRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `kan lagre tom liste`() {
        dataSource.transaction { connection ->
            val repo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            repo.lagre(behandling.id, emptyList())
            assertEquals(emptyList(), repo.hent(behandling.id).sykdomsvurderinger)
        }
    }

    @Test
    fun `kan lagre singleton-liste`() {
        dataSource.transaction { connection ->
            val repo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            repo.lagre(behandling.id, listOf(sykdomsvurdering1))
            assertEquals(listOf(sykdomsvurdering1), repo.hent(behandling.id).sykdomsvurderinger)
        }
    }

    @Test
    fun `kan lagre to elementer`() {
        dataSource.transaction { connection ->
            val repo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            repo.lagre(behandling.id, listOf(sykdomsvurdering1, sykdomsvurdering2))
            assertEquals(listOf(sykdomsvurdering1, sykdomsvurdering2), repo.hent(behandling.id).sykdomsvurderinger)
        }
    }

    @Test
    fun `lagre og hente ned yrkesskade-vurdering`() {
        dataSource.transaction { connection ->
            val repo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurdering = Yrkesskadevurdering(
                begrunnelse = "begr",
                relevanteSaker = listOf(
                    YrkesskadeSak(
                        referanse = "gokk",
                        manuellYrkesskadeDato = LocalDate.now()
                    )
                ),
                erÅrsakssammenheng = true,
                andelAvNedsettelsen = Prosent(70),
                vurdertAv = "Grokki Grokk",
                vurdertTidspunkt = LocalDateTime.now()
            )
            repo.lagre(behandling.id, vurdering)
            assertThat(repo.hent(behandling.id).yrkesskadevurdering)
                .usingRecursiveComparison().ignoringFields("id", "vurdertTidspunkt")
                .isEqualTo(vurdering)
        }
    }

    @Test
    fun `historikk viser kun vurderinger fra tidligere behandlinger`() {
        val førstegangsbehandling = dataSource.transaction { connection ->
            val repo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val førstegangsbehandling = finnEllerOpprettBehandling(connection, sak)

            repo.lagre(førstegangsbehandling.id, listOf(sykdomsvurdering1))
            førstegangsbehandling
        }

        dataSource.transaction { connection ->
            val repo = SykdomRepositoryImpl(connection)
            val revurdering = revurdering(connection, førstegangsbehandling)

            repo.lagre(revurdering.id, listOf(sykdomsvurdering2))

            val historikk = repo.hentHistoriskeSykdomsvurderinger(revurdering.sakId, revurdering.id, emptyList())
            assertEquals(listOf(sykdomsvurdering1), historikk)
        }
    }

    @Test
    fun `test sletting`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val sykdomRepository = SykdomRepositoryImpl(connection)
            sykdomRepository.lagre(
                behandling.id, listOf(
                    Sykdomsvurdering(
                        begrunnelse = "b1",
                        vurderingenGjelderFra = null,
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
                    )
                )
            )
            assertDoesNotThrow {
                sykdomRepository.slett(behandling.id)
            }
        }
    }


    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val sykdomsvurdering1 = Sykdomsvurdering(
            begrunnelse = "b1",
            vurderingenGjelderFra = null,
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
        )

        private val sykdomsvurdering2 = Sykdomsvurdering(
            begrunnelse = "b2",
            vurderingenGjelderFra = LocalDate.of(2020, 1, 1),
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
        )

        fun assertEquals(expected: List<Sykdomsvurdering>, actual: List<Sykdomsvurdering>) {
            assertEquals(expected.size, actual.size)
            for ((expected, actual) in expected.zip(actual)) {
                assertEquals(expected, actual)
            }
        }

        fun assertEquals(expected: Sykdomsvurdering, actual: Sykdomsvurdering) {
            if (expected.id != null && actual.id != null) {
                assertEquals(expected.id, actual.id)
            }
            assertEquals(expected.begrunnelse, actual.begrunnelse)
            assertEquals(expected.vurderingenGjelderFra, actual.vurderingenGjelderFra)
            assertEquals(expected.dokumenterBruktIVurdering, actual.dokumenterBruktIVurdering)
            assertEquals(expected.harSkadeSykdomEllerLyte, actual.harSkadeSykdomEllerLyte)
            assertEquals(expected.erSkadeSykdomEllerLyteVesentligdel, actual.erSkadeSykdomEllerLyteVesentligdel)
            assertEquals(
                expected.erNedsettelseIArbeidsevneAvEnVissVarighet,
                actual.erNedsettelseIArbeidsevneAvEnVissVarighet
            )
            assertEquals(
                expected.erNedsettelseIArbeidsevneMerEnnHalvparten,
                actual.erNedsettelseIArbeidsevneMerEnnHalvparten
            )
            assertEquals(
                expected.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
                actual.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense
            )
            assertEquals(expected.yrkesskadeBegrunnelse, actual.yrkesskadeBegrunnelse)
            assertEquals(expected.erArbeidsevnenNedsatt, actual.erArbeidsevnenNedsatt)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private fun revurdering(connection: DBConnection, behandling: Behandling): Behandling {
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            behandling.sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = behandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )
    }
}