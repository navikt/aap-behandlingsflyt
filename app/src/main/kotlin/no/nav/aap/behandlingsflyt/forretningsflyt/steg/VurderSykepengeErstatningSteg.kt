package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengerErstatningVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class VurderSykepengeErstatningSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val sakService: SakService,
    private val avklaringsbehovRepository: AvklaringsbehovRepositoryImpl
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        // TODO: Dette må gjøres mye mer robust og sjekkes konsistent mot 11-6...
        if (bistandsvilkåret.vilkårsperioder().all { !it.erOppfylt() } &&
            sykdomsvilkåret.vilkårsperioder()
                .any { it.utfall == Utfall.IKKE_OPPFYLT && it.avslagsårsak == Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET }) {

            val grunnlag = sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)

            if (grunnlag?.vurdering != null) {
                val sak = sakService.hent(kontekst.sakId)
                val vurderingsdato = sak.rettighetsperiode.fom
                val faktagrunnlag = SykepengerErstatningFaktagrunnlag(
                    vurderingsdato,
                    vurderingsdato.plusMonths(6),
                    grunnlag.vurdering()!!
                )
                SykepengerErstatningVilkår(vilkårsresultat).vurder(faktagrunnlag)
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            } else {
                return FantAvklaringsbehov(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
        } else {
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            val sykepengeerstatningsBehov =
                avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKEPENGEERSTATNING)

            if (sykepengeerstatningsBehov?.erÅpent() == true) {
                avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return VurderSykepengeErstatningSteg(
                VilkårsresultatRepository(connection),
                SykepengerErstatningRepository(connection),
                SakService(connection),
                AvklaringsbehovRepositoryImpl(connection)
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_SYKEPENGEERSTATNING
        }
    }
}
