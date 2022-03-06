
object Cache {
    var defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend inline fun <ResultType, RequestType> cacheNetwork(
        maxLife: MaxLife = ONE_HOUR_CACHE_MAX_LIFE,
        crossinline localQuery : suspend () -> Flow<ResultType>,
        crossinline networkFetch : suspend () -> RequestType,
        crossinline lastQueryTime : suspend (ResultType) -> Long,
        crossinline saveNetworkResult : suspend (RequestType) -> ResultType?,
    ) = flow {

        val localData = withContext(defaultScope.coroutineContext){
            try {
                localQuery().first()
            }catch (e: Exception){
                null
            }
        }

        var networkData : ResultType? = null
        var flow : ResultType? = null

        val networkJob = defaultScope.launch {
            networkData = try {
                val network = networkFetch()
                if(network is Resource<*>){
                    if(network.status == Resource.Status.SUCCESS){
                        saveNetworkResult(network)
                    }else {
                        localData
                    }
                } else {
                    localData
                }
            }catch (e: Exception){
                null

            }
        }
        flow = if(localData==null){
            networkJob.join()
            networkData
        }else {
            val lastFetchedTime = lastQueryTime(localData)
            if(abs(lastFetchedTime-System.currentTimeMillis()) > maxLife.toMills()){
                //cache has expired
                networkJob.join()
                networkData ?: localData
            }else {
                //cache not expired
                localData
            }
        }
        emit(flow)
    }
}


//examples
val ONE_HOUR_CACHE_MAX_LIFE = MaxLife(1, TimeUnit.HOURS)

class MaxLife(
    /**
     * example -
     * MaxLife(2, TimeUnit.HOURS)
     * It means 2 hours
     */
    var score : Long,
    var unit: TimeUnit
){
    fun toMills() = unit.toMillis(score)
}
