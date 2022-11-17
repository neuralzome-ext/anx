package com.flomobility.anx.hermes.other

/**
 * The Event class wrapped around some content,
 * so that any event associated with this content fires only once
 *
 * Example -> Showing a message in Snackbar. LiveData observers are re instantiated
 * when a Fragment or Activity is re instantiated since the observers are bound to their lifecycles.
 * Hence if there is an error message posted through liveData a Snackbar will be shown every time the Observer is fired
 * Hence getContentIfNotHandled is used
 * */
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set //we can change the value from within this class and not outside

    /**
     * hasBeenHandled is private set, can't be changed from outside the scope of this class
     * This method checks for that, and toggles
     * @return returns the content exactly once
     * */
    fun getContentIfNotHandled() = if (hasBeenHandled) {
        null
    } else {
        hasBeenHandled = true
        content
    }

    fun peekContent() = content

}