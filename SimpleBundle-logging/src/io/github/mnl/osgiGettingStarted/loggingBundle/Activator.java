package io.github.mnl.osgiGettingStarted.loggingBundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private ServiceTracker<LogService, LogService> logServiceTracker;
    static public LogService logService = null;
    private HelloWorld helloWorld;

    /** Our component isn't started immediately like before. Rather, a service tracker
     * is created and opened. Our component will only be started when the service
     * tracker detects the availability of all required services. */
    @Override
    public void start(BundleContext context) throws Exception {
        if (logServiceTracker == null) {
            // When called for the first time, create a new service tracker 
            // that tracks the availability of a log service.
            logServiceTracker = new ServiceTracker<LogService, LogService>(
                    context, LogService.class, null) {
                /** This method is invoked when a service (of the kind tracked)
                 * is added. In our use case, this means that the log service is 
                 * available now and that our component can be started. */
                @Override
                public LogService addingService(ServiceReference<LogService> reference) {
                    LogService result = super.addingService(reference);
                    // If there are several services available, getService() returns the
                    // preferred one. But we cannot use getService() here because the new
                    // service hasn't been added to the tracked services yet (we're in 
                    // the process of adding). We have to decide on our own.
                    if (isPreferred(reference)) {
                        logService = result;
                    }
                    // The required service has become available, so we should 
                    // start our service if it hasn't been started yet.
                    if (helloWorld == null) {
                        System.out.println("Hello World started.");
                        helloWorld = new HelloWorld();
                        helloWorld.start();
                    }
                    return result;
                }

                // When having several services, which one should be used? The ServiceTracker
                // prefers services with higher ranking and, if there is a tie, the service 
                // with the lowest service id.
                private boolean isPreferred(ServiceReference<LogService> candidate) {
                    ServiceReference<LogService> current = getServiceReference();
                    if (current == null) {
                        // If this is the first reference use it!
                        return true;
                    }
                    Object property = current.getProperty(Constants.SERVICE_RANKING);
                    int currentRanking = (property instanceof Integer) 
                            ? ((Integer) property).intValue() : 0;
                    property = candidate.getProperty(Constants.SERVICE_RANKING);
                    int candidateRanking = (property instanceof Integer) 
                            ? ((Integer) property).intValue() : 0;
                    if (candidateRanking > currentRanking) {
                        return true;
                    }
                    if (candidateRanking == currentRanking) {
                        long currentId = ((Long) 
                                (current.getProperty(Constants.SERVICE_ID))).longValue();
                        long candidateId = ((Long) 
                                (candidate.getProperty(Constants.SERVICE_ID))).longValue();
                        if (candidateId < currentId) {
                            return true;
                        }
                    }
                    return false;
                }
                
                /** This method is invoked when a service is removed. Since we model
                 * a strong relationship between our component and the log service,
                 * our component must be stopped when there's no log service left.
                 * Note that the service tracker remains open (active). When a log
                 * service becomes available again, our component will be restarted. */
                @Override
                public void removedService(ServiceReference<LogService> reference,
                                           LogService service) {
                    super.removedService(reference, service);
                    // After removing this service, another version of the service
                    // may have become the "current version". As this is called after
                    // the service has been removed from the tracker, we can simply
                    // invoke getService() to get the preferred remaining service object
                    // (if here is still one left).
                    LogService nowCurrent = getService();
                    if (nowCurrent != null) {
                        logService = nowCurrent;
                        return;
                    }
                    // If no logging service is left, we have to stop our component.
                    if (helloWorld != null) {
                        helloWorld.interrupt();
                        try {
                            helloWorld.join();
                        } catch (InterruptedException e) {
                        }
                        helloWorld = null;
                        System.out.println("Hello World stopped.");
                    }
                    // Release any left over reference to the log service.
                    logService = null;
                }
            };
        }
        // Now activate (open) the service tracker.
        logServiceTracker.open();
    }

    /** As before, this method stops our component, but in a different way. It stops 
     * (closes) the service tracker (because we don't want our component to be 
     * reactivated only because a log service (re-)appears). As this causes the
     * ServiceTracker to call removedService for the tracked service(s), this will
     * also stop our service. */
    @Override
    public void stop(BundleContext context) throws Exception {
        logServiceTracker.close();
    }
}
