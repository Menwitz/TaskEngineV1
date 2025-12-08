
package com.buzbuz.smartautoclicker.localservice

object LocalServiceProvider {

    /** List of callbacks to notify state changes. */
    private val callbacks = mutableListOf<(ILocalService?) -> Unit>()

    /** The instance of the [ILocalService], providing access for this service to the Activity. */
    var localServiceInstance: ILocalService? = null
        set(value) {
            field = value
            callbacks.forEach { it.invoke(field) }
        }

    fun setLocalService(service: ILocalService?) {
        localServiceInstance = service
    }

    /**
     * Register a callback to monitor the availability of the [ILocalService].
     * If the service is already available, the callback will be immediately called.
     */
    fun register(callback: (ILocalService?) -> Unit) {
        callbacks.add(callback)
        callback(localServiceInstance)
    }

    /** Unregister a callback. */
    fun unregister(callback: (ILocalService?) -> Unit) {
        callbacks.remove(callback)
    }

    fun isServiceStarted(): Boolean = localServiceInstance != null
}