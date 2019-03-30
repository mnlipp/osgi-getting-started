/**
 * 
 */
package io.github.mnl.osgiGettingStarted.simpleBundle;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.osgi.service.log.LogService;

/**
 * @author mnl
 *
 */
@Component(provides={})
public class HelloWorld implements Runnable {

	@ServiceDependency(required=true)
	private volatile LogService logService;

	private Thread runner;
	
	@Start
	public void start() {
		runner = new Thread(this);
		runner.start();
	}
	
	@Stop
	public void stop() {
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
