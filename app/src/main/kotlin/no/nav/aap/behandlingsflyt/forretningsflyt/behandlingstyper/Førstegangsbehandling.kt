package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortService
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
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.EtAnnetStedSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettArbeidsevneSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettBeregningstidspunktSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettGrunnlagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FritakMeldepliktSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.IverksettVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KvalitetssikringsSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SimulerUtbetalingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.UnderveisSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VisGrunnlagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderAlderSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderBistandsbehovSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderLovvalgSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderMedlemskapSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderStudentSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderSykdomSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderSykepengeErstatningSteg

object Førstegangsbehandling : BehandlingType {
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
            .medSteg(steg = VurderSykdomSteg, informasjonskrav = listOf(YrkesskadeService, UføreService))
            .medSteg(steg = FritakMeldepliktSteg)
            .medSteg(steg = FastsettArbeidsevneSteg)
            .medSteg(steg = VurderBistandsbehovSteg)
            .medSteg(steg = KvalitetssikringsSteg)
            .medSteg(steg = VurderSykepengeErstatningSteg)
            .medSteg(steg = VurderMedlemskapSteg, informasjonskrav = listOf(MedlemskapService))
            .medSteg(steg = FastsettBeregningstidspunktSteg)
            .medSteg(steg = VisGrunnlagSteg)
            .medSteg(steg = FastsettGrunnlagSteg, informasjonskrav = listOf(InntektService))
            .medSteg(steg = BarnetilleggSteg, informasjonskrav = listOf(BarnService))
            .medSteg(steg = EtAnnetStedSteg, informasjonskrav = listOf(InstitusjonsoppholdService))
            .medSteg(steg = UnderveisSteg, informasjonskrav = listOf(PliktkortService, BruddAktivitetspliktService))
            // .medSteg(steg = SamordningSteg, informasjonskrav = listOf(SamordningService))
            .medSteg(steg = BeregnTilkjentYtelseSteg)
            .medSteg(steg = SimulerUtbetalingSteg)
            .medSteg(steg = ForeslåVedtakSteg) // en-trinn
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(steg = FatteVedtakSteg) // to-trinn
            .medSteg(steg = BrevSteg)
            .medSteg(steg = IverksettVedtakSteg)
            .build()
    }
}