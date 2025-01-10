package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class LovvalgService private constructor(
    private val medlemskapGateway: MedlemskapGateway,
    private val pdlGateway: IdentGateway,
    private val sakService: SakService,
): Informasjonskrav {
    private val NORGE = "NOR"

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        val vurderingerInnenfor = lovvalgslandErNorge(sak.person)
            && erIJobbEllerMottarSykepengerSisteFemÅr()
            && bosattINorgeSisteFemÅr()
            && innvandretNyligMedLivsinteresser()

        if (!vurderingerInnenfor) {
            // TODO: Lagre dette ned (i ny table?) slik at lovvalg-steget trigger avklaringsbehov
        }

        /*if (vurderingerInnenfor != gamleVurderingerInnenfor ) {
            return ENDRET
        }*/

        return IKKE_ENDRET
    }

    // Er et annet lovvalgsland registrert i MEDL?
    private fun lovvalgslandErNorge(person: Person) : Boolean {
        val medlemskapPerioder = medlemskapGateway.innhent(person)
        return medlemskapPerioder.all { it.lovvalgsland == NORGE }
    }

    // Om bruker er i jobb eller har vært i jobb (eller mottatt sykepenger) frem til søknadstidspunktet (fem år tilbake i tid på 11-2) // Må være ja
    private fun erIJobbEllerMottarSykepengerSisteFemÅr(): Boolean {
        // AA-reg
        // A-inntekt
        // SP
        // TODO: Om bruker er i jobb eller har vært i jobb (eller mottatt sykepenger) frem til søknadstidspunktet (fem år tilbake i tid på 11-2)

        // Søknad
        // TODO: Hvis bruker oppgir at de jobber i utlandet (fra søknad) -> instablock
        // TODO: (eller mottatt sykepenger) frem til søknadstidspunktet (fem år tilbake i tid på 11-2)

        return true
    }


    // Om bruker er bosatt i Norge på søknadstidspunktet (fem år tilbake i tid på i 11-2) // Må være ja
    private fun bosattINorgeSisteFemÅr(): Boolean {
        // PDL
        // TODO: Om bruker er bosatt i Norge på søknadstidspunktet (fem år tilbake i tid på i 11-2)
        //pdlGateway.hentAlleIdenterForPerson()

        // Søknad
        // TODO: Hvis bruker oppgir at de har oppholdt seg i utlandet (fra søknad) -> instablock
        return true
    }

    //Hvis innvandret eller innflyttet for mindre enn ett år
    //siden så burde vi se nærmere på hvor bruker har senter for livsinteresser (avklaringsbehov) (PDL har info om dette)
    private fun innvandretNyligMedLivsinteresser(): Boolean {
        // PDL
        return true
    }


    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            // Skal alltid innhentes
            return true
        }

        override fun konstruer(connection: DBConnection): LovvalgService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide(SakRepository::class)
            val identGateway = GatewayProvider.provide(IdentGateway::class)
            return LovvalgService(
                MedlemskapGateway(),
                identGateway,
                SakService(sakRepository)
            )
        }
    }
}