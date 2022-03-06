

/**
* Why SupervisorJob ?
* Why Coroutines?
* You and me, already know, how kotlin coroutines efficient way to handle back or main threads.
* Basically, Rather than using Job(), SupervisorJob() better handles the Throwables or errors.
* and don't cancel automatically when there any error occurs in network side or db queries.
* 
* Why inline or crossline? 
* Ok, That's two keywords from kotlin. They Inline simply prevents the function object creation everytime we call.
* and crossline used with lambadas, They prevents the return value from inner called lambadas. Here we are using flow, So
* we don't want that anyone will return anything without our permission.
*
* Why suspend? 
* Offcourse, we use it with kotlin coroutines that suspend the ui.
* 
* Why not genric type? As we are using smart cast?
* Ok, Generic type and smart cast jobs are same. But handling the casting is better done by smart cast.
* 
* 
* SIDE EFFECTS -> 
* upcoming...
* 
*/

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
