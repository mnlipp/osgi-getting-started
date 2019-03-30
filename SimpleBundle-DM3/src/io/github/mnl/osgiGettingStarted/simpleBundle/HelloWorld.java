/**
 * 
 */
package io.github.mnl.osgiGettingStarted.simpleBundle;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

/**
 * @author mnl
 *
 */
@Component(service={})
public class HelloWorld implements Runnable {

	private static LogService logService;

	private Thread runner;
	
	@Reference
	private void setLogService(LogService logService) {
		this.logService = logService;
	}

	private void unsetLogService(LogService logService) {
		this.logService = null;
	}

	@Activate
	public void start(ComponentContext ctx) {
		runner = new Thread(this);
		runner.start();
	}
	
	@Deactivate
	public void stop(ComponentContext ctx) {
		runner.interrupt();
		try {
			runner.join();
		} catch (InterruptedException e) {
			logService.log(LogService.LOG_WARNING, 
					"Could not terminate thread properly", e);
		}
	}

	@Override
    public void run() {
        System.out.println("Hello World!");
        while (!runner.isInterrupted()) {
            try {
                logService.log(LogService.LOG_WARNING, "Hello Word sleeping");
                Thread.sleep (5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
