package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.repository.RepositoryFactory
import org.slf4j.LoggerFactory

class EtAnnetStedSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val etAnnetStedUtlederService: EtAnnetStedUtlederService
) : BehandlingSteg {

    private val log = LoggerFactory.getLogger(EtAnnetStedSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val avklaringsbehov = mutableListOf<Definisjon>()

        val harBehovForAvklaringer = etAnnetStedUtlederService.utled(kontekst.behandlingId)
        log.info("Perioder til vurdering: {}", harBehovForAvklaringer.perioderTilVurdering)
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
            val repositoryFactory = RepositoryFactory(connection)
            val behandlingRepository = repositoryFactory.create(BehandlingRepository::class)
            val sakRepository = repositoryFactory.create(SakRepository::class)
            val personRepository = repositoryFactory.create(PersonRepository::class)
            return EtAnnetStedSteg(
                AvklaringsbehovRepositoryImpl(connection), EtAnnetStedUtlederService(
                    BarnetilleggRepository(connection),
                    institusjonsoppholdRepository,
                    SakOgBehandlingService(
                        GrunnlagKopierer(connection, personRepository),
                        sakRepository,
                        behandlingRepository
                    )
                )
            )
        }

        override fun type(): StegType {
            return StegType.DU_ER_ET_ANNET_STED
        }
    }
}