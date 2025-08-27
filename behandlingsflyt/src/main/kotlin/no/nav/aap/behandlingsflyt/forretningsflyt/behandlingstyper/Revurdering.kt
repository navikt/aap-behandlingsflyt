package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgService
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeService
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingService
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
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.EtAnnetStedSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettArbeidsevneSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettGrunnlagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettMeldeperiodeSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettSykdomsvilkåretSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FritakMeldepliktSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.IkkeOppfyltMeldepliktSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.IverksettVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KansellerRevurderingSteg
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
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SykdomsurderingBrevSteg
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
import no.nav.aap.komponenter.miljo.Miljø

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(
                steg = StartBehandlingSteg,
                informasjonskrav = listOf(SøknadService, BarnService),
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle()
            )
            .medSteg(
                steg = SendForvaltningsmeldingSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.MOTTATT_SØKNAD),
                informasjonskrav = emptyList()
            )
            .medSteg(
                steg = KansellerRevurderingSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.REVURDERING_KANSELLERT),
                informasjonskrav = listOf(KansellerRevurderingService)
            )
            .medSteg(
                steg = SøknadSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.SØKNAD_TRUKKET),
                informasjonskrav = listOf(TrukketSøknadService)
            )
            .medSteg(
                steg = RettighetsperiodeSteg,
                informasjonskrav = listOf(VurderRettighetsperiodeService),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
            )
            .medSteg(
                steg = VurderLovvalgSteg,
                informasjonskrav = listOf(PersonopplysningService, LovvalgService),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_LOVVALG,
                    Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(steg = FastsettMeldeperiodeSteg)
            .medSteg(steg = VurderAlderSteg)
            .medSteg(steg = VurderStudentSteg)
            .medSteg(
                steg = VurderSykdomSteg,
                // UføreService trengs her for å trigge ytterligere nedsatt arbeidsevne-vurdering
                informasjonskrav = listOf(YrkesskadeService, LegeerklæringService, UføreService),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = FritakMeldepliktSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = FastsettArbeidsevneSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = VurderBistandsbehovSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = RefusjonkravSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .apply {
                // TODO legges kun ut i dev i første runde
                if (Miljø.erDev() || Miljø.erLokal()) {
                    medSteg(
                        steg = SykdomsurderingBrevSteg, vurderingsbehovRelevanteForSteg = listOf(
                            Vurderingsbehov.MOTTATT_SØKNAD,
                            Vurderingsbehov.MOTTATT_DIALOGMELDING,
                            Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                            Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                        )
                    )
                }
            }
            .medSteg(steg = KvalitetssikringsSteg)
            .medSteg(
                steg = VurderYrkesskadeSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = VurderSykepengeErstatningSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(steg = FastsettSykdomsvilkåretSteg)
            .medSteg(
                steg = BeregningAvklarFaktaSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_BEREGNING,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(steg = VisGrunnlagSteg)
            .medSteg(
                steg = ManglendeLigningGrunnlagSteg,
                informasjonskrav = listOf(InntektService),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_BEREGNING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.REVURDER_MANUELL_INNTEKT,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = FastsettGrunnlagSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_BEREGNING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.REVURDER_MANUELL_INNTEKT,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = VurderForutgåendeMedlemskapSteg,
                informasjonskrav = listOf(PersonopplysningForutgåendeService, ForutgåendeMedlemskapService),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.REVURDER_MEDLEMSKAP,
                    Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP,
                    Vurderingsbehov.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                // TODO: Midlertidig duplikat av BarnService, skal på sikt kun være i StartBehandlingSteg
                informasjonskrav = listOf(BarnService),
                steg = BarnetilleggSteg,
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.BARNETILLEGG,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE
                )
            )
            .medSteg(
                steg = EtAnnetStedSteg,
                informasjonskrav = listOf(InstitusjonsoppholdService),
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.INSTITUSJONSOPPHOLD)
            )
            .medSteg(
                steg = SamordningSteg,
                informasjonskrav = listOf(SamordningYtelseVurderingService),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.SAMORDNING_OG_AVREGNING,
                    Vurderingsbehov.REVURDER_SAMORDNING,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                ),
            )
            .medSteg(steg = SamordningUføreSteg, informasjonskrav = listOf(UføreService))
            .medSteg(steg = TjenestepensjonRefusjonskravSteg, informasjonskrav = listOf(TjenestePensjonService))
            .medSteg(
                steg = SamordningAndreStatligeYtelserSteg,
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.SAMORDNING_OG_AVREGNING,
                    Vurderingsbehov.REVURDER_SAMORDNING,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = SamordningArbeidsgiverSteg,
            )
            .medSteg(steg = SamordningAvslagSteg)
            .medSteg(
                steg = IkkeOppfyltMeldepliktSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering(),
                informasjonskrav = listOf(MeldekortService, AktivitetspliktInformasjonskrav)
            )
            .medSteg(steg = UnderveisSteg)
            .medSteg(
                steg = BeregnTilkjentYtelseSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            )
            .medSteg(
                steg = SimulerUtbetalingSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            )
            .medSteg(
                steg = ForeslåVedtakSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            ) // en-trinn
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(
                steg = FatteVedtakSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            ) // to-trinn
            .medSteg(
                steg = IverksettVedtakSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            )
            .medSteg(
                steg = MeldingOmVedtakBrevSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            )
            .medSteg(
                steg = OpprettRevurderingSteg
            )
            .build()
    }

}