package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BarnetilleggSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.BeregnTilkjentYtelseSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettArbeidsevneSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettBeregningstidspunktSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettGrunnlagSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FritakMeldepliktSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.GeneriskPlaceholderFlytSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.UnderveisSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderAlderSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderBistandsbehovSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderStudentSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderSykdomSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderSykepengeErstatningSteg
import no.nav.aap.verdityper.flyt.StegType

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingSteg)
            .medSteg(steg = VurderAlderSteg, informasjonskrav = listOf(PersonopplysningService))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.VURDER_LOVVALG))
            .medSteg(steg = VurderStudentSteg)
            .medSteg(steg = VurderSykdomSteg, informasjonskrav = listOf(YrkesskadeService))
            .medSteg(steg = FastsettArbeidsevneSteg)
            .medSteg(steg = FritakMeldepliktSteg)
            .medSteg(steg = VurderBistandsbehovSteg)
            .medSteg(steg = VurderSykepengeErstatningSteg)
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.VURDER_MEDLEMSKAP))
            .medSteg(steg = FastsettBeregningstidspunktSteg)
            .medSteg(steg = FastsettGrunnlagSteg, informasjonskrav = listOf(InntektService))
            .medSteg(steg = UnderveisSteg, informasjonskrav = listOf(PliktkortService))
            .medSteg(steg = BarnetilleggSteg, informasjonskrav = listOf(BarnService))
            .medSteg(steg = BeregnTilkjentYtelseSteg, informasjonskrav = listOf(PersonopplysningService))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.SIMULERING))
            .medSteg(steg = ForeslåVedtakSteg) // en-trinn
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(steg = FatteVedtakSteg) // to-trinn
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.IVERKSETT_VEDTAK))
            .build()
    }
}
