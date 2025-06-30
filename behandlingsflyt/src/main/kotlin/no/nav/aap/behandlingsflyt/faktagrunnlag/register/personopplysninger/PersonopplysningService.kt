package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PdlPersonopplysningGateway
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Duration

class PersonopplysningService private constructor(
    private val sakService: SakService,
    private val personopplysningRepository: PersonopplysningRepository,
    private val personopplysningGateway: PersonopplysningGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {
    override val navn = Companion.navn

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val personopplysninger =
            personopplysningGateway.innhent(sak.person) ?: error("fødselsdato skal alltid eksistere i PDL")
        val eksisterendeData = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)

        // TODO: Oppdatere person tabellen med identene til bruker for å detektere splitt / merge og utlede behovet for å feile
        if (personopplysninger != eksisterendeData?.brukerPersonopplysning) {
            personopplysningRepository.lagre(kontekst.behandlingId, personopplysninger)
            return ENDRET
        }
        return IKKE_ENDRET
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.PERSONOPPLYSNING

        override fun konstruer(repositoryProvider: RepositoryProvider): PersonopplysningService {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val personopplysningRepository =
                repositoryProvider.provide<PersonopplysningRepository>()
            return PersonopplysningService(
                SakService(sakRepository),
                personopplysningRepository,
                PdlPersonopplysningGateway,
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}
