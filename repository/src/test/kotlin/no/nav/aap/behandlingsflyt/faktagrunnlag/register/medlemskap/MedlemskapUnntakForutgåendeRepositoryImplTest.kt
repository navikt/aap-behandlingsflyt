package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Arbeidsforholdtype
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektInformasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMåned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Inntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.forutgåendemedlemskap.ForutgåendeMedlemskapRepositoryImpl
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
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Tag("db")
class MedlemskapUnntakForutgåendeRepositoryImplTest {
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
    fun `skal ta med manuelle vurderinger i grunnlag når arbeid og inntekt oppdateres`() {
        dataSource.transaction { connection ->
            val medlemskapArbeidInntektForutgåendeRepo = ForutgåendeMedlemskapRepositoryImpl(connection)
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val medlemskapRepository = MedlemskapUnntakForutgåendeRepositoryImpl(connection)

            medlemskapArbeidInntektForutgåendeRepo.lagreVurderinger(
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

            val medlemskapArbeidInntektGrunnlag =
                medlemskapArbeidInntektForutgåendeRepo.hentHvisEksisterer(behandling.id)

            assertThat(medlemskapArbeidInntektGrunnlag?.vurderinger?.size).isEqualTo(2)

            val medlId = medlemskapRepository.lagreUnntakMedlemskap(
                behandlingId = behandling.id,
                unntak = listOf(medlemskapData())
            )

            medlemskapArbeidInntektForutgåendeRepo.lagreArbeidsforholdOgInntektINorge(
                behandlingId = behandling.id,
                arbeidGrunnlag = arbeidGrunnlag(),
                inntektGrunnlag = inntektGrunnlag(),
                medlId = medlId,
                enhetGrunnlag = enhetGrunnlags()
            )

            val medlemskapArbeidInntektGrunnlagOppdatert =
                medlemskapArbeidInntektForutgåendeRepo.hentHvisEksisterer(behandling.id)

            assertThat(medlemskapArbeidInntektGrunnlagOppdatert?.vurderinger?.size).isEqualTo(2)
        }
    }

    @Test
    fun `skal kopiere grunnlag fra forrige behandling ved revurdering og ta med tidligere vurderinger når nye opprettes`() {
        val behandling = dataSource.transaction { connection ->
            val medlemskapArbeidInntektForutgåendeRepo = ForutgåendeMedlemskapRepositoryImpl(connection)
            val medlemskapRepository = MedlemskapUnntakForutgåendeRepositoryImpl(connection)

            val sak = opprettSak(connection, LocalDate.now())
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

            medlemskapArbeidInntektForutgåendeRepo.lagreArbeidsforholdOgInntektINorge(
                behandlingId = behandling.id,
                arbeidGrunnlag = arbeidGrunnlag(),
                inntektGrunnlag = inntektGrunnlag(),
                medlId = medlId,
                enhetGrunnlag = enhetGrunnlags()
            )

            medlemskapArbeidInntektForutgåendeRepo.lagreVurderinger(
                behandling.id,
                vurderinger
            )

            behandling
        }

        // Revurdering
        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val medlemskapArbeidInntektForutgåendeRepo = ForutgåendeMedlemskapRepositoryImpl(connection)

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

            medlemskapArbeidInntektForutgåendeRepo.kopier(behandling.id, revurdering.id)

            val eksisterendeVurderinger = medlemskapArbeidInntektForutgåendeRepo.hentHvisEksisterer(revurdering.id)
            val vurderinger = listOf(
                manuellVurdering(
                    fom = 15 desember 2025,
                    tom = null,
                    vurdertIBehandling = revurdering.id
                )
            ) + (eksisterendeVurderinger?.vurderinger ?: emptyList())

            medlemskapArbeidInntektForutgåendeRepo.lagreVurderinger(revurdering.id, vurderinger)

            val medlemskapArbeidInntektGrunnlag =
                medlemskapArbeidInntektForutgåendeRepo.hentHvisEksisterer(revurdering.id)

            assertThat(medlemskapArbeidInntektGrunnlag?.inntekterINorgeGrunnlag?.size).isEqualTo(2)
            assertThat(medlemskapArbeidInntektGrunnlag?.arbeiderINorgeGrunnlag?.size).isEqualTo(1)
            assertThat(medlemskapArbeidInntektGrunnlag?.medlemskapUnntakGrunnlag).isNotNull
            assertThat(medlemskapArbeidInntektGrunnlag?.vurderinger?.size).isEqualTo(3)
        }
    }

    @Test
    fun `test sletting`() {
        dataSource.transaction { connection ->
            val medlemskapArbeidInntektForutgåendeRepo = ForutgåendeMedlemskapRepositoryImpl(connection)
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val medlemskapRepository = MedlemskapUnntakForutgåendeRepositoryImpl(connection)

            medlemskapArbeidInntektForutgåendeRepo.lagreVurderinger(
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

            val medlemskapArbeidInntektGrunnlag =
                medlemskapArbeidInntektForutgåendeRepo.hentHvisEksisterer(behandling.id)

            assertThat(medlemskapArbeidInntektGrunnlag?.vurderinger?.size).isEqualTo(2)

            val medlId = medlemskapRepository.lagreUnntakMedlemskap(
                behandlingId = behandling.id,
                unntak = listOf(medlemskapData())
            )

            medlemskapArbeidInntektForutgåendeRepo.lagreArbeidsforholdOgInntektINorge(
                behandlingId = behandling.id,
                arbeidGrunnlag = arbeidGrunnlag(),
                inntektGrunnlag = inntektGrunnlag(),
                medlId = medlId,
                enhetGrunnlag = enhetGrunnlags()
            )

            val medlemskapArbeidInntektGrunnlagOppdatert =
                medlemskapArbeidInntektForutgåendeRepo.hentHvisEksisterer(behandling.id)

            assertThat(medlemskapArbeidInntektGrunnlagOppdatert?.vurderinger?.size).isEqualTo(2)

            assertDoesNotThrow { medlemskapArbeidInntektForutgåendeRepo.slett(behandling.id) }
        }
    }

    private fun arbeidGrunnlag(): List<ArbeidINorgeGrunnlag> = listOf(
        ArbeidINorgeGrunnlag(
            identifikator = "1234",
            arbeidsforholdKode = Arbeidsforholdtype.ORDINAERT_ARBEIDSFORHOLD,
            startdato = 1 mai 2020,
            sluttdato = null
        )
    )

    private fun inntektGrunnlag(): List<ArbeidsInntektMåned> = listOf(
        ArbeidsInntektMåned(
            årMåned = YearMonth.now(),
            arbeidsInntektInformasjon = ArbeidsInntektInformasjon(
                listOf(
                    Inntekt(
                        beløp = 1.0,
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
                        beløp = 1.0,
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

    private fun manuellVurdering(
        fom: LocalDate,
        tom: LocalDate?,
        vurdertIBehandling: BehandlingId,
        begrunnelse: String = "begrunnelse"
    ): ManuellVurderingForForutgåendeMedlemskap =
        ManuellVurderingForForutgåendeMedlemskap(
            fom = fom,
            tom = tom,
            begrunnelse = begrunnelse,
            harForutgåendeMedlemskap = true,
            varMedlemMedNedsattArbeidsevne = false,
            medlemMedUnntakAvMaksFemAar = false,
            vurdertAv = Bruker("NavIdent"),
            vurdertTidspunkt = LocalDateTime.now(),
            vurdertIBehandling = vurdertIBehandling
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