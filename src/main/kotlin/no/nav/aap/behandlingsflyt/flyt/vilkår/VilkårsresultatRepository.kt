package no.nav.aap.behandlingsflyt.flyt.vilkår

object VilkårsresultatRepository {
    private var resultater = HashMap<Long, Vilkårsresultat>()

    private val LOCK = Object()

    fun lagre(behandlingId: Long, vilkårsresultat: Vilkårsresultat) {
        synchronized(LOCK) {
            resultater[behandlingId] = vilkårsresultat
        }
    }

    fun hent(behandlingId: Long): Vilkårsresultat {
        synchronized(LOCK) {
            if (!resultater.containsKey(behandlingId)) {
                resultater[behandlingId] = Vilkårsresultat()
            }
            return resultater.getValue(behandlingId)
        }
    }
}

