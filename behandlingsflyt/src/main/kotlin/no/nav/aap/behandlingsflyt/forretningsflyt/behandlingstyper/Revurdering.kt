package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.SøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BarnetilleggSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BeregnTilkjentYtelseSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BeregningAvklarFaktaSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.EtAnnetStedSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettArbeidsevneSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettGrunnlagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettSykdomsvilkåretSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FritakMeldepliktSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.IverksettVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KvalitetssikringsSteg
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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingSteg, informasjonskrav = listOf(SøknadService))
            .medSteg(steg = VurderLovvalgSteg)
            .medSteg(
                steg = VurderAlderSteg,
                informasjonskrav = listOf(PersonopplysningService)
            )
            .medSteg(steg = VurderStudentSteg)
            // UføreService svarer med mocket respons inntil pesys-integrasjon er fullført:
            // Relevant issue: https://github.com/navikt/pensjon-pen/pull/13138
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
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING
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
            .medSteg(steg = VurderForutgåendeMedlemskapSteg, informasjonskrav = listOf(MedlemskapService))
            .medSteg(
                steg = BeregningAvklarFaktaSteg, årsakRelevanteForSteg = listOf(
                    ÅrsakTilBehandling.MOTTATT_SØKNAD,
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING
                )
            )
            .medSteg(steg = VisGrunnlagSteg)
            .medSteg(steg = FastsettGrunnlagSteg, informasjonskrav = listOf(InntektService))
            .medSteg(steg = BarnetilleggSteg, informasjonskrav = listOf(BarnService))
            .medSteg(steg = EtAnnetStedSteg, informasjonskrav = listOf(InstitusjonsoppholdService))
            .medSteg(steg = UnderveisSteg, informasjonskrav = listOf(PliktkortService, AktivitetspliktInformasjonskrav))
            .medSteg(steg = SamordningSteg, informasjonskrav = listOf(SamordningYtelseVurderingService))
            .medSteg(steg = BeregnTilkjentYtelseSteg, årsakRelevanteForSteg = ÅrsakTilBehandling.entries)
            .medSteg(steg = SimulerUtbetalingSteg, årsakRelevanteForSteg = ÅrsakTilBehandling.entries)
            .medSteg(steg = ForeslåVedtakSteg, årsakRelevanteForSteg = ÅrsakTilBehandling.entries) // en-trinn
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(steg = FatteVedtakSteg) // to-trinn
            .medSteg(steg = BrevSteg)
            .medSteg(steg = IverksettVedtakSteg)
            .build()
    }

}