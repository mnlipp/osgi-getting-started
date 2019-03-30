/**
 * 
 */
package io.github.mnl.osgiGettingStarted.loggingBundle;

import org.osgi.service.log.LogService;

/**
 * @author mnl
 *
 */
public class HelloWorld extends Thread {

    @Override
    public void run() {
        System.out.println("Hello World!");
        while (!isInterrupted()) {
            try {
                Activator.logService.log(LogService.LOG_INFO, "Hello Word sleeping");
                sleep (5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
}
