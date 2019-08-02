package ru.balmaster.linux.process.observer;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.io.IOException;

import static ru.balmaster.linux.process.observer.Main.PERSISTENT_COUNTER;

public class ResetJob implements Job {
    public ResetJob() {
    }

    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        PersistentFileCounter persistentCounter = (PersistentFileCounter) dataMap.get(PERSISTENT_COUNTER);
        try {
            persistentCounter.reset();
            System.out.println(persistentCounter.get());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
