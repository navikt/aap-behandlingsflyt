package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.avslagsårsakerVedTapAvRettPåAAP
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
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
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class UtledArenaVedtakstype(
    private val underveisRepository: UnderveisRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
) {

    data class ArenaVedtak(
        val referanse: BehandlingReferanse,
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
        O_AVSLAG(ArenaVedtakstype.O, girAAP = false),
        O_INNV_NAV(ArenaVedtakstype.O, girAAP = true),
        O_INNV_SOKNAD(ArenaVedtakstype.O, girAAP = true),
        E_FORLENGE(ArenaVedtakstype.E, girAAP = true),
        E_VERDI(ArenaVedtakstype.E, girAAP = true),
        G_AVSLAG(ArenaVedtakstype.G, girAAP = false),
        G_INNV_NAV(ArenaVedtakstype.G, girAAP = true),
        G_INNV_SOKNAD(ArenaVedtakstype.G, girAAP = true),
        S_DOD(ArenaVedtakstype.S, girAAP = false),
        S_OPPHOR(ArenaVedtakstype.S, girAAP = false),
        S_STANS(ArenaVedtakstype.S, girAAP = false),
        ;
    }

    fun utledVedtak(person: Person): Tidslinje<ArenaVedtak> {
        val behandlinger = behandlingRepository.hentAlleMedVedtakFor(
            person,
            listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
        )
            .sortedBy { it.vedtakstidspunkt }


        val saker = sakRepository.finnSakerFor(person)
        val søknader = saker.flatMapTo(HashSet()) { sak ->
            mottattDokumentRepository.hentDokumenterAvType(sak.id, InnsendingType.SØKNAD)
        }


        var eksisterendeVedtak = Tidslinje<ArenaVedtak>()
        var forrigeRettighetsTyper = Tidslinje<RettighetsType>()
        var forrigeStansOgOpphør = mapOf<LocalDate, Set<Avslagsårsak>>()

        for (behandling in behandlinger) {
            val behandlingensRettighetsTyper = underveisRepository.hent(behandling.id)
                .somTidslinje()
                .filter { (_, underveisperiode) -> underveisperiode.avslagsårsak != null }
                .mapNotNull { it.rettighetsType }

            val behandlingensStansOgOpphør =
                avslagsårsakerVedTapAvRettPåAAP(vilkårsresultatRepository.hent(behandling.id))
                    .segmenter()
                    .associate { (periode, avslagsårsaker) ->
                        require(periode.fom == periode.tom) { "Kan ikke ha to dager med stans/opphør etter hverandre" }
                        /* perioden er siste dag før stans, men det er mer hensiktsmessig for oss å se på første dag med stans. */
                        periode.fom.plusDays(1) to avslagsårsaker
                    }
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


    companion object {
        fun utledVedtak(
            eksisterendeVedtak: Tidslinje<ArenaVedtak>,
            behandling: BehandlingMedVedtak,
            søknader: Set<MottattDokument>,
            rettighetsTyper: Tidslinje<Diff<RettighetsType>>,
            stansOgOpphør: Map<LocalDate, Diff<Set<Avslagsårsak>>>,
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
                            referanse = behandling.referanse,
                            vedtaksvariant = ArenaVedtaksvariant.E_VERDI,
                        )

                        is Fjernet<*> -> {
                            null
                        }

                        is LagtTil<*> -> ArenaVedtak(
                            referanse = behandling.referanse,
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
            arenaVedtak = arenaVedtak.map { x ->
                if (skalBliGjeninntreden && x.referanse == behandling.referanse && x.vedtaksvariant == ArenaVedtaksvariant.E_VERDI) {
                    x.copy(vedtaksvariant = if (harSøkt) ArenaVedtaksvariant.G_INNV_SOKNAD else ArenaVedtaksvariant.G_INNV_NAV)
                } else {
                    skalBliGjeninntreden = !x.vedtaksvariant.girAAP
                    x
                }
            }


            val relevanteStansOgOpphør = diffMap(
                stansOgOpphør.fraMap().mapValues { (_, avslag) -> avslag - varighetavslag }
                    .filter { (_, avslag) -> avslag.isNotEmpty() },
                stansOgOpphør.tilMap().mapValues { (_, avslag) -> avslag - varighetavslag }
                    .filter { (_, avslag) -> avslag.isNotEmpty() },
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
                                    referanse = behandling.referanse,
                                    vedtaksvariant = when {
                                        stoppEndring.til.any { it.avslagstype == Avslagstype.OPPHØR } -> ArenaVedtaksvariant.S_OPPHOR
                                        stoppEndring.til.any { it.avslagstype == Avslagstype.STANS } -> ArenaVedtaksvariant.S_STANS
                                        else -> error("stopp identifisert som hverken er stans eller opphør")
                                    }
                                )
                            )
                        )
                    }

                    is LagtTil -> {
                        arenaVedtak = arenaVedtak.mergePrioriterHøyre(
                            tidslinjeOf(
                                Periode(stoppDato, stoppDato) to ArenaVedtak(
                                    referanse = behandling.referanse,
                                    vedtaksvariant = when {
                                        stoppEndring.lagtTil.any { it.avslagstype == Avslagstype.OPPHØR } -> ArenaVedtaksvariant.S_OPPHOR
                                        stoppEndring.lagtTil.any { it.avslagstype == Avslagstype.STANS } -> ArenaVedtaksvariant.S_STANS
                                        else -> error("stopp identifisert som hverken er stans eller opphør")
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
            /* Nyeste søknads knyttet til behandling, eventuelt siste søknad mottatt før behandlingen
                 * ble opprettet som fallback. */
            val søknadsdato =
                `søknader`.filter { it.behandlingId == behandling.id }.minOfOrNull { it.mottattTidspunkt.toLocalDate() }
                    ?: `søknader`.filter { it.mottattTidspunkt < behandling.opprettetTidspunkt }
                        .maxOf { it.mottattTidspunkt.toLocalDate() }

            return eksisterendeVedtak.mergePrioriterHøyre(
                tidslinjeOf(
                    Periode(søknadsdato, søknadsdato) to ArenaVedtak(
                        referanse = behandling.referanse,
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