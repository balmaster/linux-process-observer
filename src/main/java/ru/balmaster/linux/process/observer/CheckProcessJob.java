package ru.balmaster.linux.process.observer;

import io.prometheus.client.Gauge;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import static ru.balmaster.linux.process.observer.Main.*;

public class CheckProcessJob implements Job {
    static final Gauge processRunning = Gauge.build().name("process_running").help("process running counter").register();

    public CheckProcessJob() {
    }

    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        List<String> processNames = (List<String>) dataMap.get(PROCESS_NAME);
        List<String> excludeProcessNames = (List<String>) dataMap.get(EXCLUDE_PROCESS_NAME);
        Integer maxTime = dataMap.getInt(MAX_TIME);
        List<String> ids = this.getProcessIds(processNames, excludeProcessNames);
        ids.remove(dataMap.getString(CURRENT_PROCESS_ID));
        if (ids != null && ids.size() > 0) {
            PersistentFileCounter persistentCounter = (PersistentFileCounter) dataMap.get(PERSISTENT_COUNTER);

            try {
                persistentCounter.inc(dataMap.getIntValue(CHECK_INTERVAL));
                processRunning.set((double) persistentCounter.get());
                System.out.println(persistentCounter.get());
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.checkNotifyTime(maxTime, persistentCounter);
            if (persistentCounter.get() > (long) maxTime) {
                this.killProcess(ids);
            }
        }

    }

    private void checkNotifyTime(Integer maxTime, PersistentFileCounter persistentCounter) {
        long remain = persistentCounter.get() - (long) maxTime;
        if (remain == 10L || remain == 5L || remain == 2L) {
            this.sendNotify("Осталось " + remain + " мин");
        }
    }

    private void killProcess(List<String> ids) {
        ids.forEach((id) -> {
            try {
                this.sendNotify("Время закончилось");
                System.out.println("try kill: " + id);
                new ProcessBuilder("kill", "-9", id).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendNotify(String message) {
        try {
            new ProcessBuilder("notify-send", "observer", message).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private List<String> getProcessIds(List<String> processNames, List<String> excludeProcessNames) {
        try {
            Process p = new ProcessBuilder("ps", "-ef").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return reader.lines()
                        .filter(line -> processNames.stream().anyMatch(it -> line.contains(it)))
                        .filter(line -> !excludeProcessNames.stream().anyMatch(it -> line.contains(it)))
                        .map(line -> line.split("\\s+")[1])
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            return null;
        }
    }
}
