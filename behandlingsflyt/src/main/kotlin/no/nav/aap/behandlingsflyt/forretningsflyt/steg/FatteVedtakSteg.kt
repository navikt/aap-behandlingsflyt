package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.GosysService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Omgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeføresFraBeslutter
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class FatteVedtakSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val personOpplysningerRepository: PersonopplysningRepository,
    private val refusjonkravRepository: RefusjonkravRepository,
    private val personRepository: PersonRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val trekkKlageService: TrekkKlageService,
    private val gosysService: GosysService,
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        personOpplysningerRepository = repositoryProvider.provide(),
        personRepository = repositoryProvider.provide(),
        refusjonkravRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        trekkKlageService = TrekkKlageService(repositoryProvider),
        gosysService = GosysService(gatewayProvider)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) || trekkKlageService.klageErTrukket(
                kontekst.behandlingId
            )
        ) {
            avklaringsbehov.avbrytForSteg(type())
            return Fullført
        }

        if (kontekst.behandlingType == TypeBehandling.Klage) {
            val klageresultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
            if (klageresultat is Omgjøres) {
                avklaringsbehov.avbrytForSteg(type())
                return Fullført
            }
        }

        if (avklaringsbehov.skalTilbakeføresEtterTotrinnsVurdering()) {
            return TilbakeføresFraBeslutter
        }
        if (avklaringsbehov.harHattAvklaringsbehovSomHarKrevdToTrinn()) {
            return FantAvklaringsbehov(Definisjon.FATTE_VEDTAK)
        }

        val navkontorSosialRefusjon = refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId)
        if (navkontorSosialRefusjon == null) return Fullført

        val navKontorList = navkontorSosialRefusjon
            .filter { it.harKrav && it.navKontor != null }
            .mapNotNull { navKontorEnhetsNummer(it.navKontor) }

        if (navKontorList.isNotEmpty()) {
            val personopplysninger = personOpplysningerRepository.hentHvisEksisterer(kontekst.behandlingId)
            val personId = personopplysninger?.brukerPersonopplysning?.id

            if (personId == null) {
                log.error("Fant ikke personopplysninger med id $personId")
                return Fullført
            }

            val person = personRepository.hent(personId)
            val aktivIdent = person.identer().find { it.aktivIdent }

            navKontorList.forEach { navKontor ->
             /*   gosysService.opprettOppgaveHvisIkkeEksisterer(
                    aktivIdent!!,
                    kontekst.behandlingId.toString(),
                    kontekst.behandlingId,
                    navKontor
                )*/
            }
        }
        return Fullført
    }


    fun navKontorEnhetsNummer(input: String?): String? {
        return input?.substringAfterLast(" - ")
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return FatteVedtakSteg(repositoryProvider, GatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FATTE_VEDTAK
        }
    }
}
