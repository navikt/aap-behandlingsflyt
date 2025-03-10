package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgService
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
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.IverksettVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KvalitetssikringsSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.MeldingOmVedtakBrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.OpprettRevurderingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SimulerUtbetalingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg
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
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.effektuer11_7.Effektuer11_7Steg
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingSteg, informasjonskrav = listOf(SøknadService))
            .medSteg(
                steg = VurderLovvalgSteg,
                informasjonskrav = listOf(PersonopplysningService, LovvalgService),
                årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.REVURDER_LOVVALG
                )
            )
            .medSteg(steg = FastsettMeldeperiodeSteg)
            .medSteg(steg = VurderAlderSteg)
            .medSteg(steg = VurderStudentSteg)
            .medSteg(
                steg = VurderSykdomSteg,
                informasjonskrav = listOf(YrkesskadeService, UføreService, LegeerklæringService),
                årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING
                )
            )
            .medSteg(
                steg = FritakMeldepliktSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING
                )
            )
            .medSteg(
                steg = FastsettArbeidsevneSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING
                )
            )
            .medSteg(
                steg = VurderBistandsbehovSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING
                )
            )
            .medSteg(steg = KvalitetssikringsSteg)
            .medSteg(
                steg = VurderYrkesskadeSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING,
                    ÅrsakTilBehandling.REVURDER_YRKESSKADE
                )
            )
            .medSteg(steg = FastsettSykdomsvilkåretSteg)
            .medSteg(
                steg = VurderSykepengeErstatningSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING
                )
            )
            .medSteg(
                steg = BeregningAvklarFaktaSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.REVURDER_BEREGNING,
                    ÅrsakTilBehandling.REVURDER_YRKESSKADE
                )
            )
            .medSteg(steg = VisGrunnlagSteg)
            .medSteg(
                steg = FastsettGrunnlagSteg, informasjonskrav = listOf(InntektService), årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.REVURDER_BEREGNING,
                    ÅrsakTilBehandling.REVURDER_YRKESSKADE
                )
            )
            .medSteg(
                steg = VurderForutgåendeMedlemskapSteg,
                informasjonskrav = listOf(PersonopplysningForutgåendeService, ForutgåendeMedlemskapService),
                årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.REVURDER_MEDLEMSKAP
                )
            )
            .medSteg(steg = BarnetilleggSteg, informasjonskrav = listOf(BarnService))
            .medSteg(steg = EtAnnetStedSteg, informasjonskrav = listOf(InstitusjonsoppholdService))
            .medSteg(steg = UnderveisSteg, informasjonskrav = listOf(MeldekortService, AktivitetspliktInformasjonskrav))
            .medSteg(steg = SamordningSteg, informasjonskrav = listOf(SamordningYtelseVurderingService))
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
                steg = OpprettRevurderingSteg,
                årsakRelevanteForSteg = listOf(ÅrsakTilBehandling.REVURDER_SAMORDING)
            )
            .build()
    }

}