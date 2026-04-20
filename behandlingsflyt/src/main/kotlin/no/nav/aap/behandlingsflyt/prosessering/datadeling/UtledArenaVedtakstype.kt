package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.utils.Diff
import no.nav.aap.behandlingsflyt.utils.Endret
import no.nav.aap.behandlingsflyt.utils.Fjernet
import no.nav.aap.behandlingsflyt.utils.LagtTil
import no.nav.aap.behandlingsflyt.utils.Uendret
import no.nav.aap.behandlingsflyt.utils.diffMap
import no.nav.aap.behandlingsflyt.utils.diffTidslinjer
import no.nav.aap.behandlingsflyt.utils.fraMap
import no.nav.aap.behandlingsflyt.utils.fraTidslinje
import no.nav.aap.behandlingsflyt.utils.tilMap
import no.nav.aap.behandlingsflyt.utils.tilTidslinje
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class UtledArenaVedtakstype(
    private val underveisRepository: UnderveisRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val behandlingRepository: BehandlingRepository,
    private val stansOpphørRepository: StansOpphørRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        underveisRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        stansOpphørRepository = repositoryProvider.provide(),
    )

    data class ArenaVedtak(
        val vedtakId: VedtakId,
        val vedtaksvariant: ArenaVedtaksvariant,
    )

    enum class ArenaVedtakstype {
        O, /* Opprettet? */
        E, /* Endring? */
        G, /* Gjenintreden? */
        S, /* Stans? */
    }

    enum class ArenaVedtaksvariant(
        val vedtakstype: ArenaVedtakstype,
        val girAAP: Boolean,
    ) {
        O_INNV_NAV(ArenaVedtakstype.O, girAAP = true),
        O_INNV_SOKNAD(ArenaVedtakstype.O, girAAP = true),
        E_FORLENGE(ArenaVedtakstype.E, girAAP = true),
        E_VERDI(ArenaVedtakstype.E, girAAP = true),
        G_INNV_NAV(ArenaVedtakstype.G, girAAP = true),
        G_INNV_SOKNAD(ArenaVedtakstype.G, girAAP = true),

        O_AVSLAG(ArenaVedtakstype.O, girAAP = false),
        G_AVSLAG(ArenaVedtakstype.G, girAAP = false),
        S_DOD(ArenaVedtakstype.S, girAAP = false),
        S_OPPHOR(ArenaVedtakstype.S, girAAP = false),
        S_STANS(ArenaVedtakstype.S, girAAP = false),
        ;
    }

    fun utledVedtak(sak: Sak): Tidslinje<ArenaVedtak> {
        val behandlinger = behandlingerMedVedtak(sak).sortedBy { it.vedtakstidspunkt }
        val søknader = mottattDokumentRepository.hentDokumenterAvType(sak.id, InnsendingType.SØKNAD)

        var eksisterendeVedtak = Tidslinje<ArenaVedtak>()
        var forrigeRettighetsTyper = Tidslinje<RettighetsType>()
        var forrigeStansOgOpphør = mapOf<LocalDate, StansEllerOpphør>()

        for (behandling in behandlinger) {
            val behandlingensRettighetsTyper = underveisRepository.hent(behandling.id)
                .somTidslinje()
                .mapNotNull { it.rettighetsType }

            val behandlingensStansOgOpphør = stansOpphørRepository.hentHvisEksisterer(behandling.id)
                ?.gjeldendeStansOgOpphør().orEmpty()
                .associate { it.fom to it.vurdering }
            eksisterendeVedtak = utledVedtak(
                eksisterendeVedtak = eksisterendeVedtak,
                behandling = behandling,
                søknader = søknader.filter { it.mottattTidspunkt <= behandling.opprettetTidspunkt }.toSet(),
                rettighetsTyper = diffTidslinjer(forrigeRettighetsTyper, behandlingensRettighetsTyper),
                stansOgOpphør = diffMap(forrigeStansOgOpphør, behandlingensStansOgOpphør),
            )
            forrigeRettighetsTyper = behandlingensRettighetsTyper
            forrigeStansOgOpphør = behandlingensStansOgOpphør
        }

        return eksisterendeVedtak
    }

    private fun behandlingerMedVedtak(sak: Sak): List<BehandlingMedVedtak> =
        behandlingRepository.hentAlleMedVedtakFor(
            sak.person,
            listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
        )
            .filter { it.saksnummer == sak.saksnummer }

    companion object {
        fun utledVedtak(
            eksisterendeVedtak: Tidslinje<ArenaVedtak>,
            behandling: BehandlingMedVedtak,
            søknader: Set<MottattDokument>,
            rettighetsTyper: Tidslinje<Diff<RettighetsType>>,
            stansOgOpphør: Map<LocalDate, Diff<StansEllerOpphør>>,
        ): Tidslinje<ArenaVedtak> {
            val haddeIngenRett = rettighetsTyper.fraTidslinje().isEmpty()
            val fårRettPåAAP = rettighetsTyper.tilTidslinje().isNotEmpty()

            if (haddeIngenRett && !fårRettPåAAP) {
                return avslagsvedtak(søknader, behandling, eksisterendeVedtak)
            }

            val harSøkt = søknader.any { it.behandlingId == behandling.id }

            var arenaVedtak =
                Tidslinje.map2(eksisterendeVedtak, rettighetsTyper) { eksisterendeVedtak, rettighetsType ->
                    when (rettighetsType) {
                        is Endret<*> -> ArenaVedtak(
                            vedtakId = behandling.vedtakId,
                            vedtaksvariant = ArenaVedtaksvariant.E_VERDI,
                        )

                        is Fjernet<*> ->
                            null

                        is LagtTil<*> -> ArenaVedtak(
                            vedtakId = behandling.vedtakId,
                            vedtaksvariant = when {
                                haddeIngenRett && harSøkt -> ArenaVedtaksvariant.O_INNV_SOKNAD
                                haddeIngenRett -> ArenaVedtaksvariant.O_INNV_NAV
                                behandling.vurderingsbehov == setOf(Vurderingsbehov.UTVID_VEDTAKSLENGDE) -> ArenaVedtaksvariant.E_FORLENGE
                                else -> ArenaVedtaksvariant.E_VERDI
                            }
                        )

                        is Uendret<*>,
                        null -> eksisterendeVedtak
                    }
                }.filterNotNull()

            // finn gjenopptak
            var skalBliGjeninntreden = false
            arenaVedtak = arenaVedtak.map { vedtak ->
                if (skalBliGjeninntreden && vedtak.vedtakId == behandling.vedtakId && vedtak.vedtaksvariant == ArenaVedtaksvariant.E_VERDI) {
                    vedtak.copy(vedtaksvariant = if (harSøkt) ArenaVedtaksvariant.G_INNV_SOKNAD else ArenaVedtaksvariant.G_INNV_NAV)
                } else {
                    skalBliGjeninntreden = !vedtak.vedtaksvariant.girAAP
                    vedtak
                }
            }


            val relevanteStansOgOpphør = diffMap(
                stansOgOpphør.fraMap()
                    .filter { (_, avslag) -> (avslag.årsaker - varighetavslag).isNotEmpty() },
                stansOgOpphør.tilMap()
                    .filter { (_, avslag) -> (avslag.årsaker - varighetavslag).isNotEmpty() },
            )

            for ((stoppDato, stoppEndring) in relevanteStansOgOpphør) {
                when (stoppEndring) {
                    is Uendret -> {
                        /* noop */
                    }

                    is Fjernet -> {
                        if (arenaVedtak.segment(stoppDato)?.verdi?.vedtaksvariant?.vedtakstype == ArenaVedtakstype.S) {
                            arenaVedtak = arenaVedtak.fjern(Periode(stoppDato, stoppDato))
                        }
                    }

                    is Endret -> {
                        arenaVedtak = arenaVedtak.mergePrioriterHøyre(
                            tidslinjeOf(
                                Periode(stoppDato, stoppDato) to ArenaVedtak(
                                    vedtakId = behandling.vedtakId,
                                    vedtaksvariant = when (stoppEndring.til) {
                                        is Opphør -> ArenaVedtaksvariant.S_OPPHOR
                                        is Stans -> ArenaVedtaksvariant.S_STANS
                                    }
                                )
                            )
                        )
                    }

                    is LagtTil -> {
                        arenaVedtak = arenaVedtak.mergePrioriterHøyre(
                            tidslinjeOf(
                                Periode(stoppDato, stoppDato) to ArenaVedtak(
                                    vedtakId = behandling.vedtakId,
                                    vedtaksvariant = when (stoppEndring.lagtTil) {
                                        is Opphør -> ArenaVedtaksvariant.S_OPPHOR
                                        is Stans -> ArenaVedtaksvariant.S_STANS
                                    }
                                )
                            )
                        )
                    }
                }
            }

            return arenaVedtak
        }

        private fun avslagsvedtak(
            søknader: Set<MottattDokument>,
            behandling: BehandlingMedVedtak,
            eksisterendeVedtak: Tidslinje<ArenaVedtak>
        ): Tidslinje<ArenaVedtak> {
            /* Nyeste søknad knyttet til behandling, eventuelt siste søknad mottatt før behandlingen
                 * ble opprettet som fallback. */
            val søknadsdato = søknader.filter { it.behandlingId == behandling.id }
                .minOfOrNull { it.mottattTidspunkt.toLocalDate() }
                ?: søknader.filter { it.mottattTidspunkt < behandling.opprettetTidspunkt }
                    .maxOf { it.mottattTidspunkt.toLocalDate() }

            return eksisterendeVedtak.mergePrioriterHøyre(
                tidslinjeOf(
                    Periode(søknadsdato, søknadsdato) to ArenaVedtak(
                        vedtakId = behandling.vedtakId,
                        vedtaksvariant = ArenaVedtaksvariant.O_AVSLAG
                    )
                )
            )
        }

        /* I Arena, når en stønadsperiode er opphørt pga. at kvoten er brukt opp,
         * så er opphøret implisitt en del av det innvilgende/forlengende vedtaket.
         * Det er kun "uventede" stans/opphør som vises som egne vedtak.
         */
        private val varighetavslag = setOf(
            Avslagsårsak.VARIGHET_OVERSKREDET_ARBEIDSSØKER,
            Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT,
            Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE,
            Avslagsårsak.BRUKER_OVER_67,
            Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
            Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP,
        )
    }
}

/** [T] er ikke nullable (`Any`),  ellers ville koden under blandet sammen
 * `null`-verdier fra [this] og `null`-verdier som skal fjernes. */
private fun <T : Any> Tidslinje<T>.fjern(periode: Periode): Tidslinje<T> {
    return this.leftJoin(Tidslinje(periode, true)) { verdi, fjern ->
        if (fjern == true) {
            null
        } else {
            verdi
        }
    }
        .filterNotNull()
}