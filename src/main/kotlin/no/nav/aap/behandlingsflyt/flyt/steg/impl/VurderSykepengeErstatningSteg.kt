package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.SykepengerErstatningVilkår
import no.nav.aap.behandlingsflyt.sak.SakService

class VurderSykepengeErstatningSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val sakService: SakService,
    private val avklaringsbehovRepository: AvklaringsbehovRepositoryImpl
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        // TODO: Dette må gjøres mye mer robust og sjekkes konsistent mot 11-6...
        if (bistandsvilkåret.vilkårsperioder().all { !it.erOppfylt() } &&
            sykdomsvilkåret.vilkårsperioder().any { it.erOppfylt() }) {

            val grunnlag = sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)

            if (grunnlag?.vurdering != null) {
                val sak = sakService.hent(kontekst.sakId)
                val vurderingsdato = sak.rettighetsperiode.fom
                val faktagrunnlag = SykepengerErstatningFaktagrunnlag(
                    vurderingsdato,
                    vurderingsdato.plusMonths(6),
                    grunnlag.vurdering
                )
                SykepengerErstatningVilkår(vilkårsresultat).vurder(faktagrunnlag)
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            } else {
                return StegResultat(listOf(Definisjon.AVKLAR_SYKEPENGEERSTATNING))
            }
        } else {
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            val sykepengeerstatningsBehov =
                avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKEPENGEERSTATNING)

            if (sykepengeerstatningsBehov?.erÅpent() == true) {
                //TODO: Blir/skal denne endringen lagret noe sted?
                sykepengeerstatningsBehov.avbryt()
            }
        }

        return StegResultat()
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
