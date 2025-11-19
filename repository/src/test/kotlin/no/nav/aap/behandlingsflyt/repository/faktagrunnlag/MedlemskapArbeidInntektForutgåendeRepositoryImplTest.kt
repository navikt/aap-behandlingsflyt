package no.nav.aap.behandlingsflyt.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektInformasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Inntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.medlemsskap.MedlemskapRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class MedlemskapArbeidInntektForutgåendeRepositoryImplTest {
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
    fun mapperOrgnavnKorrektTilForutgåendeInntekt() {
        dataSource.transaction { connection ->
            val personOgSakService = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val medlemskapArbeidInntektForutgåendeRepo = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)

            val sak =
                personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling =
                behandlingRepo.opprettBehandling(
                    sak.id,
                    TypeBehandling.Førstegangsbehandling,
                    null,
                    VurderingsbehovOgÅrsak(
                        listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                        ÅrsakTilOpprettelse.SØKNAD
                    )
                )
            lagNyFullVurdering(behandling.id, medlemskapArbeidInntektForutgåendeRepo, "Første begrunnelse", connection)

            val lagretInntekt = medlemskapArbeidInntektForutgåendeRepo.hentHvisEksisterer(behandling.id)!!

            val inntekt1 = lagretInntekt.inntekterINorgeGrunnlag.first { it.identifikator == "1234" }
            val inntekt2 = lagretInntekt.inntekterINorgeGrunnlag.first { it.identifikator == "4321" }

            assertEquals(inntekt1.organisasjonsNavn, "Bepis AS")
            assertEquals(inntekt1.identifikator, "1234")
            assertEquals(inntekt2.organisasjonsNavn, "Rotte AS")
            assertEquals(inntekt2.identifikator, "4321")
        }
    }

    @Test
    fun `kan hente siste relevante utenlandsopplysning`() {
        val sak = dataSource.transaction { connection ->
            val personOgSakService =
                PersonOgSakService(
                    FakePdlGateway,
                    PersonRepositoryImpl(connection),
                    SakRepositoryImpl(connection)
                )
            personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        }

        val sak2 = dataSource.transaction { connection ->
            val personOgSakService =
                PersonOgSakService(
                    FakePdlGateway,
                    PersonRepositoryImpl(connection),
                    SakRepositoryImpl(connection)
                )
            personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        }

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val arbeidInntektRepo = MedlemskapArbeidInntektRepositoryImpl(connection)

            val førstegangsBehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Førstegangsbehandling, sak.id, null, emptyList(), null)
            val revurdering = opprettBehandlingMedVurdering(
                TypeBehandling.Revurdering,
                sak.id,
                førstegangsBehandling.id,
                emptyList(),
                UtenlandsOppholdData(
                    harBoddINorgeSiste5År = true,
                    harArbeidetINorgeSiste5År = false,
                    arbeidetUtenforNorgeFørSykdom = false,
                    iTilleggArbeidUtenforNorge = false,
                    utenlandsOpphold = null
                )
            )
            opprettBehandlingMedVurdering(
                TypeBehandling.Revurdering,
                sak.id,
                revurdering.id,
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_MEDLEMSKAP)),
                null
            )

            // Random ny behandling uten kobling
            behandlingRepo.opprettBehandling(
                sak2.id,
                TypeBehandling.Førstegangsbehandling,
                null,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD
                )
            )

            val sisteUtenlandsOppholdData =
                arbeidInntektRepo.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sak.id)
            assertEquals(sisteUtenlandsOppholdData?.harBoddINorgeSiste5År, true)
        }
    }

    @Test
    fun `henter relaterte historiske vurderinger`() {
        val sak = dataSource.transaction { connection ->
            val personOgSakService =
                PersonOgSakService(
                    FakePdlGateway,
                    PersonRepositoryImpl(connection),
                    SakRepositoryImpl(connection)
                )
            personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        }

        val sak2 = dataSource.transaction { connection ->
            val personOgSakService =
                PersonOgSakService(
                    FakePdlGateway,
                    PersonRepositoryImpl(connection),
                    SakRepositoryImpl(connection)
                )
            personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        }

        dataSource.transaction { connection ->
            val forutgåendeRepo = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)

            val førstegangsBehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Førstegangsbehandling, sak.id, null, emptyList(), null)
            val revurdering = opprettBehandlingMedVurdering(
                TypeBehandling.Revurdering,
                sak.id,
                førstegangsBehandling.id,
                emptyList(),
                null
            )

            val historikk = forutgåendeRepo.hentHistoriskeVurderinger(sak.id, revurdering.id)
            assertEquals(1, historikk.size)
            opprettBehandlingMedVurdering(TypeBehandling.Førstegangsbehandling, sak2.id, null, emptyList(), null)

            val nyHistorikk = forutgåendeRepo.hentHistoriskeVurderinger(sak.id, revurdering.id)
            assertEquals(1, nyHistorikk.size)
        }
    }

    @Test
    fun `skal lagre manuelle vurderinger`() {
        dataSource.transaction { connection ->
            val personOgSakService = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val medlemskapArbeidInntektForutgåendeRepo = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)

            val sak =
                personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling =
                behandlingRepo.opprettBehandling(
                    sak.id,
                    TypeBehandling.Førstegangsbehandling,
                    null,
                    VurderingsbehovOgÅrsak(
                        listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                        ÅrsakTilOpprettelse.SØKNAD
                    )
                )

            medlemskapArbeidInntektForutgåendeRepo.lagreArbeidsforholdOgInntektINorge(
                behandling.id, emptyList(),
                listOf(
                    ArbeidsInntektMaaned(
                        aarMaaned = YearMonth.now(),
                        arbeidsInntektInformasjon = ArbeidsInntektInformasjon(
                            listOf(
                                Inntekt(
                                    beloep = 1.0,
                                    opptjeningsland = null,
                                    skattemessigBosattLand = null,
                                    opptjeningsperiodeFom = null,
                                    opptjeningsperiodeTom = null,
                                    virksomhet = Virksomhet(
                                        identifikator = "1234"
                                    ),
                                    beskrivelse = null
                                ),
                                Inntekt(
                                    beloep = 1.0,
                                    opptjeningsland = null,
                                    skattemessigBosattLand = null,
                                    opptjeningsperiodeFom = null,
                                    opptjeningsperiodeTom = null,
                                    virksomhet = Virksomhet(
                                        identifikator = "4321"
                                    ),
                                    beskrivelse = null
                                ),
                            )
                        )
                    ),
                ),
                null,
                enhetGrunnlag = listOf(
                    EnhetGrunnlag("1234", "Bepis AS"),
                    EnhetGrunnlag("4321", "Rotte AS")
                )
            )

            medlemskapArbeidInntektForutgåendeRepo.lagreVurderinger(
                behandlingId = behandling.id,
                vurderinger = listOf(
                    ManuellVurderingForForutgåendeMedlemskap(
                        begrunnelse = "begrunnelse",
                        harForutgåendeMedlemskap = false,
                        varMedlemMedNedsattArbeidsevne = false,
                        medlemMedUnntakAvMaksFemAar = false,
                        vurdertAv = "NavIdent",
                        vurdertTidspunkt = LocalDateTime.now(),
                        vurdertIBehandling = behandling.id,
                        fom = 1 mai 2025,
                        tom = 31 oktober 2025,
                    ),
                    ManuellVurderingForForutgåendeMedlemskap(
                        begrunnelse = "begrunnelse",
                        harForutgåendeMedlemskap = true,
                        varMedlemMedNedsattArbeidsevne = false,
                        medlemMedUnntakAvMaksFemAar = false,
                        vurdertAv = "NavIdent",
                        vurdertTidspunkt = LocalDateTime.now(),
                        vurdertIBehandling = behandling.id,
                        fom = 1 november 2025
                    )
                )
            )

            val lagretGrunnlag =
                medlemskapArbeidInntektForutgåendeRepo.hentHvisEksisterer(behandling.id)

            assertThat(lagretGrunnlag?.vurderinger).hasSize(2)
        }
    }

    @Test
    fun `verifiserer at migrering legger på kobling mot vurderinger og at uthenting gir periodisert vurdering`() {
        // Oppretter en førstegangsbehandling med to manuelle vurderinger - gjør også en oppdatering av grunnlaget underveis
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val behandling = dataSource.transaction { connection ->
            val medlemskapArbeidInntektForutgåendeRepository = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)
            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val medlemskapRepository = MedlemskapForutgåendeRepositoryImpl(connection)

            medlemskapArbeidInntektForutgåendeRepository.lagreManuellVurdering(
                behandlingId = behandling.id,
                manuellVurdering = manuellVurderingIkkePeriodisert("begrunnelse")
            )

            val medlId = medlemskapRepository.lagreUnntakMedlemskap(
                behandlingId = behandling.id,
                unntak = listOf(medlemskapData())
            )

            medlemskapArbeidInntektForutgåendeRepository.lagreArbeidsforholdOgInntektINorge(
                behandlingId = behandling.id,
                arbeidGrunnlag = arbeidGrunnlag(),
                inntektGrunnlag = inntektGrunnlag(),
                medlId = medlId,
                enhetGrunnlag = enhetGrunnlags()
            )

            medlemskapArbeidInntektForutgåendeRepository.lagreManuellVurdering(
                behandlingId = behandling.id,
                manuellVurdering = manuellVurderingIkkePeriodisert("begrunnelse2")
            )

            behandling
        }

        // Oppretter en revurdering og kopierer grunnlaget for å sjekke at dette blir riktig etter migrering av revurdering
        val revurdering = dataSource.transaction { connection ->
            val medlemskapArbeidInntektForutgåendeRepository = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)
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

            medlemskapArbeidInntektForutgåendeRepository.kopier(behandling.id, revurdering.id)

            revurdering
        }

        dataSource.transaction { connection ->
            val medlemskapArbeidInntektForutgåendeRepository = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)

            sjekkBehandlingFørMigrering(medlemskapArbeidInntektForutgåendeRepository, behandling)
            sjekkBehandlingFørMigrering(medlemskapArbeidInntektForutgåendeRepository, revurdering)

            val antallVurderingerKoblingerFørMigrering = hentVurderingerId(connection)

            // Kjører migrering
            medlemskapArbeidInntektForutgåendeRepository.migrerManuelleVurderingerPeriodisert()

            sjekkBehandlingEtterMigrering(medlemskapArbeidInntektForutgåendeRepository, behandling, null, periode)
            sjekkBehandlingEtterMigrering(medlemskapArbeidInntektForutgåendeRepository, revurdering, behandling, periode)

            // Sjekker at det finnes riktig antall grunnlag
            assertThat(hentGrunnlag(connection, behandling.id)).hasSize(3) // 2 inaktive + 1 aktiv
            assertThat(hentGrunnlag(connection, revurdering.id)).hasSize(1) // 1 aktiv

            // Sjekker at innslag i lovvalg_medlemskap_manuell_vurderinger finnes og at det er to stk en for hver vurdering
            assertThat(hentVurderingerId(connection) - antallVurderingerKoblingerFørMigrering).hasSize(2)
        }
    }

    private fun sjekkBehandlingFørMigrering(
        medlemskapArbeidInntektForutgåendeRepository: MedlemskapArbeidInntektForutgåendeRepositoryImpl,
        behandling: Behandling,
    ) {
        val grunnlag = medlemskapArbeidInntektForutgåendeRepository.hentHvisEksisterer(behandling.id)

        // Sjekker at verdier før migrering er som forventet
        assertThat(grunnlag?.manuellVurdering).isNotNull
        assertThat(grunnlag?.manuellVurdering?.fom).isNull()
        assertThat(grunnlag?.manuellVurdering?.vurdertIBehandling).isNull()
       // assertThat(grunnlag?.vurderinger).isEmpty()
    }

    private fun sjekkBehandlingEtterMigrering(
        medlemskapArbeidInntektForutgåendeRepository: MedlemskapArbeidInntektForutgåendeRepositoryImpl,
        behandling: Behandling,
        forrigeBehandling: Behandling? = null,
        periode: Periode,
    ) {
        val grunnlag = medlemskapArbeidInntektForutgåendeRepository.hentHvisEksisterer(behandling.id)

        // Sjekker at fom-dato og vurdertIBehandling er satt korrekt etter migrering og at vurderinger returnerer vurdering
        val periodisertVurdering = grunnlag?.vurderinger?.first()
        assertThat(periodisertVurdering?.fom).isEqualTo(periode.fom)
        assertThat(periodisertVurdering?.vurdertIBehandling).isEqualTo(forrigeBehandling?.id ?: behandling.id)
        assertThat(grunnlag?.manuellVurdering).isEqualTo(periodisertVurdering)
    }

    private fun hentVurderingerId(connection: DBConnection): List<Long> {
        val query = "SELECT id FROM forutgaaende_medlemskap_manuell_vurderinger"
        val id = connection.queryList<Long>(query) {
            setRowMapper { it.getLong("id") }
        }
        return id
    }

    private fun hentGrunnlag(connection: DBConnection, behandlingId: BehandlingId): List<Long> {
        val query = "SELECT * FROM forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag WHERE behandling_id = ?"
        val id = connection.queryList(query) {
            setRowMapper { it.getLong("id") }
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
        return id
    }

    private fun manuellVurderingIkkePeriodisert(begrunnelse: String = "begrunnelse"): ManuellVurderingForForutgåendeMedlemskap =
        ManuellVurderingForForutgåendeMedlemskap(
            begrunnelse = begrunnelse,
            harForutgåendeMedlemskap = true,
            varMedlemMedNedsattArbeidsevne = false,
            medlemMedUnntakAvMaksFemAar = false,
            vurdertAv = "NavIdent",
            vurdertTidspunkt = LocalDateTime.now(),
        )

    private fun utenlandsOppholdData(): UtenlandsOppholdData = UtenlandsOppholdData(
        harBoddINorgeSiste5År = true,
        harArbeidetINorgeSiste5År = false,
        arbeidetUtenforNorgeFørSykdom = false,
        iTilleggArbeidUtenforNorge = false,
        utenlandsOpphold = null
    )

    private fun arbeidGrunnlag(): List<ArbeidINorgeGrunnlag> = listOf(
        ArbeidINorgeGrunnlag(
            identifikator = "1234",
            arbeidsforholdKode = "ordinaertArbeidsforhold",
            startdato = 1 mai 2020,
            sluttdato = null
        )
    )

    private fun inntektGrunnlag(): List<ArbeidsInntektMaaned> = listOf(
        ArbeidsInntektMaaned(
            aarMaaned = YearMonth.now(),
            arbeidsInntektInformasjon = ArbeidsInntektInformasjon(
                listOf(
                    Inntekt(
                        beloep = 1.0,
                        opptjeningsland = null,
                        skattemessigBosattLand = null,
                        opptjeningsperiodeFom = null,
                        opptjeningsperiodeTom = null,
                        virksomhet = Virksomhet(
                            identifikator = "1234"
                        ),
                        beskrivelse = null
                    ),
                    Inntekt(
                        beloep = 1.0,
                        opptjeningsland = null,
                        skattemessigBosattLand = null,
                        opptjeningsperiodeFom = null,
                        opptjeningsperiodeTom = null,
                        virksomhet = Virksomhet(
                            identifikator = "4321"
                        ),
                        beskrivelse = null
                    ),
                )
            )
        ),
    )

    private fun enhetGrunnlags(): List<EnhetGrunnlag> = listOf(
        EnhetGrunnlag("1234", "Bepis AS"),
        EnhetGrunnlag("4321", "Rotte AS")
    )

    private fun medlemskapData(): MedlemskapDataIntern = MedlemskapDataIntern(
        unntakId = 1234,
        ident = "13028911111",
        fraOgMed = "1989-02-13",
        tilOgMed = "1999-02-14",
        status = "GYLD",
        statusaarsak = null,
        medlem = true,
        grunnlag = "FLK-TRGD",
        lovvalg = "FLK_TRGD",
        helsedel = true,
        lovvalgsland = "NOR",
        kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
    )

    private fun opprettBehandlingMedVurdering(
        typeBehandling: TypeBehandling,
        sakId: SakId,
        forrigeBehandlingId: BehandlingId?,
        årsaker: List<VurderingsbehovMedPeriode>,
        utenlandsOppholdData: UtenlandsOppholdData?
    )
            : Behandling {
        return dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val forutgåendeRepo = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)
            val behandling = behandlingRepo.opprettBehandling(
                sakId,
                typeBehandling,
                forrigeBehandlingId,
                VurderingsbehovOgÅrsak(årsaker, ÅrsakTilOpprettelse.SØKNAD)
            )
            lagNyFullVurdering(behandling.id, forutgåendeRepo, "Heftig vurdering", connection, utenlandsOppholdData)
            behandling
        }
    }

    private fun lagNyFullVurdering(
        behandlingId: BehandlingId,
        forutgåendeRepository: MedlemskapArbeidInntektForutgåendeRepositoryImpl,
        begrunnelse: String,
        connection: DBConnection,
        utenlandsOppholdData: UtenlandsOppholdData? = UtenlandsOppholdData(
            harBoddINorgeSiste5År = false,
            harArbeidetINorgeSiste5År = false,
            arbeidetUtenforNorgeFørSykdom = false,
            iTilleggArbeidUtenforNorge = false,
            utenlandsOpphold = null
        )
    ) {
        val lovvalgRepository = MedlemskapArbeidInntektRepositoryImpl(connection)
        if (utenlandsOppholdData != null) lovvalgRepository.lagreOppgittUtenlandsOppplysninger(
            behandlingId,
            JournalpostId("1"),
            utenlandsOppholdData
        )
        forutgåendeRepository.lagreArbeidsforholdOgInntektINorge(
            behandlingId, emptyList(),
            listOf(
                ArbeidsInntektMaaned(
                    aarMaaned = YearMonth.now(),
                    arbeidsInntektInformasjon = ArbeidsInntektInformasjon(
                        listOf(
                            Inntekt(
                                beloep = 1.0,
                                opptjeningsland = null,
                                skattemessigBosattLand = null,
                                opptjeningsperiodeFom = null,
                                opptjeningsperiodeTom = null,
                                virksomhet = Virksomhet(
                                    identifikator = "1234"
                                ),
                                beskrivelse = null
                            ),
                            Inntekt(
                                beloep = 1.0,
                                opptjeningsland = null,
                                skattemessigBosattLand = null,
                                opptjeningsperiodeFom = null,
                                opptjeningsperiodeTom = null,
                                virksomhet = Virksomhet(
                                    identifikator = "4321"
                                ),
                                beskrivelse = null
                            ),
                        )
                    )
                ),
            ),
            null,
            enhetGrunnlag = listOf(
                EnhetGrunnlag("1234", "Bepis AS"),
                EnhetGrunnlag("4321", "Rotte AS")
            )
        )
        forutgåendeRepository.lagreVurderinger(
            behandlingId = behandlingId,
            vurderinger = listOf(
                ManuellVurderingForForutgåendeMedlemskap(
                    begrunnelse = begrunnelse,
                    harForutgåendeMedlemskap = true,
                    varMedlemMedNedsattArbeidsevne = false,
                    medlemMedUnntakAvMaksFemAar = false,
                    vurdertAv = "NavIdent",
                    vurdertTidspunkt = LocalDateTime.now()
                )
            )
        )
    }
}