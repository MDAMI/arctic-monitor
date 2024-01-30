package com.mdami.client;

import org.junit.Test;

import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ArcticClientTest {
    @Test
    public void keysAreFiltered() {
        Properties properties = new Properties();
        properties.put("sad", "sad");
        properties.put("ads", "ads");
        properties.put("rah", "rah");
        Pattern pattern = Pattern.compile("[asd]*");

        ArcticClient.applyKeyFilter(properties, pattern);

        assertEquals(properties.keySet(), Set.of("sad", "ads"));
    }
}
