package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.utils.Diff
import no.nav.aap.behandlingsflyt.utils.tilTidslinje
import no.nav.aap.etterlevelse.sporing.SporbarRepo
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje

class BeregnTilkjentYtelseSporingRepo(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
): SporbarRepo<Tilkjent> {
    override fun lagre(
        behandlingId: BehandlingId,
        resultat: Tidslinje<Diff<Tilkjent>>
    ) {
        tilkjentYtelseRepository.lagre(
            behandlingId = behandlingId,
            tilkjent = resultat.tilTidslinje().segmenter().map { TilkjentYtelsePeriode(it.periode, it.verdi) },
        )
    }

    override fun hent(behandlingId: BehandlingId): Tidslinje<Tilkjent> {
        return tilkjentYtelseRepository.hentHvisEksisterer(behandlingId).orEmpty().somTidslinje({ it.periode }, { it.tilkjent })
    }
}