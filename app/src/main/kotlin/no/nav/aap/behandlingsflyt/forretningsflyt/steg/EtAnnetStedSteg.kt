package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class EtAnnetStedSteg(
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val etAnnetStedUtlederService: EtAnnetStedUtlederService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(kontekst.behandlingId)

        val avklaringsbehov = mutableListOf<Definisjon>()

        val harBehovForAvklaringer = etAnnetStedUtlederService.harBehovForAvklaringer(kontekst.behandlingId)
        if(harBehovForAvklaringer.harBehov()){
            avklaringsbehov += harBehovForAvklaringer.avklaringsbehov()
        }

        if(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_HELSEINSTITUSJON) != null){
            avklaringsbehov.remove(Definisjon.AVKLAR_HELSEINSTITUSJON)
        }

        if(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SONINGSFORRHOLD) != null){
            avklaringsbehov.remove(Definisjon.AVKLAR_SONINGSFORRHOLD)
        }

        return StegResultat(avklaringsbehov)
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)
            return EtAnnetStedSteg(
                institusjonsoppholdRepository, AvklaringsbehovRepositoryImpl(connection), EtAnnetStedUtlederService(
                BarnetilleggRepository(connection),
                    institusjonsoppholdRepository
            ))
        }

        override fun type(): StegType {
            return StegType.DU_ER_ET_ANNET_STED
        }
    }
}