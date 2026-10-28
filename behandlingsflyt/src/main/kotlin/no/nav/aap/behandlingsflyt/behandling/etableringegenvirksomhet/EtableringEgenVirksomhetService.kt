package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet


import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.TilOgMedBeregning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.antallHverdagerIFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.beregnTomForSistePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.perioderForFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.somLegacyVisning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.somFasetidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.bistandsbehov.BistandRepository
import no.nav.aap.bistandsbehov.Bistandsvurdering
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate


class EtableringEgenVirksomhetService(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val behandlingRepository: BehandlingRepository,
    private val bistandRepository: BistandRepository,
    private val sykdomRepository: SykdomRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide()
    )

    fun validerFaseOgPeriode(
        vurdering: EtableringEgenVirksomhetVurdering,
    ): VirksomhetEtableringResultat.Ugyldig? {
        val fase = vurdering.fase ?: return null

        return validerOppstartRegistrering(fase, vurdering)
            ?: validerGyldigPeriode(vurdering)
    }

    private fun validerOppstartRegistrering(
        fase: EtableringFase,
        vurdering: EtableringEgenVirksomhetVurdering
    ): VirksomhetEtableringResultat.Ugyldig.MåOppgiRegistrertIOffentligeRegister? {
        if (fase != EtableringFase.OPPSTART || vurdering.versjon == EtableringVersjon.V0) return null
        return if (vurdering.erRegistrertINødvendigeOffentligeRegister != true) {
            VirksomhetEtableringResultat.Ugyldig.MåOppgiRegistrertIOffentligeRegister
        } else null
    }

    private fun validerGyldigPeriode(vurdering: EtableringEgenVirksomhetVurdering): VirksomhetEtableringResultat.Ugyldig.MåHaTilOgMedDato? =
        if (vurdering.tom == null) VirksomhetEtableringResultat.Ugyldig.MåHaTilOgMedDato else null

    fun beregnOgValider(
        behandlingId: BehandlingId,
        nyeVurderinger: List<EtableringEgenVirksomhetVurdering>
    ): VirksomhetEtableringResultat {
        val beregning = when (val resultat = beregnVurderinger(behandlingId, nyeVurderinger)) {
            is VirksomhetEtableringResultat.Ugyldig -> return resultat
            is VirksomhetEtableringResultat.Gyldig -> resultat.beregning
        }

        val feilmelding = validerVurdering(behandlingId, beregning.beregnedeVurderinger)
        return feilmelding ?: VirksomhetEtableringResultat.Gyldig(beregning)
    }

    data class Beregning(
        val beregnedeVurderinger: List<EtableringEgenVirksomhetVurdering>,
    )

    private fun beregnVurderinger(
        behandlingId: BehandlingId,
        nyeVurderinger: List<EtableringEgenVirksomhetVurdering>
    ): VirksomhetEtableringResultat {
        val behandling = behandlingRepository.hent(behandlingId)

        val gamleVurderinger =
            behandling.forrigeBehandlingId?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
                .map { it.somLegacyVisning() }
        val gjeldendeVurderinger = (gamleVurderinger + nyeVurderinger)
            .gjeldendeVurderinger()
            .verdier().toSet()

        val gamleVurderingerUtenOverstyrte = gamleVurderinger.filterNot { gammel ->
            nyeVurderinger.any { ny -> ny.fase == gammel.fase && ny.fom == gammel.fom }
        }

        val beregnedeVurderinger = (gamleVurderingerUtenOverstyrte + nyeVurderinger).toMutableList()
        val vurderingerSomSkalBeregnes = beregnedeVurderinger.indices
            .filter { index ->
                val vurdering = beregnedeVurderinger[index]
                vurdering.vurdertIBehandling == behandlingId && vurdering.fase != null
            }
            .sortedBy { beregnedeVurderinger[it].fom }

        for (index in vurderingerSomSkalBeregnes) {
            val vurdering = beregnedeVurderinger[index]
            val tom = when (val resultat = beregnTomForSistePeriode(
                vurderinger = beregnedeVurderinger,
                sisteVurdering = vurdering
            )) {
                is TilOgMedBeregning.Beregnet -> resultat.tom
                is TilOgMedBeregning.Feil -> return resultat.feilObjekt
            }
            beregnedeVurderinger[index] = vurdering.copy(tom = tom)
        }

        val førsteOppstartsfom = beregnedeVurderinger.gjeldendeVurderinger().segmenter().filter { it.verdi.fase == EtableringFase.OPPSTART }.minOfOrNull { it.periode.fom }

        if (førsteOppstartsfom != null) {
            for (index in beregnedeVurderinger.indices) {
                val vurdering = beregnedeVurderinger[index]
                if (
                    vurdering.fase == EtableringFase.UTVIKLING &&
                    vurdering.fom.isBefore(førsteOppstartsfom) &&
                    vurdering.tom != null
                ) {
                    beregnedeVurderinger[index] = vurdering.copy(
                        tom = minOf(vurdering.tom, førsteOppstartsfom.minusDays(1))
                    )
                }
            }
        }

        return VirksomhetEtableringResultat.Gyldig(
            Beregning(
                gamleVurderinger + beregnedeVurderinger.filter { it.vurdertIBehandling == behandlingId }
            )
        )
    }

    fun validerVurdering(
        behandlingId: BehandlingId,
        vurderinger: List<EtableringEgenVirksomhetVurdering>
    ): VirksomhetEtableringResultat.Ugyldig? {
        val perioderMedSykdomOgBistand = utledPerioderMedSykdomOgBistand(behandlingId)
        val gjeldendeVurderinger = vurderinger.somFasetidslinje()

        val alleUtviklingsPerioder = gjeldendeVurderinger.perioderForFase(EtableringFase.UTVIKLING)
        val alleOppstartsPerioder = gjeldendeVurderinger.perioderForFase(EtableringFase.OPPSTART)

        validerGyldighetsperiodeFinnes(perioderMedSykdomOgBistand)?.let { return it }
        val førsteMuligeDato = perioderMedSykdomOgBistand.first().fom

        return validerEtterFørsteMuligeDato(førsteMuligeDato, vurderinger)
            ?: validerInnenforGyldighetsperiode(perioderMedSykdomOgBistand, vurderinger)
            ?: vurderinger.firstNotNullOfOrNull { validerFaseOgPeriode(it) }
            ?: validerOppstartEtterUtvikling(alleUtviklingsPerioder, alleOppstartsPerioder)
            ?: validerDagkvoter(gjeldendeVurderinger)
    }

    private fun validerGyldighetsperiodeFinnes(gyldighetPeriode: List<Periode>): VirksomhetEtableringResultat.Ugyldig.SykdomOgBistandMåVæreOppfylt? {
        if (gyldighetPeriode.isEmpty()) {
            return VirksomhetEtableringResultat.Ugyldig.SykdomOgBistandMåVæreOppfylt
        }
        return null
    }

    private fun validerInnenforGyldighetsperiode(
        gyldighetPeriode: List<Periode>,
        vurderinger: List<EtableringEgenVirksomhetVurdering>
    ): VirksomhetEtableringResultat.Ugyldig.SykdomOgBistandMåVæreOppfyltOverlapp? {
        if (!vurderinger.all { vurdering -> gyldighetPeriode.any { it.inneholder(vurdering.fom) } }) {
            return VirksomhetEtableringResultat.Ugyldig.SykdomOgBistandMåVæreOppfyltOverlapp
        }
        return null
    }

    private fun validerEtterFørsteMuligeDato(
        førsteMuligeDato: LocalDate,
        vurderinger: List<EtableringEgenVirksomhetVurdering>
    ): VirksomhetEtableringResultat.Ugyldig.FørFørsteMuligeStartdato? {
        if (vurderinger.any { it.fom.isBefore(førsteMuligeDato) }) {
            return VirksomhetEtableringResultat.Ugyldig.FørFørsteMuligeStartdato
        }
        return null
    }

    private fun validerOppstartEtterUtvikling(
        alleUtviklingsPerioder: List<Periode>,
        alleOppstartsPerioder: List<Periode>
    ): VirksomhetEtableringResultat.Ugyldig.OppstartFørUtvikling? {
        val sisteUtviklingsPeriodeTom = alleUtviklingsPerioder.maxOfOrNull { it.tom } ?: return null
        if (alleOppstartsPerioder.any { !it.fom.isAfter(sisteUtviklingsPeriodeTom) }) {
            return VirksomhetEtableringResultat.Ugyldig.OppstartFørUtvikling
        }
        return null
    }

    private fun validerDagkvoter(
        alleVurderinger: Tidslinje<EtableringEgenVirksomhetVurdering>
    ): VirksomhetEtableringResultat.Ugyldig.OverstigerKvoter? {
        val bruktUtviklingsDager = alleVurderinger.antallHverdagerIFase(EtableringFase.UTVIKLING)
        val bruktOppstartsdager = alleVurderinger.antallHverdagerIFase(EtableringFase.OPPSTART)

        if (bruktUtviklingsDager > EtableringFase.UTVIKLING.maksHverdager) {
            return VirksomhetEtableringResultat.Ugyldig.OverstigerKvoter(EtableringFase.UTVIKLING, bruktUtviklingsDager)
        }
        if (bruktOppstartsdager > EtableringFase.OPPSTART.maksHverdager) {
            return VirksomhetEtableringResultat.Ugyldig.OverstigerKvoter(EtableringFase.OPPSTART, bruktOppstartsdager)
        }
        return null
    }



    fun utledPerioderMedSykdomOgBistand(
        behandlingId: BehandlingId
    ): List<Periode> {
        val tidslinjeSykdomOgBistandOppfylt = tidslinjeSykdomOgBistandOppfylt(behandlingId)

        if (tidslinjeSykdomOgBistandOppfylt.isEmpty()) return emptyList()

        val førsteDagIOppfyltPeriode = tidslinjeSykdomOgBistandOppfylt.helePerioden().fom

        return tidslinjeSykdomOgBistandOppfylt
            .begrensetTil(Periode(førsteDagIOppfyltPeriode.plusDays(1), Tid.MAKS))
            .perioder().toList()
    }

    private fun tidslinjeSykdomOgBistandOppfylt(behandlingId: BehandlingId) = sykdomOgBistandTidslinje(behandlingId)
        .filter {
            it.verdi.first?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() == true && it.verdi.second?.erBehovForArbeidsrettetTiltak == true
        }

    fun utledIkkeVurderbarePerioder(behandlingId: BehandlingId): List<Periode> {
        val førsteDagIOppfyltPeriode =
            tidslinjeSykdomOgBistandOppfylt(behandlingId).perioder().toList().firstOrNull()?.fom ?: return emptyList()

        return sykdomOgBistandTidslinje(behandlingId)
            .filter {
                it.verdi.first?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() != true
                        || it.verdi.second?.erBehovForArbeidsrettetTiltak != true
            }.perioder().plus(Periode(førsteDagIOppfyltPeriode, førsteDagIOppfyltPeriode)).toList()
    }

    private fun sykdomOgBistandTidslinje(behandlingId: BehandlingId): Tidslinje<Pair<Sykdomsvurdering?, Bistandsvurdering?>> {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandlingId)

        return Tidslinje.zip2(
            sykdomGrunnlag?.somSykdomsvurderingstidslinje().orEmpty(),
            bistandGrunnlag?.somBistandsvurderingstidslinje().orEmpty(),
        )
    }
}

sealed interface VirksomhetEtableringResultat {
    data class Gyldig(val beregning: EtableringEgenVirksomhetService.Beregning) : VirksomhetEtableringResultat

    sealed interface Ugyldig : VirksomhetEtableringResultat {
        val melding: String

        data class OverstigerKvoter(val fase: EtableringFase, val brukteDager: Int) : Ugyldig {
            private val dagerStr = when (fase) {
                EtableringFase.UTVIKLING -> "utviklingsdager"
                EtableringFase.OPPSTART -> "oppstartsdager"
            }
            override val melding =
                "Oppsatte $dagerStr overstiger gjenværende dager: $brukteDager / ${fase.maksHverdager}"
        }

        data object OppstartFørUtvikling : Ugyldig {
            override val melding: String = "Oppstartsperiode kan ikke ligge før en utviklingsperiode."
        }

        data object FørFørsteMuligeStartdato : Ugyldig {
            override val melding: String = "Vurderingen kan tidligst gjelde fra dagen etter første mulige dag med AAP."
        }

        data object SykdomOgBistandMåVæreOppfylt : Ugyldig {
            override val melding: String = "11-5 & 11-6b må være oppfylt i minst én periode."
        }

        data object SykdomOgBistandMåVæreOppfyltOverlapp : Ugyldig {
            override val melding: String = "Vurderte perioder må falle innen en periode med oppfylt 11-5 & 11-6b."
        }

        data object MåOppgiRegistrertIOffentligeRegister : Ugyldig {
            override val melding: String =
                "Må ha satt om virksomheten er registrert i nødvendige offentlige register for oppstartsfasen."
        }

        data object MåHaTilOgMedDato : Ugyldig {
            override val melding: String = "Må ha gyldig periode."
        }
    }
}
