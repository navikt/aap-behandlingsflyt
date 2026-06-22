package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertOverstyrtForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * TestPerson i denne testen har ingen inntekter for å kunne trigge steget. For om inntekter settes, finner
 * steget inntekt fra A-Inntekt, og da hoppes det over steget.
 */
class ForutgåendeMedlemskapFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {

    @Test
    fun `Trenger ikke manuell vurdering av forutgående medlemskap dersom automatisk vurdering er oppfylt`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.PERSON_MED_FORUTGÅENDE_MEDLEMSKAP())
        val oppdatertBehandling = behandling
            .løsFramTilForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Trenger ikke manuell vurdering for forutgående medlemskap dersom det foreligger yrkesskade med årsakssammenheng`() {
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            søknad = TestSøknader.SØKNAD_YRKESSKADE,
        )

        val oppdatertBehandling = behandling
            .løsFramTilGrunnlag(sak.rettighetsperiode.fom)
            .løsYrkesskadeVurdering(person.yrkesskade, erÅrsakssammenheng = true)
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        val vilkårsresultat = hentVilkårsresultat(behandling.id)
            .finnVilkår(Vilkårtype.MEDLEMSKAP)
            .vilkårsperioder()

        assertThat(vilkårsresultat.first().begrunnelse).isEqualTo(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG.toString())
        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Må manuelt vurdere forutgående medlemskap dersom ikke oppfylt automatisk og yrkesskade uten årsakssammenheng`() {
        val person = TestPersoner.PERSON_MED_YRKESSKADE().medInntekter(emptyList())

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            søknad = TestSøknader.SØKNAD_YRKESSKADE,
        )

        val oppdatertBehandling = behandling
            .løsFramTilGrunnlag(sak.rettighetsperiode.fom)
            .løsYrkesskadeVurdering(person.yrkesskade, erÅrsakssammenheng = false)
            .løsBeregningstidspunkt()
            .løsFastsettManuellInntekt()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).allMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Må manuelt vurdere forutgående medlemskap dersom automatisk vurdering ikke er oppfylt (ikke medlem av medlemsregisteret)`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON().medInntekter(emptyList()))
        val oppdatertBehandling = behandling
            .løsFramTilForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Kan overstyre automatisk vurdering av forutgående medlemskap dersom saksbehandler likevel ønsker annet utfall`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.PERSON_MED_FORUTGÅENDE_MEDLEMSKAP())
        val oppdatertBehandling = behandling
            .løsFramTilForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.MANUELL_OVERSTYRING_MEDLEMSKAP }
            }
            .løsAvklaringsBehov(
                AvklarPeriodisertOverstyrtForutgåendeMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = null,
                            begrunnelse = "overstyrer til ikke oppfylt",
                            harForutgåendeMedlemskap = false,
                            varMedlemMedNedsattArbeidsevne = null,
                            medlemMedUnntakAvMaksFemAar = null
                        )
                    ),
                )
            )
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)

        val vilkårsresultat = hentVilkårsresultat(behandling.id)
            .finnVilkår(Vilkårtype.MEDLEMSKAP)
            .vilkårsperioder()

        assertThat(vilkårsresultat).noneMatch { it.erOppfylt() }
        assertThat(Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE).isEqualTo(vilkårsresultat.first().avslagsårsak)
        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Oppfyller ikke forutgående medlemskap hvis ikke medlem i folketrygden og ingen unntak oppfylles`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON().medInntekter(emptyList()))

        val oppdatertBehandling = behandling
            .løsLovvalg(sak.rettighetsperiode.fom)
            .løsFramTilForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).allMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom, medlem = false)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)

        val vilkårsresultat = hentVilkårsresultat(behandling.id)
            .finnVilkår(Vilkårtype.MEDLEMSKAP)
            .vilkårsperioder()

        assertThat(vilkårsresultat).noneMatch { it.erOppfylt() }
        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Oppfyller forutgående medlemskap hvis ikke medlem i folketrygden, men unntak er oppfylt`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON().medInntekter(emptyList()))
        val oppdatertBehandling = behandling
            .løsFramTilForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactly(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            .løsAvklaringsBehov(
                AvklarPeriodisertForutgåendeMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = null,
                            begrunnelse = "begrunnelse",
                            harForutgåendeMedlemskap = false,
                            varMedlemMedNedsattArbeidsevne = true,
                            medlemMedUnntakAvMaksFemAar = null
                        )
                    )
                )
            )
            .medKontekst {
                assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
            }
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_INNVILGELSE)

        val vilkårsresultat = hentVilkårsresultat(behandling.id)
            .finnVilkår(Vilkårtype.MEDLEMSKAP)
            .vilkårsperioder()

        assertThat(vilkårsresultat).allMatch { it.erOppfylt() }
        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Kan sette forutgående medlemskap til ikke oppfylt i første periode, så oppfylt på senere dato`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON().medInntekter(emptyList()))
        val dato5ÅrForutgåendeMedlemskapErOppfylt = sak.rettighetsperiode.fom.plusMonths(2)

        val oppdatertBehandling = behandling
            .løsFramTilForutgåendeMedlemskap(vurderingerGjelderFra = sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactly(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            .løsAvklaringsBehov(
                AvklarPeriodisertForutgåendeMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = dato5ÅrForutgåendeMedlemskapErOppfylt.minusDays(1),
                            begrunnelse = "oppfyller ikke 5 år forutgående de første to månedene etter søknadstidspunkt",
                            harForutgåendeMedlemskap = false,
                            varMedlemMedNedsattArbeidsevne = null,
                            medlemMedUnntakAvMaksFemAar = null
                        ),
                        PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
                            fom = dato5ÅrForutgåendeMedlemskapErOppfylt,
                            tom = null,
                            begrunnelse = "oppfyller nå vilkåret",
                            harForutgåendeMedlemskap = true,
                            varMedlemMedNedsattArbeidsevne = null,
                            medlemMedUnntakAvMaksFemAar = null
                        )
                    )
                )
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsOppholdskrav(sak.rettighetsperiode.fom) // TODO burde kunne sette datoMedlemskapOppfylt her
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        val vilkårsresultat = hentVilkårsresultat(behandling.id)
            .finnVilkår(Vilkårtype.MEDLEMSKAP)
            .vilkårsperioder()

        assertThat(vilkårsresultat[0].erOppfylt()).isFalse()
        assertThat(vilkårsresultat[1].erOppfylt()).isTrue()

        // Virkningstidspunktet skal settes til datoen hvor medlemskap er oppfylt
        val vedtak = hentVedtak(behandling.id)

        assertThat(vedtak.virkningstidspunkt).isEqualTo(dato5ÅrForutgåendeMedlemskapErOppfylt)
        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Kan revurdere forutgående medlemskap ved å legge inn vurderingsbehov`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON().medInntekter(emptyList()))
        val oppdatertBehandling = behandling
            .løsFramTilForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactly(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)

        val revurdering = sak
            .opprettManuellRevurdering(listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP))
            .medKontekst {
                assertThat(åpneAvklaringsbehov).allMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
            }
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }


    @Test
    fun `forutgående medlemskap går fra oppfylt kun for vurdert periode til oppfylt til Tid MAKS når grunnlaget gir automatisk innvilgelse`() {
        // Person uten inntekt og uten MEDL-vedtak -> forutgående medlemskap kan ikke avgjøres automatisk
        val person = TestPersoner.STANDARD_PERSON().medInntekter(emptyList())
        val (sak, behandling) = sendInnFørsteSøknad(person = person)

        val fom = sak.rettighetsperiode.fom
        val sisteOppfylteDag = fom.plusMonths(2)
        val ikkeOppfyltFra = sisteOppfylteDag.plusDays(1)

        // Saksbehandler frem til forutgående medlemskap (løser bl.a. lovvalg manuelt på veien)
        behandling.løsFramTilForutgåendeMedlemskap(fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactly(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }

        // Fase 1: bruker legger inn to perioder - oppfylt frem til sisteOppfylteDag, ikke oppfylt etter
        val oppdatertBehandling = behandling.løsAvklaringsBehov(
            AvklarPeriodisertForutgåendeMedlemskapLøsning(
                løsningerForPerioder = listOf(
                    PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
                        fom = fom,
                        tom = sisteOppfylteDag,
                        begrunnelse = "oppfyller forutgående medlemskap",
                        harForutgåendeMedlemskap = true,
                        varMedlemMedNedsattArbeidsevne = null,
                        medlemMedUnntakAvMaksFemAar = null
                    ),
                    PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
                        fom = ikkeOppfyltFra,
                        tom = null,
                        begrunnelse = "oppfyller ikke forutgående medlemskap",
                        harForutgåendeMedlemskap = false,
                        varMedlemMedNedsattArbeidsevne = null,
                        medlemMedUnntakAvMaksFemAar = null
                    )
                )
            )
        )

        // Fase 1: forutgående medlemskap er oppfylt KUN for den vurderte perioden (frem til
        // sisteOppfylteDag), og ikke oppfylt etterpå
        val medlemskapFase1 = hentVilkårsresultat(oppdatertBehandling.id)
            .finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertThat(medlemskapFase1.filter { it.erOppfylt() }.maxOf { it.periode.tom }).isEqualTo(sisteOppfylteDag)
        assertThat(medlemskapFase1.any { !it.erOppfylt() }).isTrue()

        // Fase 2: grunnlaget oppdateres - et MEDL-vedtak om sammenhengende medlemskap i Norge
        // siste 5 år kommer inn, slik at forutgående medlemskap nå kan avgjøres automatisk som
        // oppfylt for hele perioden
        FakePersoner.leggTil(
            TestPerson(
                identer = person.identer,
                fødselsdato = person.fødselsdato,
                inntekter = emptyList(),
                medlStatus = listOf(
                    MedlemskapDataIntern(
                        unntakId = 100087727,
                        ident = person.aktivIdent().identifikator,
                        fraOgMed = fom.minusYears(6).toString(),
                        tilOgMed = fom.plusYears(2).toString(),
                        status = "GYLD",
                        statusaarsak = null,
                        medlem = true,
                        grunnlag = "grunnlag",
                        lovvalg = "lovvalg",
                        helsedel = true,
                        lovvalgsland = "NOR",
                        kilde = null
                    )
                )
            )
        )

        nullstillInformasjonskravOppdatert(InformasjonskravNavn.FORUTGÅENDE_MEDLEMSKAP, sak.id)
        val oppdatertBehandlingNyttGrunnlag = prosesserBehandling(oppdatertBehandling)

        // Fase 2: forutgående medlemskap er nå oppfylt for hele perioden, helt til Tid.MAKS
        val medlemskapFase2 = hentVilkårsresultat(oppdatertBehandlingNyttGrunnlag.id)
            .finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertThat(medlemskapFase2).allMatch { it.erOppfylt() }
        assertThat(medlemskapFase2.maxOf { it.periode.tom }).isEqualTo(Tid.MAKS)
    }


}