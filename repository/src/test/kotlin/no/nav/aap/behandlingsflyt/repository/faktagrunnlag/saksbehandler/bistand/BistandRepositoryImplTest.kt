package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

internal class BistandRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `Finner ikke bistand hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val bistandRepository = BistandRepositoryImpl(connection)
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter bistand`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag?.vurderinger).isEqualTo(
                listOf(
                    BistandVurdering(
                        begrunnelse = "begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
        }
    }

    @Test
    fun `test sletting`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "begrunnelse",
                        erBehovForAktivBehandling = true,
                        erBehovForArbeidsrettetTiltak =true,
                        erBehovForAnnenOppfølging = true,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z022222",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            assertDoesNotThrow { bistandRepository.slett(behandling.id) }
        }
    }

    @Test
    fun `Lagrer ikke like bistand flere ganger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "en begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            bistandRepository.lagre(
                behandling.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
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
                .hasSize(2)
                .containsExactly("en begrunnelse", "annen begrunnelse")
        }
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            assertThat(bistandGrunnlag?.vurderinger).isEqualTo(
                listOf(
                    BistandVurdering(
                        begrunnelse = "begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
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
            val behandling1 = behandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "en begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            assertThat(bistandGrunnlag?.vurderinger).isEqualTo(
                listOf(
                    BistandVurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
        }
    }

    @Test
    fun `Lagrer nye bistandsopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)

            bistandRepository.lagre(
                behandling.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "en begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            val orginaltGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurderinger).isEqualTo(
                listOf(
                    BistandVurdering(
                        begrunnelse = "en begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )

            bistandRepository.lagre(
                behandling.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            val oppdatertGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurderinger).isEqualTo(
                listOf(
                    BistandVurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
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
            val behandling1 = behandling(connection, sak)
            val bistandRepository = BistandRepositoryImpl(connection)
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "en begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
                )
            )
            bistandRepository.lagre(
                behandling1.id,
                listOf(
                    BistandVurdering(
                        begrunnelse = "annen begrunnelse",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = null,
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                    )
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

    @Test
    fun `Kan hente historiske vurderinger fra tidligere behandlinger`() {
        val bistandsvurdering1 = BistandVurdering(
            begrunnelse = "Begrunnelse",
            erBehovForAktivBehandling = true,
            erBehovForArbeidsrettetTiltak = true,
            erBehovForAnnenOppfølging = false,
            vurderingenGjelderFra = null,
            vurdertAv = "Z00000",
            skalVurdereAapIOvergangTilUføre = null,
            skalVurdereAapIOvergangTilArbeid = null,
            overgangBegrunnelse = null,
        )
        val bistandsvurdering2 = BistandVurdering(
            begrunnelse = "Ny begrunnelse",
            erBehovForAktivBehandling = true,
            erBehovForArbeidsrettetTiltak = false,
            erBehovForAnnenOppfølging = false,
            vurderingenGjelderFra = null,
            vurdertAv = "Z00000",
            skalVurdereAapIOvergangTilUføre = null,
            skalVurdereAapIOvergangTilArbeid = null,
            overgangBegrunnelse = null,
        )

        val (førstegangsbehandling, sak) = dataSource.transaction { connection ->
            val repo = BistandRepositoryImpl(connection)
            val sak = sak(connection)
            val førstegangsbehandling = behandling(connection, sak)

            repo.lagre(førstegangsbehandling.id, listOf(bistandsvurdering1))
            repo.lagre(førstegangsbehandling.id, listOf(bistandsvurdering2))
            Pair(førstegangsbehandling, sak)
        }

        val revurderingUtenOppdatertBistandsvurdering = dataSource.transaction { connection ->
            val repo = BistandRepositoryImpl(connection)
            val revurdering = revurdering(connection, førstegangsbehandling, sak)
            val historikk = repo.hentHistoriskeBistandsvurderinger(revurdering.sakId, revurdering.id)
            assertEquals(listOf(bistandsvurdering2), historikk)
            revurdering
        }

        dataSource.transaction { connection ->
            val repo = BistandRepositoryImpl(connection)
            val revurdering = revurdering(connection, revurderingUtenOppdatertBistandsvurdering, sak)
            val bistandsvurdering3 = BistandVurdering(
                begrunnelse = "Tredje begrunnelse",
                erBehovForAktivBehandling = true,
                erBehovForArbeidsrettetTiltak = false,
                erBehovForAnnenOppfølging = false,
                vurderingenGjelderFra = null,
                vurdertAv = "Z00000",
                skalVurdereAapIOvergangTilUføre = null,
                skalVurdereAapIOvergangTilArbeid = null,
                overgangBegrunnelse = null,
            )
            repo.lagre(revurdering.id, listOf(bistandsvurdering3))
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
            postgresRepositoryRegistry.provider(connection),
            unleashGateway = FakeUnleash,
        )
            .finnEllerOpprettBehandling(
                sak.saksnummer,
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                ÅrsakTilOpprettelse.SØKNAD
            )
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