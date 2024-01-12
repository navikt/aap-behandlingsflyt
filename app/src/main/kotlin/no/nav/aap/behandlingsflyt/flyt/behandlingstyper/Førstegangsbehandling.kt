package no.nav.aap.behandlingsflyt.flyt.behandlingstyper

import no.nav.aap.behandlingsflyt.behandling.BehandlingType
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.PliktkortService
import no.nav.aap.behandlingsflyt.faktagrunnlag.barn.BarnService
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.InntektService
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersonopplysningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.impl.BarnetilleggSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FastsettArbeidsevneSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FastsettGrunnlagSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FritakMeldepliktSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.GeneriskPlaceholderFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.SamordningSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.UnderveisSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderAlderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderBistandsbehovSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderStudentSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykdomSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykepengeErstatningSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderYrkesskadeÅrsakssammenhengSteg

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingSteg)
            .medSteg(steg = VurderAlderSteg, informasjonskrav = listOf(PersonopplysningService))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.VURDER_LOVVALG))
            .medSteg(steg = VurderStudentSteg)
            .medSteg(steg = VurderYrkesskadeÅrsakssammenhengSteg, informasjonskrav = listOf(YrkesskadeService))
            .medSteg(steg = VurderSykdomSteg)
            .medSteg(steg = FastsettArbeidsevneSteg)
            .medSteg(steg = FritakMeldepliktSteg)
            .medSteg(steg = VurderBistandsbehovSteg)
            .medSteg(steg = VurderSykepengeErstatningSteg)
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.VURDER_MEDLEMSKAP))
            .medSteg(steg = FastsettGrunnlagSteg, informasjonskrav = listOf(InntektService))
            .medSteg(steg = UnderveisSteg, informasjonskrav = listOf(PliktkortService))
            .medSteg(steg = BarnetilleggSteg, informasjonskrav = listOf(BarnService))
            .medSteg(steg = SamordningSteg)
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.BEREGN_TILKJENT_YTELSE))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.SIMULERING))
            .medSteg(steg = ForeslåVedtakSteg) // en-trinn
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(steg = FatteVedtakSteg) // to-trinn
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.IVERKSETT_VEDTAK))
            .build()
    }

    override fun identifikator(): String {
        return "ae0034"
    }
}
