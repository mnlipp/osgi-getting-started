/**
 * 
 */
package io.github.mnl.osgiGettingStarted.simpleBundle;

import org.osgi.service.log.LogService;

/**
 * @author mnl
 *
 */
public class HelloWorld extends Thread {

	private volatile LogService logService;
	
    @Override
    public void run() {
        System.out.println("Hello World!");
        while (!isInterrupted()) {
            try {
                logService.log(LogService.LOG_WARNING, "Hello Word sleeping");
                sleep (5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
}
