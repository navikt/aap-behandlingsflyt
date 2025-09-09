package no.nav.aap.behandlingsflyt.prosessering


import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BehandlingOgMeldekortService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BehandlingOgMeldekortServiceImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class MeldekortTilApiInternJobbUtfører(
    private val behandlingOgMeldekortService: BehandlingOgMeldekortService,
    private val saksRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val apiInternGateway: ApiInternGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())

        val sak = saksRepository.hent(sakId)
        val meldekortOgBehandling = behandlingOgMeldekortService.hentAlle(sak)

        // TODO filter på behandlingId hvis det passer for Jobben?
/*        val meldekortene = behandlingOgMeldekortService.hentAlle(behandlingId)
          val kontraktObjekter = meldekortene.map { meldekort ->
              val behandling = behandlingRepository.hent(behandlingId)
              val underveisGrunnlag = underveisRepository.hent(behandling.id)

              tilKontrakt(sak, behandling, meldekort, underveisGrunnlag)
        }*/

        val kontraktObjekter: List<DetaljertMeldekortDTO> =
            meldekortOgBehandling.flatMap { (behandling, meldekortListe) ->
                val underveisGrunnlag = underveisRepository.hent(behandling.id)

                meldekortListe.map { meldekort ->
                    tilKontrakt(sak, behandling, meldekort, underveisGrunnlag)
                }
            }

        try {
            apiInternGateway.sendDetaljertMeldekort(kontraktObjekter)
        } catch (e: Exception) {
            log.error(
                "Feil ved sending av meldekort til API-intern for sak=${sakId}," + " behandling=${behandlingId}", e
            )
            throw e
        }
    }

    private fun tilKontrakt(
        sak: Sak,
        behandling: Behandling,
        meldekort: Meldekort,
        underveisGrunnlag: UnderveisGrunnlag
    ): DetaljertMeldekortDTO {

        val underveisPeriode = finnUnderveisperiode(meldekort, underveisGrunnlag.perioder)
        val meldePeriode = underveisPeriode.meldePeriode
        val meldepliktStatus = underveisPeriode.meldepliktStatus
        val rettighetsType = underveisPeriode.rettighetsType
        val avslagsårsak = underveisPeriode.avslagsårsak
        val personIdent = sak.person.aktivIdent()

        // TODO: vurder sammen med NKS om vi har mer relevant info å sende med
        // Underveisperiode har informasjon som kan være nyttig å sende med
        // Behandling har også noen kandidater
        // Vi kan også sjekke for fritak fra meldeplikt, og rimelig grunn for å ikke oppfylle meldeplikt
        // men denne koden skrives om pt. så best å vente.

        return DetaljertMeldekortDTO(
            personIdent = personIdent.identifikator,
            saksnummer = sak.saksnummer,
            behandlingId = behandling.id.toLong(),
            meldeperiodeFom = meldePeriode.fom,
            meldeperiodeTom = meldePeriode.fom,
            mottattTidspunkt = meldekort.mottattTidspunkt,
            timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map {
                ArbeidIPeriodeDTO(
                    it.periode.fom,
                    it.periode.tom,
                    it.timerArbeid.antallTimer
                )
            },
            meldepliktStatusKode = meldepliktStatus?.name,
            rettighetsTypeKode = rettighetsType?.name,
            avslagsårsakKode = avslagsårsak?.name
        )
    }

    private fun finnUnderveisperiode(
        meldekort: Meldekort, underveisPerioder: List<Underveisperiode>
    ): Underveisperiode {
        val arbeidsperiode = arbeidsperiodeFraMeldekort(meldekort)
        return underveisPerioder.first {
            // alle arbeidsperiodene i meldekortet må være innenfor en og samme underveisperiode
            it.meldePeriode.inneholder(arbeidsperiode)
        }
    }

    /**
    @return Tidsperioden meldekortet inneholder arbeidstimer for.
    Merk at det kan være dager i perioden meldekortet gjelder for som det ikke er rapportert timer på.
    @param meldekort -
     **/
    private fun arbeidsperiodeFraMeldekort(meldekort: Meldekort): Periode {
        val arbeidPerioder = meldekort.timerArbeidPerPeriode.map { it.periode }
        val arbeidsPerioderStart = arbeidPerioder.minBy { it.fom }.fom
        val arbeidsPerioderSlutt = arbeidPerioder.maxBy { it.tom }.tom

        return Periode(arbeidsPerioderStart, arbeidsPerioderSlutt)
    }


    companion object : ProvidersJobbSpesifikasjon {
        override val navn = "Meldekort til API-intern"
        override val type = "kelvin.meldekort.til.apiintern"
        override val beskrivelse = """
                Push informasjon til API-intern slik at NKS kan hente den.
                """.trimIndent()

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            val sakRepository = repositoryProvider.provide<SakRepository>()

            val service = BehandlingOgMeldekortServiceImpl(
                behandlingRepository = repositoryProvider.provide<BehandlingRepository>(),
                meldekortRepository = repositoryProvider.provide<MeldekortRepository>(),
                mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>(),
                personOgSakService = PersonOgSakService(
                    gatewayProvider.provide(IdentGateway::class),
                    repositoryProvider.provide<PersonRepository>(),
                    sakRepository,
                )

            )
            return MeldekortTilApiInternJobbUtfører(
                behandlingOgMeldekortService = service,
                apiInternGateway = gatewayProvider.provide(ApiInternGateway::class),
                saksRepository = sakRepository,
                underveisRepository = repositoryProvider.provide<UnderveisRepository>(),
            )
        }

        fun nyJobb(
            sakId: SakId,
            behandlingId: BehandlingId,
        ) = JobbInput(MeldekortTilApiInternJobbUtfører).apply {
            forBehandling(sakId.toLong(), behandlingId.toLong())
        }
    }


}