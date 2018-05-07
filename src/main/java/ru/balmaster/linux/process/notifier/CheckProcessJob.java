package ru.balmaster.linux.process.notifier;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.PushGateway;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CheckProcessJob implements Job {
    static final CollectorRegistry registry = new CollectorRegistry();
    static final Counter processRunning = Counter.build()
            .name("process_running")
            .help("process running counter")
            .register(registry);


    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String processName = dataMap.getString(Main.PROCESS_NAME);
        if (isProcessRunning(processName)) {
            System.out.println("process running");

            processRunning.inc(dataMap.getIntValue(Main.CHECK_INTERVAL));

            PushGateway pg = new PushGateway(dataMap.getString(Main.PUSH_GATEWAY));
            try {
                pg.pushAdd(registry, "job");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private boolean isProcessRunning(String processName) {
        try {
            Process p = new ProcessBuilder("ps", "-ef").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return reader.lines()
                        .filter(s -> s.contains(processName))
                        .count() > 1;
            }
        } catch (IOException e) {
            return false;
        }
    }

}
