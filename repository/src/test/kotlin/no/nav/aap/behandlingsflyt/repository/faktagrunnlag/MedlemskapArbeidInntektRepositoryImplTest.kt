package no.nav.aap.behandlingsflyt.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektInformasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Inntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.medlemsskap.MedlemskapRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class MedlemskapArbeidInntektRepositoryImplTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        @AfterAll
        @JvmStatic
        fun afterAll() {
            InitTestDatabase.closerFor(dataSource)
        }
    }

    @Test
    fun mapperOrgnavnKorrektTilInntekt() {
        dataSource.transaction { connection ->
            val medlemskapArbeidInntektRepository = MedlemskapArbeidInntektRepositoryImpl(connection)
            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            lagNyFullVurdering(behandling.id, medlemskapArbeidInntektRepository, "Første begrunnelse")

            val lagretInntekt = medlemskapArbeidInntektRepository.hentHvisEksisterer(behandling.id)!!

            val inntekt1 = lagretInntekt.inntekterINorgeGrunnlag.first { it.identifikator == "1234" }
            val inntekt2 = lagretInntekt.inntekterINorgeGrunnlag.first { it.identifikator == "4321" }

            assertEquals(inntekt1.organisasjonsNavn, "Bepis AS")
            assertEquals(inntekt1.identifikator, "1234")
            assertEquals(inntekt2.organisasjonsNavn, "Rotte AS")
            assertEquals(inntekt2.identifikator, "4321")
        }
    }

    @Test
    fun henterRelaterteHistoriskeVurderinger() {
        // Førstegangsbehandling
        val behandling = dataSource.transaction { connection ->
            val repo = MedlemskapArbeidInntektRepositoryImpl(connection)
            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            lagNyFullVurdering(behandling.id, repo, "Første begrunnelse")

            val historikk = repo.hentHistoriskeVurderinger(sak.id, behandling.id)
            assertEquals(0, historikk.size)

            behandling
        }

        // Revurdering
        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val repo = MedlemskapArbeidInntektRepositoryImpl(connection)

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

            val historikk = repo.hentHistoriskeVurderinger(revurdering.sakId, revurdering.id)
            lagNyFullVurdering(revurdering.id, repo, "Andre begrunnelse")
            assertThat(historikk).hasSize(1)
        }
    }

    @Test
    fun `skal kunne lagre og hente manuell vurdering (gammel variant, ikke periodisert)`() {
        dataSource.transaction { connection ->
            val medlemskapArbeidInntektRepository = MedlemskapArbeidInntektRepositoryImpl(connection)
            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            medlemskapArbeidInntektRepository.lagreManuellVurdering(
                behandlingId = behandling.id,
                manuellVurdering = manuellVurderingIkkePeriodisert("begrunnelse")
            )

            val medlemskapArbeidInntektGrunnlag = medlemskapArbeidInntektRepository.hentHvisEksisterer(behandling.id)

            assertThat(medlemskapArbeidInntektGrunnlag?.manuellVurdering).isNotNull
        }
    }

    @Test
    fun `skal kunne lagre og hente manuelle vurderinger over flere perioder`() {
        dataSource.transaction { connection ->
            val medlemskapArbeidInntektRepository = MedlemskapArbeidInntektRepositoryImpl(connection)
            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            medlemskapArbeidInntektRepository.lagreVurderinger(
                behandling.id,
                listOf(
                    manuellVurdering(
                        fom = 1 mai 2025,
                        tom = 31 oktober 2025,
                        vurdertIBehandling = behandling.id
                    ),
                    manuellVurdering(
                        fom = 1 november 2025,
                        tom = null,
                        vurdertIBehandling = behandling.id
                    ),
                )
            )

            val medlemskapArbeidInntektGrunnlag = medlemskapArbeidInntektRepository.hentHvisEksisterer(behandling.id)

            assertThat(medlemskapArbeidInntektGrunnlag?.vurderinger?.size).isEqualTo(2)
        }
    }

    @Test
    fun `skal kopiere grunnlag fra forrige behandling ved revurdering og ta med tidligere vurderinger når nye opprettes`() {
        val behandling = dataSource.transaction { connection ->
            val medlemskapArbeidInntektRepository = MedlemskapArbeidInntektRepositoryImpl(connection)
            val medlemskapRepository = MedlemskapRepositoryImpl(connection)

            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val vurderinger = listOf(
                manuellVurdering(
                    fom = 1 mai 2025,
                    tom = 31 oktober 2025,
                    vurdertIBehandling = behandling.id
                ),
                manuellVurdering(
                    fom = 1 november 2025,
                    tom = null,
                    vurdertIBehandling = behandling.id
                ),
            )

            val medlId = medlemskapRepository.lagreUnntakMedlemskap(
                behandlingId = behandling.id,
                unntak = listOf(medlemskapData())
            )

            medlemskapArbeidInntektRepository.lagreArbeidsforholdOgInntektINorge(
                behandlingId = behandling.id,
                arbeidGrunnlag = arbeidGrunnlag(),
                inntektGrunnlag = inntektGrunnlag(),
                medlId = medlId,
                enhetGrunnlag = enhetGrunnlags()
            )

            medlemskapArbeidInntektRepository.lagreOppgittUtenlandsOppplysninger(
                behandling.id,
                JournalpostId("1"),
                utenlandsOppholdData()
            )

            medlemskapArbeidInntektRepository.lagreVurderinger(
                behandling.id,
                vurderinger
            )

            behandling
        }

        // Revurdering
        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val medlemskapArbeidInntektRepository = MedlemskapArbeidInntektRepositoryImpl(connection)

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

            medlemskapArbeidInntektRepository.kopier(behandling.id, revurdering.id)

            val eksisterendeVurderinger = medlemskapArbeidInntektRepository.hentHvisEksisterer(revurdering.id)
            val vurderinger = listOf(
                manuellVurdering(
                    fom = 15 desember  2025,
                    tom = null,
                    vurdertIBehandling = revurdering.id
                )
            ) + (eksisterendeVurderinger?.vurderinger ?: emptyList())

            medlemskapArbeidInntektRepository.lagreVurderinger(revurdering.id, vurderinger)

            val medlemskapArbeidInntektGrunnlag = medlemskapArbeidInntektRepository.hentHvisEksisterer(revurdering.id)

            assertThat(medlemskapArbeidInntektGrunnlag?.inntekterINorgeGrunnlag?.size).isEqualTo(2)
            assertThat(medlemskapArbeidInntektGrunnlag?.arbeiderINorgeGrunnlag?.size).isEqualTo(1)
            assertThat(medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag).isNotNull
            assertThat(medlemskapArbeidInntektGrunnlag?.vurderinger?.size).isEqualTo(3)
        }
    }

    private fun lagNyFullVurdering(
        behandlingId: BehandlingId,
        repo: MedlemskapArbeidInntektRepositoryImpl,
        begrunnelse: String
    ) {
        repo.lagreArbeidsforholdOgInntektINorge(
            behandlingId = behandlingId,
            arbeidGrunnlag = emptyList(),
            inntektGrunnlag = inntektGrunnlag(),
            medlId = null,
            enhetGrunnlag = enhetGrunnlags()
        )

        repo.lagreManuellVurdering(
            behandlingId,
            manuellVurderingIkkePeriodisert(begrunnelse)
        )

        repo.lagreOppgittUtenlandsOppplysninger(
            behandlingId,
            JournalpostId("1"),
            utenlandsOppholdData()
        )
    }

    private fun manuellVurderingIkkePeriodisert(begrunnelse: String): ManuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskap(
        LovvalgVedSøknadsTidspunktDto(begrunnelse, EØSLand.NOR),
        MedlemskapVedSøknadsTidspunktDto(begrunnelse, true),
        "SAKSBEHANDLER",
        LocalDateTime.now()
    )

    private fun manuellVurdering(fom: LocalDate, tom: LocalDate?, vurdertIBehandling: BehandlingId? = null): ManuellVurderingForLovvalgMedlemskap =
        ManuellVurderingForLovvalgMedlemskap(
            fom = fom,
            tom = tom,
            lovvalgVedSøknadsTidspunkt = LovvalgVedSøknadsTidspunktDto("begrunnelse", EØSLand.NOR),
            medlemskapVedSøknadsTidspunkt = MedlemskapVedSøknadsTidspunktDto("begrunnelse", true),
            vurdertAv = "SAKSBEHANDLER",
            vurdertDato = LocalDateTime.now(),
            vurdertIBehandling = vurdertIBehandling
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
}