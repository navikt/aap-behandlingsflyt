package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.utils.Uendret
import no.nav.aap.behandlingsflyt.utils.diffTidslinjer
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class RettighetstypeService(
    private val rettighetstypeRepository: RettighetstypeRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val underveisRepository: UnderveisRepository,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        rettighetstypeRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    fun harRettInnenforPeriode(behandlingId: BehandlingId, periode: Periode): Boolean {
        val rettighetstidslinje = rettighetstypeRepository.hentHvisEksisterer(behandlingId)?.rettighetstypeTidslinje
            ?: utledRettighetstidslinjeBakoverkompatibel(behandlingId)

        return rettighetstidslinje.begrensetTil(periode).isNotEmpty()
    }

    fun sisteDagMedRett(saksnummer: Saksnummer): LocalDate? {
        val sak = sakRepository.hentHvisFinnes(saksnummer)
            ?: throw UgyldigForespørselException("Sak med saksnummer $saksnummer finnes ikke")

        return behandlingRepository.finnGjeldendeVedtattBehandlingForSak(sak.id)?.let {
            rettighetstypeRepository.hentHvisEksisterer(it.behandlingId)?.rettighetstypeTidslinje
        }?.let {
            it.perioder().maxOfOrNull { periode -> periode.tom }
        }
    }

    /**
     * Henter rettighetstypetidslinje for en behandling, og utleder denne dersom den ikke er lagret. Dette vil typisk
     * kunne skje i gamle behandlinger som ikke har kjørt RettighetstypeSteg.
     */
    fun rettighetstypeTidslinjeBakoverkompatibel(behandlingId: BehandlingId): Tidslinje<RettighetsType> {
        return rettighetstypeRepository.hentHvisEksisterer(behandlingId)?.rettighetstypeTidslinje
            ?: utledRettighetstidslinjeBakoverkompatibel(behandlingId)
    }

    /**
     * Denne utleder rettighetstype og kvote basert på lagret vilkårsresultat.
     * Bør bruke nedlagret rettighetstype med kvote, men dette finnes ikke for eldre behandlinger.
     * Underveis er avgrenset i fremtiden, og kan derfor ikke brukes alene,
     * men de periodene vi har i underveis bør samsvare med utledet rettighetstidslinje.
     * Perioder som ikke samsvarer kaster en feil slik at disse sakene kan håndteres manuelt
     */
    private fun utledRettighetstidslinjeBakoverkompatibel(behandlingId: BehandlingId): Tidslinje<RettighetsType> {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val underveisperioder = underveisRepository.hentHvisEksisterer(behandlingId)?.somTidslinje()
        val utlededeRettighetstyper = vurderRettighetstypeOgKvoter(vilkårsresultat, KvoteService().beregn())
            .filter { it.verdi is KvoteOk }
            .mapNotNull { it.rettighetsType }

        if (underveisperioder == null || underveisperioder.isEmpty()) {
            return utlededeRettighetstyper
        }

        val differanse = diffTidslinjer(
            underveisperioder.mapNotNull { it.rettighetsType },
            utlededeRettighetstyper.begrensetTil(underveisperioder.helePerioden())
        ).filter { (_, it) -> it !is Uendret<*> }
        if (differanse.isNotEmpty()) {
            error("Vedtatte underveisperioder samsvarer ikke med utledede rettighetstyper behandling $behandlingId: $differanse")
        }

        return utlededeRettighetstyper
    }
}