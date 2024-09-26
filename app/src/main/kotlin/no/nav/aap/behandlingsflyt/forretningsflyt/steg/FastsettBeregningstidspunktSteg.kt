package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType

class FastsettBeregningstidspunktSteg private constructor(
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        if (erBehovForÅFastsette(vilkårsresultat)) {
            val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
            if (beregningVurdering == null) {
                return StegResultat(listOf(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT))
            }
        }
        return StegResultat()
    }

    private fun erBehovForÅFastsette(vilkårsresultat: Vilkårsresultat): Boolean {
        // TODO: Sjekke om det faktisk er behov for innhenting av opplysninger
        // DVS sjekk mot vilkår forutfor dette om alle er oppfylt fra inngang
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        return sykdomsvilkåret.vilkårsperioder()
            .any { it.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FastsettBeregningstidspunktSteg(
                BeregningVurderingRepository(connection),
                VilkårsresultatRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_BEREGNINGSTIDSPUNKT
        }
    }
}
