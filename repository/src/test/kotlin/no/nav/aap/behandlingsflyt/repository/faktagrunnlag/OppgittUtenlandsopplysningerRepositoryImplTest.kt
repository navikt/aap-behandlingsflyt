package no.nav.aap.behandlingsflyt.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.OppgittUtenlandsopplysningerRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsPeriode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.lovvalgmedlemskap.LovvalgMedlemskapRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.lovvalgmedlemskap.OppgittUtenlandsopplysningerRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytrevurdering.AvbrytRevurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OppgittUtenlandsopplysningerRepositoryImplTest {
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
    fun `returnerer null når opplysninger ikke finnes`() {
        dataSource.transaction { connection ->
            val repository = OppgittUtenlandsopplysningerRepositoryImpl(connection)
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)

            assertThat(repository.hentOppgittUtenlandsOppholdHvisEksisterer(behandling.id)).isNull()
            assertThat(repository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sak.id)).isNull()
        }
    }

    @Test
    fun `lagrer og henter opplysninger med utenlandsperioder via registrert repository`() {
        dataSource.transaction { connection ->
            val repository = postgresRepositoryRegistry.provider(connection)
                .provide<OppgittUtenlandsopplysningerRepository>()
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val opplysninger = utenlandsOppholdData()

            repository.lagreOppgittUtenlandsOppplysninger(behandling.id, JournalpostId("1"), opplysninger)

            assertThat(repository.hentOppgittUtenlandsOppholdHvisEksisterer(behandling.id)).isEqualTo(opplysninger)
            assertThat(repository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sak.id))
                .isEqualTo(opplysninger)
        }
    }

    @Test
    fun `lagrer opplysninger uten utenlandsperioder`() {
        dataSource.transaction { connection ->
            val repository = OppgittUtenlandsopplysningerRepositoryImpl(connection)
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val opplysninger = utenlandsOppholdData().copy(utenlandsOpphold = null)

            repository.lagreOppgittUtenlandsOppplysninger(behandling.id, JournalpostId("1"), opplysninger)

            assertThat(repository.hentOppgittUtenlandsOppholdHvisEksisterer(behandling.id))
                .isEqualTo(opplysninger.copy(utenlandsOpphold = emptyList()))
        }
    }

    @Test
    fun `ny lagring erstatter aktivt grunnlag og beholder journalpostreferansen`() {
        dataSource.transaction { connection ->
            val repository = OppgittUtenlandsopplysningerRepositoryImpl(connection)
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val oppdatert = utenlandsOppholdData().copy(harBoddINorgeSiste5År = true, utenlandsOpphold = emptyList())
            repository.lagreOppgittUtenlandsOppplysninger(behandling.id, JournalpostId("1"), utenlandsOppholdData())

            repository.lagreOppgittUtenlandsOppplysninger(behandling.id, JournalpostId("2"), oppdatert)

            assertThat(repository.hentOppgittUtenlandsOppholdHvisEksisterer(behandling.id)).isEqualTo(oppdatert)
            assertThat(repository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sak.id)).isEqualTo(oppdatert)
            val journalposter = connection.queryList(
                "SELECT journalpost_id FROM OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG WHERE behandling_id = ? AND aktiv"
            ) {
                setParams { setLong(1, behandling.id.toLong()) }
                setRowMapper { it.getString("journalpost_id") }
            }
            assertThat(journalposter).containsExactly("2")
        }
    }

    @Test
    fun `kopierer ikke opplysninger men henter fra siste relevante behandling`() {
        dataSource.transaction { connection ->
            val repository = OppgittUtenlandsopplysningerRepositoryImpl(connection)
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val revurdering = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id,
                TypeBehandling.Revurdering,
                behandling.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD
                )
            )
            repository.lagreOppgittUtenlandsOppplysninger(behandling.id, JournalpostId("1"), utenlandsOppholdData())

            repository.kopier(behandling.id, revurdering.id)

            assertThat(repository.hentOppgittUtenlandsOppholdHvisEksisterer(revurdering.id)).isNull()
            assertThat(repository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sak.id))
                .isEqualTo(utenlandsOppholdData())
        }
    }

    @Test
    fun `ser bort fra avbrutte revurderinger og opplysninger i andre saker`() {
        val sak = dataSource.transaction { connection ->
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)
            OppgittUtenlandsopplysningerRepositoryImpl(connection)
                .lagreOppgittUtenlandsOppplysninger(behandling.id, JournalpostId("1"), utenlandsOppholdData())
            sak
        }

        dataSource.transaction { connection ->
            val repository = OppgittUtenlandsopplysningerRepositoryImpl(connection)
            val revurdering = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id,
                TypeBehandling.Revurdering,
                null,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD
                )
            )
            val andreOpplysninger = utenlandsOppholdData().copy(harBoddINorgeSiste5År = true)
            repository.lagreOppgittUtenlandsOppplysninger(revurdering.id, JournalpostId("2"), andreOpplysninger)
            AvbrytRevurderingRepositoryImpl(connection).lagre(
                revurdering.id,
                AvbrytRevurderingVurdering(null, "Avbrytes", Bruker("SAKSBEHANDLER"))
            )
            val annenSak = opprettSak(connection, LocalDate.now())
            val annenBehandling = finnEllerOpprettBehandling(connection, annenSak)
            repository.lagreOppgittUtenlandsOppplysninger(annenBehandling.id, JournalpostId("3"), andreOpplysninger)

            assertThat(repository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sak.id))
                .isEqualTo(utenlandsOppholdData())
        }
    }

    @Test
    fun `sletter aktive og inaktive opplysninger uten å slette annet grunnlag`() {
        dataSource.transaction { connection ->
            val repository = OppgittUtenlandsopplysningerRepositoryImpl(connection)
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val annenSak = opprettSak(connection, LocalDate.now())
            val annenBehandling = finnEllerOpprettBehandling(connection, annenSak)
            val lovvalgRepository = LovvalgMedlemskapRepositoryImpl(connection)
            lovvalgRepository.lagreVurderinger(behandling.id, emptyList())
            repository.lagreOppgittUtenlandsOppplysninger(behandling.id, JournalpostId("1"), utenlandsOppholdData())
            repository.lagreOppgittUtenlandsOppplysninger(behandling.id, JournalpostId("2"), utenlandsOppholdData())
            repository.lagreOppgittUtenlandsOppplysninger(annenBehandling.id, JournalpostId("3"), utenlandsOppholdData())
            val oppholdIds = connection.queryList(
                "SELECT oppgitt_utenlandsopphold_id FROM OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG WHERE behandling_id = ?"
            ) {
                setParams { setLong(1, behandling.id.toLong()) }
                setRowMapper { it.getLong("oppgitt_utenlandsopphold_id") }
            }
            assertThat(oppholdIds).hasSize(2)

            repository.slett(behandling.id)
            repository.slett(behandling.id)

            assertThat(repository.hentOppgittUtenlandsOppholdHvisEksisterer(behandling.id)).isNull()
            assertThat(repository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sak.id)).isNull()
            assertThat(repository.hentOppgittUtenlandsOppholdHvisEksisterer(annenBehandling.id))
                .isEqualTo(utenlandsOppholdData())
            assertThat(lovvalgRepository.hentHvisEksisterer(behandling.id)).isNotNull()
            for ((tabell, idKolonne) in listOf(
                "OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG" to "oppgitt_utenlandsopphold_id",
                "OPPGITT_UTENLANDSOPPHOLD" to "id",
                "UTENLANDS_PERIODE" to "oppgitt_utenlandsopphold_id"
            )) {
                val antall = connection.queryFirst("SELECT count(*) AS antall FROM $tabell WHERE $idKolonne = ANY(?::bigint[])") {
                    setParams { setLongArray(1, oppholdIds) }
                    setRowMapper { it.getLong("antall") }
                }
                assertThat(antall).describedAs("Rader i $tabell etter sletting").isZero()
            }
        }
    }

    private fun utenlandsOppholdData() = UtenlandsOppholdData(
        harBoddINorgeSiste5År = false,
        harArbeidetINorgeSiste5År = true,
        arbeidetUtenforNorgeFørSykdom = true,
        iTilleggArbeidUtenforNorge = false,
        utenlandsOpphold = listOf(
            UtenlandsPeriode(
                land = "SWE",
                tilDato = LocalDate.of(2024, 12, 31),
                fraDato = LocalDate.of(2024, 1, 1),
                iArbeid = true,
                utenlandsId = "utenlands-id"
            ),
            UtenlandsPeriode(
                land = null,
                tilDato = null,
                fraDato = null,
                iArbeid = false,
                utenlandsId = null
            )
        )
    )
}
