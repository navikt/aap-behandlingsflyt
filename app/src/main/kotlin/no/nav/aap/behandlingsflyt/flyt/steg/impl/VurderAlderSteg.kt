package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersgrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersvilkåret
import no.nav.aap.behandlingsflyt.sak.SakService

class VurderAlderSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService,
    private val personopplysningRepository: PersonopplysningRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val periodeTilVurdering =
            periodeTilVurderingService.utled(kontekst = kontekst, vilkår = Vilkårtype.ALDERSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            for (periode in periodeTilVurdering) {
                val aldersgrunnlag = Aldersgrunnlag(periode, personopplysningGrunnlag.personopplysning.fødselsdato)
                Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlag)
            }
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return VurderAlderSteg(
                VilkårsresultatRepository(connection),
                PeriodeTilVurderingService(SakService(connection)),
                PersonopplysningRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_ALDER
        }
    }
}
