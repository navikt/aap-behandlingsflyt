package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class BistandRepositoryImplTest {
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

    val sammenligner: RecursiveComparisonConfiguration =
        RecursiveComparisonConfiguration.builder().withIgnoredFields("opprettet", "id").build()

    @Test
    fun `Finner ikke bistand hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val bistandRepository = BistandRepositoryImpl(connection)
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter bistand`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling.id
                    )
                )
            )
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag?.vurderinger)
                .usingRecursiveComparison(sammenligner)
                .isEqualTo(
                    listOf(
                        Bistandsvurdering(
                            begrunnelse = "begrunnelse",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            vurderingenGjelderFra = sak.rettighetsperiode.fom,
                            vurdertAv = "Z00000",
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            vurdertIBehandling = behandling.id
                        )
                    )
                )
        }
    }

    @Test
    fun `test sletting`() {
        TestDataSource().use { dataSource ->
            dataSource.transaction { connection ->
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)
                val bistandRepository = BistandRepositoryImpl(connection)
                bistandRepository.lagre(
                    behandling.id,
                    listOf(
                        Bistandsvurdering(
                            begrunnelse = "begrunnelse",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            vurderingenGjelderFra = sak.rettighetsperiode.fom,
                            vurdertAv = "Z00000",
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            vurdertIBehandling = behandling.id
                        )
                    )
                )
                bistandRepository.lagre(
                    behandling.id,
                    listOf(
                        Bistandsvurdering(
                            begrunnelse = "begrunnelse",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = true,
                            erBehovForAnnenOppfølging = true,
                            vurderingenGjelderFra = sak.rettighetsperiode.fom,
                            vurdertAv = "Z022222",
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            vurdertIBehandling = behandling.id
                        )
                    )
                )
                assertDoesNotThrow { bistandRepository.slett(behandling.id) }
            }
        }
    }

    @Test
    fun `Lagrer ikke like bistand flere ganger for samme behandling`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)

            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "en begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling.id
                    )
                )
            )
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling.id
                    )
                )
            )
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling.id
                    )
                )
            )

            val opplysninger = connection.queryList(
                """
                    SELECT bi.BEGRUNNELSE
                    FROM BEHANDLING b
                    INNER JOIN BISTAND_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BISTAND_VURDERINGER biv ON biv.id = g.bistand_vurderinger_id
                    INNER JOIN BISTAND bi ON bi.bistand_vurderinger_id = biv.id
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
            ) {
                setParams {
                    setLong(1, sak.id.toLong())
                }
                setRowMapper { row -> row.getString("BEGRUNNELSE") }
            }
            assertThat(opplysninger)
                .containsExactly("en begrunnelse", "annen begrunnelse")
                .hasSize(2)
        }
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling1.id
                    )
                )
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            assertThat(bistandGrunnlag?.vurderinger)
                .usingRecursiveComparison(sammenligner)
                .isEqualTo(
                    listOf(
                        Bistandsvurdering(
                            begrunnelse = "begrunnelse",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            vurderingenGjelderFra = sak.rettighetsperiode.fom,
                            vurdertAv = "Z00000",
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            vurdertIBehandling = behandling1.id
                        )
                    )
                )
        }
    }

    @Test
    fun `Kopiering av bistand fra en behandling uten opplysningene skal ikke føre til feil`() {
        dataSource.transaction { connection ->
            val bistandRepository = BistandRepositoryImpl(connection)
            assertDoesNotThrow {
                bistandRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "en begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling1.id
                    )
                )
            )
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling1.id
                    )
                )
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)

            assertThat(bistandGrunnlag?.vurderinger)
                .usingRecursiveComparison(sammenligner)
                .isEqualTo(
                    listOf(
                        Bistandsvurdering(
                            begrunnelse = "annen begrunnelse",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            vurderingenGjelderFra = sak.rettighetsperiode.fom,
                            vurdertAv = "Z00000",
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            vurdertIBehandling = behandling1.id
                        )
                    )
                )
        }
    }

    @Test
    fun `Lagrer nye bistandsopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)

            bistandRepository.lagre(
                behandling.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "en begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling.id
                    )
                )
            )
            val orginaltGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurderinger)
                .usingRecursiveComparison(sammenligner)
                .isEqualTo(
                    listOf(
                        Bistandsvurdering(
                            begrunnelse = "en begrunnelse",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            vurderingenGjelderFra = sak.rettighetsperiode.fom,
                            vurdertAv = "Z00000",
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            vurdertIBehandling = behandling.id
                        )
                    )
                )

            bistandRepository.lagre(
                behandling.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling.id
                    )
                )
            )
            val oppdatertGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurderinger)
                .usingRecursiveComparison(sammenligner)
                .isEqualTo(
                    listOf(
                        Bistandsvurdering(
                            begrunnelse = "annen begrunnelse",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            vurderingenGjelderFra = sak.rettighetsperiode.fom,
                            vurdertAv = "Z00000",
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            vurdertIBehandling = behandling.id
                        )
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
                    INNER JOIN BISTAND_VURDERINGER biv ON biv.id = g.bistand_vurderinger_id
                    INNER JOIN BISTAND bi ON bi.bistand_vurderinger_id = biv.id
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
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "en begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling1.id
                    )
                )
            )
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    Bistandsvurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = sak.rettighetsperiode.fom,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        vurdertIBehandling = behandling1.id
                    )
                )
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

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
                    INNER JOIN BISTAND_VURDERINGER biv ON biv.id = g.bistand_vurderinger_id
                    INNER JOIN BISTAND bi ON bi.bistand_vurderinger_id = biv.id
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
                .containsExactlyInAnyOrder(
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

}