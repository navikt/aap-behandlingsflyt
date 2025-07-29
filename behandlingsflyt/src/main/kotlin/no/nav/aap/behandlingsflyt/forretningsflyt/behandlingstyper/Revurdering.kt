package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgService
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.SøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.ForutgåendeMedlemskapService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BarnetilleggSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BeregnTilkjentYtelseSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BeregningAvklarFaktaSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.Effektuer11_7Steg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.EtAnnetStedSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettArbeidsevneSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettGrunnlagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettMeldeperiodeSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettSykdomsvilkåretSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FritakMeldepliktSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.IverksettVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KvalitetssikringsSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.ManglendeLigningGrunnlagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.MeldingOmVedtakBrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.OpprettRevurderingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.RefusjonkravSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.RettighetsperiodeSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningAndreStatligeYtelserSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningArbeidsgiverSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningAvslagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningUføreSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SendForvaltningsmeldingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SimulerUtbetalingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SøknadSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.TjenestepensjonRefusjonskravSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.UnderveisSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VisGrunnlagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderAlderSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderBistandsbehovSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderForutgåendeMedlemskapSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderLovvalgSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderStudentSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderSykdomSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderSykepengeErstatningSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderYrkesskadeSteg
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(
                steg = StartBehandlingSteg,
                informasjonskrav = listOf(SøknadService),
                årsakRelevanteForSteg = Vurderingsbehov.alle()
            )
            .medSteg(
                steg = SendForvaltningsmeldingSteg,
                årsakRelevanteForSteg = listOf(Vurderingsbehov.MOTTATT_SØKNAD),
                informasjonskrav = emptyList()
            )
            .medSteg(
                steg = SøknadSteg,
                årsakRelevanteForSteg = listOf(Vurderingsbehov.SØKNAD_TRUKKET),
                informasjonskrav = listOf(TrukketSøknadService),
            )
            .medSteg(
                steg = RettighetsperiodeSteg,
                informasjonskrav = listOf(VurderRettighetsperiodeService),
                årsakRelevanteForSteg = listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
            )
            .medSteg(
                steg = VurderLovvalgSteg,
                informasjonskrav = listOf(PersonopplysningService, LovvalgService),
                årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_LOVVALG,
                    Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                )
            )
            .medSteg(steg = FastsettMeldeperiodeSteg)
            .medSteg(steg = VurderAlderSteg)
            .medSteg(steg = VurderStudentSteg)
            .medSteg(
                steg = VurderSykdomSteg,
                // UføreService trengs her for å trigge ytterligere nedsatt arbeidsevne-vurdering
                informasjonskrav = listOf(YrkesskadeService, LegeerklæringService, UføreService),
                årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(
                steg = FritakMeldepliktSteg, årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(
                steg = FastsettArbeidsevneSteg, årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(
                steg = VurderBistandsbehovSteg, årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                )
            )
            .medSteg(
                steg = RefusjonkravSteg, årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING
                )
            )
            .medSteg(steg = KvalitetssikringsSteg)
            .medSteg(
                steg = VurderYrkesskadeSteg, årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(
                steg = VurderSykepengeErstatningSteg, årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(steg = FastsettSykdomsvilkåretSteg)
            .medSteg(
                steg = BeregningAvklarFaktaSteg, årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_BEREGNING,
                )
            )
            .medSteg(steg = VisGrunnlagSteg)
            .medSteg(
                steg = ManglendeLigningGrunnlagSteg,
                informasjonskrav = listOf(InntektService),
                årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_BEREGNING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.REVURDER_MANUELL_INNTEKT
                )
            )
            .medSteg(
                steg = FastsettGrunnlagSteg, årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_BEREGNING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.REVURDER_MANUELL_INNTEKT
                )
            )
            .medSteg(
                steg = VurderForutgåendeMedlemskapSteg,
                informasjonskrav = listOf(PersonopplysningForutgåendeService, ForutgåendeMedlemskapService),
                årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_MEDLEMSKAP,
                    Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP,
                    Vurderingsbehov.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                )
            )
            .medSteg(
                steg = BarnetilleggSteg,
                informasjonskrav = listOf(BarnService),
                årsakRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.BARNETILLEGG,
                )
            )
            .medSteg(
                steg = EtAnnetStedSteg,
                informasjonskrav = listOf(InstitusjonsoppholdService),
                årsakRelevanteForSteg = listOf(Vurderingsbehov.INSTITUSJONSOPPHOLD)
            )
            .medSteg(
                steg = SamordningSteg,
                informasjonskrav = listOf(SamordningYtelseVurderingService),
                årsakRelevanteForSteg = listOf(Vurderingsbehov.SAMORDNING_OG_AVREGNING, Vurderingsbehov.REVURDER_SAMORDNING),
            )
            .medSteg(steg = SamordningUføreSteg, informasjonskrav = listOf(UføreService))
            .medSteg(steg = TjenestepensjonRefusjonskravSteg, informasjonskrav = listOf(TjenestePensjonService))
            .medSteg(
                steg = SamordningAndreStatligeYtelserSteg,
                årsakRelevanteForSteg = listOf(Vurderingsbehov.SAMORDNING_OG_AVREGNING, Vurderingsbehov.REVURDER_SAMORDNING)
            )
            .medSteg(
                steg = SamordningArbeidsgiverSteg,
            )
            .medSteg(steg = SamordningAvslagSteg)
            .medSteg(steg = UnderveisSteg, informasjonskrav = listOf(MeldekortService, AktivitetspliktInformasjonskrav))
            .medSteg(steg = Effektuer11_7Steg)
            .medSteg(
                steg = BeregnTilkjentYtelseSteg,
                årsakRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            )
            .medSteg(steg = SimulerUtbetalingSteg, årsakRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering())
            .medSteg(
                steg = ForeslåVedtakSteg,
                årsakRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            ) // en-trinn
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(
                steg = FatteVedtakSteg,
                årsakRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            ) // to-trinn
            .medSteg(steg = IverksettVedtakSteg, årsakRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering())
            .medSteg(
                steg = MeldingOmVedtakBrevSteg,
                årsakRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            )
            .medSteg(
                steg = OpprettRevurderingSteg
            )
            .build()
    }

}