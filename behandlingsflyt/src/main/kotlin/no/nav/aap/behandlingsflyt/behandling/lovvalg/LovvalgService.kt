package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.AARegisterGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ARBEIDSFORHOLDSTATUSER
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.YearMonth

class LovvalgService private constructor(
    private val medlemskapGateway: MedlemskapGateway,
    private val sakService: SakService,
    private val medlemskapRepository: MedlemskapRepository
): Informasjonskrav {
    val LOGGER = LoggerFactory.getLogger(LovvalgService::class.java)

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        val medlemskapUnntakPersonId = innhentMedlemskapGrunnlagOgLagre(sak, kontekst.behandlingId)
        val aaRegId = innhentAARegisterGrunnlagOgLagre(sak, kontekst.behandlingId)
        val aInntektId = innhentAInntektGrunnlagOgLagre(sak, kontekst.behandlingId)

        val sykepenger = "" // TODO: Hva skulle vi gjøre her?

        /*
        val relatertePersonopplysninger = personRepository.hentHvisEksisterer(kontekst.behandlingId)?.relatertePersonopplysninger?.personopplysninger
        val soknad = TODO() // Her mangler mye knask
        */

        // Kombinér til aggregat-tabell
        //genererMedlemskapLovvalgGrunnlagOgLagre(medlemskapUnntakPersonId, aaRegId, aInntektId)

        return IKKE_ENDRET
    }

    fun innhentMedlemskapGrunnlagOgLagre(sak: Sak, behandlingId: BehandlingId): Long {
        try {
            val medlemskapPerioder = medlemskapGateway.innhent(sak.person, Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.fom))
            //return medlemskapRepository.lagreUnntakMedlemskap(behandlingId, medlemskapPerioder)
        } catch (e: Exception) {
            LOGGER.warn("innhentMedlemskapGrunnlagOgLagre: ${e.message}, stacktrace: $e")
        }
        return 1
    }

    fun innhentAARegisterGrunnlagOgLagre(sak: Sak, behandlingId: BehandlingId): Long {
        //TODO: Repo for dette
        try {
            val aaRegisterGateway = AARegisterGateway()
            val request = ArbeidsforholdRequest(
                arbeidstakerId = sak.person.aktivIdent().identifikator,
                arbeidsforholdstatuser = listOf(ARBEIDSFORHOLDSTATUSER.AKTIV.toString())
            )

            val response = aaRegisterGateway.hentAARegisterData(request)
        } catch (e: Exception) {
            LOGGER.warn("innhentAARegisterGrunnlagOgLagre: ${e.message}, stacktrace: $e")
        }
        return 1
    }

    fun innhentAInntektGrunnlagOgLagre(sak: Sak, behandlingId: BehandlingId): Long {
        try {
            val inntektskomponentGateway = InntektkomponentenGateway()
            val response = inntektskomponentGateway.hentAInntekt(
                sak.person.aktivIdent().identifikator,
                YearMonth.from(sak.rettighetsperiode.fom),
                YearMonth.from(sak.rettighetsperiode.fom)
            )
        } catch (e: Exception) {
            LOGGER.warn("innhentAInntektGrunnlagOgLagre: ${e.message}, stacktrace: $e")
        }

        return 1
    }

    fun genererMedlemskapLovvalgGrunnlagOgLagre(medlemskapUnntakPersonId: Long, aaRegId: Long, aInntektId: Long) {
        // Putt alt inn i aggregeringstabellen/MEDLEMSKAP_GRUNNLAG ELNS som du ikke har laget
        //TODO: Repo for dette
    }

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            // Skal alltid innhentes
            return true
        }

        override fun konstruer(connection: DBConnection): LovvalgService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return LovvalgService(
                MedlemskapGateway(),
                SakService(sakRepository),
                MedlemskapRepository(connection)
            )
        }
    }
}