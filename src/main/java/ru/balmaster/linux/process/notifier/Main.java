package ru.balmaster.linux.process.notifier;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.concurrent.CountDownLatch;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

public class Main {
    public static final String PROCESS_NAME = "process-name";
    public static final String CHECK_INTERVAL = "check-interval";
    public static final String PUSH_GATEWAY = "push-gateway";


    public static void main(String ... args) {
        Options options = new Options();
        options.addOption(Option.builder().longOpt(PROCESS_NAME).hasArg().required().build());
        options.addOption(Option.builder().longOpt(CHECK_INTERVAL).hasArg().build());
        options.addOption(Option.builder().longOpt(PUSH_GATEWAY).hasArg().build());

        DefaultParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if(cmd.hasOption(PROCESS_NAME)) {
                String processName = cmd.getOptionValue(PROCESS_NAME);
                int checkInterval = cmd.hasOption(CHECK_INTERVAL) ?
                        Integer.parseInt(cmd.getOptionValue(CHECK_INTERVAL)) : 5;

                String prometheusGateway = cmd.hasOption(PUSH_GATEWAY) ?
                        cmd.getOptionValue(PUSH_GATEWAY) : "localhost:9091";


                // run scheduler
                Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
                try {
                    JobDetail job = JobBuilder.newJob(CheckProcessJob.class)
                            .withIdentity("job")
                            .usingJobData(PROCESS_NAME,processName)
                            .usingJobData(CHECK_INTERVAL,checkInterval)
                            .usingJobData(PUSH_GATEWAY,prometheusGateway)
                            .build();
                    SimpleTrigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity("job")
                            .startNow()
                            .withSchedule(simpleSchedule().withIntervalInSeconds(checkInterval).repeatForever())
                            .build();

                    scheduler.scheduleJob(job, trigger);

                    CountDownLatch latch = new CountDownLatch(1);

                    scheduler.start();

                    //
                    latch.await();

                } finally {
                    scheduler.shutdown(true);
                }
                // if process
            } else {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
