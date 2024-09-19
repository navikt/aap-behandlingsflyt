package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType

class EtAnnetStedSteg(
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(kontekst.behandlingId)

        val avklaringsbehov = mutableListOf<Definisjon>()

        if (grunnlag?.opphold?.any { segment -> segment.verdi.type == Institusjonstype.FO } == true &&
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SONINGSFORRHOLD) == null) {
            avklaringsbehov.add(Definisjon.AVKLAR_SONINGSFORRHOLD)
        }
        if (grunnlag?.opphold?.any { segment -> segment.verdi.type == Institusjonstype.HS } == true &&
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_HELSEINSTITUSJON) == null) {
            avklaringsbehov.add(Definisjon.AVKLAR_HELSEINSTITUSJON)
        }

        return StegResultat(avklaringsbehov)
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return EtAnnetStedSteg(InstitusjonsoppholdRepository(connection), AvklaringsbehovRepositoryImpl(connection))
        }

        override fun type(): StegType {
            return StegType.DU_ER_ET_ANNET_STED
        }
    }
}