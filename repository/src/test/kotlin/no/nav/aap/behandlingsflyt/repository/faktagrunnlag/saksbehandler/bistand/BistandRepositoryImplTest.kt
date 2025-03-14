package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class BistandRepositoryImplTest {

    @BeforeEach
    fun setUp() {
        RepositoryRegistry.register(BistandRepositoryImpl::class)
    }

    @Test
    fun `Finner ikke bistand hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val bistandRepository = BistandRepositoryImpl(connection)
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter bistand`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling.id,
                BistandVurdering(
                    begrunnelse = "begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag?.vurdering).isEqualTo(
                BistandVurdering(
                    begrunnelse = "begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
        }
    }

    @Test
    fun `Lagrer ikke like bistand flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling.id,
                BistandVurdering(
                    begrunnelse = "en begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            bistandRepository.lagre(
                behandling.id,
                BistandVurdering(
                    begrunnelse = "annen begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            bistandRepository.lagre(
                behandling.id,
                BistandVurdering(
                    begrunnelse = "annen begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )

            val opplysninger = connection.queryList(
                """
                    SELECT bi.BEGRUNNELSE
                    FROM BEHANDLING b
                    INNER JOIN BISTAND_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BISTAND bi ON g.BISTAND_ID = bi.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
            ) {
                setParams {
                    setLong(1, sak.id.toLong())
                }
                setRowMapper { row -> row.getString("BEGRUNNELSE") }
            }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly("en begrunnelse", "annen begrunnelse")
        }
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling1.id,
                BistandVurdering(
                    begrunnelse = "begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            assertThat(bistandGrunnlag?.vurdering).isEqualTo(
                BistandVurdering(
                    begrunnelse = "begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
        }
    }

    @Test
    fun `Kopiering av bistand fra en behandling uten opplysningene skal ikke føre til feil`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val bistandRepository = BistandRepositoryImpl(connection)
            assertDoesNotThrow {
                bistandRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling1.id,
                BistandVurdering(
                    begrunnelse = "en begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            bistandRepository.lagre(
                behandling1.id,
                BistandVurdering(
                    begrunnelse = "annen begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            assertThat(bistandGrunnlag?.vurdering).isEqualTo(
                BistandVurdering(
                    begrunnelse = "annen begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
        }
    }

    @Test
    fun `Lagrer nye bistandsopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)

            bistandRepository.lagre(
                behandling.id,
                BistandVurdering(
                    begrunnelse = "en begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            val orginaltGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurdering).isEqualTo(
                BistandVurdering(
                    begrunnelse = "en begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )

            bistandRepository.lagre(
                behandling.id,
                BistandVurdering(
                    begrunnelse = "annen begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            val oppdatertGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurdering).isEqualTo(
                BistandVurdering(
                    begrunnelse = "annen begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )

            data class Opplysning(
                val aktiv: Boolean,
                val begrunnelse: String,
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.AKTIV, bi.BEGRUNNELSE
                    FROM BEHANDLING b
                    INNER JOIN BISTAND_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BISTAND bi ON g.BISTAND_ID = bi.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Opplysning(
                            aktiv = row.getBoolean("AKTIV"),
                            begrunnelse = row.getString("BEGRUNNELSE")
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(aktiv = false, begrunnelse = "en begrunnelse"),
                    Opplysning(aktiv = true, begrunnelse = "annen begrunnelse")
                )
        }
    }

    @Test
    fun `Ved kopiering av bistandsopplysninger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling1.id,
                BistandVurdering(
                    begrunnelse = "en begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            bistandRepository.lagre(
                behandling1.id,
                BistandVurdering(
                    begrunnelse = "annen begrunnelse",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
                )
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val begrunnelse: String
            )

            data class Grunnlag(val bistandId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, bi.ID AS BISTAND_ID, g.AKTIV, bi.BEGRUNNELSE
                    FROM BEHANDLING b
                    INNER JOIN BISTAND_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BISTAND bi ON g.BISTAND_ID = bi.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Grunnlag(
                            bistandId = row.getLong("BISTAND_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                aktiv = row.getBoolean("AKTIV"),
                                begrunnelse = row.getString("BEGRUNNELSE"),
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(Grunnlag::bistandId).distinct())
                .hasSize(2)
            assertThat(opplysninger.map(Grunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = false,
                        begrunnelse = "en begrunnelse",
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        begrunnelse = "annen begrunnelse",
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        begrunnelse = "annen begrunnelse",
                    )
                )
        }
    }
    
    @Test
    fun `Kan hente historiske vurderinger fra tidligere behandlinger`() {
        val bistandsvurdering1 = BistandVurdering(
            begrunnelse = "Begrunnelse",
            erBehovForAktivBehandling = true,
            erBehovForArbeidsrettetTiltak = true,
            erBehovForAnnenOppfølging = false,
            vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
        )
        val bistandsvurdering2 = BistandVurdering(
            begrunnelse = "Ny begrunnelse",
            erBehovForAktivBehandling = true,
            erBehovForArbeidsrettetTiltak = false,
            erBehovForAnnenOppfølging = false,
            vurderingenGjelderFra = null,
                    vurdertAv = "Z00000"
        )
        
        val (førstegangsbehandling, sak) = InitTestDatabase.dataSource.transaction { connection ->
            val repo = BistandRepositoryImpl(connection)
            val sak = sak(connection)
            val førstegangsbehandling = behandling(connection, sak)
            
            repo.lagre(førstegangsbehandling.id, bistandsvurdering1)
            repo.lagre(førstegangsbehandling.id, bistandsvurdering2)
            Pair(førstegangsbehandling, sak)
        }
        
        val revurderingUtenOppdatertBistandsvurdering = InitTestDatabase.dataSource.transaction { connection ->
            val repo = BistandRepositoryImpl(connection)
            val revurdering = revurdering(connection, førstegangsbehandling, sak)
            val historikk = repo.hentHistoriskeBistandsvurderinger(revurdering.sakId, revurdering.id)
            assertEquals(listOf(bistandsvurdering2), historikk)
            revurdering
        }
        
        InitTestDatabase.dataSource.transaction { connection ->
            val repo = BistandRepositoryImpl(connection)
            val revurdering = revurdering(connection, revurderingUtenOppdatertBistandsvurdering, sak)
            val bistandsvurdering3 = BistandVurdering(
                begrunnelse = "Tredje begrunnelse",
                erBehovForAktivBehandling = true,
                erBehovForArbeidsrettetTiltak = false,
                erBehovForAnnenOppfølging = false,
                vurderingenGjelderFra = null,
                vurdertAv = "Z00000"
            )
            repo.lagre(revurdering.id, bistandsvurdering3)
            val historikk = repo.hentHistoriskeBistandsvurderinger(revurdering.sakId, revurdering.id)
            assertEquals(listOf(bistandsvurdering2), historikk)
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(
            GrunnlagKopierer(connection), SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection)
        ).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
    }

    private fun revurdering(connection: DBConnection, behandling: Behandling, sak: Sak): Behandling {
        connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
            setParams {
                setLong(1, behandling.id.toLong())
            }
        }
        
        return behandling(connection, sak)
    }

}