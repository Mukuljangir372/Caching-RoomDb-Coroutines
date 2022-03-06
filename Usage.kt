  

/**
* 
* How to use ? How to cache network call?
* Example -> from DummyViewModel.kt
*/
suspend fun viewTicket2(ticketId: Int) = cacheNetwork(
       localQuery = {
           ticketDao.getViewTicketData(ticketId) //return type flow
       },
       networkFetch = {
           repository.viewTickets(ticketId) //return type Resource<*>
       },
       lastQueryTime = {
           it.lastFetchedTime //return type long 
       },
       saveNetworkResult = {
           it.data!!.data.lastFetchedTime = System.currentTimeMillis()
           ticketDao.insertViewTicketData(it.data.data)
           it.data.data //return the actual data you collected here
       }
   )


/**
* Colleting cache data 
* Example -> from DummyClass.kt
*/

class Usage{
  fun getUsers() {
     viewModel.viewTicket2(ticketId).collectLatest{
       //collect here
       //sometimes null collection when network calls and db both fails to get data
     }
  }
}
