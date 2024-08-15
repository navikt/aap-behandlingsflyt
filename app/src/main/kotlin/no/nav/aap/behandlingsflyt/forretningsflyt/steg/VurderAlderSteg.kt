package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.vilkår.alder.Aldersgrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.alder.Aldersvilkåret
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType

class VurderAlderSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val personopplysningRepository: PersonopplysningRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        if (kontekst.perioderTilVurdering.isNotEmpty()) {
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            for (periode in kontekst.perioder()) {
                val aldersgrunnlag = Aldersgrunnlag(periode, personopplysningGrunnlag.brukerPersonopplysning.fødselsdato)
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
                PersonopplysningRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_ALDER
        }
    }
}
