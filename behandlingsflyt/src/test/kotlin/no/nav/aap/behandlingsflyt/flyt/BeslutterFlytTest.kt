package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeslutterFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `to-trinn og ingen endring i gruppe etter sendt tilbake fra beslutter`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()
        // Sender inn en søknad
        val (_, behandling) = sendInnFørsteSøknad(
            periode = periode,
            person = person,
            søknad = TestSøknader.SØKNAD_STUDENT
        )
        behandling.medKontekst {
            assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
            assertThat(åpneAvklaringsbehov).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsAvklaringsBehov(
                AvklarStudentLøsning(
                    studentvurdering = StudentVurderingDTO(
                        begrunnelse = "Er student",
                        avbruttStudieDato = LocalDate.now(),
                        avbruddMerEnn6Måneder = true,
                        harBehovForBehandling = true,
                        harAvbruttStudie = true,
                        avbruttPgaSykdomEllerSkade = true,
                        godkjentStudieAvLånekassen = false,
                    )
                ),
            ).løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                            dokumenterBruktIVurdering = listOf(JournalpostId("12312983")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = periode.fom,
                            tom = null
                        )
                    )
                )
            ).løsBistand(periode.fom)

            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()

            .medKontekst {
                // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(åpneAvklaringsbehov).anySatisfy { behov -> assertThat(behov.definisjon == Definisjon.KVALITETSSIKRING).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = LocalDate.now(),
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
            )
            .løsForutgåendeMedlemskap(periode.fom)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .beslutterGodkjennerIkke(returVed = Definisjon.AVKLAR_SYKDOM)
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
            }.løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123190923")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = periode.fom,
                            tom = null
                        )
                    )
                ),
                ingenEndringIGruppe = true,
                bruker = Bruker("SAKSBEHANDLER")
            )
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = LocalDate.now(),
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
                Bruker("SAKSBEHANDLER")
            ).løsForutgåendeMedlemskap(periode.fom)
            .løsOppholdskrav(periode.fom)
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
                // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { behov -> assertThat(behov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                //Henter vurder alder-vilkår
                //Assert utfall
                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

                assertThat(aldersvilkår.vilkårsperioder())
                    .hasSize(2)
                    .anyMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
                    .anyMatch { vilkårsperiode -> !vilkårsperiode.erOppfylt() }

                val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

                assertThat(sykdomsvilkåret.vilkårsperioder())
                    .hasSize(1)
                    .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
            }
    }

    @Test
    fun `Når beslutter ikke godkjenner vurdering av samordning skal flyt tilbakeføres`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(periode = periode)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = behandling.løsAvklaringsBehov(
            AvklarSykdomLøsning(
                løsningerForPerioder = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                        dokumenterBruktIVurdering = listOf(JournalpostId("1231o9024")),
                        harSkadeSykdomEllerLyte = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                        erArbeidsevnenNedsatt = true,
                        yrkesskadeBegrunnelse = null,
                        fom = periode.fom,
                        tom = null
                    )
                )
            )
        ).løsBistand(periode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAvklaringsBehov(
                AvklarSamordningGraderingLøsning(
                    VurderingerForSamordning(
                        begrunnelse = "Sykepengervurdering",
                        maksDatoEndelig = true,
                        fristNyRevurdering = null,
                        vurderteSamordningerData = listOf(
                            SamordningVurderingData(
                                ytelseType = Ytelse.SYKEPENGER,
                                periode = Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(5),
                                ),
                                gradering = 50,
                                manuell = true
                            )
                        ),
                    )
                )
            )
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(returVed = Definisjon.AVKLAR_SAMORDNING_GRADERING)
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
                // Avklar samordning gradering gjenåpnes, behandlingen står i samordning-steget
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SAMORDNING_GRADERING) }
                assertThat(this.behandling.aktivtSteg()).isEqualTo(StegType.SAMORDNING_GRADERING)
            }
    }

}