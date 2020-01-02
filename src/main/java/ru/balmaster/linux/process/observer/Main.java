package ru.balmaster.linux.process.observer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static final String PROCESS_NAME = "process-name";
    public static final String EXCLUDE_PROCESS_NAME = "exclude-process-name";
    public static final String CHECK_INTERVAL = "check-interval";
    public static final String PERSISTENT_COUNTER = "persistent-counter";
    public static final String MAX_TIME = "max-time";
    public static final String CURRENT_PROCESS_ID = "currentProcessId";

    public Main() {
    }

    public static void main(String... args) {
        Options options = new Options();
        options.addOption(Option.builder().longOpt(PROCESS_NAME).hasArg().required().build());
        options.addOption(Option.builder().longOpt(EXCLUDE_PROCESS_NAME).hasArg().build());
        options.addOption(Option.builder().longOpt(CHECK_INTERVAL).hasArg().build());
        options.addOption(Option.builder().longOpt(MAX_TIME).hasArg().build());
        DefaultParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption(PROCESS_NAME)) {
                String processName = cmd.getOptionValue(PROCESS_NAME);
                String excludeProcessName = cmd.hasOption(EXCLUDE_PROCESS_NAME) ? cmd.getOptionValue(EXCLUDE_PROCESS_NAME) : "linux-process-observer";
                int checkInterval = cmd.hasOption(CHECK_INTERVAL) ? Integer.parseInt(cmd.getOptionValue(CHECK_INTERVAL)) : 60;
                int maxTime = cmd.hasOption(MAX_TIME) ? Integer.parseInt(cmd.getOptionValue(MAX_TIME)) : 3600;
                File userHome = new File(System.getProperty("user.home"));
                PersistentFileCounter persistentCounter = new PersistentFileCounter(new File(userHome, ".process-running"));
                Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

                try {
                    JobDataMap jobDataMap = new JobDataMap();
                    jobDataMap.put(PROCESS_NAME, Arrays.asList(processName.split(",")));
                    jobDataMap.put(EXCLUDE_PROCESS_NAME, Arrays.asList(excludeProcessName.split(",")));
                    jobDataMap.put(CHECK_INTERVAL, checkInterval);
                    jobDataMap.put(PERSISTENT_COUNTER, persistentCounter);
                    jobDataMap.put(MAX_TIME, maxTime);
                    jobDataMap.put(CURRENT_PROCESS_ID, getProcessId(""));
                    scheduleCheck(scheduler, jobDataMap, checkInterval);
                    CountDownLatch latch = new CountDownLatch(1);
                    scheduler.start();
                    latch.await();
                } finally {
                    scheduler.shutdown(true);
                    persistentCounter.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void scheduleCheck(Scheduler scheduler, JobDataMap jobDataMap, int checkInterval) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(CheckProcessJob.class)
                .withIdentity("check")
                .usingJobData(jobDataMap)
                .build();
        SimpleTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("check")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(checkInterval)
                        .repeatForever())
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    private static String getProcessId(String fallback) {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int index = jvmName.indexOf(64);
        if (index < 1) {
            return fallback;
        } else {
            try {
                return Long.toString(Long.parseLong(jvmName.substring(0, index)));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }
}
