package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Hent meldeperiode fra Arena for å synkronisere. Hvis brukeren går rett fra en
 * AAP-sak i Arena til en ny AAP-sak i Kelvin, ønsker vi at medlemmet fortsetter med
 * samme fastsatte dag. */
class ArenaMeldeperiodesyklusInformasjonskrav(
    private val sakRepository: SakRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val unleashGateway: UnleashGateway,
    private val apiInternGateway: ApiInternGateway,
) : Informasjonskrav<ArenaMeldeperiodesyklusInformasjonskrav.Input, ArenaMeldeperiodesyklusInformasjonskrav.Registerdata> {
    class Input(val ident: Ident) : InformasjonskravInput

    sealed interface Registerdata : InformasjonskravRegisterdata
    data class HarSyklus(
        val år: Int,
        /** Ukenummer i henhold til ISO-8601. */
        val ukenummer: Long,
    ) : Registerdata

    data object IngenSyklus : Registerdata

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return unleashGateway.isEnabled(BehandlingsflytFeature.SynkroniserArenaMeldeperiodesyklus)
                && kontekst.erFørstegangsbehandlingEllerRevurdering()
                && meldeperiodeRepository.hentFastsattDag(kontekst.behandlingId) == null
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder): Input {
        val sak = sakRepository.hent(kontekst.sakId)
        return Input(sak.person.aktivIdent())
    }

    override fun hentData(input: Input): Registerdata {
        val syklus = apiInternGateway.hentArenaMeldekortsyklus(input.ident)
            ?: return IngenSyklus
        return HarSyklus(
            år = syklus.år,
            ukenummer = syklus.ukenummer.toLong(),
        )
    }

    override fun oppdater(
        input: Input,
        registerdata: Registerdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        when (registerdata) {
            IngenSyklus -> {
                return Informasjonskrav.Endret.IKKE_ENDRET
            }
            is HarSyklus -> {
                val (år, isoUke) = registerdata
                val fastsattDag = LocalDate.parse("$år-W$isoUke-1", DateTimeFormatter.ISO_WEEK_DATE)
                meldeperiodeRepository.lagreFastsattDag(kontekst.behandlingId, fastsattDag)
                return Informasjonskrav.Endret.ENDRET
            }
        }
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.ARENA_MELDEKORTSYKLUS

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): Informasjonskrav<Input, Registerdata> {
            return ArenaMeldeperiodesyklusInformasjonskrav(
                sakRepository = repositoryProvider.provide(),
                meldeperiodeRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
                apiInternGateway = gatewayProvider.provide(),
            )
        }
    }
}