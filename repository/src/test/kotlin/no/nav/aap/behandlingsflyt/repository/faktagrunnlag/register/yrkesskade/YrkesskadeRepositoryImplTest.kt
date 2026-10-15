package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.SkadekombinasjonRegister
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class YrkesskadeRepositoryImplTest {
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

        private val yrkesskadeMedAlleFelter = Yrkesskade(
            ref = "ref",
            saksnummer = 123,
            kildesystem = "KOMPYS",
            skadedato = 4 juni 2019,
            vedtaksdato = 1 mai 2020,
            skadeart = "Arbeidsulykke",
            diagnose = "Lumbago",
            skadekombinasjoner = listOf(
                SkadekombinasjonRegister(
                    kroppsdel = "korsrygg",
                    skadetype = "belastningsskade"
                )
            ),
            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
        )
    }

    @Test
    fun `Finner ikke yrkesskadeopplysninger hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(yrkesskadeGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter yrkesskadeopplysninger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeMedAlleFelter)),
                oppgittYrkesskadeISøknad = true,
            )
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)

            assertThat(yrkesskadeGrunnlag?.oppgittYrkesskadeISøknad).isTrue()

            val hentet = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.single()
            assertThat(hentet?.ref).isEqualTo("ref")
            assertThat(hentet?.saksnummer).isEqualTo(123)
            assertThat(hentet?.kildesystem).isEqualTo("KOMPYS")
            assertThat(hentet?.skadedato).isEqualTo(4 juni 2019)
            assertThat(hentet?.vedtaksdato).isEqualTo(1 mai 2020)
            assertThat(hentet?.skadeart).isEqualTo("Arbeidsulykke")
            assertThat(hentet?.diagnose).isEqualTo("Lumbago")
            assertThat(hentet?.skadekombinasjonerTekst).isEqualTo("Belastningsskade i korsrygg")
        }
    }

    @Test
    fun `Lagrer og henter oppgittYrkesskadeISøknad uten registerdata`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = null,
                oppgittYrkesskadeISøknad = true,
            )
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(yrkesskadeGrunnlag).isNotNull()
            assertThat(yrkesskadeGrunnlag?.oppgittYrkesskadeISøknad).isTrue()
            assertThat(yrkesskadeGrunnlag?.yrkesskader?.yrkesskader).isEmpty()
        }
    }

    @Test
    fun `Lagrer og henter yrkesskadeopplysninger med manglende skadedato`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val yrkesskade = Yrkesskade(
                ref = "ref",
                saksnummer = 123,
                kildesystem = "INFOTRYGD",
                skadedato = null,
                vedtaksdato = 1 mai 2020,
                skadeart = "Yrkessykdom",
                diagnose = "Karpaltunnelsyndrom",
                skadekombinasjoner = listOf(SkadekombinasjonRegister(kroppsdel = "håndledd", skadetype = "nerveskade")),
                skadekombinasjonerTekst = "Nerveskade i håndledd",
            )
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskade)),
                oppgittYrkesskadeISøknad = false,
            )
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(yrkesskadeGrunnlag?.oppgittYrkesskadeISøknad).isFalse()

            val hentet = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.single()
            assertThat(hentet?.skadedato).isNull()
            assertThat(hentet?.vedtaksdato).isEqualTo(1 mai 2020)
            assertThat(hentet?.skadeart).isEqualTo("Yrkessykdom")
            assertThat(hentet?.diagnose).isEqualTo("Karpaltunnelsyndrom")
            assertThat(hentet?.skadekombinasjonerTekst).isEqualTo("Nerveskade i håndledd")
        }
    }

    @Test
    fun `Lagrer ikke like opplysninger flere ganger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019,
                            vedtaksdato = 1 mai 2020, skadeart = "Arbeidsulykke", diagnose = "Lumbago",
                            skadekombinasjoner = listOf(
                                SkadekombinasjonRegister(
                                    kroppsdel = "korsrygg",
                                    skadetype = "belastningsskade"
                                )
                            ),
                            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = true,
            )
            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019,
                            vedtaksdato = 1 mai 2020, skadeart = "Arbeidsulykke", diagnose = "Lumbago",
                            skadekombinasjoner = listOf(
                                SkadekombinasjonRegister(
                                    kroppsdel = "korsrygg",
                                    skadetype = "belastningsskade"
                                )
                            ),
                            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = true,
            )
            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019,
                            vedtaksdato = 1 mai 2020, skadeart = "Arbeidsulykke", diagnose = "Lumbago",
                            skadekombinasjoner = listOf(
                                SkadekombinasjonRegister(
                                    kroppsdel = "korsrygg",
                                    skadetype = "belastningsskade"
                                )
                            ),
                            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = true,
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID, g.AKTIV, p.REFERANSE, p.SKADEDATO
                    FROM BEHANDLING b
                    INNER JOIN YRKESSKADE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN YRKESSKADE y ON g.YRKESSKADE_ID = y.ID
                    INNER JOIN YRKESSKADE_DATO p ON y.ID = p.YRKESSKADE_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams { setLong(1, sak.id.toLong()) }
                    setRowMapper { row -> row.getLocalDate("SKADEDATO") }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(4 juni 2019, 4 mai 2019)
        }
    }

    @Test
    fun `Kopierer yrkesskadeopplysninger fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeMedAlleFelter)),
                oppgittYrkesskadeISøknad = true,
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling2.id)
            assertThat(yrkesskadeGrunnlag?.oppgittYrkesskadeISøknad).isTrue()

            val hentet = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.single()
            assertThat(hentet?.skadedato).isEqualTo(4 juni 2019)
            assertThat(hentet?.vedtaksdato).isEqualTo(1 mai 2020)
            assertThat(hentet?.skadeart).isEqualTo("Arbeidsulykke")
            assertThat(hentet?.diagnose).isEqualTo("Lumbago")
            assertThat(hentet?.skadekombinasjonerTekst).isEqualTo("Belastningsskade i korsrygg")
        }
    }

    @Test
    fun `Kopiering av yrkesskadeopplysninger fra en behandling uten opplysningene skal ikke føre til feil`() {
        dataSource.transaction { connection ->
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            assertDoesNotThrow {
                yrkesskadeRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer yrkesskadeopplysninger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019,
                            skadeart = "Arbeidsulykke", diagnose = "Lumbago",
                            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = false,
            )
            yrkesskadeRepository.lagre(
                behandling1.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019,
                            skadeart = "Arbeidsulykke", diagnose = "Lumbago",
                            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = false,
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling2.id)
            assertThat(yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.single()?.skadedato).isEqualTo(4 mai 2019)
        }
    }

    @Test
    fun `test sletting`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019,
                            skadeart = "Arbeidsulykke", diagnose = "Hjernerystelse",
                            skadekombinasjonerTekst = "Kjemikalieeksponering i hjerne",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = true,
            )
            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "rexxf", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 5 juni 2019,
                            skadeart = "Yrkessykdom", diagnose = "Hørselstap",
                            skadekombinasjonerTekst = "Hørseltap i venstre øre",
                        ),
                        Yrkesskade(
                            ref = "rxef", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 8 mai 2019,
                            skadeart = "Arbeidsulykke", diagnose = "Håndleddsfraktur",
                            skadekombinasjonerTekst = "Bruddskade i håndledd",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = true,
            )

            assertDoesNotThrow { yrkesskadeRepository.slett(behandling.id) }
        }
    }

    @Test
    fun `Lagrer nye opplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)

            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019,
                            vedtaksdato = 1 mai 2020, skadeart = "Arbeidsulykke", diagnose = "Lumbago",
                            skadekombinasjoner = listOf(
                                SkadekombinasjonRegister(
                                    kroppsdel = "korsrygg",
                                    skadetype = "belastningsskade"
                                )
                            ),
                            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = true,
            )
            val orginaltGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.yrkesskader?.yrkesskader?.single()?.skadedato).isEqualTo(4 juni 2019)
            assertThat(orginaltGrunnlag?.oppgittYrkesskadeISøknad).isTrue()

            yrkesskadeRepository.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019,
                            vedtaksdato = 1 mai 2020, skadeart = "Arbeidsulykke", diagnose = "Lumbago",
                            skadekombinasjoner = listOf(
                                SkadekombinasjonRegister(
                                    kroppsdel = "korsrygg",
                                    skadetype = "belastningsskade"
                                )
                            ),
                            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = true,
            )
            val oppdatertGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.yrkesskader?.yrkesskader?.single()?.skadedato).isEqualTo(4 mai 2019)

            data class Opplysning(val behandlingId: Long, val aktiv: Boolean, val ref: String, val skadedato: LocalDate)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID, g.AKTIV, p.REFERANSE, p.SKADEDATO
                    FROM BEHANDLING b
                    INNER JOIN YRKESSKADE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN YRKESSKADE y ON g.YRKESSKADE_ID = y.ID
                    INNER JOIN YRKESSKADE_DATO p ON y.ID = p.YRKESSKADE_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams { setLong(1, sak.id.toLong()) }
                    setRowMapper { row ->
                        Opplysning(
                            behandlingId = row.getLong("ID"),
                            aktiv = row.getBoolean("AKTIV"),
                            ref = row.getString("REFERANSE"),
                            skadedato = row.getLocalDate("SKADEDATO")
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(behandling.id.toLong(), false, "ref", 4 juni 2019),
                    Opplysning(behandling.id.toLong(), true, "ref", 4 mai 2019)
                )
        }
    }

    @Test
    fun `Ved kopiering av fritaksvurderinger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019,
                            skadeart = "Arbeidsulykke", diagnose = "Lumbago",
                            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = true,
            )
            yrkesskadeRepository.lagre(
                behandling1.id,
                registerYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019,
                            skadeart = "Arbeidsulykke", diagnose = "Lumbago",
                            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
                        )
                    )
                ),
                oppgittYrkesskadeISøknad = true,
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            data class Opplysning(val behandlingId: Long, val aktiv: Boolean, val ref: String, val skadedato: LocalDate)
            data class Grunnlag(val yrkesskadeId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, y.ID AS YRKESSKADE_ID, g.AKTIV, p.REFERANSE, p.SKADEDATO
                    FROM BEHANDLING b
                    INNER JOIN YRKESSKADE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN YRKESSKADE y ON g.YRKESSKADE_ID = y.ID
                    INNER JOIN YRKESSKADE_DATO p ON y.ID = p.YRKESSKADE_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams { setLong(1, sak.id.toLong()) }
                    setRowMapper { row ->
                        Grunnlag(
                            yrkesskadeId = row.getLong("YRKESSKADE_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                aktiv = row.getBoolean("AKTIV"),
                                ref = row.getString("REFERANSE"),
                                skadedato = row.getLocalDate("SKADEDATO")
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(Grunnlag::yrkesskadeId).distinct()).hasSize(2)
            assertThat(opplysninger.map(Grunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = false,
                        ref = "ref",
                        skadedato = 4 juni 2019
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        ref = "ref",
                        skadedato = 4 mai 2019
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        ref = "ref",
                        skadedato = 4 mai 2019
                    )
                )
        }
    }

    private fun yrkesskadeForLagring(skadedato: LocalDate = LocalDate.of(2019, 6, 4)) = Yrkesskade(
        ref = "ref",
        saksnummer = 123,
        kildesystem = "INFOTRYGD",
        skadedato = skadedato,
        vedtaksdato = LocalDate.of(2020, 5, 1),
        skadeart = "Arbeidsulykke",
        diagnose = "Lumbago",
        skadekombinasjoner = emptyList(),
        skadekombinasjonerTekst = null,
    )

    private fun grunnlagRader(connection: DBConnection, behandlingId: BehandlingId): List<Pair<Long, Boolean>> =
        connection.queryList(
            "SELECT ID, AKTIV FROM YRKESSKADE_GRUNNLAG WHERE BEHANDLING_ID = ? ORDER BY ID"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("ID") to it.getBoolean("AKTIV") }
        }

    @Test
    fun `lagrer grunnlag uten yrkesskader når register er null`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = YrkesskadeRepositoryImpl(connection)

            repository.lagre(behandling.id, registerYrkesskader = null, oppgittYrkesskadeISøknad = false)

            val grunnlag = repository.hentHvisEksisterer(behandling.id)
            assertThat(grunnlag).isNotNull
            assertThat(grunnlag!!.yrkesskader.yrkesskader).isEmpty()
            assertThat(grunnlag.oppgittYrkesskadeISøknad).isFalse()
        }
    }

    @Test
    fun `tom Yrkesskader behandles likt som null`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = YrkesskadeRepositoryImpl(connection)

            repository.lagre(behandling.id, registerYrkesskader = Yrkesskader(emptyList()), oppgittYrkesskadeISøknad = false)

            val grunnlag = repository.hentHvisEksisterer(behandling.id)
            assertThat(grunnlag).isNotNull
            assertThat(grunnlag!!.yrkesskader.yrkesskader).isEmpty()
        }
    }

    @Test
    fun `null etterfulgt av tom liste med samme søknadsverdi lager ikke nytt grunnlag`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = YrkesskadeRepositoryImpl(connection)

            repository.lagre(behandling.id, registerYrkesskader = null, oppgittYrkesskadeISøknad = false)
            val førsteId = repository.hentHvisEksisterer(behandling.id)!!.id

            repository.lagre(behandling.id, registerYrkesskader = Yrkesskader(emptyList()), oppgittYrkesskadeISøknad = false)
            repository.lagre(behandling.id, registerYrkesskader = null, oppgittYrkesskadeISøknad = false)

            assertThat(repository.hentHvisEksisterer(behandling.id)!!.id).isEqualTo(førsteId)
            assertThat(grunnlagRader(connection, behandling.id)).containsExactly(førsteId to true)
        }
    }

    @Test
    fun `samme yrkesskader lagret to ganger lager ikke nytt grunnlag`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = YrkesskadeRepositoryImpl(connection)
            val yrkesskader = Yrkesskader(listOf(yrkesskadeForLagring()))

            repository.lagre(behandling.id, yrkesskader, oppgittYrkesskadeISøknad = true)
            val førsteId = repository.hentHvisEksisterer(behandling.id)!!.id

            repository.lagre(behandling.id, yrkesskader, oppgittYrkesskadeISøknad = true)

            assertThat(repository.hentHvisEksisterer(behandling.id)!!.id).isEqualTo(førsteId)
            assertThat(grunnlagRader(connection, behandling.id)).hasSize(1)
        }
    }

    @Test
    fun `endret oppgittYrkesskadeISøknad lager nytt grunnlag og deaktiverer det gamle`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = YrkesskadeRepositoryImpl(connection)

            repository.lagre(behandling.id, registerYrkesskader = null, oppgittYrkesskadeISøknad = false)
            val førsteId = repository.hentHvisEksisterer(behandling.id)!!.id

            repository.lagre(behandling.id, registerYrkesskader = null, oppgittYrkesskadeISøknad = true)

            val nytt = repository.hentHvisEksisterer(behandling.id)!!
            assertThat(nytt.id).isNotEqualTo(førsteId)
            assertThat(nytt.oppgittYrkesskadeISøknad).isTrue()
            assertThat(grunnlagRader(connection, behandling.id)).containsExactly(førsteId to false, nytt.id to true)
        }
    }

    @Test
    fun `yrkesskader som erstattes av tom liste lager nytt grunnlag uten yrkesskader`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = YrkesskadeRepositoryImpl(connection)

            repository.lagre(behandling.id, Yrkesskader(listOf(yrkesskadeForLagring())), oppgittYrkesskadeISøknad = false)
            val førsteId = repository.hentHvisEksisterer(behandling.id)!!.id

            repository.lagre(behandling.id, Yrkesskader(emptyList()), oppgittYrkesskadeISøknad = false)

            val nytt = repository.hentHvisEksisterer(behandling.id)!!
            assertThat(nytt.id).isNotEqualTo(førsteId)
            assertThat(nytt.yrkesskader.yrkesskader).isEmpty()
            assertThat(grunnlagRader(connection, behandling.id)).containsExactly(førsteId to false, nytt.id to true)
        }
    }

    @Test
    fun `tom liste som erstattes av yrkesskader lager nytt grunnlag med yrkesskader`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = YrkesskadeRepositoryImpl(connection)

            repository.lagre(behandling.id, Yrkesskader(emptyList()), oppgittYrkesskadeISøknad = false)
            val førsteId = repository.hentHvisEksisterer(behandling.id)!!.id

            repository.lagre(behandling.id, Yrkesskader(listOf(yrkesskadeForLagring())), oppgittYrkesskadeISøknad = false)

            val nytt = repository.hentHvisEksisterer(behandling.id)!!
            assertThat(nytt.id).isNotEqualTo(førsteId)
            assertThat(nytt.yrkesskader.yrkesskader).hasSize(1)
            assertThat(nytt.yrkesskader.yrkesskader.single().skadedato).isEqualTo(LocalDate.of(2019, 6, 4))
        }
    }

    @Test
    fun `endret skadedato lager nytt grunnlag`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = YrkesskadeRepositoryImpl(connection)

            repository.lagre(behandling.id, Yrkesskader(listOf(yrkesskadeForLagring())), oppgittYrkesskadeISøknad = false)
            val førsteId = repository.hentHvisEksisterer(behandling.id)!!.id

            repository.lagre(
                behandling.id,
                Yrkesskader(listOf(yrkesskadeForLagring(LocalDate.of(2019, 5, 4)))),
                oppgittYrkesskadeISøknad = false
            )

            val nytt = repository.hentHvisEksisterer(behandling.id)!!
            assertThat(nytt.id).isNotEqualTo(førsteId)
            assertThat(nytt.yrkesskader.yrkesskader.single().skadedato).isEqualTo(LocalDate.of(2019, 5, 4))
        }
    }
}
