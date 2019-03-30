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

    @Override
    public void start(BundleContext context) throws Exception {
        if (logServiceTracker == null) {
            logServiceTracker = new ServiceTracker<LogService, LogService>(
                    context, LogService.class, null) {
                @Override
                public LogService addingService(ServiceReference<LogService> reference) {
                    LogService result = super.addingService(reference);
                    if (isPreferred(reference)) {
                        logService = result;
                    }
                    if (helloWorld == null) {
                        logService.log(LogService.LOG_WARNING, "Hello World starting.");
                        helloWorld = new HelloWorld();
                        helloWorld.start();
                        logService.log(LogService.LOG_WARNING, "Hello World started.");
                    }
                    return result;
                }

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
                
                @Override
                public void removedService(ServiceReference<LogService> reference,
                                           LogService service) {
                    super.removedService(reference, service);
                    LogService nowCurrent = getService();
                    if (nowCurrent != null) {
                        logService = nowCurrent;
                        return;
                    }
                    if (helloWorld != null) {
                        logService.log(LogService.LOG_INFO, "Stopping Hello World.");
                        helloWorld.interrupt();
                        try {
                            helloWorld.join();
                        } catch (InterruptedException e) {
                        }
                        helloWorld = null;
                        logService.log(LogService.LOG_INFO, "Hello World stopped.");
                    }
                    logService = null;
                }
            };
        }
        logServiceTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logServiceTracker.close();
    }
}
