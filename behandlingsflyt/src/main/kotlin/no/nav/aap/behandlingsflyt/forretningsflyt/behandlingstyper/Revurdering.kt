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
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningAvslagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningUføreSteg
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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingSteg, informasjonskrav = listOf(SøknadService))
            .medSteg(
                steg = SøknadSteg,
                årsakRelevanteForSteg = listOf(ÅrsakTilBehandling.SØKNAD_TRUKKET),
                informasjonskrav = listOf(TrukketSøknadService),
            )
            .medSteg(
                steg = RettighetsperiodeSteg,
                informasjonskrav = listOf(VurderRettighetsperiodeService),
                årsakRelevanteForSteg = listOf(ÅrsakTilBehandling.VURDER_RETTIGHETSPERIODE)
            )
            .medSteg(
                steg = VurderLovvalgSteg,
                informasjonskrav = listOf(PersonopplysningService, LovvalgService),
                årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.REVURDER_LOVVALG,
                    ÅrsakTilBehandling.LOVVALG_OG_MEDLEMSKAP,
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
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING,
                    ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(
                steg = FritakMeldepliktSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING,
                    ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(
                steg = FastsettArbeidsevneSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING,
                    ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(
                steg = VurderBistandsbehovSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING,
                    ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    ÅrsakTilBehandling.VURDER_RETTIGHETSPERIODE,
                )
            )
            .medSteg(
                steg = RefusjonkravSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING
                )
            )
            .medSteg(steg = KvalitetssikringsSteg)
            .medSteg(
                steg = VurderYrkesskadeSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING,
                    ÅrsakTilBehandling.REVURDER_YRKESSKADE,
                    ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(
                steg = VurderSykepengeErstatningSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING,
                    ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(steg = FastsettSykdomsvilkåretSteg)
            .medSteg(
                steg = BeregningAvklarFaktaSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.REVURDER_BEREGNING,
                    ÅrsakTilBehandling.REVURDER_YRKESSKADE,
                    ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                )
            )
            .medSteg(steg = VisGrunnlagSteg)
            .medSteg(
                steg = ManglendeLigningGrunnlagSteg, informasjonskrav = listOf(InntektService), årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.REVURDER_BEREGNING,
                    ÅrsakTilBehandling.REVURDER_YRKESSKADE,
                    ÅrsakTilBehandling.REVURDER_MANUELL_INNTEKT
                )
            )
            .medSteg(
                steg = FastsettGrunnlagSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.REVURDER_BEREGNING,
                    ÅrsakTilBehandling.REVURDER_YRKESSKADE,
                    ÅrsakTilBehandling.REVURDER_MANUELL_INNTEKT
                )
            )
            .medSteg(
                steg = VurderForutgåendeMedlemskapSteg,
                informasjonskrav = listOf(PersonopplysningForutgåendeService, ForutgåendeMedlemskapService),
                årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.REVURDER_MEDLEMSKAP,
                    ÅrsakTilBehandling.FORUTGAENDE_MEDLEMSKAP,
                    ÅrsakTilBehandling.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT,
                )
            )
            .medSteg(
                steg = BarnetilleggSteg,
                informasjonskrav = listOf(BarnService),
                årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.BARNETILLEGG,
                )
            )
            .medSteg(
                steg = EtAnnetStedSteg,
                informasjonskrav = listOf(InstitusjonsoppholdService),
                årsakRelevanteForSteg = listOf(ÅrsakTilBehandling.INSTITUSJONSOPPHOLD)
            )
            .medSteg(
                steg = SamordningSteg,
                informasjonskrav = listOf(SamordningYtelseVurderingService),
                årsakRelevanteForSteg = listOf(ÅrsakTilBehandling.SAMORDNING_OG_AVREGNING)
            )
            .medSteg(steg = SamordningUføreSteg, informasjonskrav = listOf(UføreService))
            .medSteg(steg = TjenestepensjonRefusjonskravSteg, informasjonskrav = listOf(TjenestePensjonService))
            .medSteg(
                steg = SamordningAndreStatligeYtelserSteg,
                årsakRelevanteForSteg = listOf(ÅrsakTilBehandling.SAMORDNING_OG_AVREGNING)
            )
            .medSteg(steg = SamordningAvslagSteg)
            .medSteg(steg = UnderveisSteg, informasjonskrav = listOf(MeldekortService, AktivitetspliktInformasjonskrav))
            .medSteg(steg = Effektuer11_7Steg)
            .medSteg(
                steg = BeregnTilkjentYtelseSteg,
                årsakRelevanteForSteg = ÅrsakTilBehandling.alleInklusivGRegulering()
            )
            .medSteg(steg = SimulerUtbetalingSteg, årsakRelevanteForSteg = ÅrsakTilBehandling.alleInklusivGRegulering())
            .medSteg(
                steg = ForeslåVedtakSteg,
                årsakRelevanteForSteg = ÅrsakTilBehandling.alleInklusivGRegulering()
            ) // en-trinn
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(
                steg = FatteVedtakSteg,
                årsakRelevanteForSteg = ÅrsakTilBehandling.alleInklusivGRegulering()
            ) // to-trinn
            .medSteg(steg = IverksettVedtakSteg, årsakRelevanteForSteg = ÅrsakTilBehandling.alleInklusivGRegulering())
            .medSteg(
                steg = MeldingOmVedtakBrevSteg,
                årsakRelevanteForSteg = ÅrsakTilBehandling.alleInklusivGRegulering()
            )
            .medSteg(
                steg = OpprettRevurderingSteg
            )
            .build()
    }

}