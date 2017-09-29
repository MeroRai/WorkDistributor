package io.mikecroft.demo.WorkDistributor;

import fish.payara.micro.PayaraMicro;
import fish.payara.micro.PayaraMicroRuntime;
import fish.payara.micro.cdi.Outbound;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 *
 * @author Mike Croft
 */
@Singleton
@Startup
@Path("boss")
public class Boss {

    static Logger logger = Logger.getLogger(Boss.class.getCanonicalName());

    @Inject
    @Outbound(eventName = "serf")
    Event<WorkRequest> serf;

    @Inject
    @Outbound(eventName = "lackey")
    Event<WorkRequest> lackey;

    @Inject
    @Outbound(eventName = "dogsbody")
    Event<WorkRequest> dogsbody;

    final PayaraMicroRuntime payaraMicroRuntime = PayaraMicro.getInstance().getRuntime();

    @PostConstruct
    public void start() {
        setRequestTracing();
        setHealthCheck();
    }

    private void setRequestTracing() {
        payaraMicroRuntime.run("requesttracing-configure", "--enabled=true", "--thresholdValue=1", "--thresholdUnit=SECONDS", "--dynamic=true");
    }

    private void setHealthCheck() {

        payaraMicroRuntime.run("healthcheck-configure", "--enabled=true", "--dynamic=true");

        payaraMicroRuntime.run("healthcheck-configure-service", "--serviceName=healthcheck-cpu", "--enabled=true",
                "--time=60", "--unit=SECONDS", "--dynamic=true");
        payaraMicroRuntime.run("healthcheck-configure-service-threshold", "--serviceName=healthcheck-cpu",
                "--thresholdCritical=90", "--thresholdWarning=50", "--thresholdGood=0", "--dynamic=true");

        payaraMicroRuntime.run("healthcheck-configure-service", "--serviceName=healthcheck-machinemem",
                "--enabled=true", "--dynamic=true", "--time=60", "--unit=SECONDS");
        payaraMicroRuntime.run("healthcheck-configure-service-threshold", "--serviceName=healthcheck-machinemem",
                "--thresholdCritical=90", "--thresholdWarning=50", "--thresholdGood=0", "--dynamic=true");
    }

    @GET
    @Path("/{param}")
    public Response getMsg(@PathParam("param") String parameter) {
        switch (parameter.toLowerCase()) {
            case "serf":
            case "lackey":
            case "dogsbody":
                sendWork(parameter);
                break;
            case "loop":
                infiniteLoop();
                break;
            case "exitboss":
                exitBoss();
                break;
            case "exitserf":
            case "exitlackey":
            case "exitdogsbody":
                exitWorker(parameter);
                break;
            case "slowrequest":
                slowRequest();
                break;
            case "memory":
                consumeMemory();
                break;
            default:
                System.out.println("Invalid entry");
        }

        String ouput = "Entered parameter : " + parameter.toLowerCase();
        return Response.status(200).entity(ouput).build();
    }

    private void infiniteLoop() {

        for (int i = 0; i < Long.MAX_VALUE; i++) {
            System.out.println(i);
        }
    }

    private void slowRequest() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Boss.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void exitBoss() {
        System.exit(0);
    }

    private void consumeMemory() {
        Vector vector = new Vector();
        while (true) {
            // 1MB byte 
            byte bytes[] = new byte[1048576];
            try {
                Thread.sleep(20);
            } catch (InterruptedException ex) {
                Logger.getLogger(Boss.class.getName()).log(Level.SEVERE, null, ex);
            }
            vector.add(bytes);
        }
    }

    private void exitWorker(String worker) {
        System.out.println("Exiting worker");

        if (worker.equalsIgnoreCase("exitserf")) {
            serf.fire(WorkRequest.EXIT);
            System.out.println("Exited serf");
        } else if (worker.equalsIgnoreCase("exitlackey")) {
            lackey.fire(WorkRequest.EXIT);
            System.out.println("Exitedlackey");
        } else if (worker.equalsIgnoreCase("exitdogsbody")) {
            dogsbody.fire(WorkRequest.EXIT);
            System.out.println("Exited dogsbody");
        } else {
            System.out.println("Invalid worker");
        }

    }

    //@Schedule(hour = "*", minute = "*", second = "*/10", info = "Work Generator", timezone = "UTC", persistent = false)
    public void sendWork(String worker) {
        System.out.println("Sending work");

        if (worker.equalsIgnoreCase("serf")) {
            serf.fire(WorkRequest.TARTAN_PAINT);
            System.out.println("Sent work to serf");
        } else if (worker.equalsIgnoreCase("lackey")) {
            lackey.fire(WorkRequest.LONG_STAND);
            System.out.println("Sent work to lackey");
        } else if (worker.equalsIgnoreCase("dogsbody")) {
            dogsbody.fire(WorkRequest.BUSYWORK);
            System.out.println("Sent work to dogsbody");
        } else {
            System.out.println("Invalid worker");
        }

    }

}
