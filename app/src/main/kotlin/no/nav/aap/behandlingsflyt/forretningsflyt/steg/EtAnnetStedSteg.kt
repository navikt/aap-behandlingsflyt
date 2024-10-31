package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class EtAnnetStedSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val etAnnetStedUtlederService: EtAnnetStedUtlederService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val avklaringsbehov = mutableListOf<Definisjon>()

        val harBehovForAvklaringer = etAnnetStedUtlederService.utled(kontekst.behandlingId)
        if (harBehovForAvklaringer.harBehovForAvklaring()) {
            avklaringsbehov += harBehovForAvklaringer.avklaringsbehov()
        }

        if (!avklaringsbehov.contains(Definisjon.AVKLAR_HELSEINSTITUSJON)) {
            avbrytHvisFinnesOgIkkeTrengs(avklaringsbehovene, Definisjon.AVKLAR_HELSEINSTITUSJON)
        }

        if (!avklaringsbehov.contains(Definisjon.AVKLAR_SONINGSFORRHOLD)) {
            avbrytHvisFinnesOgIkkeTrengs(avklaringsbehovene, Definisjon.AVKLAR_SONINGSFORRHOLD)
        }
        if (avklaringsbehov.isNotEmpty()) {
            return FantAvklaringsbehov(avklaringsbehov)
        }

        return Fullført
    }

    private fun avbrytHvisFinnesOgIkkeTrengs(avklaringsbehovene: Avklaringsbehovene, definisjon: Definisjon) {
        val eksisterendeBehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

        if (eksisterendeBehov?.erÅpent() == true) {
            avklaringsbehovene.avbryt(definisjon)
        }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)
            return EtAnnetStedSteg(
                AvklaringsbehovRepositoryImpl(connection), EtAnnetStedUtlederService(
                    BarnetilleggRepository(connection),
                    institusjonsoppholdRepository
                )
            )
        }

        override fun type(): StegType {
            return StegType.DU_ER_ET_ANNET_STED
        }
    }
}