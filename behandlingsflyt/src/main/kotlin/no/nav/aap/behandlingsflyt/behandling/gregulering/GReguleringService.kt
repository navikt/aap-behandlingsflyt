package no.nav.aap.behandlingsflyt.behandling.gregulering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Year

class GReguleringService(
    private val underveisRepository: UnderveisRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        underveisRepository = repositoryProvider.provide(),
        tilkjentYtelseRepository = repositoryProvider.provide(),
    )

    fun hentSakerForGRegulering(datoForGJustering: LocalDate): Set<SakId> {
        val nyttGrunnbeløp = Grunnbeløp.finnGrunnbeløp(datoForGJustering)
        return underveisRepository.hentSakerForGRegulering(datoForGJustering, nyttGrunnbeløp)
    }

    fun finnesGrunnbeløpForÅr(år: Year): Grunnbeløp.GrunnbeløpMedDato? {
        return Grunnbeløp.finnesGrunnbeløpForÅr(år)
    }

    fun erGrunnbeløpEndretForBehandling(behandlingId: BehandlingId): Boolean {
        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandlingId)
            ?: return false

        val tilkjentYtelsePerioder = tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)
            ?: return false

        val oppfyltUnderveisTidslinje = underveisGrunnlag.somTidslinje().filter { it.verdi.utfall == Utfall.OPPFYLT }
        val tilkjentYtelseTidslinje = tilkjentYtelsePerioder.tilTidslinje()
        val grunnbeløpTidslinje = Grunnbeløp.tilTidslinje()

        /*
         * Eksempel: 2-ukers meldeperiode (24.apr – 7.mai) som krysser G-grensen 1.mai
         *
         *                       24.apr              1.mai             7.mai
         *                       |                   |                 |
         * Tilkjent ytelse:      |-------------- G=124 028 ------------|
         * Oppfylt underveis:    |--------------- OPPFYLT -------------|
         * Gjeldende grunnbeløp: |----- 124 028 -----|----- 130 160 ---|
         *                       |                   |                 |
         * 1) join(underveis):   |-------------- G=124 028 ------------|
         * 2) join(grunnbeløp):  |------- false -----|----- true ------|
         * 3) filter { true }:                       |----- true ------|
         *
         * → true (grunnbeløpet har endret seg etter 1. mai)
         */
        val endredeGrunnbeløp = tilkjentYtelseTidslinje
            .innerJoin(oppfyltUnderveisTidslinje, { ty, _ -> ty })
            .innerJoin(grunnbeløpTidslinje, { ty, grunnbeløp ->
                ty.grunnbeløp != grunnbeløp
            })
            .filter { it.verdi }

        if (endredeGrunnbeløp.isNotEmpty()) {
            logger.info("Perioder med endrede grunnbeløp: " + endredeGrunnbeløp.perioder())
            return true
        }
        return false
    }
        /*
         * Er grunnbeløpet endret i perioden som tidslinjen dekker
         */
    fun erGrunnbeløpEndretForRettighetsTypeTidslinje(behandlingId: BehandlingId): Boolean {
        val rettighetsTypeTidslinje = underveisRepository.hentHvisEksisterer(behandlingId)
                ?.somTidslinje().orEmpty()
                .mapNotNull { it.rettighetsType }
                .komprimer()
        val grunnbeløpTidslinje = Grunnbeløp.tilTidslinje()

        val rettighetstypeKombinertMedGrunnbeløp = rettighetsTypeTidslinje
            .innerJoin(grunnbeløpTidslinje) { _, beløp -> beløp }
            .komprimer()

        return rettighetstypeKombinertMedGrunnbeløp.perioder().toSet().size > 1
    }
}