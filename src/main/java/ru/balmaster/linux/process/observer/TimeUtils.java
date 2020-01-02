package ru.balmaster.linux.process.observer;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.time.Instant;

public class TimeUtils {
    public static Instant getNetTime() {
        try {
            NTPUDPClient timeClient = new NTPUDPClient();
            InetAddress inetAddress = InetAddress.getByName("pool.ntp.org");
            TimeInfo timeInfo = timeClient.getTime(inetAddress);
            return Instant.ofEpochMilli(timeInfo.getMessage().getTransmitTimeStamp().getTime());
        } catch (Exception e) {
            throw new RuntimeException("getTimeError", e);
        }
    }
}
